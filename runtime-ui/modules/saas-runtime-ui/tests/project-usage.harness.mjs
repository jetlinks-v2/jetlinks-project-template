import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { dirname, resolve } from 'node:path'
import { setImmediate } from 'node:timers/promises'
import { fileURLToPath } from 'node:url'
import { transformSync } from 'esbuild'
import { createRenderer, defineComponent, h, nextTick, reactive } from 'vue'
import { createI18n } from 'vue-i18n'
import { compileScript, parse } from 'vue/compiler-sfc'

const require = createRequire(import.meta.url)
const moduleRoot = fileURLToPath(new URL('../', import.meta.url))
const pageRoot = resolve(moduleRoot, 'views/system/ProjectUsage')
const messages = Object.fromEntries(['zh', 'en'].map(locale => [locale,
  Object.fromEntries(Object.entries(JSON.parse(readFileSync(resolve(moduleRoot, `locales/lang/${locale}.json`), 'utf8')))
    .filter(([key]) => key.startsWith('ProjectUsage.'))),
]))

export const deferred = () => {
  let resolve, reject
  const promise = new Promise((ok, fail) => { resolve = ok; reject = fail })
  return { promise, resolve, reject }
}

export const flushUsage = async () => { await setImmediate(); await nextTick() }

// 使用真实 hook、service、API、SFC 和 Vue 生命周期，仅替换请求、入口上下文及基础 UI。
export function mountUsage(t, options = {}) {
  const runtime = reactive({ project: true, application: false, code: 'project-a', ...options.runtime })
  const route = reactive({ fullPath: '/system/project-usage' })
  const calls = []
  const fixture = { request: options.request || (async () => []) }
  const i18n = createI18n({ legacy: false, locale: options.locale || 'zh', messages: structuredClone(messages), flatJson: true })
  const dependencies = {
    'vue-router': { useRoute: () => route },
    '@jetlinks-web-core/locales': i18n,
    '@jetlinks-web-core/utils/project-runtime': {
      isProjectRuntime: () => runtime.project,
      isApplicationRuntime: () => runtime.application,
      getProjectCodeFromLocation: () => runtime.code,
    },
    '@jetlinks-web/core': { request: { get: (...args) => {
      calls.push({ code: runtime.code, args })
      return fixture.request(...args)
    } } },
  }
  const cache = new Map()
  const load = filename => {
    if (cache.has(filename)) return cache.get(filename)
    let source = readFileSync(filename, 'utf8')
    if (filename.endsWith('.vue')) {
      const { descriptor } = parse(source, { filename })
      source = compileScript(descriptor, { id: filename, inlineTemplate: true }).content
    }
    const module = { exports: {} }
    const { code } = transformSync(source, { loader: 'ts', format: 'cjs' })
    new Function('require', 'module', 'exports', code)(id => {
      if (Object.hasOwn(dependencies, id)) return dependencies[id]
      if (id.startsWith('.')) return load(resolve(dirname(filename), /\.(vue|ts)$/.test(id) ? id : `${id}.ts`))
      return require(id)
    }, module, module.exports)
    cache.set(filename, module.exports)
    return module.exports
  }
  const { useProjectUsagePage } = load(resolve(pageRoot, 'useProjectUsagePage.ts'))
  const Section = load(resolve(pageRoot, 'components/ResourceUsageSection.vue')).default
  const node = (type, text = '') => ({ type, text, props: {}, children: [], parent: null })
  const renderer = createRenderer({
    createElement: type => node(type), createText: text => node('#text', text),
    createComment: text => node('#comment', text),
    setText: (target, text) => { target.text = text },
    setElementText: (target, text) => { target.text = text; target.children = [] },
    patchProp: (target, key, _old, value) => { target.props[key] = value },
    parentNode: target => target.parent,
    nextSibling: target => target.parent?.children[target.parent.children.indexOf(target) + 1] || null,
    insert(target, parent, anchor = null) {
      if (target.parent) target.parent.children.splice(target.parent.children.indexOf(target), 1)
      const index = anchor ? parent.children.indexOf(anchor) : -1
      parent.children.splice(index < 0 ? parent.children.length : index, 0, target)
      target.parent = parent
    },
    remove(target) {
      target.parent.children.splice(target.parent.children.indexOf(target), 1)
      target.parent = null
    },
  })
  let page
  const app = renderer.createApp(defineComponent({ setup() {
    page = useProjectUsagePage()
    return () => h(Section, { resources: page.resources.value, state: page.state.value, onRetry: page.loadResources })
  } }))
  app.use(i18n)
  app.component('CloudEmpty', defineComponent({
    props: ['description', 'type'], setup: props => () => h('empty', { type: props.type }, props.description),
  }))
  app.component('a-result', defineComponent({
    props: ['title', 'subTitle', 'status'],
    setup: (props, { slots }) => () => h('result', { status: props.status }, [props.title, props.subTitle, slots.extra?.()]),
  }))
  app.component('a-button', defineComponent({ setup: (_, { slots }) => () => h('button', slots.default?.()) }))
  app.component('a-modal', defineComponent({
    props: ['open'], setup: (props, { slots }) => () => props.open ? h('dialog', slots.default?.()) : null,
  }))
  for (const name of ['AIcon', 'a-spin', 'a-progress', 'a-qrcode']) {
    app.component(name, defineComponent({ setup: () => () => h(name) }))
  }
  const container = node('root')
  app.mount(container)
  let mounted = true
  const unmount = () => { if (mounted) { app.unmount(); mounted = false } }
  t.after(unmount)
  const find = (type, target = container) => [
    ...(target.type === type ? [target] : []), ...target.children.flatMap(child => find(type, child)),
  ]
  const text = (target = container) => target.type === '#comment' ? '' : target.text + target.children.map(text).join('')
  return { page, runtime, route, calls, fixture, i18n, find, text, unmount }
}
