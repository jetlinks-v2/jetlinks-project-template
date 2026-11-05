import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { build } from 'esbuild'

const moduleRoot = new URL('../', import.meta.url)
const apiPath = fileURLToPath(new URL('../../../jetlinks-web-core/src/api/account/center.ts', import.meta.url))
const fixtures = {
  '@jetlinks-web-core/locales': 'export default { global: { t: key => key } }',
  '@jetlinks-web/core': 'export const request = { post: (...args) => globalThis.personalTokenFixture.post(...args) }',
  '@jetlinks-web/utils': `
    export const randomString = () => 'generated-source';
    export const onlyMessage = (...args) => globalThis.personalTokenFixture.messages.push(args);
  `,
}

const bundle = await build({
  stdin: {
    contents: `
      export { useTokenDialog } from './views/PersonCenter/components/PersonalToken/components/useTokenDialog.ts';
      export { savePersonalToken_api } from '@jetlinks-web-core/api/account/center';
      export { effectScope, reactive, nextTick } from 'vue';
    `,
    resolveDir: fileURLToPath(moduleRoot),
  },
  bundle: true,
  format: 'esm',
  platform: 'node',
  write: false,
  plugins: [{
    name: 'personal-token-api-boundary',
    setup(builder) {
      builder.onResolve({ filter: /.*/ }, args => {
        if (args.path === '@jetlinks-web-core/api/account/center') return { path: apiPath }
        if (Object.hasOwn(fixtures, args.path)) return { path: args.path, namespace: 'fixture' }
      })
      builder.onLoad({ filter: /.*/, namespace: 'fixture' }, args => ({ contents: fixtures[args.path] }))
    },
  }],
})

const { useTokenDialog, savePersonalToken_api, effectScope, reactive, nextTick } = await import(
  `data:text/javascript;base64,${Buffer.from(bundle.outputFiles[0].text).toString('base64')}`
)

const token = {
  id: 'existing-record',
  name: 'API access',
  description: 'original description',
  expires: -1,
  sourceType: 'account-center',
  sourceId: 'original-source',
  scope: { permissions: [{ id: 'device', actions: ['query'] }] },
}

function createDialog(mode = 'edit', post = async () => ({ success: true, result: { accessToken: 'fixture-new-token' } })) {
  const fixture = { calls: [], messages: [] }
  fixture.post = (...args) => {
    fixture.calls.push(args)
    return post(...args)
  }
  globalThis.personalTokenFixture = fixture
  const events = []
  const scope = effectScope()
  const props = reactive({ visible: true, mode, token: mode === 'add' ? null : structuredClone(token) })
  const dialog = scope.run(() => useTokenDialog(props, (...args) => events.push(args)))
  dialog.formRef.value = { validate: async () => {}, clearValidate: () => {} }
  if (mode === 'add') {
    dialog.formData.value.name = 'new token'
    dialog.formData.value.expires = -1
    dialog.formData.value.scope.permissions = [{ id: 'device', actions: ['query'] }]
  }
  return { dialog, props, events, fixture, dispose: () => scope.stop() }
}

test('刷新保存更新原记录，吊销旧凭证并等待用户复制新令牌后刷新列表', async () => {
  const { dialog, events, fixture, dispose } = createDialog()
  try {
    dialog.formData.value.scope.permissions = [{ id: 'device', actions: ['add'] }]
    await dialog.handleOk(true)
    const [url, data, options] = fixture.calls[0]
    assert.equal(url, '/personal/token/me/_save')
    assert.equal(data.id, token.id)
    assert.deepEqual(data.scope.permissions, [{ id: 'device', actions: ['add'] }])
    assert.deepEqual(options, { params: { revokeAccessTokens: true } })
    assert.equal(dialog.generatedToken.value, 'fixture-new-token')
    assert.equal(dialog.showSuccessModal.value, true)
    assert.equal(dialog.visible.value, false)
    assert.equal(dialog.loading.value, false)
    assert.deepEqual(events, [])
    dialog.handleSuccessClose()
    assert.equal(events[0][0], 'save')
    assert.equal(events[0][1].id, token.id)
  } finally {
    dispose()
  }
})

test('不刷新保存保留原记录，直接刷新列表并传递原令牌生效的反馈语义', async () => {
  const { dialog, events, fixture, dispose } = createDialog('edit', async () => ({ success: true }))
  try {
    await dialog.handleOk(false)
    assert.equal(fixture.calls.length, 1)
    assert.equal(fixture.calls[0][1].id, token.id)
    assert.equal(fixture.calls[0][1].sourceId, token.sourceId)
    assert.deepEqual(fixture.calls[0][2], { params: { revokeAccessTokens: false } })
    assert.equal(dialog.generatedToken.value, '')
    assert.equal(dialog.showSuccessModal.value, false)
    assert.equal(dialog.visible.value, false)
    assert.equal(dialog.loading.value, false)
    assert.equal(events[0][0], 'save')
    assert.equal(events[0][2], false)
  } finally {
    dispose()
  }
})

