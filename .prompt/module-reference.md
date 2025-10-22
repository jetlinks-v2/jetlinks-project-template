# JetLinks 模块参考手册

## 概述

本文档列举了 JetLinks 项目中所有业务模块（Manager 模块）的功能说明，帮助开发者了解各模块的职责和依赖关系。

---

## 🤖 AI 辅助开发重要原则

### 核心要求：代码优先，避免推测

在使用本文档辅助开发时，**必须遵循以下原则**：

1. **📖 优先阅读源代码**
   ```
   ❌ 不要：基于文档描述凭空推测 API 的使用方式
   ✅ 要做：实际查看源代码中的类定义、方法签名、注解使用
   ```

2. **🔍 查找真实用例**
   ```
   在实现任何功能前，必须先搜索项目中的现有用例：
   - 使用 grep 或 codebase_search 搜索相关的类名、注解、方法
   - 查看其他模块中是如何使用该功能的
   - 参考测试代码中的用例
   ```

3. **📦 善用 Maven JAR Sources**
   ```
   如果本地代码中找不到用例，查看依赖的 JAR 包源码：
   - 在 IDE 中跳转到依赖类的源码
   - 查看接口的实现类和示例
   - 阅读框架组件的 JavaDoc 注释
   ```

4. **🎯 具体工作流程**
   ```
   步骤1: 确定需要使用的功能（如：跨服务调用、事件监听等）
          ↓
   步骤2: 在项目中搜索相关类名或注解
          grep -r "@CommandService" modules/
          grep -r "CommandSupportManagerProviders" modules/
          ↓
   步骤3: 阅读找到的实际代码示例
          - 查看导入语句（确认包路径）
          - 查看方法签名（确认参数类型）
          - 查看返回值处理（确认响应式/阻塞式）
          ↓
   步骤4: 如果项目中没有用例，查看依赖源码
          - 跳转到框架类的定义
          - 阅读类和方法的 JavaDoc
          - 查看接口的默认实现
          ↓
   步骤5: 基于真实代码编写新功能
          - 复制相似场景的代码结构
          - 根据实际需求调整参数
          - 保持代码风格一致
   ```

5. **⚠️ 禁止的行为**
   ```
   ❌ 不要仅根据方法名推测参数类型
   ❌ 不要假设某个类存在而不验证
   ❌ 不要编造不存在的 API 或配置
   ❌ 不要跳过源码直接生成代码
   ```

6. **✅ 推荐的搜索策略**
   ```java
   // 场景1: 需要使用命令调用
   → 搜索: "CommandSupportManagerProviders"
   → 搜索: "@CommandService"
   → 阅读: cross-service-call-rules.md
   → 查看: 实际项目中的调用示例
   
   // 场景2: 需要订阅设备消息
   → 搜索: "@Subscribe"
   → 搜索: "EventBus"
   → 阅读: realtime-subscription-rules.md
   → 查看: 现有的订阅处理类
   
   // 场景3: 需要监听实体事件
   → 搜索: "@EventListener"
   → 搜索: "EntityModifyEvent"
   → 阅读: event-driven-rules.md
   → 查看: 现有的事件监听器
   
   // 场景4: 创建 CRUD 功能
   → 搜索同类型的 Entity/Service/Controller
   → 对比现有代码的注解和继承关系
   → 参考: common-crud-rules.md
   → 复制并调整相似代码
   ```

7. **📝 生成代码前的检查清单**
   ```
   □ 是否已搜索项目中的相关用例？
   □ 是否已阅读相关类的源码？
   □ 是否已确认所有类和方法都真实存在？
   □ 是否已验证导入语句的正确性？
   □ 是否已确认参数类型和返回值类型？
   □ 是否已查看相似功能的实现方式？
   ```

### 示例：正确的开发流程

**场景**: 用户要求"查询设备信息"

❌ **错误做法**：
```java
// 直接根据文档描述编写代码，未验证 API 是否存在
public Mono<Device> getDevice(String id) {
    return CommandSupportManagerProviders
        .getCommandSupport("device")  // 错误的 ID
        .flatMap(cmd -> cmd.execute(new GetDeviceCommand(id)));  // 错误的命令类
}
```

