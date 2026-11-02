import type {
    MenuFilterDefinition,
    MenuItem,
} from '@jetlinks-web-core/types/module'
import { isApplicationRuntime } from '@jetlinks-web-core/utils/project-runtime'

type RuntimeMenuOwner = 'iot' | 'app'

/** 按运行端归属裁剪菜单树；不匹配的父节点及其子树整体移除。 */
const filterMenuTreeByRuntimeOwner = (
    menus: MenuItem[],
    owner: RuntimeMenuOwner,
): MenuItem[] => menus.reduce<MenuItem[]>((result, item) => {
    if (item.owner !== owner) {
        return result
    }

    const children = filterMenuTreeByRuntimeOwner(item.children || [], owner)

    result.push({
        ...item,
        children: children.length ? children : undefined,
    })

    return result
}, [])

/** 每次加载菜单时识别运行端，应用端只保留 app，项目端只保留 iot。 */
export const filterSaasMenusByRuntimeOwner = (
    menus: MenuItem[],
): MenuItem[] => {
    // 应用端也可能具有项目上下文，优先识别应用运行态。
    const owner: RuntimeMenuOwner = isApplicationRuntime() ? 'app' : 'iot'

    return filterMenuTreeByRuntimeOwner(menus, owner)
}

/** 注册路由生成前执行的菜单过滤器。 */
export const getSaasMenuFilters = (): MenuFilterDefinition[] => [
    {
        code: 'saas-menu-condition',
        order: 0,
        filter: filterSaasMenusByRuntimeOwner,
    },
]
