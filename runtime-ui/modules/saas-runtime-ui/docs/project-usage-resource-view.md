# 资源用量

## 项目上下文缺失与权限反馈

- 目标与范围：仅调整 `saas-runtime-ui` 的资源用量加载、状态展示、相邻中英文资源和专项测试。项目运行态且具有有效 code 才请求 `/tenant/me/limits`，兼容固定项目配置与路径取值，不依赖本地项目缓存。
- 实现：`utils/projectUsage.ts#getProjectUsageUnavailableReason` 在原运行态判断后检查有效 code；`views/system/ProjectUsage/useProjectUsagePage.ts` 与 `types.ts` 增加 `unavailable`、`forbidden` 状态，`components/ResourceUsageSection.vue` 分别使用页面级 `CloudEmpty` 和权限结果页。非项目运行态保留进入项目的提示；以上状态均无重试和扩容入口。其他请求失败保留手动重试。`api/projectUsage.ts` 仅为本接口启用 `hiddenError`，由页面承载错误，保留请求层登录失效及身份验证流程；接口地址、参数和响应契约不变。
- 并发边界：每次加载先使旧请求失效并清空数据，再检查项目上下文；保留项目切换、连续重试及卸载后忽略过期结果的保护。
- 不做：不改 `ui/`、后端、全局项目运行态判断、菜单、三列布局、指标计算及扩容行为；不增加更新时间或数值展示调整。
- 验证结果：`node --test modules/saas-runtime-ui/tests/project-usage.test.mjs` 19/19 通过。新增 `tests/project-usage.harness.mjs` 编译实际 hook、service、API 与 Vue 组件，使用真实 Vue 生命周期及模块中英文资源，覆盖空白 code 首次加载/重试零请求、有效 code 无本地缓存加载、403/网络错误/空结果、项目切换与连续重试竞态、卸载后不回填、缺省态/权限态无操作入口以及正常扩容弹窗开关。既有菜单测试同步调用当前的 `filterMenuDefinitions` 导出，保留原有权限和路由断言，不改生产菜单。
- 静态验证：修改组件的脚本、模板、样式编译和中英文 `ProjectUsage.*` 键一致性检查通过；`node ../ui/scripts/check-ui-style.mjs --files modules/saas-runtime-ui/views/system/ProjectUsage/components/ResourceUsageSection.vue` 及 `git diff --check` 通过。工作区未配置独立 lint 脚本。`pnpm exec vue-tsc --noEmit -p modules/saas-runtime-ui/tsconfig.json --pretty false` 修改前后均为 441 项诊断，忽略行号位移后完全一致，本次页面/API/工具无诊断。
- 剩余验证风险：`pnpm --filter saas-runtime-ui build` 已转换 10,066 个模块，仍被既有 `views/device/Template/Save/index.vue` 从 `@device-manager-ui/assets` 导入不存在的 `device` 阻断；完整构建未通过。清理既有类型及导出错误后需重跑上述类型检查和构建命令。真实部署的登录态、身份验证弹窗及浏览器布局尚未联调，可通过 `pnpm dev` 在无 code、有 code 和无权限账号下复核；本次组件渲染测试隔离了基础 UI 和请求边界，不能替代部署验证。

## 三列分组展示调整

- 目标：将当前项目运行时的资源卡片改为截图中的“基础服务 / AI服务 / 智能体服务”三列用量列表，每个指标显示图标、名称、用量/上限、进度与中文说明；桌面各列独立滚动，窄屏收为单列。
- 范围与归属：仅 `runtime-ui/modules/saas-runtime-ui/views/system/ProjectUsage/`、相邻 `locales/lang/{zh,en}.json`、现有测试和本说明。继续使用 `api/projectUsage.ts` 的 `GET /tenant/me/limits`，保留页面加载、重试、项目切换竞态保护以及现有“扩容 → 联系技术支持”弹窗。
- 不做：不再修改 `ui/`；不改后端、菜单、权限、配额口径或扩容业务；不添加虚构资源和用量。
- 实施：核对运行时资源与指标的稳定标识；将接口结果按指标展平并分配到三组，无法识别的资源仍可见；优先用接口中文说明，已知资源缺少说明时使用本模块国际化文案；复用现有格式化、`AIcon`、进度条、空态和支持弹窗，完成响应式样式与针对性测试。
- 风险与待确认：`/tenant/me/limits` 当前映射只有资源 `id/name` 和指标 `id/name/usage/limit`，没有明确的三组分类字段；分类需要依赖可核验的资源标识，未知项不可静默丢弃。截图中的“扩容”可直接沿用当前支持弹窗，不是运营端的购买抽屉。
- 验证：`node --test modules/saas-runtime-ui/tests/project-usage.test.mjs`、SFC/国际化检查、定向类型诊断、`pnpm --filter saas-runtime-ui build`、桌面与窄屏布局和滚动、扩容弹窗、`git diff --check`；既有构建阻塞和真实登录联调风险在本节回填。

