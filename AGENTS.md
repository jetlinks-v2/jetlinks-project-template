# JetLinks Project Template Agent Guide

本仓库不再内置本地 JetLinks Codex skills。

处理 JetLinks 二开任务时，统一使用共享技能仓库 `jetlinks-develop-skills`：

- `https://github.com/jetlinks/jetlinks-develop-skills`

## Shared Skills First

1. 先检查当前会话是否已经安装并可用：
   - `jetlinks-router`
   - 或当前任务明确需要的 `jetlinks-*` focused skill
2. 如果任务边界还不清楚、同时跨多个场景，优先先用 `jetlinks-router`，再切到最少必要的 focused skill。
3. 如果任务已经明显对应某个 focused skill，优先直接使用该 skill，而不是退回到无边界的大范围搜索。
4. 如果当前环境还没有这些 skill，不要继续在本仓库补本地 skill；先尝试使用 `$skill-installer` 自动安装共享 skill。
5. 自动安装时，优先先装 `jetlinks-router`，再按当前任务补充需要的 `jetlinks-*` focused skill。
6. 只有在 `$skill-installer` 不可用、自动安装失败或当前会话无法加载新 skill 时，才向用户说明阻塞；必要时提示重启 Codex 以加载新安装的 skill。
7. 本文件只补充当前仓库特有的目录边界、仓库结构、文档和交付约束；JetLinks 通用方法以共享 skill 本身为准，不在这里重复维护一套平行工作流。

## Skill Routing
优先按场景切到最少必要的 shared skill：

- 不确定任务归类，或同时跨多个场景：`jetlinks-router`
- 协议包、设备接入、编解码、认证链路：`jetlinks-protocol`
- 判断模块落点、聚合结构或是否需要新模块：`jetlinks-routing`
- 标准 CRUD、复杂查询、批量处理：`jetlinks-crud`
- 跨模块依赖、命令服务、代理、远程调用：`jetlinks-boundary`
- 事件监听、Topic 订阅、消息处理：`jetlinks-events`
- 响应式链路、非阻塞实现：`jetlinks-reactive`
- 代码规范、命名、注解、导入、i18n：`jetlinks-conventions`
- 运营端或运行时前端页面实现：`jetlinks-web`
- 前端页面壳层/风格先选型，再实现：`jetlinks-web-style`
- 提交、测试证据、PR 文案与交付：`jetlinks-delivery`
- 判断是否值得沉淀经验、回写 prompt 或 skill：`jetlinks-capture`
## Workspace Structure Snapshot

当前仓库是二次开发脚手架，根目录下的主要边界如下：

- `modules/`：独立应用的业务模块；`modules/examples/` 提供 CRUD、命令、事件、规则引擎、MVC 等参考。
- `modules/examples/mvc-service/`：独立 MVC 业务微服务启动示例；新应用应建立自己的启动模块并只装配所需业务能力。
- `bootstrap/`：平台单体装配参考，不作为二开业务的默认部署边界。
- `microservices/`：平台网关、认证、设备、通知等标准服务的启动参考，不在其中直接添加二开业务。
- `runtime-ui/`：脚手架自有的 pnpm 前端工作区，维护依赖、环境、TypeScript 和构建配置；前端壳、认证、设备、规则引擎与边缘网关 UI 是根 `.gitmodules` 登记的独立子模块，具体清单见 README。
- `runtime-ui/modules/saas-runtime-ui/`：从上游指定目录引入的 SaaS 运行时扩展源码，来源见其中的 `UPSTREAM.md`；不通过聚合仓库引入整套标准业务 UI。
- 根 `pom.xml`：应用版本、平台 BOM、构建插件与启动 profile 的统一入口。平台版本通过 `jetlinks.pro.version` 管理，不绑定到应用自己的版本。
- 根 `README.md`：初始化、构建、SaaS profile、前后端开发边界及 Agent 入口说明。

额外约束：

