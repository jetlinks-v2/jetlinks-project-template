import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { compileScript, parse } from 'vue/compiler-sfc'
import { build } from 'esbuild'

const moduleRoot = new URL('../', import.meta.url)
const apiPath = fileURLToPath(new URL('../../../jetlinks-web-core/src/api/account/center.ts', import.meta.url))
const fixtures = {
  '@jetlinks-web-core/locales': 'export default { global: { t: key => key } }',
  '@jetlinks-web-core/api/system/permission': 'export const queryPermission_api = data => globalThis.permissionInitializationFixture.query(data);',
  '@jetlinks-web/core': 'export const request = { post: (...args) => globalThis.permissionInitializationFixture.post(...args) };',
  '@jetlinks-web/utils': `
    export const randomString = () => 'generated-source';
    export const onlyMessage = (...args) => globalThis.permissionInitializationFixture.messages.push(args);
  `,
  'vue-i18n': 'export const useI18n = () => ({ t: (key, args) => args ? `${key}:${args[0]}` : key });',
}

// 编译并挂载真实 SFC；仅替换 API 发送与 Ant Design 宿主，保留 Vue 生命周期和双向回传。
const bundle = await build({
  stdin: {
    contents: `
      export { default as PermissionSelector } from './views/PersonCenter/components/PersonalToken/components/PermissionSelector.vue';
      export { useTokenDialog } from './views/PersonCenter/components/PersonalToken/components/useTokenDialog.ts';
      export { createRenderer, defineComponent, h, nextTick, reactive } from 'vue';
    `,
    resolveDir: fileURLToPath(moduleRoot),
  },
  bundle: true,
  format: 'esm',
  platform: 'node',
  write: false,
  plugins: [{
    name: 'permission-initialization-boundaries',
    setup(builder) {
      builder.onResolve({ filter: /.*/ }, args => {
        if (args.path === '@jetlinks-web-core/api/account/center') return { path: apiPath }
        if (Object.hasOwn(fixtures, args.path)) return { path: args.path, namespace: 'fixture' }
      })
      builder.onLoad({ filter: /.*/, namespace: 'fixture' }, args => ({ contents: fixtures[args.path] }))
      builder.onLoad({ filter: /\.vue$/ }, args => {
        const { descriptor, errors } = parse(readFileSync(args.path, 'utf8'), { filename: args.path })
        assert.deepEqual(errors, [])
        const script = compileScript(descriptor, { id: args.path, inlineTemplate: true })
        return { contents: script.content, loader: 'ts' }
      })
    },
  }],
})
const { PermissionSelector, useTokenDialog, createRenderer, defineComponent, h, nextTick, reactive } = await import(
  `data:text/javascript;base64,${Buffer.from(bundle.outputFiles[0].text).toString('base64')}`
)

const definitions = [
  { id: 'device', name: '设备', actions: [{ action: 'query', name: '查询' }, { action: 'save', name: '保存' }] },
  { id: 'ai-alarm', name: 'AI告警', actions: [
    { action: 'query', name: '查询' }, { action: 'save', name: '保存' }, { action: 'delete', name: '删除' },
  ] },
]
const fullScope = [
  { id: 'device', actions: ['query', 'save'] },
  { id: 'ai-alarm', actions: ['query', 'save', 'delete'] },
]
const oldScope = [
  { id: 'device', actions: ['query'] },
  { id: 'no-longer-grantable', actions: ['save'] },
]

function node(type, text = '') {
  return { type, text, props: {}, children: [], parent: null }
}
const renderer = createRenderer({
  createElement: type => node(type),
  createText: text => node('text', text),
  createComment: text => node('comment', text),
  setText: (target, text) => { target.text = text },
  setElementText: (target, text) => { target.text = text; target.children = [] },
  patchProp: (target, key, _previous, value) => { target.props[key] = value },
  insert(target, parent, anchor = null) {
    if (target.parent) target.parent.children.splice(target.parent.children.indexOf(target), 1)
    const index = anchor ? parent.children.indexOf(anchor) : -1
    parent.children.splice(index < 0 ? parent.children.length : index, 0, target)
    target.parent = parent
  },
  remove(target) {
    if (target.parent) target.parent.children.splice(target.parent.children.indexOf(target), 1)
    target.parent = null
  },
  parentNode: target => target.parent,
  nextSibling: target => target.parent?.children[target.parent.children.indexOf(target) + 1] || null,
})