✅ **正确做法**：
```bash
# 第1步：搜索现有用例
grep -r "CommandSupportManagerProviders" modules/

# 第2步：阅读找到的代码
# 发现 device-manager 中有：
# CommandSupportManagerProviders
#     .getCommandSupport(SdkServices.deviceService, "device")
#     .flatMap(cmd -> cmd.execute(QueryByIdCommand.of(DeviceInfo.class, id)))

# 第3步：查看相关类的定义
# 打开 SdkServices.java 确认常量
# 打开 QueryByIdCommand.java 确认用法
# 打开 DeviceInfo.java 确认返回类型

# 第4步：基于真实代码编写
```

```java
// 基于实际项目中的代码编写
public Mono<DeviceInfo> getDevice(String id) {
    return CommandSupportManagerProviders
        .getCommandSupport(SdkServices.deviceService, "device")
        .flatMap(cmd -> cmd.execute(QueryByIdCommand.of(DeviceInfo.class, id)));
}
```

---

**重要决策**：当需要使用其他模块的功能时，请根据以下原则选择：

```
┌─────────────────────────────────────┐
│ 需要使用其他模块的功能时              │
└─────────────────┬───────────────────┘
                  │
                  ▼
         ┌────────────────────┐
         │ 判断依赖关系         │
         └────────┬───────────┘
                  │
    ┌─────────────┴─────────────┐
    │                           │
    ▼                           ▼
┌───────────┐            ┌──────────────┐
│ 模块引入   │            │ 跨服务调用    │
└───┬───────┘            └──────┬───────┘
    │                           │
    ▼                           ▼
适用场景:                   适用场景:
✅ 需要扩展模块功能          ✅ 只是查询数据
✅ 需要监听模块事件          ✅ 触发某个操作
✅ 深度集成                 ✅ 跨服务边界
✅ 频繁调用                 ✅ 松耦合需求

示例:                       示例:
@Autowired                  CommandSupportManagerProviders
private DeviceRegistry       .getCommandSupport(...)
    deviceRegistry;          .flatMap(cmd -> cmd.execute(...))
```

---

## 一、核心业务模块

### 1.1 device-manager（设备管理模块）

**Maven 坐标**:
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>device-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能说明**:
- 设备实例管理（DeviceInstanceEntity）
- 设备产品管理（DeviceProductEntity）
- 设备分组管理（DeviceGroupEntity）
- 产品分类管理（DeviceCategoryEntity）
- 固件管理（FirmwareEntity）
- 透传消息处理（TransparentMessageCodec）
- 设备虚拟属性计算（VirtualPropertyEntity）
- 设备数据存储和查询（DeviceDataService）

**主要服务**:
- `LocalDeviceInstanceService`: 设备实例服务
- `LocalDeviceProductService`: 设备产品服务
- `DeviceDataService`: 设备数据服务
- `DeviceRegistry`: 设备注册表（核心）
- `DeviceMessageConnector`: 设备消息转发

**命令服务**:
- `deviceService:device` - 设备命令支持
- `deviceService:product` - 产品命令支持
- `deviceService:protocol` - 协议命令支持
- `deviceService:firmware` - 固件命令支持

**何时使用**:
- ✅ **模块引入**: 需要操作设备注册表（DeviceRegistry）、监听设备事件
- ✅ **跨服务调用**: 查询设备信息、产品信息、调用设备功能

**示例**:
```java
// 模块引入方式（需要深度集成）
@Autowired
private DeviceRegistry deviceRegistry;

public Mono<DeviceOperator> getDevice(String deviceId) {
    return deviceRegistry.getDevice(deviceId);
}

// 跨服务调用方式（只查询数据）
public Mono<DeviceInfo> queryDevice(String deviceId) {
    return CommandSupportManagerProviders
        .getCommandSupport(SdkServices.deviceService, "device")
        .flatMap(cmd -> cmd.execute(QueryByIdCommand.of(DeviceInfo.class, deviceId)));
}
```

---

### 1.2 authentication-manager（认证权限管理模块）

**Maven 坐标**:
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>authentication-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能说明**:
- 用户管理（UserEntity）
- 角色管理（RoleEntity）
- 权限管理（PermissionEntity）
- 菜单管理（MenuEntity）
- 组织架构管理（OrganizationEntity）
- 维度管理（DimensionEntity）
- 第三方认证（OAuth2、SSO、LDAP）
- Token 管理