test('新增和共享 API 的既有调用继续使用后端默认值并展示新令牌', async () => {
  const { dialog, fixture, dispose } = createDialog('add')
  try {
    await dialog.handleOk()
    assert.equal(fixture.calls[0][1].id, undefined)
    assert.equal(fixture.calls[0][2], undefined)
    assert.equal(dialog.showSuccessModal.value, true)
    await savePersonalToken_api({ name: 'other caller' })
    assert.equal(fixture.calls[1][2], undefined)
  } finally {
    dispose()
  }
})

test('查看模式关闭弹窗，不提交保存请求', async () => {
  const { dialog, events, fixture, dispose } = createDialog('view')
  try {
    await dialog.handleOk(false)
    assert.deepEqual(fixture.calls, [])
    assert.deepEqual(events, [['close']])
  } finally {
    dispose()
  }
})

test('校验失败保留输入，释放加载状态并阻止保存', async () => {
  const { dialog, events, fixture, dispose } = createDialog()
  try {
    dialog.formRef.value.validate = async () => { throw { errorFields: [{ name: ['name'] }] } }
    await dialog.handleOk(false)
    assert.deepEqual(fixture.calls, [])
    assert.deepEqual(events, [])
    assert.deepEqual(fixture.messages, [])
    assert.equal(dialog.formData.value.id, token.id)
    assert.equal(dialog.visible.value, true)
    assert.equal(dialog.loading.value, false)
  } finally {
    dispose()
  }
})

test('接口拒绝或网络错误保留表单并提供失败反馈，可重试另一个保存方式', async () => {
  for (const failure of [async () => ({ success: false }), async () => { throw new Error('request failed') }]) {
    let attempt = 0
    const { dialog, events, fixture, dispose } = createDialog('edit', async () => (
      attempt++ === 0 ? failure() : { success: true, result: { accessToken: 'fixture-retry-token' } }
    ))
    try {
      await dialog.handleOk(false)
      assert.equal(dialog.formData.value.id, token.id)
      assert.equal(dialog.visible.value, true)
      assert.equal(dialog.loading.value, false)
      assert.equal(dialog.showSuccessModal.value, false)
      assert.deepEqual(events, [])
      assert.deepEqual(fixture.messages, [['PersonalToken.TokenDialog.saveFailed', 'error']])
      await dialog.handleOk(true)
      assert.equal(fixture.calls.length, 2)
      assert.deepEqual(fixture.calls[1][2], { params: { revokeAccessTokens: true } })
      assert.equal(dialog.generatedToken.value, 'fixture-retry-token')
    } finally {
      dispose()
    }
  }
})

test('校验和请求期间连续点击只执行首次保存，不能取消正在提交的表单', async () => {
  let finishValidation
  let finishRequest
  const { dialog, events, fixture, dispose } = createDialog('edit', () => new Promise(resolve => { finishRequest = resolve }))
  try {
    dialog.formRef.value.validate = () => new Promise(resolve => { finishValidation = resolve })
    const saving = dialog.handleOk(false)
    await dialog.handleOk(true)
    dialog.handleCancel()
    assert.equal(dialog.loading.value, true)
    assert.equal(dialog.savingWithRefresh.value, false)
    assert.deepEqual(events, [])
    assert.deepEqual(fixture.calls, [])
    finishValidation()
    await nextTick()
    await dialog.handleOk(true)
    assert.equal(fixture.calls.length, 1)
    assert.deepEqual(fixture.calls[0][2], { params: { revokeAccessTokens: false } })
    finishRequest({ success: true })
    await saving
    assert.equal(dialog.loading.value, false)
    assert.equal(events.length, 1)
  } finally {
    dispose()
  }
})

test('保存反馈中英文资源同步，原令牌提示与新令牌失效说明明确', () => {
  const zh = JSON.parse(readFileSync(new URL('locales/lang/zh.json', moduleRoot)))
  const en = JSON.parse(readFileSync(new URL('locales/lang/en.json', moduleRoot)))
  const keys = Object.keys(zh).filter(key => key.startsWith('PersonalToken.TokenDialog.')).sort()
  assert.deepEqual(keys, Object.keys(en).filter(key => key.startsWith('PersonalToken.TokenDialog.')).sort())
  assert.ok(keys.length >= 6)
  assert.match(zh['PersonalToken.TokenDialog.savedWithoutRefresh'], /权限已在原令牌生效/)
  assert.match(zh['PersonalToken.TokenDialog.refreshDescription'], /原令牌失效/)
})