function find(target, predicate) {
  if (predicate(target)) return target
  for (const child of target.children) {
    const match = find(child, predicate)
    if (match) return match
  }
}
function textContent(target) {
  return target.type === 'comment' ? '' : target.text + target.children.map(textContent).join('')
}
function tags(target) {
  return target.type === 'tag' ? [textContent(target)] : target.children.flatMap(tags)
}
async function flush() {
  await new Promise(setImmediate)
  await nextTick()
}

const InputSearch = defineComponent({
  props: ['value', 'disabled'],
  emits: ['search'],
  setup(props, { emit }) {
    return () => h('search', { disabled: props.disabled, onSearch: value => emit('search', value) })
  },
})
const Table = defineComponent({
  props: ['columns', 'dataSource', 'loading'],
  setup(props, { slots }) {
    return () => h('table', { loading: props.loading }, [
      h('header', props.columns.map(column => slots.headerCell({ column }))),
      ...props.dataSource.map(record => h('permission-row', { id: record.id },
        props.columns.map(column => h('cell', slots.bodyCell({ column, record }))),
      )),
    ])
  },
})

function createDialog(mode = 'edit', query, scope = oldScope) {
  const fixture = { queries: [], posts: [], messages: [] }
  fixture.query = params => {
    fixture.queries.push(params)
    if (query) return query(params, fixture.queries.length)
    const keyword = params.terms?.[0].value.slice(1, -1)
    return Promise.resolve({ success: true, result: definitions.filter(item => !keyword || item.name.includes(keyword)) })
  }
  fixture.post = (...args) => {
    fixture.posts.push(args)
    return Promise.resolve({ success: true, result: { accessToken: 'fixture-token' } })
  }
  globalThis.permissionInitializationFixture = fixture
  const props = reactive({
    visible: true,
    mode,
    token: mode === 'add' ? null : {
      id: 'old-token', name: '测试', expires: -1, sourceId: 'original-source',
      scope: { permissions: structuredClone(scope) },
    },
  })
  let dialog
  const root = node('root')
  const app = renderer.createApp(defineComponent({
    setup() {
      dialog = useTokenDialog(props, () => {})
      dialog.formRef.value = { validate: async () => {}, clearValidate: () => {} }
      if (mode === 'add') {
        dialog.formData.value.name = '新增'
        dialog.formData.value.expires = -1
      }
      return () => h(PermissionSelector, {
        value: dialog.formData.value.scope.permissions,
        disabled: mode === 'view',
        'onUpdate:value': value => { dialog.formData.value.scope.permissions = value },
      })
    },
  }))
  app.component('a-input-search', InputSearch)
  app.component('a-table', Table)
  for (const [name, type] of [['a-space', 'space'], ['a-tag', 'tag']]) {
    app.component(name, defineComponent({ setup: (_props, { slots }) => () => h(type, slots.default?.()) }))
  }
  app.mount(root)
  return {
    dialog, fixture, props, root,
    row: id => find(root, target => target.type === 'permission-row' && target.props.id === id),
    search: keyword => find(root, target => target.type === 'search').props.onSearch(keyword),
    dispose: () => app.unmount(),
  }
}

