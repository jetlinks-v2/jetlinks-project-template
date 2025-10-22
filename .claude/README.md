# Claude Code 开发规则

本目录包含了专门为 Claude Code 优化的 JetLinks 项目开发规则。

## 📁 文件结构

```
.claude/
├── README.md              # 本说明文件
├── config.json            # Claude Code 配置文件
├── rules.md               # 基础开发规则（必读）
├── advanced-rules.md      # 高级开发规则和工具使用指南
├── crud-generation-rules.md  # CRUD 代码生成规范（重要修正）
└── ../.prompt/            # 原有的详细规则文件
    ├── module-list.md
    ├── module-creation-rules.md
    ├── common-crud-rules.md
    ├── advanced-crud-rules.md
    ├── cross-service-call-rules.md
    ├── realtime-subscription-rules.md
    ├── event-driven-rules.md
    └── module-reference.md
```

## 🚀 快速开始

### 对于 Claude Code

1. **首先阅读 `rules.md`** - 了解基本开发流程和原则
2. **根据任务类型选择详细规则** - 查看相应的 `.prompt/*.md` 文件
3. **参考 `advanced-rules.md`** - 学习具体的工具使用技巧

### 核心原则

- 🔴 **代码优先**：始终基于现有代码生成，不要凭空推测
- 🔍 **搜索先行**：使用 Glob/Grep 工具搜索现有实现
- ✅ **验证为本**：确保所有类和方法都真实存在
- 📋 **遵循规范**：严格按照规则中的模板和命名规范

## 📖 规则使用指南

### 任务类型判断

| 用户需求       | 使用规则                                     | 说明                           |
|------------|------------------------------------------|------------------------------|
| 创建新模块      | `.prompt/module-creation-rules.md`       | 完整的 Maven 模块创建               |
| 添加 CRUD 功能 | `.prompt/common-crud-rules.md`           | Entity/Service/Controller 开发 |
| 跨服务调用      | `.prompt/cross-service-call-rules.md`    | 命令模式 RPC 调用                  |
| 实时数据订阅     | `.prompt/realtime-subscription-rules.md` | @Subscribe 注解使用              |
| 事件驱动开发     | `.prompt/event-driven-rules.md`          | @EventListener 使用            |
| 查看可用模块     | `.prompt/module-list.md`                 | 模块列表和 Maven 依赖               |
| 模块选择决策     | `.prompt/module-reference.md`            | 模块引入 vs 跨服务调用                |

### Claude Code 工具使用

```bash
# 搜索文件
Glob: "modules/**/*Service.java"

# 搜索内容
Grep: pattern="GenericReactiveCrudService" glob="modules/**/*.java"

# 读取规则文件
Read: ".prompt/common-crud-rules.md"

# 使用 Task 工具进行复杂搜索
Task: 搜索现有模块中的响应式服务实现示例
```

## ⚠️ 重要提醒

1. **不要跳过搜索步骤** - 这是保证代码质量���关键
2. **不要凭记忆生成代码** - 项目结构可能已变化
3. **不要忽略验证步骤** - 确保代码可以实际运行
4. **遵循命名规范** - 保持代码风格一致

## 🔄 从 Cursor 迁移

如果你之前使用 Cursor，这些规则完全兼容原有的 `.cursor/rules/` 规则，但针对 Claude Code 进行了优化：

- 增加了 Claude Code 工具使用指南
- 强化了代码优先原则
- 提供了具体的搜索命令示例
- 优化了工作流程描述

## 📞 问题反馈

如发现规则不完善或需要补充的内容，请及时更新相关文件。