- 子模块是当前 workspace 的真实代码边界，进入前检查其已有规则与文档；不因为是 submodule 就忽略。
- 使用锁定提交初始化子模块，不无差别执行 `git submodule update --remote` 推进所有仓库。
- 标准模块源码不在后端脚手架内时，读取依赖源码、公开 API/命令契约或对应仓库确认事实，不复制一套标准实现。
- `target/`、`dist/`、`node_modules/`、运行数据及日志不作为优先业务代码入口。

## Repo Scope Routing

- 后端二开优先落在 `modules/` 和应用自己的独立启动服务，不直接扩展标准服务的内部实体或字段。
- 不为设备等标准对象的业务扩展引入 `device-manager` 等实现模块；业务信息使用应用自己的关联表，通过标准对象 ID 建立关联，标准数据及操作通过平台公开命令获取，保留身份和权限上下文。
- 数据权限按平台统一机制实现：业务实体继承 `ExtendableEntity`，通过 `@Asset` 声明资产类型，并配套 `AssetsHolder` 控制器、查询或操作权限校验；不手写平行的租户/部门/创建人过滤体系。
- 项目内部前端只接入 `runtime-ui/`；新增业务放在 `runtime-ui/modules/` 的独立业务模块，复用平台路由、菜单、权限及公共组件，标准 UI 按实际依赖按需引入。
- 前端菜单、按钮的 `permissions[].permission` / `actions` 与后端资源及操作标识一致；前端显隐不能替代后端功能权限和资产数据权限。
- 平台启动参考中的标准模块装配不是业务应用的依赖清单；SaaS 运行时与运营控制端分属不同边界，业务服务不引入 `saas-manager`。

## Search Baseline

- 搜索默认排除 `node_modules/`、`dist/`、`target/`、`coverage/`、`.idea/`、`.git/`。
- 不把三方依赖里的 `docs/`、示例代码、生成产物当作业务文档或业务实现。
- 不要忽略 `.gitmodules` 定义的子模块根目录；它们同样属于当前仓库事实来源。
- 先看目标模块已有的 `README.md`、`docs/plans/README.md`、功能说明或索引文档，再看相近历史文档，最后回到当前代码确认真实实现。
- 历史文档只用于加速理解；当前代码、配置、资源目录和相邻模块才是最终事实来源。

## Task Level

为避免小任务和大任务采用同一套文档重量，先判断任务级别：

- `S`：答疑、审查、排障、单点小修、低风险小改动
- `M`：单模块功能开发或重构，多文件改动但边界清晰
- `L`：跨模块、前后端联动、接口/事件/数据结构变更、架构调整

## Plan-First Execution Gate

在开始实现前，先判断这次任务是否需要进入 `plan-first` 流程；不是所有任务都必须停下来等确认，但复杂任务默认不能直接开改。

### 1. 哪些任务必须先 plan 再等确认

以下任一情况成立时，先输出计划并等待用户确认，再继续实施：

- `M` / `L` 级任务
- 需求仍在变化，或用户明确要求“先看方案 / 先出计划”
- 一次需求涉及多个子任务、多个模块、前后端联动或分阶段交付
- 存在多种实现方案，或会影响接口、数据结构、事件、权限、配置、菜单、路由、兼容性边界

### 2. 哪些任务可简短计划后直接做

- `S` 级任务
- 已明确范围的低风险修复
- 纯文档小改、纯格式调整、纯答疑 / 审查

但如果 `S` 级任务实际暴露出方案分歧、风险升级或需求变化，也要立即切回 `plan-first`。

### 3. 计划最少包含

1. 目标
2. 影响范围与 owning module
3. 明确不做什么
4. 实施步骤
5. 风险 / 待确认点
6. 验证方式

### 4. 执行要求

