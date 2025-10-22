# JetLinks 开发规则库 - Claude Code 版本

开发相关问题请遵循以下规则：

## 🔍 任务类型识别

当用户请求创建或修改 JetLinks 代码时，首先判断任务类型：

1. **不知道有哪些模块** → 先查看 `.prompt/module-list.md`
2. **创建新模块** → 使用 `.prompt/module-creation-rules.md`
3. **在现有模块中添加基础 CRUD 功能** → 使用 `.prompt/common-crud-rules.md` **和** `.claude/crud-generation-rules.md`
4. **CRUD 进阶使用**（事件驱动、复杂查询、编程模式选择） → 使用 `.prompt/advanced-crud-rules.md`
5. **不确定是模块引入还是跨服务调用** → 先查看 `.prompt/module-reference.md`
6. **跨服务调用**（查询设备、触发告警等） → 使用 `.prompt/cross-service-call-rules.md`
7. **订阅实时数据** → 使用 `.prompt/realtime-subscription-rules.md`
8. **监听实体事件** → 使用 `.prompt/event-driven-rules.md`

## 🔴 核心原则：代码优先

**最重要的原则**：
- ❌ 禁止：根据文档描述凭空推测 API 的使用方式
- ✅ 必须：先搜索项目中的现有用例，阅读实际代码
- ✅ 必须：验证所有类和方法都真实存在
- ✅ 必须：基于真实代码调整生成新代码

**工作流程**：
1. 搜索相关功能的现有实现
2. 阅读找到的真实代码示例
3. 验证类存在性和方法签名
4. 参考真实代码生成新代码
5. 说明代码参考了哪个现有实现

## 🛠️ 搜索命令速查表

在生成任何代码前，使用以下命令搜索现有实现：

| 功能场景 | 搜索模式 | 目的 |
|---------|---------|------|
| 跨服务调用 | `CommandSupportManagerProviders` | 查找命令调用示例 |
| | `@CommandService` | 查找命令服务定义 |
| | `QueryByIdCommand` | 查找查询命令用法 |
| 订阅消息 | `@Subscribe` | 查找订阅注解用法 |
| | `EventBus.subscribe` | 查找编程式订阅 |
| | `device/message` | 查找设备消息订阅 |
| 监听事件 | `@EventListener` | 查找事件监听器 |
| | `EntityModifyEvent` | 查找实体修改事件 |
| | `event.async()` | 查找响应式事件处理 |
| CRUD 实现 | `GenericReactiveCrudService` | 查找响应式服务示例 |
| | `AssetsHolderCrudController` | 查找控制器示例 |
| | `@Table` | 查找实体类定义 |
| 权限控制 | `@Authorize` | 查找权限注解用法 |
| | `@AssetsController` | 查找资产权限控制 |

## 📋 代码生成要求

1. **读取完整规则**：使用 Read 工具读取完整的规则文件
2. **严格遵循规则**��使用规则中的代码模板，保持命名规范一致
3. **生成完整代码**：包含完整的导入语句、必需的注解和配置
4. **验证完整性**：使用规则文件中的检查清单验证

## 🎯 命名规范

- 模块名: kebab-case (template-manager)
- 包名: camelCase (org.jetlinks.pro.template)
- 类名: PascalCase (TemplateManagerConfiguration)
- 变量名: camelCase (templateService)

## ⚠️ 重要提醒

你的优势是搜索和理解，而不是凭空创造。始终基于现有代码生成新实现。