**主要服务**:
- `UserService`: 用户服务
- `RoleService`: 角色服务
- `MenuService`: 菜单服务
- `OrganizationService`: 组织架构服务
- `DimensionService`: 维度服务

**命令服务**:
- `authService:user` - 用户命令支持
- `authService:menu` - 菜单命令支持
- `authService:dimension` - 维度命令支持

**何时使用**:
- ✅ **模块引入**: 需要扩展认证方式、自定义权限逻辑
- ✅ **跨服务调用**: 查询用户信息、查询菜单、验证权限

**示例**:
```java
// 跨服务调用方式（推荐）
public Mono<List<MenuInfo>> queryUserMenus(String userId) {
    return CommandSupportManagerProviders
        .getCommandSupport("authService", "menu")
        .flatMapMany(cmd -> cmd.execute(new QueryUserMenusCommand(userId)))
        .collectList();
}
```

---

### 1.3 rule-engine-manager（规则引擎管理模块）

**Maven 坐标**:
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>rule-engine-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能说明**:
- 场景联动管理（SceneEntity）
- 告警配置管理（AlarmConfigEntity）
- 告警历史记录（AlarmHistoryEntity）
- 告警记录管理（AlarmRecordEntity）
- 规则实例管理（RuleInstanceEntity）
- 流程编排（可视化规则引擎）

**主要服务**:
- `SceneService`: 场景联动服务
- `AlarmConfigService`: 告警配置服务
- `AlarmHistoryService`: 告警历史服务
- `AlarmRecordService`: 告警记录服务
- `AlarmHandler`: 告警处理器

**命令服务**:
- `ruleService:alarm` - 告警触发/解除
- `ruleService:alarmConfig` - 告警配置管理
- `ruleService:alarmHistory` - 告警历史查询
- `ruleService:alarmRecord` - 告警记录管理
- `ruleService:alarmRuleBind` - 告警规则绑定
- `ruleService:scene` - 场景联动管理

**何时使用**:
- ✅ **模块引入**: 需要扩展告警类型、自定义规则节点
- ✅ **跨服务调用**: 触发告警、查询告警历史、执行场景

**示例**:
```java
// 跨服务调用方式（推荐）
// 触发告警
public Mono<Void> triggerAlarm(AlarmInfo alarmInfo) {
    return CommandSupportManagerProviders
        .getCommandSupport(InternalSdkServices.ruleService, "alarm")
        .flatMap(cmd -> cmd.execute(new TriggerAlarmCommand(alarmInfo)));
}

// 查询告警历史
public Mono<PagerResult<AlarmHistoryInfo>> queryAlarmHistory(QueryParamEntity query) {
    return CommandSupportManagerProviders
        .getCommandSupport(InternalSdkServices.ruleService, "alarmHistory")
        .flatMap(cmd -> cmd.execute(QueryPagerCommand.of(AlarmHistoryInfo.class, query)));
}
```

---

### 1.4 notify-manager（通知管理模块）

**Maven 坐标**:
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>notify-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能说明**:
- 通知配置管理（NotifierEntity）
- 通知模板管理（NotifyTemplateEntity）
- 通知历史记录（NotifyHistoryEntity）
- 多种通知方式：
  - 邮件通知（Email）
  - 短信通知（SMS）
  - 钉钉通知（DingTalk）
  - 企业微信通知（WeChat Work）
  - Webhook 通知
  - 语音通知

**主要服务**:
- `NotifierService`: 通知器服务
- `NotifyTemplateService`: 通知模板服务
- `NotifierManager`: 通知器管理器

**命令服务**:
- `notifyService` - 通知发送服务

**何时使用**:
- ✅ **模块引入**: 需要扩展通知类型、自定义通知逻辑
- ✅ **跨服务调用**: 发送通知消息

**示例**:
```java
// 跨服务调用方式（推荐）
public Mono<NotifyResult> sendNotify(String notifierId, String templateId, Map<String, Object> context) {
    return CommandSupportManagerProviders
        .getCommandSupport(SdkServices.notifyService)
        .flatMap(cmd -> cmd.execute(new SendNotifyCommand(notifierId, templateId, context)));
}
```

---

### 1.5 network-manager（网络组件管理模块）