- `plan-first` 任务默认先把精简计划写入本次要更新的原始文档；现有文档无法承载时，才新增精简专题文档。
- 写完计划后，先把关键步骤、边界和待确认点摘要发给用户，得到确认后再改代码 / 配置 / 文档实现。
- 如果当前工具支持系统级 Plan mode，优先使用；如果不支持，也必须按同样的行为手动执行。
- 如果需求变更影响范围、方案、步骤顺序或风险判断，先更新同一份计划，再次确认后继续实施。

## Documentation Update Policy

在完成 skill 检查、边界确认和任务分级后，先判断本次任务是否真的需要留下可复用的变更文档；不是每个操作都要单独写一篇任务记录。

### README 文档边界

- README 只保留长期有效的模块职责、使用方式、行为契约和文档入口，不记任务流水。
- 测试次数与结果、单次验证命令、复现过程、临时计划、提交号、PR 地址和交付进度放在 PR 描述或 CI 中，不写入 README。
- 下文关于计划和验证结果、commit hash、PR 地址的回填要求不适用于 README；不得为满足留痕要求向 README 追加这些内容。

### 1. 什么时候必须更新文档

仅当出现以下任一情况时，才要求同步更新文档：

- 代码、配置、接口、数据结构、页面交互、枚举/字典或业务规则发生实际变更
- 仓库结构、模块边界、开发工作流规则发生变更
- 当前任务会影响后续协作者判断落点、兼容策略、验证方式或交付入口

以下情况默认不在 `docs/plans/` 留文档：

- 纯答疑、纯审查、纯排障结论、纯同步 / merge / rebase、仅提交 PR、无行为变化的格式化或注释调整
- 只服务当前会话、后续没有复用价值的临时分析

### 2. 优先更新原始文档

优先更新离改动最近、已经存在的原始文档：

- 模块 `README.md`、功能说明、接口说明、`docs/plans/README.md`
- 仓库级规则优先更新根 `AGENTS.md`、`README.md`、`CLAUDE.md`、`ai-prompt.md`
- 只有现有文档无法承载，且该变更对后续任务有长期复用价值时，才在对应 `docs/plans/` 新增一个精简专题文档

不要默认按日期创建新的任务文档，也不要为了“留痕”重复复制同一背景。

### 3. 先在目标文档写精简计划，再实现

如果判断本次属于需要文档同步的代码 / 业务变更：

- 先在将要更新的原始文档中补充精简计划、影响范围或实现入口，再开始改代码
- 需求、模块边界或方案变化后，直接回写同一文档，不再额外生成“变更说明 2/3”
- 文档与实现必须保持一致，文档更新应与实现变更放在同一批次提交中

### 4. 按任务级别控制文档重量

- `S`：默认只更新已有原始文档中的一小段说明；无行为变化时可不写
- `M`：更新 owning module 的原始文档；若没有合适落点，再新增一个精简专题文档
- `L`：除 owning doc 外，同步更新对应的 `docs/plans/README.md` 索引；只有形成可复用长期决策时，才补单独决策文档

### 5. `docs/plans` 的角色

- `docs/plans/README.md` 默认作为索引和入口，不是逐任务流水账
- 不默认维护 `active/`、`archive/`、`adr/` 三段式任务流
- 现有历史文档可作为背景参考，但新任务不要机械复用该结构
- 只有跨模块、长期复用且无法放入原始文档的内容，才新增独立专题文档

### 6. 文档内容最少包含

1. 当前结论或目标
2. 影响范围与 owning module
3. 关键代码引用（直接写仓库内路径，必要时补类、方法、页面入口）
4. 验证结果或待验证风险
5. Git commit hash、Pull Request 地址（已有时再补；没有则不要为此单独新建文档）

### 7. 完成后只回填到同一文档

- 若本次已经更新文档，完成后只在同一文档补充验证结果、关键代码落点、commit hash 和 PR 地址
- 不要为了回填交付记录再新建独立文档或归档流水
- 如果 commit 或 PR 还没生成，可先记 `pending`，后续补到同一文档

### 8. `docs/plans` 不等于知识沉淀

