# 个人令牌编辑保存

## 目标与接口语义

运行时个人中心的令牌编辑提供两种保存方式；新增令牌继续使用现有生成、展示与复制流程。

当前后端接口为 `POST /personal/token/me/_save`，查询参数含义如下：

| 参数 | 后端实际行为 | 按钮与反馈 |
| --- | --- | --- |
| `revokeAccessTokens=true`（默认） | 轮换凭证，旧访问令牌失效，返回新令牌 | 刷新令牌保存，展示新令牌供复制 |
| `revokeAccessTokens=false` | 保留凭证并清理鉴权缓存，原访问令牌使用最新权限 | 不刷新令牌保存，提示“保存成功，权限已在原令牌生效” |

采用上表的实际接口语义：刷新保存生成新令牌，不刷新保存让原令牌使用最新权限。

## 范围与实现入口

Owning module：`runtime-ui/modules/saas-runtime-ui`。仅修改本模块个人中心、相邻中英文资源及运行时 core 的现有保存 API。

1. `runtime-ui/jetlinks-web-core/src/api/account/center.ts`：`savePersonalToken_api` 的可选第二参数承载 `{ revokeAccessTokens: boolean }` 查询参数，入参和响应有类型，其他调用方继续使用后端默认值。
2. `views/PersonCenter/components/PersonalToken/components/TokenDialog.vue`：编辑页脚提供“不刷新令牌保存”和“刷新令牌保存”，悬停提示分别说明原令牌权限生效与原令牌失效；新增/查看模式保持原流程。
3. 同目录 `useTokenDialog.ts`：编辑回填保留 `id`，确保更新原记录；不刷新保存通过 `save` 事件的第三参数 `false` 通知列表，刷新保存仍先展示新令牌供复制，关闭成功弹窗后通知列表。校验和请求期间锁定两种提交及取消操作，失败时保留输入、提示错误并释放加载状态。
4. 列表入口 `views/PersonCenter/components/PersonalToken/index.vue`：按保存方式显示成功提示、刷新列表并关闭编辑弹窗；`locales/lang/{zh,en}.json` 同步按钮、说明、成功和失败提示。

不改运营端 `ui/`、后端语义、菜单、路由、令牌列表样式或无关已有逻辑。权限选择器的后续修复见“新增与编辑的权限操作显示”。

## 事实来源与风险

- `modules/authentication-manager/src/main/java/org/jetlinks/pro/auth/web/PersonalTokenController.java#saveToken`：参数默认 `true`，接口说明明确 `false` 仅更新配置和权限。
- `modules/authentication-manager/src/main/java/org/jetlinks/pro/auth/token/PersonalTokenService.java#prepareToken`：`true` 轮换凭证，`false` 保留凭证；保存后清理缓存。
- `modules/authentication-manager/src/test/java/org/jetlinks/pro/auth/web/PersonalTokenControllerTest.java#testEditWithoutRevokingShouldKeepOldAccessTokenAndRefreshScope`：`false` 时旧令牌仍可用并反映最新权限。
- `useTokenDialog.ts` 保留编辑记录的 `id`；后端依赖该字段区分新增与编辑，原令牌权限更新必须命中原记录。
- 共享 API 另有 runtime core 调用方，可选查询参数保留现有调用的默认语义。

## 验证

在 `runtime-ui/` 执行：

```text
node --test modules/saas-runtime-ui/tests/personal-token-save.test.mjs
pnpm exec vue-tsc --noEmit -p modules/saas-runtime-ui/tsconfig.json
pnpm --filter saas-runtime-ui build
git diff --check -- modules/saas-runtime-ui
git -C jetlinks-web-core diff --check -- src/api/account/center.ts
```