实现落点：`resourceUsageGroups.ts` 按指标分组，智能体资源中的成本、并发和会话指标拆成独立行；`projectUsage.mapper.ts` 保留接口指标说明；`ResourceUsageSection.vue` 承载三列独立滚动和响应式布局，`ResourceUsageCard.vue` 改为紧凑指标行。三列是首屏唯一主内容，无额外筛选、详情或编辑壳；未知资源保留在按需出现的“其他服务”。每行“扩容”仍打开同一个技术支持弹窗。

验证结果：专项测试 10/10 通过；三个变更 Vue 文件 SFC 脚本/模板编译通过且均低于 300 行；中英文 `ProjectUsage.*` 键一致，`git diff --check` 通过。`vue-tsc` 的全模块检查存在既有错误，定向筛查本次页面/API 文件无诊断。`pnpm --filter saas-runtime-ui build` 转换 10,026 个模块后仍被 `views/device/Template/Save/index.vue` 从 `@device-manager-ui/assets` 导入 `device` 的错误阻断，完整构建无通过证据。用户反馈已在登录态浏览器验证页面、窄屏与实际接口正常，并提供桌面三列分组及非零用量截图；此项为用户验收反馈，非自动化浏览器测试。

交付：功能提交 `0ae7710`；[PR #51](https://github.com/jetlinks-v2/saas-runtime-ui/pull/51) 指向 `2.12-uat-next`，已按用户确认转为 ready for review，评审时仍需关注完整构建风险。

## 目标

项目运行时的“配置与用量”菜单改为展示当前项目已开通资源的额度与实际用量，数据来自运行时接口：

```text
GET /tenant/me/limits
```

页面不再请求 SaaS 控制台的 `/console/project/{projectId}/services` 和
`/console/project/{projectId}/service/runtime`，也不展示服务、套餐和规格卡片。
每个指标行提供“扩容”入口，点击后展示联系技术支持的二维码弹窗；当前不新增扩容接口。

## 影响范围

- 前端 owning module：`runtime-ui/modules/saas-runtime-ui`
- API：`api/projectUsage.ts`
- 页面数据流：`views/system/ProjectUsage/`
- 测试：`tests/project-usage.test.mjs`
- 后端：不修改 `modules/jetlinks-saas`、`modules/jetlinks-tenant`

## 页面契约

- 请求依赖当前项目运行时上下文，不传 `projectId`。
- 页面不依赖 `project_{code}` 本地项目存储；仅在项目运行态且具有有效 code 时调用
  `/tenant/me/limits`，重试同样遵循此条件。没有 code 时展示正常缺省态，HTTP 403
  单独展示权限提示，均不提供重试和扩容入口。
- `/tenant/me/limits` 直接返回资源数组，不使用 `{ success, result }` 包装。
- 页面按基础服务、AI服务、智能体服务三列展示资源指标、usage、limit 和使用率，未知资源保留在“其他服务”。
- `limit = -1` 表示不限量；无有效上限时不计算最高使用率。
- 扩容动作由 `ResourceUsageCard` 指标行向资源区派发，支持弹窗由
  `ResourceExpansionSupportModal` 在页面级单例承载。
- 保留加载、错误、空数据、重试和项目切换竞态保护。

## 验证

```text
cd runtime-ui
node --test modules/saas-runtime-ui/tests/project-usage.test.mjs
pnpm --filter saas-runtime-ui build
```

原始功能验证结果（分组调整前）：

- `node --test modules/saas-runtime-ui/tests/project-usage.test.mjs`：通过，9 个测试全部通过，包含扩容入口到支持弹窗的页面契约检查。
- `vue-tsc --noEmit -p modules/saas-runtime-ui/tsconfig.json`：工作区存在大量既有类型错误；筛选 `ProjectUsage`、`projectUsage` 和 `ResourceUsage` 后无本次目标文件错误。
- `pnpm --filter saas-runtime-ui build`：被既有页面 `views/device/Template/Save/index.vue` 对
  `@device-manager-ui/assets` 的缺失 `device` 导出引用阻塞，未发现指向本次资源用量页面的构建错误。
