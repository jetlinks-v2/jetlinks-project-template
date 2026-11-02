import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { build } from 'esbuild'
import { AxiosError } from 'axios'
import { deferred, flushUsage, mountUsage } from './project-usage.harness.mjs'

const moduleRoot = new URL('../', import.meta.url)

async function loadModule(path, mocks = {}) {
  const result = await build({
    entryPoints: [fileURLToPath(new URL(path, moduleRoot))],
    bundle: true,
    format: 'esm',
    platform: 'node',
    write: false,
    plugins: [{
      name: 'usage-boundary-fixtures',
      setup(builder) {
        builder.onResolve({ filter: /.*/ }, args => (
          Object.hasOwn(mocks, args.path) ? { path: args.path, namespace: 'fixture' } : undefined
        ))
        builder.onLoad({ filter: /.*/, namespace: 'fixture' }, args => ({ contents: mocks[args.path] }))
      },
    }],
  })
  return import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`)
}

const pagePath = 'views/system/ProjectUsage/'
const mapper = await loadModule(`${pagePath}projectUsage.mapper.ts`)
const grouping = await loadModule(`${pagePath}resourceUsageGroups.ts`)
const service = await loadModule(`${pagePath}projectUsage.service.ts`, {
  '@jetlinks-web-core/locales': 'export default { global: { t: key => key } }',
  '@jetlinks-web/core': 'export const request = { get: (...args) => globalThis.usageFixture.request(...args) }',
})
const context = await loadModule('utils/projectUsage.ts', {
  '@jetlinks-web-core/utils/project-runtime': `
    export const isProjectRuntime = () => globalThis.usageFixture.project;
    export const isApplicationRuntime = () => globalThis.usageFixture.application;
    export const getProjectCodeFromLocation = () => globalThis.usageFixture.code;
  `,
})
const { default: getBaseMenus } = await loadModule('baseMenu.ts')
const menuSetup = await loadModule('../authentication-manager-ui/views/system/Menu/Setting/utils.ts', {
  '@jetlinks-web-core/utils/consts': 'export const messageSubscribe = "message-subscribe"; export const USER_CENTER_MENU_CODE = "account";',
})

const metric = (usage, limit, id = 'count') => ({
  metric: { id, name: '设备数' },
  usage,
  limit: limit === undefined ? undefined : { limit },
})
const resource = (id, name, metrics) => ({ id, name, metrics })

test('配额百分比：零、缺失、无限、负用量、舍入与超额', () => {
  assert.equal(mapper.calculateUsagePercent(1, { limit: 3 }), 33.3)
  assert.equal(mapper.calculateUsagePercent(120, { limit: 100 }), 100)
  assert.equal(mapper.calculateUsagePercent(-5, { limit: 100 }), 0)
  for (const limit of [{ limit: 0 }, { limit: -1 }, {}, { limit: '' }, { limit: 'invalid' }]) {
    assert.equal(mapper.calculateUsagePercent(10, limit), 0)
  }
  assert.equal(mapper.calculateUsagePercent('5', { value: '10' }), 50)
})

test('资源卡片映射指标和最高使用率，排除无限/无配额指标', () => {
  const result = mapper.mapProjectResourceUsage([
    resource('device', '设备', [
      metric(60, 100),
      metric(9999, -1, 'unlimited'),
      metric(7, undefined, 'missing'),
    ]),
    resource('video', '视频', [metric(3, 10)]),
  ])

  assert.equal(result[0].name, '设备')
  assert.equal(result[0].maxUsagePercent, 60)
  assert.equal(result[0].metrics[0].name, '设备数')
  assert.equal(result[0].metrics[1].unlimited, true)
  assert.equal(result[0].metrics[2].limit, null)
  assert.equal(result[1].maxUsagePercent, 30)
})

test('没有指标或只有无限配额的资源不伪造最高使用率', () => {
  const result = mapper.mapProjectResourceUsage([
    resource('empty', '空资源', []),
    resource('unlimited', '不限量资源', [metric(300, -1)]),
  ])
  assert.deepEqual(result[0].metrics, [])
  assert.equal(result[0].maxUsagePercent, null)
  assert.equal(result[1].maxUsagePercent, null)
  assert.deepEqual(mapper.mapProjectResourceUsage(undefined), [])
})

test('用量按基础、AI、智能体指标分组，未知资源保持可见', () => {
  const resources = mapper.mapProjectResourceUsage([
    resource('product', '产品', [metric(2, 100)]),
    resource('ai_task', 'AI任务', [metric(3, -1)]),
    resource('ai_agent', 'AI智能体', [
      { metric: { id: 'entityCount', name: '实体数量' }, usage: 1, limit: { limit: -1 } },
      { metric: { id: 'costQuota', name: 'LLM成本总量', description: '大模型调用成本总额度' }, usage: 0, limit: { limit: -1 } },
    ]),
    resource('custom', '自定义资源', [metric(1, 10)]),
  ])
  const groups = grouping.groupResourceUsage(resources)
  assert.deepEqual(groups.map(group => [group.key, group.items.length]), [
    ['basic', 1], ['ai', 1], ['agent', 2], ['other', 1],
  ])
  assert.equal(groups[2].items[1].titleKey, 'ProjectUsage.item.llmCost')
  assert.equal(groups[2].items[1].description, '大模型调用成本总额度')
  assert.deepEqual(grouping.groupResourceUsage([]).map(group => group.key), ['basic', 'ai', 'agent'])
})

test('资源用量请求使用当前项目运行时接口，不传项目 ID 或控制台上下文配置', async () => {
  const calls = []
  globalThis.usageFixture = {
    request: async (...args) => {
      calls.push(args)
      return [resource('device', '设备', [metric(31, 100)])]
    },
  }

  const result = await service.fetchProjectResourceUsage()
  assert.equal(calls.length, 1)
  assert.equal(calls[0][0], '/tenant/me/limits')
  assert.deepEqual(calls[0], ['/tenant/me/limits', undefined, { hiddenError: true }])
  assert.equal(result[0].metrics[0].usage, 31)
})

test('资源接口返回数组或兼容包装结果，非法响应向页面抛出错误', async () => {
  globalThis.usageFixture.request = async () => ({
    success: true,
    result: [resource('device', '设备', [metric(1, 10)])],
  })
  assert.equal((await service.fetchProjectResourceUsage())[0].id, 'device')

  for (const response of [
    { success: false, message: 'denied' },
    { success: true, result: {} },
  ]) {
    globalThis.usageFixture.request = async () => response
    await assert.rejects(service.fetchProjectResourceUsage())
  }
})

for (const code of ['', '   ']) {
  test(`固定项目运行态但 code=${JSON.stringify(code)} 时首次加载和重试均零请求`, async t => {
    const f = mountUsage(t, { runtime: { code } })
    await flushUsage()
    assert.equal(f.page.state.value.status, 'unavailable')
    assert.equal(f.text(), '当前环境暂不支持查看项目资源用量')
    assert.equal(f.find('empty')[0].props.type, 'page')
    assert.equal(f.find('button').length, 0)
    await f.page.loadResources()
    assert.equal(f.calls.length, 0)
    assert.deepEqual(f.page.resources.value, [])
  })
}

test('非项目运行态和业务应用运行态展示进入项目提示，不请求数据', async t => {
  for (const runtime of [{ project: false }, { application: true }]) {
    const f = mountUsage(t, { runtime })
    await flushUsage()
    assert.equal(f.page.state.value.status, 'unavailable')
    assert.equal(f.text(), '请进入项目后查看资源用量')
    assert.equal(f.calls.length, 0)
    assert.equal(f.find('button').length, 0)
    f.unmount()
  }
})

test('有效 code 无本地缓存可加载用量，真实扩容按钮仍打开支持弹窗', async t => {
  const pending = deferred()
  const f = mountUsage(t, { request: () => pending.promise })
  assert.equal(f.page.state.value.status, 'loading')
  assert.equal(f.find('a-spin').length, 1)
  pending.resolve([resource('device', '设备', [metric(31, 100)])])
  await flushUsage()
  assert.equal(f.page.state.value.status, 'success')
  assert.equal(f.page.resources.value[0].metrics[0].usage, 31)
  assert.deepEqual(f.calls, [{ code: 'project-a', args: ['/tenant/me/limits', undefined, { hiddenError: true }] }])
  assert.match(f.text(), /31 \/ 100/)
  assert.equal(f.find('dialog').length, 0)
  f.find('button')[0].props.onClick()
  await flushUsage()
  assert.equal(f.find('dialog').length, 1)
  assert.match(f.text(), /请联系技术支持/)
  f.find('button').find(button => f.text(button) === '我知道了').props.onClick()
  await flushUsage()
  assert.equal(f.find('dialog').length, 0)
})

const forbiddenError = () => new AxiosError('Request failed with status code 403', 'ERR_BAD_REQUEST', undefined, undefined, {
  status: 403, data: {}, headers: {}, config: {}, statusText: 'Forbidden',
})

test('真实 HTTP 403 展示权限提示且没有重试或扩容入口，不自动重试', async t => {
  const f = mountUsage(t, { request: async () => { throw forbiddenError() } })
  await flushUsage()
  assert.equal(f.page.state.value.status, 'forbidden')
  assert.equal(f.find('result')[0].props.status, '403')
  assert.equal(f.text(), '暂无查看资源用量的权限，请联系管理员')
  assert.equal(f.find('button').length, 0)
  assert.equal(f.calls.length, 1)
})

test('普通失败保留手动重试，重试成功空数组展示数据空态', async t => {
  const f = mountUsage(t, { request: async () => { throw new Error('Network Error') } })
  await flushUsage()
  assert.equal(f.page.state.value.status, 'error')
  assert.match(f.text(), /资源用量加载失败.*Network Error.*重新加载/)
  assert.equal(f.calls.length, 1)
  f.fixture.request = async () => []
  f.find('button')[0].props.onClick()
  await flushUsage()
  assert.equal(f.page.state.value.status, 'empty')
  assert.match(f.text(), /当前项目暂无可展示的资源用量/)
  assert.equal(f.calls.length, 2)
})

test('项目切换清空旧数据，旧项目迟到的成功或 403 均不覆盖当前项目', async t => {
  for (const rejectOld of [false, true]) {
    const old = deferred()
    const current = deferred()
    const f = mountUsage(t, { request: () => old.promise })
    f.fixture.request = () => current.promise
    f.runtime.code = 'project-b'
    f.route.fullPath = '/system/project-usage?project=b'
    await flushUsage()
    assert.deepEqual(f.page.resources.value, [])
    current.resolve([resource('current', '当前资源', [metric(2, 10)])])
    await flushUsage()
    if (rejectOld) old.reject(forbiddenError())
    else old.resolve([resource('old', '旧资源', [metric(1, 10)])])
    await flushUsage()
    assert.equal(f.page.state.value.status, 'success')
    assert.equal(f.page.resources.value[0].id, 'current')
    assert.deepEqual(f.calls.map(call => call.code), ['project-a', 'project-b'])
    f.unmount()
  }
})

test('切换为无 code 清空已有用量并阻止在途结果回填', async t => {
  const f = mountUsage(t, { request: async () => [resource('device', '设备', [metric(1, 10)])] })
  await flushUsage()
  assert.equal(f.page.resources.value.length, 1)
  const pending = deferred()
  f.fixture.request = () => pending.promise
  const reload = f.page.loadResources()
  assert.deepEqual(f.page.resources.value, [])
  f.runtime.code = ''
  f.route.fullPath = '/system/project-usage?project=none'
  await flushUsage()
  assert.equal(f.page.state.value.status, 'unavailable')
  pending.resolve([resource('old', '旧资源', [metric(1, 10)])])
  await reload
  await flushUsage()
  assert.equal(f.page.state.value.status, 'unavailable')
  assert.deepEqual(f.page.resources.value, [])
  assert.equal(f.calls.length, 2)
  assert.equal(f.find('button').length, 0)
})

test('连续重试和卸载后，过期请求不得修改页面状态', async t => {
  const old = deferred()
  const newer = deferred()
  const f = mountUsage(t, { request: () => old.promise })
  f.fixture.request = () => newer.promise
  const reload = f.page.loadResources()
  newer.resolve([])
  await reload
  old.reject(forbiddenError())
  await flushUsage()
  assert.equal(f.page.state.value.status, 'empty')
  const pending = deferred()
  f.fixture.request = () => pending.promise
  const last = f.page.loadResources()
  f.unmount()
  const before = f.page.state.value
  pending.resolve([resource('old', '旧资源', [metric(1, 10)])])
  await last
  assert.equal(f.page.state.value, before)
  assert.deepEqual(f.page.resources.value, [])
})

test('无项目和权限不足使用真实英文资源', async t => {
  const missing = mountUsage(t, { runtime: { code: '' }, locale: 'en' })
  assert.equal(missing.text(), 'Project resource usage is not available in this environment')
  const forbidden = mountUsage(t, { locale: 'en', request: async () => { throw forbiddenError() } })
  await flushUsage()
  assert.equal(forbidden.text(), 'You do not have permission to view resource usage. Contact your administrator.')
})

test('菜单过滤仅裁剪本功能，保留其他模块与服务端权限，不补造未授权菜单', () => {
  const usage = { code: 'system/ProjectUsage', name: '资源用量', owner: 'iot', url: '/system/project-usage' }
  const other = { code: 'system/Other', name: '其他', url: '/system/other', owner: 'app' }
  const menus = [{ code: 'system', name: '系统', url: '/system', children: [usage, other] }]
  assert.deepEqual(context.filterProjectUsageMenus(menus, true), menus)
  assert.deepEqual(context.filterProjectUsageMenus(menus, false)[0].children, [other])
  assert.equal(menus[0].children.length, 2)
  assert.deepEqual(context.filterProjectUsageMenus([], true), [])
  assert.deepEqual(context.filterProjectUsageMenus([{ ...usage, owner: 'cloud' }], true), [])
  globalThis.usageFixture = { project: true, application: false }
  const filter = context.getProjectUsageMenuFilters()[0].filter
  assert.deepEqual(filter(menus, { applicationScope: 'application-1' })[0].children, [other])
  assert.deepEqual(filter(menus, {}), menus)
})

test('菜单种子通过现有系统菜单同步的 showPage 门禁，保留项目查询权限和路由契约', () => {
  const available = menuSetup.filterMenuDefinitions(getBaseMenus(), new Set(['system_config']))
  const usage = available.find(item => item.code === 'system')?.children.find(item => item.code === 'system/ProjectUsage')
  assert.ok(usage, '资源用量必须出现在系统菜单同步候选中')
  assert.equal(usage.name, '资源用量')
  assert.equal(usage.owner, 'iot')
  assert.equal(usage.url, '/system/project-usage')
  assert.equal(usage.options.appName, 'saas-runtime')
  assert.equal(usage.options.routeTarget, undefined)
  assert.deepEqual(usage.permissions, [{ permission: 'self-project', actions: ['query'] }])
  assert.deepEqual(menuSetup.filterMenuDefinitions(getBaseMenus(), new Set()), [])
})

test('资源卡片扩容入口打开页面级技术支持二维码弹窗', () => {
  const card = readFileSync(new URL(`${pagePath}components/ResourceUsageCard.vue`, moduleRoot), 'utf8')
  const section = readFileSync(new URL(`${pagePath}components/ResourceUsageSection.vue`, moduleRoot), 'utf8')
  const modal = readFileSync(new URL(`${pagePath}components/ResourceExpansionSupportModal.vue`, moduleRoot), 'utf8')

  assert.match(card, /@click="emit\('expand'\)"/)
  assert.match(section, /@expand="supportModalOpen = true"/)
  assert.match(section, /<ResourceExpansionSupportModal v-model:open="supportModalOpen" \/>/)
  assert.match(modal, /<a-qrcode value="https:\/\/www\.jetlinks\.cn\/" :size="320" \/>/)
  assert.match(modal, /ProjectUsage\.support\.confirm/)
})
