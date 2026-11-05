import { moduleRegistry } from '@jetlinks-web-core/utils/module-registry'
import { name } from './package.json'
import registerSetting from './register'

const views = import.meta.glob('./views/**/index.vue')

export default {
  getAsyncRoutesMap: () => Object.fromEntries(Object.entries(views).map(([path, component]) => [
    path.replace('./views/', '').replace('/index.vue', ''),
    component,
  ])),
  register: () => moduleRegistry.register(name, registerSetting),
}
