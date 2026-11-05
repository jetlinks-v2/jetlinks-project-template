import type { MenuItem } from '@jetlinks-web-core/types/module'
import baseMenu from './baseMenu.json'
import { id } from './package.json'

export default () => (baseMenu as MenuItem[]).map(menu => ({
  ...menu,
  options: { ...menu.options, appName: id },
  runtime: true,
}))