- 专项测试：8/8 通过，使用真实 Vue 响应式与保存 API 封装、请求替身，覆盖两种查询参数、原记录 `id`、成功事件和新令牌展示、新增/查看模式、校验失败、接口/网络失败重试、校验与请求阶段重复提交及取消防护、中英文资源同步。
- SFC：变更的弹窗和列表脚本、模板编译通过，均小于 300 行；`git diff --check` 通过。
- Lint：当前工作区未安装 ESLint，`package.json` 未定义 lint 脚本，未执行 lint。
- Typecheck：完整模块检查未通过。既有诊断包含弹窗表单规则、Select 回调和日期组件值/回调类型，以及其他页面、模块别名和共享请求包类型问题；本次保存 hook 无诊断。
- Build：此前转换 10,033 个模块后被既有 `views/device/Template/Save/index.vue` 从 `@device-manager-ui/assets` 导入缺失的 `device` 阻断，完整构建未通过；代码恢复后未重新执行完整构建。
- 保存流程的真实提交与鉴权联调尚未执行。验收需检查编辑请求带原记录 `id`，刷新保存后新令牌可用且旧令牌失效，不刷新保存后原令牌反映最新权限，并确认列表刷新与成功提示。

## 权限搜索 GET 参数编码

目标：修复输入“设备”等关键字时 `GET /permission/_query/for-grant` 返回 500 的问题。修复范围为运行时共享 API `runtime-ui/jetlinks-web-core/src/api/system/permission.ts`；运营端、后端接口、权限过滤规则和保存方式不在本次修复范围内。

当前结构化条件经 Axios 默认编码得到 `terms[0][column]=name$like`，Spring `QueryParamEntity` 数据绑定将其视为 Map 下标并抛出 `InvalidPropertyException`。框架 `Term#setColumn` 能正确解析 `name$like`；字段路径应为 `terms[0].column`、`terms[0].value`。

实施：复用 `runtime-ui/jetlinks-web-core/src/utils/encodeQuery.ts#paramsEncodeQuery` 展平 `terms`、`sorts`，保留 `paging` 等其他参数；共享 API 的 SaaS 个人中心和 core 个人中心调用方继续传递原结构化查询对象。

验证：

- 使用当前模块的真实 Spring `DataBinder` 和 `QueryParamEntity` 复现方括号字段路径异常；点号字段路径可正确绑定 `column=name`、`termType=like`、`value=%设备%`。
- `node --test modules/saas-runtime-ui/tests/personal-token-permission-query.test.mjs`：3/3 通过。测试使用真实查询编码工具与 Axios URL 序列化，覆盖中文关键字、清空条件、嵌套条件与排序、根参数保留及输入对象不被修改。
- 登录态浏览器联调通过：在个人中心编辑“测试”令牌，搜索“设备”后显示设备实例、设备产品等 7 项相关权限；清空后恢复完整权限列表。验证期间未更改权限或提交保存。
- `git -C jetlinks-web-core diff --check -- src/api/system/permission.ts` 通过；完整 lint/typecheck/build 的现有阻塞见上方“验证”，尚无完整构建通过证据。

## 新增与编辑的权限操作显示

诊断：原 `views/PersonCenter/components/PersonalToken/components/PermissionList.vue#getCheckedOptions` 仅渲染 `record.checkedList` 中的操作。新增通过 `fullaccess` 预选当前全部操作，编辑通过 `PermissionSelector.vue` 回填原令牌的 `scope.permissions`；后续增加的权限或操作不在原授权中，因而即使查询接口返回完整 `actions`，编辑行仍可能显示 `--`。这两种数据分别表示当前可授权操作和令牌已授权操作。

本次级别为 M，owning module 为 `runtime-ui/modules/saas-runtime-ui`。用户已确认：新增和编辑均默认全选当前接口返回的全部操作，编辑保存同步这份最新全量权限；查看模式仍只显示原令牌已授权的操作。编辑可能增加原令牌未包含的操作或移除已不可授权的操作，该规则已获得确认。

实施步骤：

1. 在现有 `PermissionList.vue` 首次完整查询成功后统一初始化新增、编辑权限，保留当前标签展示与弹窗布局；`PermissionSelector.vue` 直接转发受控值，去掉可能覆盖全量权限的旧授权回填。
2. 完整查询完成前清空新增、编辑的待保存权限，阻止提前保存旧权限；搜索只过滤显示，不改变全量保存范围。查看模式跳过初始化，失败查询可重试。
3. 增加旧令牌缺少 AI 告警、全量权限传递到两种保存请求、新增默认权限、搜索与清空、查看只读及加载失败的回归验证，并回填同一文档。