**Maven 坐标**:
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>network-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能说明**:
- 网络组件配置管理（NetworkConfigEntity）
- 证书管理（CertificateEntity）
- 支持的网络类型：
  - MQTT 服务端/客户端
  - TCP 服务端/客户端
  - UDP 服务端
  - HTTP 客户端
  - WebSocket 服务端/客户端
  - CoAP 服务端/客户端

**主要服务**:
- `NetworkConfigService`: 网络配置服务
- `CertificateService`: 证书服务
- `NetworkManager`: 网络管理器

**何时使用**:
- ✅ **模块引入**: 需要创建网络服务、使用网络组件
- ⚠️ **跨服务调用**: 一般不推荐，网络组件通常在本地使用

**示例**:
```java
// 模块引入方式（推荐）
@Autowired
private NetworkManager networkManager;

public Mono<NetworkClient> getNetworkClient(String id) {
    return networkManager.getClient(id);
}
```

---

### 1.6 datasource-manager（数据源管理模块）

**Maven 坐标**:
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>datasource-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能说明**:
- 数据源配置管理（DataSourceConfigEntity）
- 支持多种数据源：
  - MySQL
  - PostgreSQL
  - Oracle
  - SQL Server
  - ClickHouse
  - TDengine
  - InfluxDB
- SQL 执行和查询
- 数据源连接池管理

**主要服务**:
- `DataSourceConfigService`: 数据源配置服务
- `DynamicDataSourceService`: 动态数据源服务

**命令服务**:
- `datasource:{datasourceId}` - 数据源命令支持

**何时使用**:
- ✅ **模块引入**: 需要扩展数据源类型
- ✅ **跨服务调用**: 执行 SQL 查询、获取数据

**示例**:
```java
// 跨服务调用方式（推荐）
public Flux<Map<String, Object>> executeQuery(String datasourceId, String sql) {
    return CommandSupportManagerProviders
        .getCommandSupport("datasource:" + datasourceId)
        .flatMapMany(cmd -> cmd.execute(new ExecuteSqlCommand(sql)));
}
```

---

### 1.7 visualization-manager（可视化管理模块）

**Maven 坐标**:
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>visualization-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能说明**:
- 可视化项目管理（VisProjectEntity）
- 可视化模板管理（VisTemplateEntity）
- 大屏配置管理
- 数据源集成
- 实时数据展示

**主要服务**:
- `VisProjectService`: 可视化项目服务
- `VisTemplateService`: 可视化模板服务

**何时使用**:
- ✅ **模块引入**: 需要扩展可视化组件
- ✅ **跨服务调用**: 查询可视化配置

---

### 1.8 logging-manager（日志管理模块）

**Maven 坐标**:
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>logging-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能说明**:
- 系统日志管理（AccessLoggerEntity）
- 操作日志记录
- 日志查询和检索
- 日志导出

**主要服务**:
- `AccessLoggerService`: 访问日志服务
- `SystemLoggerService`: 系统日志服务

**何时使用**:
- ✅ **模块引入**: 需要自定义日志记录策略
- ✅ **跨服务调用**: 查询日志记录

---

### 1.9 jetlinks-function（函数管理模块）

**Maven 坐标**:
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>jetlinks-function</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能说明**:
- 自定义函数配置管理（FunctionEntity）
- 支持多种函数类型：
  - 脚本函数（Groovy、JavaScript）
  - HTTP 调用函数
  - 设备属性计算函数
- 函数执行和管理

**主要服务**:
- `FunctionService`: 函数服务
- `FunctionExecutor`: 函数执行器

**何时使用**:
- ✅ **模块引入**: 需要扩展函数类型
- ✅ **跨服务调用**: 执行自定义函数

---

### 1.10 jetlinks-media（视频流媒体管理模块）

**Maven 坐标**:
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>media-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能说明**:
- 视频设备接入和管理（MediaServerEntity）
- 视频通道管理（ChannelEntity）
- 实时视频点播
- 历史视频回放
- 云台控制
- 支持协议：
  - GB28181
  - ONVIF
  - RTSP
  - RTMP

**主要服务**:
- `MediaServerService`: 流媒体服务器服务
- `ChannelService`: 通道服务
- `PlaybackService`: 回放服务

**命令服务**:
- `mediaService:channel` - 视频通道命令支持

