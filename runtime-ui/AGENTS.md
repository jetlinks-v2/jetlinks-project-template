# Runtime UI Agent Guide

先读取根目录 `../AGENTS.md`。本目录是脚手架自有的 pnpm 工作区，配置和业务代码由当前仓库维护。

- `jetlinks-web-core/`：平台前端壳子模块，提供登录、账户、路由、菜单、权限及公共组件。
- `modules/authentication-manager-ui/`、`modules/device-manager-ui/`：SaaS 扩展所需的标准 UI 子模块。
- `modules/rule-engine-manager-ui/`、`modules/edge-master-ui/`：规则引擎与边缘网关标准 UI 子模块。
- `modules/saas-runtime-ui/`：从上游指定目录引入的 SaaS 扩展源码，来源见其 `UPSTREAM.md`。不导入上游整个聚合工作区。
- 新业务页面放在 `modules/<业务名>-ui/`，使用平台模块入口、菜单、权限和国际化机制。不要为单个业务修改标准模块私有实现。
- 开发使用 `pnpm dev`，后端代理可使用 `pnpm dev:proxy http://localhost:8844`；构建使用 `pnpm build`，类型检查使用 `pnpm typecheck`。
- 进入子模块前检查其中适用的规则，修改前端时使用共享 `jetlinks-web`；配置、模块锁定和文档在一个阶段完成后集中验证。
