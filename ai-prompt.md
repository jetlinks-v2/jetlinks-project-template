# JetLinks 开发辅助工具 - AI 开发规则库

本目录包含用于辅助 AI 快速、规范地生成 JetLinks 项目代码的完整规则集。

---

## 📚 目录

- [快速开始](#快速开始)
- [规则文件说明](#规则文件说明)
- [AI 使用指南](#ai-使用指南)
- [使用场景决策树](#使用场景决策树)
- [完整示例](#完整示例)
- [快速参考](#快速参考)
- [注解和导入语句参考](#注解和导入语句参考)

---

## 🚀 快速开始

### 对于 AI 助手

当用户请求创建或修改 JetLinks 代码时：

1. **首先判断任务类型**：
    - **不知道有哪些模块** → 先查看 [module-list](.prompt/module-list.md)
    - **不确定注解和包名** → 先查看 [annotations-and-imports-reference](.prompt/annotations-and-imports-reference.md)
    - **配置国际化** → 先查看 [i18n](.prompt/i18n.md)
    - 创建新模块 → 使用 [module-creation-rules](.prompt/module-creation-rules.md)
    - 在现有模块中添加基础 CRUD 功能 → 使用 [common-crud-rules](.prompt/common-crud-rules.md)
    - **CRUD 进阶使用**（事件驱动、复杂查询、编程模式选择） → 使用 [advanced-crud-rules](.prompt/advanced-crud-rules.md)
    - **不确定是模块引入还是跨服务调用** → 先查看 [module-reference](.prompt/module-reference.md)
    - 跨服务调用（查询设备、触发告警等） → 使用 [cross-service-call-rules](.prompt/cross-service-call-rules.md)
    - 订阅实时数据 → 使用 [realtime-subscription-rules](.prompt/realtime-subscription-rules.md)
    - 监听实体事件 → 使用 [event-driven-rules](.prompt/event-driven-rules.md)

2. **读取相应的规则文件**：
   ```
   请使用 read_file 工具读取完整规则，而不是依赖记忆
   规则文件包含最新的代码模板和实际示例
   ```

3. **严格遵循规则**：
    - 使用规则中的代码模板
    - 保持命名规范一致
    - 包含所有必需的注解和配置

4. **生成完整代码**：
    - 不要只生成片段，要生成完整的类
    - 包含必要的导入语句
    - 添加完整的注释和文档

---

## 📖 规则文件说明

### 0. 模块列表速查 - [module-list](.prompt/module-list.md)

**用途**: 快速检索所有可用模块及其 Maven 引入方式

**包含内容**:

- ✅ 所有业务管理模块（10个）
- ✅ 所有基础组件（19个）
- ✅ 所有网络组件（8个）
- ✅ 所有数据存储组件（10个）
- ✅ 所有消息中间件组件（3个）
- ✅ 所有通知组件（7个）
- ✅ AI组件、微服务组件
- ✅ Maven 依赖配置（开箱即用）
- ✅ 简要功能说明
- ✅ 常用组件组合建议

**适用场景**:

- ✨ 不知道项目有哪些可用模块
- ✨ 需要快速查找特定功能的模块
- ✨ 需要复制 Maven 依赖配置
- ✨ 了解模块的基本功能

**何时使用**:

```
用户说："有哪些可用的模块？"
用户说："如何引入XXX模块？"
用户说："XXX功能在哪个模块？"
AI说："让我先查看模块列表，确认有哪些可用模块"
```

---

### 0.1 国际化配置规范 - [i18n](.prompt/i18n.md)

**用途**: 完整的 JetLinks 国际化(i18n)配置规范和开发指南

**包含内容**:

- ✅ 国际化文件位置和命名规范
- ✅ 枚举类国际化配置（完整包名.枚举类名.枚举值）
- ✅ 实体类字段国际化配置（完整包名.实体类名.字段名）
- ✅ **功能权限国际化**（hswebframework.web.system.permission.{权限ID}）
- ✅ **按钮操作国际化**（hswebframework.web.system.action.{操作ID}）
- ✅ 错误消息和提示消息国际化
- ✅ 完整的配置示例和快速参考表
- ✅ AI 代码生成时的国际化规范

**适用场景**:

- ✨ 创建或修改实体类时需要配置国际化
- ✨ 创建枚举类时需要配置枚举值显示名称
- ✨ 创建 Controller 时需要配置功能权限和操作按钮的国际化
- ✨ 不确定国际化配置的格式和规范
- ✨ 需要了解权限和操作的国际化前缀

**何时使用**:

```
用户说："添加国际化配置"
用户说："配置枚举的显示名称"
用户说："如何配置功能权限的国际化？"
用户说："按钮操作的国际化怎么写？"
用户说："创建 Controller 需要配置什么国际化？"
AI说："让我先查看国际化规范，了解正确的配置格式"
```

**核心规范**:

```properties
# 1. 枚举国际化
完整包名.枚举类名.枚举值=显示名称
org.jetlinks.pro.device.enums.DeviceState.online=在线

# 2. 实体字段国际化
完整包名.实体类名.字段名=显示名称
org.jetlinks.pro.device.entity.DeviceInstanceEntity.productName=产品名称

# 3. 功能权限国际化（必须配置，对应 @Resource 的 id）
hswebframework.web.system.permission.{权限ID}=权限名称
hswebframework.web.system.permission.device-product=设备产品

# 4. 按钮操作国际化
hswebframework.web.system.action.{操作ID}=操作名称
hswebframework.web.system.action.query=查询

# 5. 错误消息
error.标识=错误消息
error.device_not_found=设备[{0}]不存在

# 6. 提示消息
message.标识=提示消息
message.operation_success=操作成功
```

---

### 1. 模块创建规则 - [module-creation-rules](.prompt/module-creation-rules.md)

**用途**: 从零创建一个新的业务模块（完整的 Maven 模块）

**包含内容**:

- ✅ 完整的 Maven pom.xml 配置模板
- ✅ 目录结构规范和创建步骤
- ✅ 必需依赖和可选依赖说明（含详细表格）
- ✅ Configuration 配置类模板
- ✅ AutoConfiguration.imports 配置
- ✅ 国际化文件模板
- ✅ 资产类型定义模板（可选）
- ✅ 模块开发检查清单
- ✅ 常见场景示例（简单 CRUD、资产权限、树形结构）
- ✅ 依赖选择指南（8种常见场景）

**适用场景**:

- ✨ 创建新的业务模块（如：template-manager, workflow-manager）
- ✨ 需要完整的 Maven 模块结构
- ✨ 需要独立的 pom.xml 和配置

**何时使用**:

```
用户说："创建一个XXX管理模块"
用户说："新建一个模块用于XXX"
用户说："我需要一个新的业务模块"
```

---

### 2. 跨服务调用规则 - [cross-service-call-rules](.prompt/cross-service-call-rules.md)

**用途**: 使用命令模式进行跨服务调用（RPC）

**包含内容**:

- ✅ 命令模式概述和优势
- ✅ Consumer 端调用方式（响应式、阻塞式、路由）
- ✅ Provider 端服务定义（使用 @CommandService 注解）
- ✅ 常用命令服务速查表
  - 设备相关命令（device, product, protocol, firmware）
  - 告警相关命令（alarm, alarmConfig, alarmHistory, alarmRecord）
  - 通知相关命令（sendNotify）
  - 数据源相关命令
  - 文件服务、认证服务
- ✅ 自定义命令定义
- ✅ 最佳实践（优先使用注解、错误处理、性能优化）
- ✅ 完整示例代码

**适用场景**:

- ✨ 跨模块调用其他服务（不要直接注入 Service）
- ✨ 查询设备信息、产品信息
- ✨ 触发告警、查询告警历史
- ✨ 发送通知消息
- ✨ 微服务间通信
- ✨ 需要集群路由和负载均衡的场景

**何时使用**:

```
用户说："查询设备信息"
用户说："调用其他服务的方法"
用户说："触发告警"
用户说："发送通知"
用户说："跨模块访问数据"
用户说："在A服务中调用B服务的功能"
```

**核心原则**:

```java
// ❌ 不要直接注入其他模块的 Service
@Autowired
private DeviceInstanceService deviceService;

// ✅ 使用命令模式调用
CommandSupportManagerProviders
    .getCommandSupport(SdkServices.deviceService, "device")
    .flatMap(cmd -> cmd.execute(QueryByIdCommand.of(DeviceInfo.class, id)));

// ✅ Provider 端使用 @CommandService 注解定义服务
@Component
@CommandService(id = "myService", name = "我的服务")
public class MyCommandService implements CrudCommandHandler<MyEntity, String> {
    // 自动提供 CRUD 命令
}
```

---

### 3. 实时数据订阅规则 - [realtime-subscription-rules](.prompt/realtime-subscription-rules.md)

**用途**: 使用 @Subscribe 注解和 EventBus 实现实时数据订阅

**包含内容**:

- ✅ Subscribe 注解详解和使用方法
- ✅ Topic 规则和通配符使用
- ✅ 订阅设备消息的各种方式
- ✅ 常见应用场景(4个核心示例)
  - 订阅设备属性上报并更新 Redis
  - 订阅设备上下线并发送通知
  - 订阅设备事件并处理告警
  - 使用 Buffer 处理耗时操作(持久化缓冲)
- ✅ 订阅特性详解(本地、集群、共享订阅等)
- ✅ 性能优化建议
- ✅ 常见问题解答(5个核心问题)
- ✅ 快速参考表

**适用场景**:

- ✨ 订阅设备实时消息(属性上报、事件、上下线等)
- ✨ 实现数据实时同步到 Redis、数据库等
- ✨ 处理设备告警和通知
- ✨ 实时数据流处理和转发
- ✨ 集群环境下的消息订阅

**何时使用**:

```
用户说："订阅设备属性上报数据"
用户说："监听设备上下线消息"
用户说："实时处理设备事件"
用户说："将设备数据同步到Redis"
用户说："如何使用@Subscribe注解"
```

---

### 4. 事件驱动开发规范 - [event-driven-rules](.prompt/event-driven-rules.md)

**用途**: 使用 Spring Event 处理实体增删改查事件实现业务解耦

**包含内容**:

- ✅ 通用CRUD事件监听(响应式和阻塞式)
- ✅ 完整的事件类型说明表
- ✅ event.async() 响应式操作组合
- ✅ @TransactionalEventListener 事务事件
- ✅ 集群事件监听(ClusterEntityEventListener)
- ✅ 实战示例(4个核心场景)
  - 设备激活时同步资产
  - 实体保存时发布到EventBus
  - 用户Token变更通知
  - 获取修改前后的值对比
- ✅ 开启实体事件(@EnableEntityEvent)
- ✅ 注意事项和最佳实践

**适用场景**:

- ✨ 监听实体的增删改查操作
- ✨ 数据变更后触发业务逻辑
- ✨ 实现功能解耦和异步处理
- ✨ 数据同步、日志记录、消息通知
- ✨ 集群环境下的事件同步

**何时使用**:

```
用户说："监听实体保存事件"
用户说："数据修改后发送通知"
用户说："如何使用@EventListener"
用户说："实体删除时清理关联数据"
用户说："使用事件驱动实现业务解耦"
```

---

### 5. 模块参考手册 - [module-reference](.prompt/module-reference.md)

**用途**: 了解项目中所有业务模块的功能和依赖关系

**包含内容**:

- ✅ 所有业务模块（Manager）的详细说明
- ✅ 每个模块的功能、主要服务、命令服务
- ✅ **模块引入 vs 跨服务调用决策指南**
- ✅ 决策流程图和适用场景对比
- ✅ Maven 依赖配置示例
- ✅ 命令服务速查表
- ✅ 常见问题解答

**包含的模块**:

- **device-manager**: 设备管理（设备、产品、分组、固件）
- **authentication-manager**: 认证权限（用户、角色、菜单、组织）
- **rule-engine-manager**: 规则引擎（场景联动、告警管理）
- **notify-manager**: 通知管理（邮件、短信、钉钉、企业微信）
- **network-manager**: 网络组件（MQTT、TCP、UDP、HTTP）
- **datasource-manager**: 数据源管理（MySQL、PostgreSQL、ClickHouse）
- **visualization-manager**: 可视化（大屏、图表）
- **logging-manager**: 日志管理
- **jetlinks-function**: 函数管理（脚本函数）
- **jetlinks-media**: 视频流媒体（GB28181、ONVIF）

**适用场景**:

- ✨ 不确定某个功能属于哪个模块
- ✨ 需要决定是模块引入还是跨服务调用
- ✨ 想了解模块提供的命令服务
- ✨ 需要查看 Maven 依赖配置
- ✨ 了解模块间的数据传递方式

**何时使用**:

```
用户说："我需要查询设备信息，是引入 device-manager 还是用命令调用？"
用户说："这个功能属于哪个模块？"
用户说："有哪些可用的命令服务？"
用户说："如何在模块间传递数据？"
AI说："让我先查看模块参考手册，确定使用方式"
```

**核心决策**:

```
需要使用其他模块功能时
    ↓
是否需要以下任一场景？
- 深度集成（使用核心类）
- 扩展功能（实现接口）
- 监听事件
- 频繁调用（性能敏感）
    ↓
┌───────┴───────┐
│               │
是             否
│               │
↓               ↓
模块引入      跨服务调用
(Maven依赖)   (命令模式)
```

---

### 6. 通用 CRUD 规则 - [common-crud-rules](.prompt/common-crud-rules.md)

**用途**: 在现有模块中创建或修改基础 CRUD 功能相关代码

**包含内容**:

- ✅ Entity（实体类）开发规范和模板
    - 基础实体、树形实体
    - 枚举类型定义
    - 所有注解的详细说明
- ✅ Service（服务层）开发规范和模板
    - 响应式服务（GenericReactiveCrudService）
    - 阻塞式服务（GenericCrudService）
    - 树形服务、查询示例
- ✅ Controller（控制器层）开发规范和模板
    - 响应式控制器（AssetsHolderCrudController）
    - 阻塞式控制器（BlockingAssetsHolderCrudController）
    - 权限注解、资产权限控制
- ✅ 响应式 vs 阻塞式编程模式对比
- ✅ 权限控制最佳实践
- ✅ 查询模式示例（基础查询、分页、关联查询、分组统计）
- ✅ 响应式编程规范（Mono/Flux 操作符）
- ✅ 国际化支持
- ✅ 异常处理规范
- ✅ 测试规范
- ✅ 代码生成指南（供 AI 使用）

**适用场景**:

- ✨ 在现有模块中添加新的实体和基础 CRUD 功能
- ✨ 修改现有的 Entity/Service/Controller
- ✨ 理解 JetLinks CRUD 代码规范
- ✨ 学习响应式或阻塞式编程模式

**何时使用**:

```
用户说："在XXX模块中添加一个XXX实体"
用户说："创建一个XXX的增删改查功能"
用户说："修改XXXController添加一个接口"
用户说："为XXX添加权限控制"
```

---

### 7. CRUD 进阶使用规则 - [advanced-crud-rules](.prompt/advanced-crud-rules.md)

**用途**: CRUD 功能的进阶使用场景，包括事件驱动开发、复杂查询构建、编程模式选择等

**包含内容**:

- ✅ 事件驱动开发规范
    - 场景识别和引导
    - 实际项目案例（设备资产同步、数据收集器管理等）
    - 响应式与阻塞式事件处理
- ✅ 复杂查询构建规范
    - 响应式复杂查询模式
    - 阻塞式复杂查询模式
    - 嵌套条件查询构建
    - 动态条件构建
- ✅ 编程模式选择指南
    - 响应式编程适用场景
    - 阻塞式编程适用场景
    - 混合使用场景
- ✅ 查询优化最佳实践
    - 分页查询优化
    - 字段选择优化
    - 批量处理优化
- ✅ 实际项目案例速查
    - 事件驱动案例
    - 复杂查询案例
    - 响应式编程案例
- ✅ AI 使用指南
    - 场景识别模板
    - 代码生成检查清单

**适用场景**:

- ✨ 当XXX保存时，同时处理YYY（事件驱动）
- ✨ 需要复杂的查询条件构建
- ✨ 不确定使用响应式还是阻塞式编程
- ✨ 需要查询性能优化
- ✨ 需要基于实际项目案例的代码生成

**何时使用**:

```
用户说："当XXX保存时，同时处理YYY"
用户说："需要复杂的查询条件"
用户说："多表关联查询"
用户说："动态条件构建"
用户说："不确定用响应式还是阻塞式"
用户说："需要查询性能优化"
```

**核心原则**:

```
CRUD 进阶使用决策流程：
    ↓
用户需求分析
    ↓
┌─────────────────────────────────────────┐
│ 事件驱动？ → 引导到 event-driven-rules  │
│ 复杂查询？ → 使用 advanced-crud-rules   │
│ 编程模式？ → 使用 advanced-crud-rules   │
│ 基础CRUD？ → 使用 common-crud-rules     │
└─────────────────────────────────────────┘
```

---

## 🤖 AI 使用指南

### 基本原则

1. **🔴 最重要：代码优先，避免推测**
   ```
   ❌ 禁止：根据文档描述凭空推测 API 的使用方式
   ✅ 必须：先搜索项目中的现有用例，阅读实际代码
   ✅ 必须：验证所有类和方法都真实存在
   ✅ 必须：基于真实代码调整生成新代码

   工作流程：
   1. 搜索相关功能的现有实现 (grep/codebase_search)
   2. 阅读找到的真实代码示例
   3. 验证类存在性和方法签名
   4. 参考真实代码生成新代码
   5. 说明代码参考了哪个现有实现
   ```

1.5. **🔴 重要：正确使用注解和包名**
   ```
   ⚠️ 注解使用规范：
   - @Table 和 @Column 使用 JPA 注解：javax.persistence.Table, javax.persistence.Column
   - @EnableEntityEvent 使用 HSWeb 注解：org.hswebframework.web.crud.annotation.EnableEntityEvent
   - @ColumnType 等其他注解使用 HSWeb/EasyORM 注解
   
   ⚠️ 工具类包名：
   - FastBeanCopier 完整包名：org.hswebframework.web.bean.FastBeanCopier
   - 不要使用错误的包名如：org.jetlinks.pro.fastjson.FastBeanCopier
   
   ⚠️ 依赖最小化原则：
   - 代码注释中引用的类不代表本类需要依赖该类
   - 只 import 实际使用的类，不要因注释引用而引入不必要的依赖
   - Consumer 端命令调用：参考 cross-service-call-rules.md 第一章
   - Provider 端命令定义：参考 cross-service-call-rules.md 第二章
   
   ✅ 必须：在生成代码前，搜索现有代码验证导入语句
   ✅ 必须：使用项目中实际使用的注解和包名
   ```

2. **始终读取完整规则**
   ```
   不要依赖记忆，使用 read_file 读取完整的规则文件
   规则文件包含最新的模板和最佳实践
   ```

3. **明确用户需求**
   ```
   - 模块名称是什么？
   - 使用响应式还是阻塞式编程？（默认：响应式）
   - 需要哪些实体和字段？
   - 是否需要资产权限控制？
   - 是否需要树形结构？
   ```

4. **生成完整代码**
   ```
   - 包含完整的包声明和导入语句
   - 包含所有必需的注解
   - 添加完整的 JavaDoc 注释
   - 提供 Swagger 文档注解
   ```

5. **遵循命名规范**
   ```
   - 模块名: kebab-case (template-manager)
   - 包名: camelCase (org.jetlinks.pro.template)
   - 类名: PascalCase (TemplateManagerConfiguration)
   - 变量名: camelCase (templateService)
   ```

6. **使用检查清单**
   ```
   代码生成后，参考规则文件中的检查清单验证完整性
   ```

### 工作流程

```
┌─────────────────────────────────────────────────┐
│  1. 接收用户需求                                  │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  2. 判断任务类型                                  │
│     ├─ 创建新模块？→ module-creation-rules      │
│     ├─ 添加CRUD？  → common-crud-rules          │
│     ├─ 配置国际化？→ i18n                       │
│     ├─ 跨服务调用？→ cross-service-call-rules   │
│     ├─ 订阅消息？  → realtime-subscription-rules│
│     ├─ 监听事件？  → event-driven-rules         │
│     └─ 不确定？    → module-reference           │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  3. 使用 read_file 读取完整规则                   │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  🔴 4. 搜索现有代码（最重要！）                    │
│     - 使用 grep 搜索相关类名、注解、方法          │
│     - 使用 codebase_search 查找功能实现          │
│     - 阅读找到的真实代码示例                      │
│     - 验证类、方法、注解的实际使用方式             │
│     - 如果找不到，查看依赖的 JAR 包源码           │
│     ⚠️ 禁止跳过此步骤！                          │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  5. 收集详细信息                                  │
│     - 模块名称、实体名称、字段列表                │
│     - 编程模式（响应式/阻塞式）                   │
│     - 权限和资产要求                              │
│     - 确认所有使用的类都真实存在                  │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  6. 基于真实代码生成新代码                        │
│     - 参考找到的实际代码示例                      │
│     - 保持导入语句、注解、方法签名一致            │
│     - 调整为用户的具体需求                        │
│     - 保持代码风格和项目结构一致                  │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  7. 配置国际化（如果涉及）                        │
│     - 创建枚举类：配置枚举值国际化                │
│     - 创建实体类：配置字段国际化（可选）          │
│     - 创建Controller：配置功能权限和操作国际化    │
│     - 参考 i18n.md 规范                          │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  8. 验证和检查                                    │
│     - 使用检查清单验证                            │
│     - 确保所有注解完整                            │
│     - 说明代码参考了哪个现有实现                  │
│     - 提供使用说明                                │
└─────────────────────────────────────────────────┘
```

---

## 🌳 使用场景决策树

```
用户的请求
    │
    ├─ "有哪些可用的模块？" / "如何引入XXX模块？"
    │   └─→ 查看 module-list
    │       ├─ 快速查找模块
    │       ├─ 复制 Maven 配置
    │       └─→ 了解模块基本功能
    │
    ├─ "添加国际化配置" / "配置枚举/字段显示名称" / "功能权限国际化"
    │   └─→ 查看 i18n
    │       ├─ 枚举国际化：包名.枚举类名.枚举值=显示名称
    │       ├─ 实体字段国际化：包名.实体类名.字段名=显示名称
    │       ├─ 功能权限：hswebframework.web.system.permission.{权限ID}=名称
    │       ├─ 按钮操作：hswebframework.web.system.action.{操作ID}=名称
    │       └─→ 查看完整配置示例和规范
    │
    ├─ "创建一个XXX管理模块"
    │   │
    │   ├─ 需要完整的模块结构（pom.xml、配置类等）
    │   │   └─→ 使用 module-creation-rules
    │   │       └─→ 生成：pom.xml, Configuration, Entity, Service, Controller, 国际化文件
    │   │
    │   └─ 只需要 Entity/Service/Controller
    │       └─→ 使用 common-crud-rules
    │           └─→ 生成：Entity, Service, Controller
    │
    ├─ "不确定某功能属于哪个模块" / "不知道用模块引入还是跨服务调用"
    │   └─→ 先查看 module-reference
    │       ├─ 查看模块功能列表
    │       ├─ 查看决策流程图
    │       └─→ 根据场景选择方式
    │
    ├─ "查询设备信息" / "调用其他服务"
    │   └─→ 使用 cross-service-call-rules
    │       ├─ 不要直接注入其他模块的 Service
    │       ├─ 使用 CommandSupportManagerProviders 进行调用
    │       └─→ 查看常用命令服务速查表
    │
    ├─ "订阅设备XXX消息" / "监听设备XXX"
    │   └─→ 使用 realtime-subscription-rules
    │       ├─ 查看常见 Topic 速查表
    │       ├─ 选择合适的订阅方式（@Subscribe 注解 或 EventBus）
    │       └─→ 参考相应场景的完整示例
    │
    ├─ "将设备数据同步到Redis/数据库"
    │   └─→ 使用 realtime-subscription-rules
    │       └─→ 参考"场景1: 订阅设备属性上报并更新Redis"
    │
    ├─ "实时处理设备事件/告警"
    │   └─→ 使用 realtime-subscription-rules
    │       └─→ 参考"场景3: 订阅设备事件并处理告警"
    │
    ├─ "监听实体增删改查" / "数据变更后处理"
    │   └─→ 使用 event-driven-rules
    │       ├─ 查看完整事件类型说明表
    │       ├─ 选择响应式或阻塞式环境
    │       └─→ 参考实战示例
    │
    ├─ "实体保存/删除/修改后发送通知"
    │   └─→ 使用 event-driven-rules
    │       └─→ 参考"实体保存时发布到EventBus"示例
    │
    ├─ "使用事件驱动实现业务解耦"
    │   └─→ 使用 event-driven-rules
    │       └─→ 查看通用CRUD事件监听章节
    │
    ├─ "在XXX模块中添加XXX功能"
    │   │
    │   ├─ 基础CRUD功能
    │   │   └─→ 使用 common-crud-rules
    │   │       └─→ 根据需求生成相应的类
    │   │
    │   ├─ 事件驱动功能（当XXX时处理YYY）
    │   │   └─→ 使用 advanced-crud-rules
    │   │       └─→ 引导到 event-driven-rules
    │   │
    │   ├─ 复杂查询功能
    │   │   └─→ 使用 advanced-crud-rules
    │   │       └─→ 参考复杂查询构建规范
    │   │
    │   └─ 不确定编程模式
    │       └─→ 使用 advanced-crud-rules
    │           └─→ 参考编程模式选择指南
    │
    ├─ "修改XXXController添加接口"
    │   └─→ 使用 common-crud-rules
    │       └─→ 参考第三章"控制器层开发规范"
    │
    ├─ "创建一个XXX实体类"
    │   └─→ 使用 common-crud-rules
    │       └─→ 参考第一章"实体层开发规范"
    │
    ├─ "当XXX保存时，同时处理YYY"
    │   └─→ 使用 advanced-crud-rules
    │       └─→ 引导到 event-driven-rules
    │
    ├─ "需要复杂的查询条件"
    │   └─→ 使用 advanced-crud-rules
    │       └─→ 参考复杂查询构建规范
    │
    ├─ "不确定用响应式还是阻塞式"
    │   └─→ 使用 advanced-crud-rules
    │       └─→ 参考编程模式选择指南
    │
    ├─ "需要添加资产权限控制"
    │   └─→ 同时参考两个规则
    │       ├─ module-creation-rules: 2.9 创建资产类型
    │       └─ common-crud-rules: 3.3 关联资产权限控制
    │
    └─ "如何在JetLinks中实现XXX"
        └─→ 先查看 common-crud-rules 相关章节
            └─→ 如需模块级配置，参考 module-creation-rules
            └─→ 如需实时数据处理，参考 realtime-subscription-rules
```

---

## 📝 完整示例

### 示例 1: 创建完整的新模块

**用户请求**:

```markdown
创建一个配置模板管理模块(template-manager)，
用于管理各种配置模板，使用响应式编程。
```

**AI 处理步骤**:

1. **读取规则**:
   ```
   read_file: ai/module-creation-rules (获取完整的模块创建指南)
   read_file: ai/common-crud-rules (获取 Entity/Service/Controller 规范)
   ```

2. **收集信息**:
   ```
   - 模块名: template-manager
   - 包名: org.jetlinks.pro.template
   - 编程模式: 响应式(WebFlux)
   - 核心实体: ConfigTemplateEntity
   - 主要字段: name, type, content, state
   ```

3. **生成文件**:
   ```
   ✅ pom.xml (使用 module-creation-rules 第2.2节模板)
   ✅ TemplateManagerConfiguration.java (第2.4节)
   ✅ AutoConfiguration.imports (第2.4节)
   ✅ messages_zh.properties & messages_en.properties (第2.5节)
   ✅ ConfigTemplateEntity.java (common-crud-rules 第1.1节)
   ✅ TemplateTypeEnum.java (common-crud-rules 第1.3节)
   ✅ ConfigTemplateService.java (common-crud-rules 第2.1节)
   ✅ ConfigTemplateController.java (common-crud-rules 第3.1节)
   ```

4. **验证**:
   ```
   使用 module-creation-rules 第3章"模块开发检查清单"验证
   ```

---

### 示例 2: 在现有模块中添加 CRUD 功能

**用户请求**:

```markdown
在 device-manager 模块中添加设备标签(DeviceTag)的管理功能，
包括增删改查接口。
```

**AI 处理步骤**:

1. **读取规则**:
   ```
   read_file: ai/common-crud-rules (获取完整的 CRUD 开发规范)
   ```

2. **收集信息**:
   ```
   - 模块: device-manager (已存在)
   - 实体名: DeviceTagEntity
   - 包名: org.jetlinks.pro.device
   - 编程模式: 响应式(根据现有模块)
   - 字段: name, color, deviceId 等
   ```

3. **生成文件**:
   ```
   ✅ DeviceTagEntity.java (第1.1节模板)
   ✅ DeviceTagService.java (第2.1节模板)
   ✅ DeviceTagController.java (第3.1节模板)
   ✅ 更新 messages_zh.properties (第7.1节)
   ```

---

### 示例 3: 添加资产权限控制

**用户请求**:

```markdown
为 template-manager 模块添加资产权限控制。
```

**AI 处理步骤**:

1. **读取规则**:
   ```
   read_file: ai/module-creation-rules (第2.9节 - 创建资产类型)
   read_file: ai/common-crud-rules (第3.3节 - 关联资产权限控制)
   ```

2. **生成文件**:
   ```
   ✅ TemplateAssetType.java (资产类型枚举)
   ✅ TemplateAsset.java (资产注解)
   ✅ TemplateAssetSupplier.java (资产提供者)
   ✅ 更新 pom.xml (添加 assets-component 依赖)
   ✅ 更新 ConfigTemplateController (添加 @TemplateAsset 注解)
   ```

---

### 示例 4: 创建树形结构模块

**用户请求**:

```markdown
创建一个组织架构管理模块(organization-manager)，
需要支持树形结构。
```

**AI 处理步骤**:

1. **读取规则**:
   ```
   read_file: ai/module-creation-rules (完整模块创建指南)
   read_file: ai/common-crud-rules (第1.2节 - 树形实体，第2.2节 - 树形服务)
   ```

2. **关键点**:
   ```
   - Entity 继承 GenericTreeSortSupportEntity
   - Service 继承 GenericReactiveTreeSupportCrudService
   - Controller 添加 /_tree 接口
   ```

---

## 🎯 AI 快速参考

### 搜索命令速查表（必读！）

在生成任何代码前，使用以下命令搜索现有实现：

| 功能场景 | 搜索命令 | 目的 |
|---------|---------|------|
| **跨服务调用** | `grep -r "CommandSupportManagerProviders" modules/` | 查找命令调用示例 |
| | `grep -r "@CommandService" modules/` | 查找命令服务定义 |
| | `grep -r "QueryByIdCommand" modules/` | 查找查询命令用法 |
| **订阅消息** | `grep -r "@Subscribe" modules/` | 查找订阅注解用法 |
| | `grep -r "EventBus.subscribe" modules/` | 查找编程式订阅 |
| | `grep -r "device/message" modules/` | 查找设备消息订阅 |
| **监听事件** | `grep -r "@EventListener" modules/` | 查找事件监听器 |
| | `grep -r "EntityModifyEvent" modules/` | 查找实体修改事件 |
| | `grep -r "event.async()" modules/` | 查找响应式事件处理 |
| **CRUD 实现** | `grep -r "GenericReactiveCrudService" modules/` | 查找响应式服务示例 |
| | `grep -r "AssetsHolderCrudController" modules/` | 查找控制器示例 |
| | `grep -r "@Table" modules/` | 查找实体类定义 |
| **权限控制** | `grep -r "@Authorize" modules/` | 查找权限注解用法 |
| | `grep -r "@AssetsController" modules/` | 查找资产权限控制 |
| **查找类定义** | `find modules -name "ClassName.java"` | 验证类是否存在 |
| | `grep -r "import.*ClassName" modules/` | 查看导入路径 |

### 常用代码模板快速定位

| 需求              | 规则文件                     | 章节    | 搜索建议 |
|-----------------|--------------------------|-------|---------|
| **查看所有模块**    | **module-list** | **全文** | 快速检索所有可用模块 |
| **国际化配置规范**    | **i18n** | **全文** | 枚举、实体、权限、操作的国际化配置 |
| Maven pom.xml   | module-creation-rules    | 2.2   | 查看现有模块的 pom.xml |
| Configuration 类 | module-creation-rules    | 2.4   | `grep -r "Configuration.java" modules/` |
| 基础实体类           | common-crud-rules        | 1.1   | `grep -r "@Table" modules/` |
| 响应式服务           | common-crud-rules        | 2.1   | `grep -r "GenericReactiveCrudService" modules/` |
| 阻塞式服务           | common-crud-rules        | 2.A.1 | `grep -r "GenericCrudService" modules/` |
| 响应式控制器          | common-crud-rules        | 3.1   | `grep -r "AssetsHolderCrudController" modules/` |
| 阻塞式控制器          | common-crud-rules        | 3.A.1 | `grep -r "BlockingAssetsHolderCrudController" modules/` |
| 资产类型定义          | module-creation-rules    | 2.9   | `grep -r "AssetType" modules/` |
| 权限控制            | common-crud-rules        | 4     | `grep -r "@Authorize" modules/` |
| **枚举国际化**        | **i18n** | **1** | `grep -r "org.jetlinks.*enums.*=" modules/` |
| **功能权限国际化**    | **i18n** | **5.1** | `grep -r "hswebframework.web.system.permission" modules/` |
| **按钮操作国际化**    | **i18n** | **5.2** | `grep -r "hswebframework.web.system.action" modules/` |
| **模块列表查询**      | **module-reference** | **一** | 查看各模块的功能说明 |
| **模块引入决策**      | **module-reference** | **二.1** | 何时添加 Maven 依赖 |
| **跨服务调用决策**    | **module-reference** | **二.2** | 何时使用命令模式 |
| **命令服务速查**      | **module-reference** | **四.1** | 可用的命令服务列表 |
| **跨服务调用**        | **cross-service-call-rules** | **全文** | `grep -r "CommandSupportManagerProviders" modules/` |
| **@CommandService** | **cross-service-call-rules** | **二** | `grep -r "@CommandService" modules/` |
| **查询设备信息**      | **cross-service-call-rules** | **三.1** | `grep -r "deviceService" modules/` |
| **触发告警**        | **cross-service-call-rules** | **三.2** | `grep -r "TriggerAlarmCommand" modules/` |
| **订阅设备消息**      | **realtime-subscription-rules** | **全文** | `grep -r "@Subscribe" modules/` |
| **@Subscribe 注解**  | **realtime-subscription-rules** | **全文** | `grep -r "device/message" modules/` |
| **实时数据处理**      | **realtime-subscription-rules** | **场景示例** | 查看具体的订阅处理类 |
| **实体事件监听**      | **event-driven-rules** | **一、二** | `grep -r "@EventListener" modules/` |
| **@EventListener** | **event-driven-rules** | **一** | `grep -r "EntityModifyEvent" modules/` |
| **event.async()** | **event-driven-rules** | **一、六** | `grep -r "event.async()" modules/` |
| **集群事件监听**      | **event-driven-rules** | **四** | `grep -r "ClusterEntityEventListener" modules/` |

### 必需注解检查清单

```java
// Entity
@Table(name = "表名")
@Comment("表注释")
@EnableEntityEvent  // 开启实体事件
@Column
@Schema

// Service
@Service
@Slf4j

// Controller
@RestController
@RequestMapping("/路径")
@Authorize
@Resource(id = "资源ID", name = "资源名称")
@Tag(name = "API标签")
@AssetsController(type = "资产类型")  // 如需要
@QueryAction /@SaveAction /@DeleteAction
@Operation(summary = "接口说明")

// Event Handler (响应式环境)
@Component
@Slf4j
@EventListener
// 使用 event.async() 包装响应式操作

// Event Handler (阻塞式环境)
@Component
@Slf4j
@EventListener 或 @TransactionalEventListener

// 命令服务定义
@Component
@CommandService(id = "serviceName", name = "服务名称")
implements CrudCommandHandler<Entity, String>
```

---

## 💡 提示

### 对于 AI 开发助手：

1. **🔴 代码优先原则（最重要）**
   - ❌ **禁止**：根据文档凭空推测 API 的使用方式
   - ✅ **必须**：先搜索项目现有用例，阅读实际代码
   - ✅ **必须**：验证所有类和方法都真实存在
   - ✅ **必须**：基于真实代码生成新代码
   - ✅ **必须**：说明代码参考了哪个现有实现

2. **📖 读取规则文件**
   - 始终完整读取规则文件，不要依赖记忆或假设
   - 规则文件包含最新的模板和最佳实践

3. **🔍 搜索工作流**
   ```bash
   # 步骤1: 搜索相关功能
   grep -r "关键词" modules/

   # 步骤2: 阅读找到的代码
   # 使用 read_file 工具读取完整文件

   # 步骤3: 验证类的存在性
   find modules -name "ClassName.java"

   # 步骤4: 基于真实代码生成
   # 参考实际代码的结构和风格
   ```

4. **📝 交付代码时说明**
   - 告诉用户代码参考了哪个现有文件
   - 说明使用的搜索命令和找到的示例
   - 如有不确定的地方，明确指出需要验证

### 对于人工开发者：

- **开发规范参考**: 这些规则同样适用于人工开发，可以作为开发规范参考
- **保持更新**: 规则文件会持续更新，请关注最新版本
- **善用搜索**: 在实现新功能前，先搜索项目中的相似实现

---

## 📞 反馈

如发现规则不完善或需要补充的内容，请及时更新规则文件。

---

## 📖 注解和导入语句参考

### 重要提醒

在生成实体类代码时，必须使用正确的注解和包名。详细信息请参考：[annotations-and-imports-reference](.prompt/annotations-and-imports-reference.md)

### 核心要点

1. **JPA 注解**（使用 `javax.persistence`）
   ```java
   import javax.persistence.Column;  // ✅ 正确
   import javax.persistence.Table;   // ✅ 正确
   ```

2. **HSWeb 注解**
   ```java
   import org.hswebframework.web.crud.annotation.EnableEntityEvent;  // ✅ 正确
   import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType; // ✅ 正确
   ```

3. **FastBeanCopier**
   ```java
   import org.hswebframework.web.bean.FastBeanCopier;  // ✅ 正确
   ```

### 常见错误

```java
// ❌ 错误：不要使用 HSWeb 的 @Table 和 @Column
import org.hswebframework.ezorm.rdb.mapping.annotation.Table;
import org.hswebframework.ezorm.rdb.mapping.annotation.Column;

// ❌ 错误：不要使用错误的 FastBeanCopier 包名
import org.jetlinks.pro.fastjson.FastBeanCopier;
```

### 验证方法

在生成代码前，使用以下命令验证：
```bash
grep -r "import javax.persistence.Table" modules/
grep -r "import org.hswebframework.web.bean.FastBeanCopier" modules/
```

---

## ⚠️ AI 重要提醒

**在使用本开发辅助工具时，请牢记以下核心原则：**

```
┌─────────────────────────────────────────────────┐
│                                                 │
│  🔴 代码优先，避免推测                            │
│                                                 │
│  ❌ 不要凭文档描述推测 API                        │
│  ✅ 必须搜索项目现有代码                          │
│  ✅ 必须阅读真实实现示例                          │
│  ✅ 必须验证类和方法存在                          │
│  ✅ 必须基于实际代码生成                          │
│  ✅ 必须使用正确的注解和包名                      │
│                                                 │
│  记住：你的优势是搜索和理解，而不是凭空创造        │
│                                                 │
└─────────────────────────────────────────────────┘
```