- `docs/plans` 主要是仓库级索引和少量变更说明入口。
- 它不替代 `.ai/` 下的经验沉淀，也不等于 shared skill 更新。
- 只有任务完成后确实产出了稳定、可复用、非显然的经验，才切到 `jetlinks-capture`。
- 使用 `jetlinks-capture` 时，先给出是否值得沉淀的建议；只有用户确认后，再决定写入本地 `.ai/`、playbook，还是回写 `jetlinks-develop-skills`。

## Frontend Scope Guardrails

本仓库的前端边界是 `runtime-ui/`，它是脚手架自有目录，可独立构建部署。配置与业务源码由当前仓库维护，前端壳和标准 UI 通过各自子模块复用；不要把整个 `cloud.jetlinks.ui` 或 `saas-runtime-ui` 聚合仓库作为前端入口。

1. 在前端子模块内部先读取适用的 `AGENTS.md`、`CLAUDE.md`、模块说明和导出，按目标业务模块收敛搜索。
2. 业务页面及扩展放入独立业务模块，沿平台模块注册、菜单、路由、权限声明和国际化约定实现。
3. 未经用户要求，不修改前端壳、共享组件或标准模块私有实现来适配一个业务场景。
4. 新页面、页面壳层或主要交互先使用 `jetlinks-web-style` 选型，再使用 `jetlinks-web` 实现；局部字段、样式和文案调整按 skill 的局部调整规则处理。
5. 视觉打磨需先确认本地样式锚点，再结合 `frontend-design`，最终以工作区组件及 Ant Design 风格为准。
6. `microservices/ui-service` 与 `bootstrap` 的 `ui` profile 是历史 Maven 资源装配参考，不代替 `runtime-ui` 的二开入口。

## Delivery And Verification

- 页面与交互验证默认直接使用 Codex 内置浏览器（`@浏览器`）；仅在内置浏览器无法完成所需验证时，才使用外部浏览器。
- 验证方式、命令和交付格式以当前使用的 focused skill 与 `jetlinks-delivery` 为准；本仓库只额外要求把必要结果回填到本次更新的原始文档或专题文档，不要为验证回填单独生成流水文档。
- 后端任务至少记录编译/测试结果，以及关键接口、事件、数据结构影响。
- 前端任务至少记录 lint/typecheck/build 结果，以及关键页面或交互验证结果。
- 全栈任务除前后端各自验证外，还要记录接口契约、字段命名、枚举/字典和值域是否一致。
- 如果当前环境无法完成某项验证，必须在本次更新的文档中写明待执行命令、未验证项和剩余风险。
- 如果任务涉及 commit、push、PR、测试证据或交付文案，先切到 `jetlinks-delivery`，再把结果补回同一文档。

## Auto Install Prompt

优先引导智能体直接执行自动安装，而不是只提示用户手动安装。

推荐提示词：

```text
Use $skill-installer to install the required JetLinks skills from https://github.com/jetlinks/jetlinks-develop-skills, starting with jetlinks-router.
```

如需给出更明确的单技能安装指令，优先使用：

```text
Use $skill-installer to install skill from https://github.com/jetlinks/jetlinks-develop-skills/tree/master/jetlinks-router
```

如果当前任务只需要某个 focused skill，则将上面的 `jetlinks-router` 替换为对应目录名。

## Local References

- `README.md`：脚手架结构、子模块初始化、构建、SaaS 及开发边界。
- `AGENTS.md`：本仓库统一规则来源；进入子模块时继续检查其中适用的规则。
- `CLAUDE.md`：Claude Code 桥接入口，导入 `AGENTS.md`。
- `ai-prompt.md`：共享 JetLinks skills 的兼容入口。
- `.cursor/rules/develop.mdc`：Cursor 的统一规则桥接入口。
- 重复的 `.prompt/*.md` 和旧 Claude 规则副本已移除，不再作为本地规则来源。
- 项目事实以当前代码、`pom.xml`、`.gitmodules`、资源及公开依赖契约为准。