边界：不改运营端、后端查询与授权规则、两种保存接口语义，也不改 core 个人中心。

诊断验证：在当前登录态个人中心编辑“测试”令牌，搜索“AI告警”可复现操作列为 `--`、已选数量为 587；用户提供的新增截图数量为 590，差值与 AI 告警的 3 项操作一致。仅进行了打开弹窗与搜索操作，未修改或保存实际授权。

实现入口：`PermissionList.vue#handleSearch` 完成首次完整权限初始化并处理查询失败；`PermissionSelector.vue` 使用受控值同步表单；`data.ts#handleData` 生成已授权操作标签并提供权限定义、范围类型，`useTokenDialog.ts` 复用同一范围类型。失败提示同步 `locales/lang/{zh,en}.json`。

验证结果：

- `node --test modules/saas-runtime-ui/tests/personal-token-permission-initialization.test.mjs`：5/5 通过。挂载真实 SFC，覆盖编辑等待完整查询、新增/编辑操作一致、搜索与清空不截断保存范围、两种保存请求提交最新全量权限及原记录 ID、查看原授权/空授权、查询失败重试。
- 原保存测试 8/8、GET 查询编码测试 3/3 通过；共 16 项专项回归测试通过。代码更新后已补回权限查询编码、SaaS 权限映射类型与操作标签、中英文文案，恢复后的 16 项测试再次全部通过；保存按钮、原记录 ID 与全量初始化保留现存实现，未覆盖更新后的其他代码和已有语言资源。
- 此前登录态浏览器：新增与编辑均显示已选择 590 项，搜索 AI 告警均显示查询、保存、删除；编辑清空搜索后恢复完整列表并维持 590 项。恢复后未重新进行浏览器操作，也未提交保存；真实保存及令牌鉴权效果仍按上方验收项联调。
- 新增和编辑的权限 SFC 脚本、模板编译通过，`git diff --check -- modules/saas-runtime-ui` 通过。
- Typecheck：恢复后执行 `pnpm exec vue-tsc --noEmit -p modules/saas-runtime-ui/tsconfig.json` 仍因既有错误未通过；恢复的查询 API、`PermissionList.vue`、`PermissionSelector.vue`、`data.ts` 与保存 hook 无诊断，个人令牌部分仍为弹窗表单规则、Select 回调和日期组件类型的既有诊断。Lint 未配置，完整构建此前的阻塞及待执行命令见上方“验证”，本次未重复完整构建。

## 交付依赖

SaaS 改动基于 `saas-runtime-ui` 的 `2.20`（`74e91e0`），开发分支为 `codex/personal-token-save-permissions-2.20`。

两处共享 API 修复已包含在 [jetlinks-web-core PR #171](https://github.com/jetlinks-v2/jetlinks-web-core/pull/171) 中。本次同步 `runtime-ui/jetlinks-web-core` 引用到该 PR 已推送的 `78c8af67fca3a04c9c670c35e1d7d69fed7902a5`，确保检出 SaaS 分支后可以使用保存参数和权限查询编码；合并 SaaS PR 前需先合并 core PR，并确认该提交可访问。

交付时复用恢复阶段的 16 项专项回归结果：SaaS 源码、测试及两处 API 的相关实现与恢复检查点一致；core 已提交文件的差异仅为未涉及令牌流程的主题 API。目标 `2.20` 基线未漂移，本次未重复完整测试或构建，最终 `git diff --cached --check` 通过。

实现提交：`953b768273b7fdbfdfcb7a1d488753bd427b9c0d`；评审入口：[saas-runtime-ui PR #57](https://github.com/jetlinks-v2/saas-runtime-ui/pull/57)，合并目标为 `2.20`。完整类型检查、构建和真实保存鉴权的剩余验证项见上方“验证”。
