import { defineAsyncComponent } from 'vue';
import i18n from "@jetlinks-web-core/locales";
import { handleSiderBreadcrumb } from '@jetlinks-web-core/store'
import { RouteSecurityLevel, type ModuleRouteOverride } from '@jetlinks-web-core/router/types'
import {
    BASIS_FORM_FIELD,
    createHiddenBasisFormFields,
} from '@authentication-manager-ui/views/system/Basis/fieldRegistry'

const routerModules = import.meta.glob('./views/**/index.vue')

const getAsyncRoutesMap = () => {
    const modules = {}
    Object.keys(routerModules).forEach(item => {
        const code = item.replace('./views/', '').replace('/index.vue', '')
        const key = `${code}`
        // const key = `${MODULE_CODE}/${code}` // views下不存在多模块时
        modules[key] = routerModules[item]
        // 存量数据库菜单编码为 system/Domain，保留别名避免升级后菜单页面空白。
        if (code === 'system/TenantDomain') {
            modules['system/Domain'] = routerModules[item]
        }
    })

    return modules
}

const getExtraRoutesMap = () => {
    return {
        'device/Product': {
            mode: 'union',
            children: [
                {
                    code: 'Add',
                    url: '/add',
                    name: '新增产品',
                    component: () => import('./views/device/product/Add/index.vue')
                }
            ]
        },
        'device/Template': {
            children: [
                {
                    code: 'Detail',
                    url: '/detail/:id',
                    name: '详情',
                    component: () => import('./views/device/Template/Detail/index.vue')
                }
            ]
        },
    }
}

const getCoreRouteOverrides = () => {
    return handleSiderBreadcrumb([
        {
            path: '/scan/product/list',
            name: 'scan-product/list',
            meta: {
                title: '产品列表',
                hideInMenu: true,
            },
            component: defineAsyncComponent(() => import('./views/scan/Product/index.vue')),
        },
        {
            path: '/scan/product/save',
            name: 'scan-product/save',
            meta: {
                title: '产品新增',
                hideInMenu: true,
            },
            component: defineAsyncComponent(() => import('./views/scan/Product/components/Save.vue')),
        },
        {
            path: '/scan/product/success',
            name: 'scan-product/success',
            meta: {
                title: '绑定成功',
                hideInMenu: true,
            },
            component: defineAsyncComponent(() => import('./views/scan/Product/components/Success.vue')),
        },
        {
            name: 'SystemLogin',
            path: '/system/login',
            component: () => import('@jetlinks-web-core/views/login/index.vue'),
            meta: {
                title: '系统菜单',
                hideInMenu: true,
                security: RouteSecurityLevel.PUBLIC
            }
        },
        {
            path: '/account',
            name: 'Account',
            component: () => import('@jetlinks-web-core/layout/BasicLayoutPage.vue'),
            meta: {
                title: i18n.global.t('PersonCenter.route.title'),
                hideInMenu: true
            },
            children: [
                {
                    path: 'center',
                    name: 'account/center',
                    component: () => import('./views/PersonCenter/index.vue'),
                    meta: {
                        title: i18n.global.t('PersonCenter.route.title'),
                        icon: '',
                        hideInMenu: true
                    }
                }
            ]
        },
    ])
}

const getComponents = () => {
    return {
    }
}

// const register = () => {
//     console.log(registerSetting)
//     moduleRegistry.register('saas-runtime-ui', registerSetting)
// }

const getRegisterComponents = () => [
    ...createHiddenBasisFormFields([
        BASIS_FORM_FIELD.BASE_PATH,
        BASIS_FORM_FIELD.RECORD_NUMBER,
        BASIS_FORM_FIELD.BACKGROUND,
        BASIS_FORM_FIELD.ICO,
    ]),
    {
        targetPage: 'system/basis',
        targetModule: 'basic',
        component: defineAsyncComponent(() => import('./components/Tenant/TenantDomainSection.vue')),
        target: 'basic',
        mode: 'append',
        code: 'tenant',
    },
    {
        targetPage: 'system/Department',
        targetModule: 'department-tabs',
        target: 'position', // 或 property
        mode: 'hide',
        code: 'hide-department-position',
    },
    {
        targetPage: 'system/Department',
        targetModule: 'department-tabs',
        target: 'property', // 或 property
        mode: 'hide',
        code: 'hide-department-property',
    },
    // {
    //     targetPage: 'device/Product',
    //     targetModule: 'productAddButton',
    //     component: null,
    //     target: 'add',
    //     mode: 'hide',
    //     code: 'hide-add',
    // },
    // {
    //     targetPage: 'device/Product',
    //     targetModule: 'productAddButton',
    //     component: defineAsyncComponent(() => import('./views/device/product/save/index.vue')),
    //     target: 'batch',
    //     mode: 'before',
    //     code: 'add-before-batch',
    // },
    // {
    //     targetPage: 'device/Instance',
    //     targetModule: 'addTool',
    //     component: defineAsyncComponent(() => import('./views/device/instance/save/index.vue')),
    //     target: 'add',
    //     mode: 'replace',
    //     code: 'add',
    // },
    // {
    //     targetPage: 'layout',
    //     targetModule: 'headerRight',
    //     component: defineAsyncComponent(() => import('./components/SentinelButton/index.vue')),
    //     target: 'Language',
    //     mode: 'before',
    //     code: 'sentinelButton',
    // }
]


export default {
    getAsyncRoutesMap,
    getExtraRoutesMap,
    getComponents,
    getRegisterComponents,
    getCoreRouteOverrides,
    priority: 9
//     register
}