**何时使用**:
- ✅ **模块引入**: 需要扩展视频协议
- ✅ **跨服务调用**: 查询视频通道、控制视频播放

---

## 二、决策指南

### 2.1 何时使用模块引入（Maven 依赖）

**适用场景**：

| 场景 | 说明 | 示例 |
|-----|------|------|
| **深度集成** | 需要使用模块的核心功能类 | 使用 DeviceRegistry 操作设备 |
| **扩展功能** | 需要实现模块提供的接口 | 实现自定义认证方式 |
| **监听事件** | 需要监听模块的实体事件 | 监听设备上下线事件 |
| **频繁调用** | 性能敏感，需要本地调用 | 高频设备操作 |
| **依赖注入** | 需要在启动时注入服务 | @Autowired 注入服务 |

**引入方式**：

```xml
<!-- 在模块的 pom.xml 中添加依赖 -->
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>device-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**使用方式**：

```java
@Service
public class MyService {
    
    @Autowired
    private DeviceRegistry deviceRegistry;
    
    @Autowired
    private LocalDeviceInstanceService deviceService;
    
    public Mono<DeviceOperator> getDevice(String deviceId) {
        return deviceRegistry.getDevice(deviceId);
    }
}
```

---

### 2.2 何时使用跨服务调用（命令模式）

**适用场景**：

| 场景 | 说明 | 示例 |
|-----|------|------|
| **数据查询** | 只需要查询数据，不需要操作对象 | 查询设备列表 |
| **触发操作** | 触发某个动作或命令 | 触发告警、发送通知 |
| **松耦合** | 不希望模块间强依赖 | 微服务架构 |
| **跨边界** | 跨服务、跨进程调用 | 从认证服务调用设备服务 |
| **RPC 调用** | 需要集群路由和负载均衡 | 分布式部署 |

**调用方式**：

```java
@Service
public class MyService {
    
    // 查询设备信息
    public Mono<DeviceInfo> queryDevice(String deviceId) {
        return CommandSupportManagerProviders
            .getCommandSupport(SdkServices.deviceService, "device")
            .flatMap(cmd -> cmd.execute(QueryByIdCommand.of(DeviceInfo.class, deviceId)));
    }
    
    // 触发告警
    public Mono<Void> triggerAlarm(AlarmInfo alarmInfo) {
        return CommandSupportManagerProviders
            .getCommandSupport(InternalSdkServices.ruleService, "alarm")
            .flatMap(cmd -> cmd.execute(new TriggerAlarmCommand(alarmInfo)));
    }
}
```

---

### 2.3 决策流程图

```
开发者需要使用其他模块的功能
          ↓
    是否需要以下任一场景？
    1. 深度集成（使用核心类）
    2. 扩展功能（实现接口）
    3. 监听事件
    4. 频繁调用（性能敏感）
          ↓
    ┌─────┴─────┐
    │           │
   是          否
    │           │
    ↓           ↓
模块引入     跨服务调用
(Maven依赖)  (命令模式)
```

---

## 三、常见问题

### Q1: 如何查看模块提供了哪些命令服务？

**方式1**: 查看模块中的 `*CommandSupport.java` 文件

```bash
# 查找命令服务定义
find modules -name "*CommandSupport.java"
```

**方式2**: 查看 @CommandService 注解

```java
@CommandService(id = "deviceService:device", name = "设备命令支持")
public class DeviceCommandSupport {
    // ...
}
```

**方式3**: 通过 API 查询

```bash
# 查询所有命令服务
GET /api/command/_services

# 查询特定服务的命令
GET /api/command/deviceService/_supports
```

---

### Q2: 模块引入后会增加应用的启动时间吗？

**是的**，每个模块都会增加一定的启动时间。因此：

- ✅ 只引入必需的模块
- ✅ 优先使用跨服务调用
- ⚠️ 避免引入不必要的依赖

---

### Q3: 跨服务调用的性能如何？

**性能对比**：

| 调用方式 | 延迟 | 适用场景 |
|---------|------|---------|
| 本地调用（模块引入） | ~0.1ms | 高频操作 |
| 跨服务调用（本地进程） | ~1-5ms | 一般操作 |
| 跨服务调用（远程RPC） | ~10-50ms | 分布式部署 |

**建议**：
- 高频调用（>100次/秒）→ 使用模块引入
- 一般调用（<100次/秒）→ 使用跨服务调用

---

### Q4: 如何在不同模块间传递数据？

**方式1**: 使用命令调用（推荐）

```java
// 在 A 模块中调用 B 模块
CommandSupportManagerProviders
    .getCommandSupport("bService")
    .flatMap(cmd -> cmd.execute(command));