test('编辑先等待完整查询，再全选新权限并移除已不可授权的旧操作', async () => {
  let finishQuery
  const context = createDialog('edit', () => new Promise(resolve => { finishQuery = resolve }))
  try {
    assert.equal(context.dialog.canSubmit.value, false)
    assert.deepEqual(context.dialog.formData.value.scope.permissions, [])
    await nextTick()
    assert.equal(find(context.root, target => target.type === 'search').props.disabled, true)
    finishQuery({ success: true, result: definitions })
    await flush()
    assert.equal(context.dialog.canSubmit.value, true)
    assert.deepEqual(context.dialog.formData.value.scope.permissions, fullScope)
    assert.deepEqual(tags(context.row('ai-alarm')), ['查询', '保存', '删除'])
    assert.match(textContent(context.root), /061384-5:5/)
    assert.deepEqual(context.props.token.scope.permissions, oldScope)
  } finally {
    context.dispose()
  }
})

test('新增和编辑使用相同全量权限及操作展示', async () => {
  for (const mode of ['add', 'edit']) {
    const context = createDialog(mode)
    try {
      await flush()
      assert.deepEqual(context.fixture.queries, [{ paging: false }])
      assert.deepEqual(context.dialog.formData.value.scope.permissions, fullScope)
      assert.deepEqual(tags(context.row('ai-alarm')), ['查询', '保存', '删除'])
    } finally {
      context.dispose()
    }
  }
})

test('搜索及清空只改变显示，两种编辑保存均提交全量权限和原记录 ID', async () => {
  for (const refresh of [true, false]) {
    const context = createDialog()
    try {
      await flush()
      context.search('AI告警')
      await flush()
      assert.equal(context.row('device'), undefined)
      assert.deepEqual(tags(context.row('ai-alarm')), ['查询', '保存', '删除'])
      assert.deepEqual(context.dialog.formData.value.scope.permissions, fullScope)
      if (refresh) {
        context.search('')
        await flush()
        assert.ok(context.row('device'))
        assert.deepEqual(context.dialog.formData.value.scope.permissions, fullScope)
      }
      await context.dialog.handleOk(refresh)
      assert.equal(context.fixture.posts.length, 1)
      const [url, data, options] = context.fixture.posts[0]
      assert.equal(url, '/personal/token/me/_save')
      assert.equal(data.id, 'old-token')
      assert.deepEqual(data.scope.permissions, fullScope)
      assert.deepEqual(options, { params: { revokeAccessTokens: refresh } })
    } finally {
      context.dispose()
    }
  }
})

test('查看保留原授权，空授权也不被自动替换为全量权限', async () => {
  for (const scope of [oldScope, []]) {
    const context = createDialog('view', undefined, scope)
    try {
      await flush()
      assert.deepEqual(context.dialog.formData.value.scope.permissions, scope)
      assert.deepEqual(tags(context.row('ai-alarm')), [])
      assert.deepEqual(tags(context.row('device')), scope.length ? ['查询'] : [])
      context.search('AI告警')
      await flush()
      context.search('')
      await flush()
      assert.deepEqual(context.dialog.formData.value.scope.permissions, scope)
      assert.deepEqual(context.fixture.posts, [])
    } finally {
      context.dispose()
    }
  }
})

test('完整查询失败时不能保存旧权限，重试搜索会先加载全量权限', async () => {
  for (const failure of [() => ({ success: false }), () => { throw new Error('network unavailable') }]) {
    const context = createDialog('edit', (params, attempt) => {
      if (attempt === 1) return failure()
      return { success: true, result: params.terms ? [definitions[1]] : definitions }
    })
    try {
      await flush()
      assert.equal(context.dialog.canSubmit.value, false)
      assert.deepEqual(context.dialog.formData.value.scope.permissions, [])
      assert.deepEqual(context.fixture.messages, [['PersonalToken.PermissionList.loadFailed', 'error']])
      context.search('AI告警')
      await flush()
      assert.deepEqual(context.fixture.queries, [
        { paging: false }, { paging: false },
        { paging: false, terms: [{ column: 'name$like', value: '%AI告警%' }] },
      ])
      assert.deepEqual(context.dialog.formData.value.scope.permissions, fullScope)
      assert.deepEqual(tags(context.row('ai-alarm')), ['查询', '保存', '删除'])
      assert.equal(context.row('device'), undefined)
      assert.equal(context.dialog.canSubmit.value, true)
    } finally {
      context.dispose()
    }
  }
})
