# JetLinks AI 使用入口

当前仓库不再维护本地 JetLinks Codex skills。

从现在开始，统一使用共享技能仓库 `jetlinks-develop-skills`：

- `https://github.com/jetlinks/jetlinks-develop-skills`

## 使用规则

1. 先检查当前会话是否已经具备以下共享 skill：
   - `jetlinks-router`
   - 或当前任务明确需要的 `jetlinks-*` focused skill
2. 如果任务边界不清晰、同时跨多个场景，优先直接使用：
   - `$jetlinks-router`
3. 如果任务属于复杂开发、需求变更、跨模块联动或多子任务拆解，先走 `plan-first`：
   - 先给出目标、范围、不做什么、实施步骤、风险/待确认点和验证方式
   - 先等用户确认，再进入实现
   - 简单低风险小任务可在简短计划后直接实施
4. 如果任务已经明显对应某个 focused skill，优先直接使用该 focused skill，而不是做无边界的大范围搜索。
5. 如果当前环境没有这些 skill，不要继续在本仓库补本地 skill。
   - 应先使用 `$skill-installer` 自动安装共享 skill
   - 优先先安装 `jetlinks-router`
   - 再按当前任务补充需要的 `jetlinks-*` focused skill
6. 只有在 `$skill-installer` 不可用、自动安装失败或当前会话无法加载新 skill 时，才向用户说明阻塞；必要时提示重启 Codex。
7. `AGENTS.md` 是当前仓库的统一规则入口；JetLinks 通用工作流以 shared skills 为准。
8. 若使用 Claude Code，`CLAUDE.md` 只作为桥接文件导入 `AGENTS.md`，避免维护两套相互漂移的规则。

## 常用路由

- 不确定场景或跨多个场景：`$jetlinks-router`
- 协议包、设备接入、编解码、认证链路：`$jetlinks-protocol`
- 找模块或判断落点：`$jetlinks-routing`
- 做 CRUD 或复杂查询：`$jetlinks-crud`
- 处理跨模块边界：`$jetlinks-boundary`
- 处理事件或订阅：`$jetlinks-events`
- 处理响应式链路：`$jetlinks-reactive`
- 处理规范、导入、i18n：`$jetlinks-conventions`
- 做前端页面实现：`$jetlinks-web`
- 先选前端页面风格/壳层：`$jetlinks-web-style`
- 处理提交、测试证据、PR：`$jetlinks-delivery`
- 判断是否值得沉淀经验：`$jetlinks-capture`

## 自动安装提示词

优先引导智能体执行：

```text
Use $skill-installer to install the required JetLinks skills from https://github.com/jetlinks/jetlinks-develop-skills, starting with jetlinks-router.
```

如果需要更明确的单技能安装指令，使用：

```text
Use $skill-installer to install skill from https://github.com/jetlinks/jetlinks-develop-skills/tree/master/jetlinks-router
```

如果当前任务只需要某个 focused skill，则将上面的 `jetlinks-router` 替换为对应目录名。

## 本地资料的角色

- 当前仓库只保留 `ai-prompt.md` 作为本地兼容说明。
- `AGENTS.md` 是当前仓库统一规则来源；`CLAUDE.md` 仅作 Claude Code 桥接文件。
- 重复的 `.prompt/*.md` 已移除，不再代表当前仓库内置规则。
- 在共享 skill 已安装的前提下，如需确认当前项目特有约定，直接读取当前工作区代码、`pom.xml`、资源目录、相邻模块和对应 `docs/plans/`。
