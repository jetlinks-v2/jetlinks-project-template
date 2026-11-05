import type { MenuFilterDefinition, MenuItem } from '@jetlinks-web-core/types/module'
import {
  getProjectCodeFromLocation,
  isApplicationRuntime,
  isProjectRuntime,
} from '@jetlinks-web-core/utils/project-runtime'

export const PROJECT_USAGE_MENU_CODE = 'system/ProjectUsage'

export const isProjectUsageRuntime = () => isProjectRuntime() && !isApplicationRuntime()

/** 固定项目部署也可能没有 code；仅阻止用量请求，不改变菜单和全局运行态。 */
export const getProjectUsageUnavailableReason = (): 'projectOnly' | 'projectUnavailable' | undefined => {
  if (!isProjectUsageRuntime()) return 'projectOnly'
  if (!getProjectCodeFromLocation().trim()) return 'projectUnavailable'
}

/** 仅裁剪本功能菜单，保留其他模块的层级和权限；不从本地种子补造服务端未授权菜单。 */
export const filterProjectUsageMenus = (menus: MenuItem[], available: boolean): MenuItem[] => (
  menus
    .filter(item => item.code !== PROJECT_USAGE_MENU_CODE || (available && item.owner === 'iot'))
    .map(item => item.children
      ? { ...item, children: filterProjectUsageMenus(item.children, available) }
      : item)
)

export const getProjectUsageMenuFilters = (): MenuFilterDefinition[] => [{
  code: 'project-resource-usage',
  order: 0,
  filter: (menus, context) => filterProjectUsageMenus(
    menus,
    isProjectUsageRuntime() && !context.applicationScope,
  ),
}]