```

**方式2**: 使用 EventBus（异步）

```java
// 发布事件
eventBus.publish("/data/updated", data);

// 订阅事件
@Subscribe("/data/updated")
public Mono<Void> handleDataUpdated(Data data) {
    return processData(data);
}
```

**方式3**: 使用 Spring Event（同进程）

```java
// 发布事件
applicationEventPublisher.publishEvent(new DataUpdatedEvent(data));

// 监听事件
@EventListener
public void handleDataUpdated(DataUpdatedEvent event) {
    processData(event.getData());
}
```

---

## 四、快速参考表

### 4.1 模块与命令服务对照表

| 模块 | 命令服务 ID | 主要功能 |
|-----|-----------|---------|
| device-manager | `deviceService:device` | 设备管理 |
| device-manager | `deviceService:product` | 产品管理 |
| device-manager | `deviceService:protocol` | 协议管理 |
| authentication-manager | `authService:user` | 用户管理 |
| authentication-manager | `authService:menu` | 菜单管理 |
| rule-engine-manager | `ruleService:alarm` | 告警触发 |
| rule-engine-manager | `ruleService:alarmConfig` | 告警配置 |
| rule-engine-manager | `ruleService:scene` | 场景联动 |
| notify-manager | `notifyService` | 通知发送 |
| datasource-manager | `datasource:{id}` | 数据源操作 |
| jetlinks-media | `mediaService:channel` | 视频通道 |

### 4.2 常用依赖引入

```xml
<!-- 设备管理 -->
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>device-manager</artifactId>
</dependency>

<!-- 认证权限 -->
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>authentication-manager</artifactId>
</dependency>

<!-- 规则引擎 -->
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>rule-engine-manager</artifactId>
</dependency>

<!-- 通知管理 -->
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>notify-manager</artifactId>
</dependency>
```

---

## 五、总结

**模块选择原则**：

1. ✅ **优先使用跨服务调用（命令模式）** - 保持松耦合
2. ✅ **仅在必要时引入模块依赖** - 避免不必要的耦合
3. ✅ **参考本文档的决策指南** - 根据场景选择方式
4. ✅ **查看命令服务速查表** - 了解可用的服务

**记住**：
- 跨服务调用 = 松耦合、易维护、支持分布式
- 模块引入 = 性能更好、功能更强、深度集成

根据实际需求选择合适的方式！

---

## 六、AI 开发者特别注意事项

### 🎯 代码生成的黄金法则

在使用本文档辅助生成代码时，**严格遵循以下流程**：

#### 1. **搜索优先原则**

```bash
# 在编写任何代码前，必须先搜索！
# 示例1: 需要调用设备服务
grep -r "SdkServices.deviceService" modules/
grep -r "DeviceInfo" modules/

# 示例2: 需要订阅消息
grep -r "@Subscribe" modules/
grep -r "EventBus.subscribe" modules/

# 示例3: 需要监听实体事件
grep -r "@EventListener" modules/
grep -r "EntityModifyEvent" modules/
```

#### 2. **验证类的存在性**

```bash
# 在使用任何类之前，确认它确实存在
find modules -name "CommandSupportManagerProviders.java"
find modules -name "QueryByIdCommand.java"
find modules -name "DeviceInfo.java"

# 或使用 grep 查找类的使用
grep -r "import.*CommandSupportManagerProviders" modules/
grep -r "import.*QueryByIdCommand" modules/
```

#### 3. **阅读实际代码示例**

```java
// 不要假设，去找真实的代码！
// 
// 步骤：
// 1. 找到使用该功能的现有类
// 2. 打开文件，阅读完整的方法实现
// 3. 注意以下细节：
//    - import 语句（确认包路径）
//    - 泛型参数（确认类型）
//    - 方法链（确认响应式操作符）
//    - 异常处理（确认错误处理方式）
//    - 注解使用（确认配置方式）
```

#### 4. **跨越项目边界查找**

```bash
# 如果项目代码中找不到，查看依赖的 JAR 包
# 方法1: 在 IDE 中按 Ctrl/Cmd + 点击类名，跳转到源码
# 方法2: 查看 Maven 依赖的 sources.jar
# 方法3: 在 jetlinks-components 中搜索

grep -r "CommandSupportManagerProviders" jetlinks-components/
grep -r "QueryByIdCommand" jetlinks-components/
```

#### 5. **参考测试代码**

```bash
# 测试代码通常包含最完整的用例
find modules -name "*Test.java" -exec grep -l "CommandSupport" {} \;
find modules -name "*Test.java" -exec grep -l "@Subscribe" {} \;
find modules -name "*Test.java" -exec grep -l "@EventListener" {} \;
```

### ⚠️ 常见错误及预防

| 错误类型 | 错误示例 | 正确做法 |
|---------|---------|---------|
| **类名拼写错误** | `CommandSupportManager` | 搜索确认：`CommandSupportManagerProviders` |
| **方法名推测** | `.getCommand()` | 查看源码确认：`.getCommandSupport()` |
| **参数类型错误** | `new QueryCommand(id)` | 查看源码确认：`QueryByIdCommand.of(DeviceInfo.class, id)` |
| **导入路径错误** | `import org.jetlinks.core.command.*` | 查看实际代码确认正确路径 |
| **注解使用错误** | `@Command` | 搜索确认：`@CommandService` |
| **返回类型推测** | `Flux<Device>` | 查看接口确认：`Mono<DeviceInfo>` |

### 📋 代码生成前的最终检查

在提交任何生成的代码前，**必须完成以下检查**：

```
□ 1. 已在项目中搜索相关功能的现有实现
□ 2. 已阅读至少一个真实的代码示例
□ 3. 已确认所有使用的类名都真实存在
□ 4. 已验证所有方法签名都正确
□ 5. 已检查导入语句的包路径
□ 6. 已确认泛型参数类型正确
□ 7. 已保持与项目代码风格一致
□ 8. 如果使用了新的依赖，已确认 pom.xml 中有对应配置
□ 9. 如果使用了注解，已确认注解的属性配置正确
□ 10. 已准备好说明代码参考了哪个现有实现
```

### 🔍 推荐的代码审查方法

```bash
# 生成代码后，使用以下命令验证：

# 1. 检查类是否存在
grep -r "class QueryByIdCommand" jetlinks-components/

# 2. 检查方法是否存在
grep -r "CommandSupportManagerProviders.getCommandSupport" modules/

# 3. 检查注解是否正确
grep -r "@CommandService" modules/

# 4. 检查导入是否完整
# 查看生成的代码中的 import 语句，确保每个都能在项目中找到对应的类文件
```

### 💡 最佳实践总结

1. **永远不要猜测** - 如果不确定，就去搜索和阅读代码
2. **复制胜于创造** - 找到相似的代码，复制并调整比从头写更可靠
3. **测试代码是好老师** - 测试用例通常展示了最标准的使用方式
4. **注释是重要线索** - JavaDoc 注释通常包含使用示例
5. **保持谦逊** - 遇到不确定的情况，明确告诉用户需要查看代码
6. **提供溯源** - 告诉用户生成的代码参考了哪些现有实现

### 🚫 禁止行为清单

```
❌ 禁止根据文档描述直接编写代码而不查看源码
❌ 禁止假设某个类或方法存在而不验证
❌ 禁止编造看起来合理但实际不存在的 API
❌ 禁止使用过时的或想象中的注解
❌ 禁止猜测方法的参数类型或返回值类型
❌ 禁止跳过搜索步骤直接生成代码
❌ 禁止在不确定的情况下表现得很确定
```

### ✅ 正确的工作方式

```
用户请求 → 理解需求 → 确定需要什么功能
    ↓
搜索现有代码 → 找到相关实现 → 阅读完整代码
    ↓
验证类存在 → 确认方法签名 → 检查导入语句
    ↓
参考真实代码 → 调整为用户需求 → 生成代码
    ↓
说明代码来源 → 提示需要验证 → 交付给用户
```

---

**记住**: 作为 AI 开发助手，你的优势是快速搜索和理解代码，而不是凭空创造。始终基于真实的代码生成新代码，这样才能确保质量和正确性！

