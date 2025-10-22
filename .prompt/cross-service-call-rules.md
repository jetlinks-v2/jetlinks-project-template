# JetLinks 跨服务调用规范 - 命令模式

## 概述

在 JetLinks 的微服务架构或模块化开发中，**跨服务调用应使用命令模式（Command Pattern）**，而不是直接注入其他服务的 Service 类。

命令模式通过统一的 RPC 机制实现服务间通信，具有以下优势：
- ✅ **解耦合**：服务间不直接依赖，易于拆分和部署
- ✅ **可路由**：支持集群环境下的服务路由和负载均衡
- ✅ **权限控制**：命令自动继承服务的权限注解
- ✅ **跨语言**：基于标准协议，支持不同语言实现
- ✅ **可扩展**：易于添加新命令和服务

---

## 一、调用命令服务（Consumer 端）

### 1.1 响应式调用方式

```java
import org.jetlinks.pro.command.CommandSupportManagerProviders;
import org.jetlinks.sdk.server.SdkServices;
import org.jetlinks.sdk.server.device.DeviceCommandSupportTypes;
import org.jetlinks.sdk.server.device.DeviceInfo;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/example")
public class ExampleController {

    // 方式1: 查询设备列表
    @GetMapping("/devices")
    public Flux<DeviceInfo> getDevices() {
        return CommandSupportManagerProviders
            // 获取设备服务的设备命令支持
            .getCommandSupport(SdkServices.deviceService, DeviceCommandSupportTypes.device)
            // 执行查询列表命令
            .flatMapMany(cmd -> cmd.execute(QueryListCommand.of(DeviceInfo.class)));
    }

    // 方式2: 查询单个设备
    @GetMapping("/device/{id}")
    public Mono<DeviceInfo> getDevice(@PathVariable String id) {
        return CommandSupportManagerProviders
            .getCommandSupport(SdkServices.deviceService, DeviceCommandSupportTypes.device)
            .flatMap(cmd -> cmd.execute(QueryByIdCommand.of(DeviceInfo.class, id)));
    }

    // 方式3: 分页查询
    @PostMapping("/devices/pager")
    public Mono<PagerResult<DeviceInfo>> queryPager(@RequestBody QueryParamEntity query) {
        return CommandSupportManagerProviders
            .getCommandSupport(SdkServices.deviceService, DeviceCommandSupportTypes.device)
            .flatMap(cmd -> cmd.execute(QueryPagerCommand.of(DeviceInfo.class, query)));
    }
}
```

### 1.2 阻塞式调用方式

```java
import org.jetlinks.pro.command.CommandSupportManagerProviders;
import java.util.List;

@RestController
@RequestMapping("/example")
public class ExampleController {

    // 同步阻塞方式执行命令
    @GetMapping("/devices/sync")
    public List<DeviceInfo> getDevicesSync() {
        return CommandSupportManagerProviders
            // 获取命令支持（阻塞等待）
            .getCommandSupportNow(SdkServices.deviceService, DeviceCommandSupportTypes.device)
            // 执行并返回List
            .executeToList(QueryListCommand.of(DeviceInfo.class));
    }

    // 执行并返回单个对象
    @GetMapping("/device/{id}/sync")
    public DeviceInfo getDeviceSync(@PathVariable String id) {
        return CommandSupportManagerProviders
            .getCommandSupportNow(SdkServices.deviceService, DeviceCommandSupportTypes.device)
            .executeToMono(QueryByIdCommand.of(DeviceInfo.class, id))
            .block(); // 阻塞等待结果
    }
}
```

### 1.3 指定服务路由

在集群环境中，可以指定调用特定服务节点：

```java
import reactor.util.context.Context;

// 指定服务节点ID
@GetMapping("/devices/from-node")
public Flux<DeviceInfo> getDevicesFromNode() {
    return CommandSupportManagerProviders
        .getCommandSupport(SdkServices.deviceService, DeviceCommandSupportTypes.device)
        .flatMapMany(cmd -> cmd.execute(QueryListCommand.of(DeviceInfo.class)))
        // 指定路由到特定服务节点
        .contextWrite(Context.of(
            ClusterCommandSupportManagerProviderRegister.CLUSTER_ROUTER_KEY,
            "iot-service-node-1"
        ));
}
```

---

## 二、提供命令服务（Provider 端）

### 2.1 基础命令服务定义（推荐方式）

**✅ 推荐：使用 @CommandService 注解自动注册**

```java
package org.jetlinks.pro.example.cmd;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.annotation.command.CommandService;
import org.jetlinks.pro.command.crud.CrudCommandHandler;
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.jetlinks.pro.example.service.ExampleService;
import org.springframework.stereotype.Component;

/**
 * 示例命令服务
 * <p>
 * 使用 @CommandService 注解自动注册，无需手动配置
 * <p>
 * 自动提供标准的 CRUD 命令：
 * - QueryList: 查询列表
 * - QueryPager: 分页查询
 * - QueryById: 根据ID查询
 * - Count: 统计数量
 * - Save: 保存（新增/更新）
 * - Delete: 删除
 */
@Component
@Getter
@CommandService(id = "exampleService", name = "示例服务")
@AllArgsConstructor
public class ExampleCommandService implements CrudCommandHandler<ExampleEntity, String> {

    private final ExampleService service;

    // CrudCommandHandler 自动提供基础 CRUD 命令
    // 无需额外实现，框架自动处理
    // 使用 @CommandService 注解后，框架自动扫描并注册
}
```

**@CommandService 注解说明**：

- `id`: 服务标识，可以是 `"serviceName"` 或 `"serviceName:supportName"`
- `name`: 服务名称，用于显示和文档
- `description`: 服务描述（可选）
- `autoRegistered`: 是否自动注册（默认 true）
- `commands`: 声明支持的命令（可选）

**命名规范**：
- 简单服务：`id = "exampleService"` → 调用时使用 `"exampleService"`
- 带子类型：`id = "deviceService:device"` → 调用时使用 `"deviceService", "device"`

### 2.2 带资产权限的命令服务

```java
package org.jetlinks.pro.device.cmd;

import org.hswebframework.web.authorization.annotation.*;
import org.jetlinks.pro.annotation.command.CommandService;
import org.jetlinks.pro.command.crud.AssetsCrudCommandHandler;
import org.jetlinks.pro.device.entity.DeviceInstanceEntity;
import org.jetlinks.pro.device.service.LocalDeviceInstanceService;
import org.springframework.stereotype.Component;

/**
 * 设备命令服务（带资产权限控制）
 */
@Component
@CommandService(id = "deviceService:device", name = "设备命令支持")
@Resource(id = "device-instance", name = "设备实例")
@Authorize(anonymous = true)
public class DeviceCommandSupport implements AssetsCrudCommandHandler<DeviceInstanceEntity, String> {

    private final LocalDeviceInstanceService deviceInstanceService;

    public DeviceCommandSupport(LocalDeviceInstanceService deviceInstanceService) {
        this.deviceInstanceService = deviceInstanceService;
    }

    @Override
    public LocalDeviceInstanceService getService() {
        return deviceInstanceService;
    }

    // AssetsCrudCommandHandler 自动支持：
    // 1. 标准 CRUD 命令
    // 2. 资产权限过滤
    // 3. 权限注解继承
}
```

### 2.3 自定义命令

```java
package org.jetlinks.pro.device.cmd;

import org.jetlinks.core.annotation.command.CommandHandler;
import org.jetlinks.core.metadata.Metadata;
import org.hswebframework.web.authorization.annotation.QueryAction;
import reactor.core.publisher.Mono;

/**
 * 设备命令服务（带自定义命令）
 */
@Component
@CommandService(id = "deviceService:device", name = "设备命令支持")
public class DeviceCommandSupport implements AssetsCrudCommandHandler<DeviceInstanceEntity, String> {

    // 标准CRUD方法...

    // 自定义命令1: 获取设备配置
    @CommandHandler
    @QueryAction
    public Mono<Map<String, Object>> getConfigs(GetConfigCommand command) {
        return deviceInstanceService
            .findById(command.getId())
            .flatMap(device -> device.getConfigs(command.getConfigKey()))
            .map(Values::getAllValues);
    }

    // 自定义命令2: 获取设备物模型
    @CommandHandler
    @QueryAction
    public Mono<DeviceMetadata> getMetadata(GetMetadataCommand command) {
        return deviceInstanceService.getMetadata(command.getId());
    }

    // 自定义命令3: 调用设备功能
    @CommandHandler
    @SaveAction
    public Mono<FunctionInvokeMessageReply> functionInvoke(FunctionInvokeCommand command) {
        return deviceInstanceService
            .invokeFunction(command.getDeviceId(), command.getFunctionId(), command.getInputs());
    }
}
```

### 2.4 多服务管理（可选，推荐使用注解）

**⚠️ 仅在需要统一管理多个相关服务时使用，优先使用 @CommandService 注解**

如果一个大的服务包含多个子服务，有两种方式：

**方式1：使用 @CommandService 注解（推荐）**

```java
// 设备服务
@Component
@CommandService(id = "deviceService:device", name = "设备命令支持")
public class DeviceCommandSupport implements CrudCommandHandler<DeviceInstanceEntity, String> {
    // 实现...
}

// 产品服务
@Component
@CommandService(id = "deviceService:product", name = "产品命令支持")
public class ProductCommandSupport implements CrudCommandHandler<DeviceProductEntity, String> {
    // 实现...
}

// 协议服务
@Component
@CommandService(id = "deviceService:protocol", name = "协议命令支持")
public class ProtocolCommandSupport implements CrudCommandHandler<ProtocolEntity, String> {
    // 实现...
}

// 框架自动扫描并注册，无需额外配置
```

**方式2：使用 Provider 统一管理（仅特殊场景）**

```java
package org.jetlinks.pro.device.cmd;

import org.jetlinks.pro.command.StaticCommandSupportManagerProvider;
import org.jetlinks.sdk.server.SdkServices;
import org.springframework.stereotype.Component;

/**
 * 设备命令服务管理器
 * 
 * ⚠️ 注意：只在需要统一管理、动态注册或特殊配置时使用
 * 大多数情况下直接使用 @CommandService 注解即可
 */
@Component
public class DeviceCommandSupportManager extends StaticCommandSupportManagerProvider {

    public DeviceCommandSupportManager(DeviceCommandSupport deviceCommandSupport,
                                       ProtocolCommandSupport protocolCommandSupport,
                                       FirmwareCommandSupport firmwareCommandSupport) {
        // 指定Provider名称
        super(SdkServices.deviceService);

        // 注册各种命令服务
        register("device", deviceCommandSupport);      // deviceService:device
        register("protocol", protocolCommandSupport);  // deviceService:protocol
        register("firmware", firmwareCommandSupport);  // deviceService:firmware
    }
}
```

**使用建议**：
- ✅ **优先使用方式1**（@CommandService 注解），代码简洁，自动注册
- ⚠️ 方式2 仅用于需要动态注册、统一配置或特殊逻辑的场景

---

## 三、常用命令服务速查

### 3.1 设备相关命令

| 服务ID | 命令支持ID | 说明 | 常用命令 |
|--------|-----------|------|---------|
| `deviceService` | `device` | 设备实例管理 | QueryList, QueryPager, QueryById, Count, Save, Delete, GetMetadata, FunctionInvoke, ReadProperty, WriteProperty |
| `deviceService` | `product` | 设备产品管理 | QueryList, QueryPager, QueryById, Count, Save, Delete, Deploy, Undeploy |
| `deviceService` | `protocol` | 设备协议管理 | QueryList, QueryPager, QueryById, GetTransports, GetDetail |
| `deviceService` | `firmware` | 固件管理 | QueryList, QueryPager, QueryById, Upload, Upgrade |

**使用示例**：

```java
// 查询设备列表
CommandSupportManagerProviders
    .getCommandSupport(SdkServices.deviceService, "device")
    .flatMapMany(cmd -> cmd.execute(QueryListCommand.of(DeviceInfo.class)));

// 查询产品列表
CommandSupportManagerProviders
    .getCommandSupport(SdkServices.deviceService, "product")
    .flatMapMany(cmd -> cmd.execute(QueryListCommand.of(ProductInfo.class)));

// 调用设备功能
CommandSupportManagerProviders
    .getCommandSupport(SdkServices.deviceService, "device")
    .flatMap(cmd -> cmd.execute(new FunctionInvokeCommand(deviceId, functionId, inputs)));
```

### 3.2 告警相关命令

| 服务ID | 命令支持ID | 说明 | 常用命令 |
|--------|-----------|------|---------|
| `ruleService` | `alarm` | 告警触发/解除 | TriggerAlarm, RelieveAlarm |
| `ruleService` | `alarmConfig` | 告警配置管理 | QueryList, QueryPager, QueryById, Save, Delete |
| `ruleService` | `alarmHistory` | 告警历史查询 | QueryList, QueryPager, QueryById |
| `ruleService` | `alarmRecord` | 告警记录管理 | QueryList, QueryPager, QueryById, Solve, HandleAlarm |
| `ruleService` | `scene` | 场景联动管理 | QueryList, QueryPager, Save, Execute |

**使用示例**：

```java
// 触发告警
CommandSupportManagerProviders
    .getCommandSupport(InternalSdkServices.ruleService, "alarm")
    .flatMap(cmd -> cmd.execute(new TriggerAlarmCommand(alarmInfo)));

// 查询告警配置
CommandSupportManagerProviders
    .getCommandSupport(InternalSdkServices.ruleService, "alarmConfig")
    .flatMapMany(cmd -> cmd.execute(QueryListCommand.of(AlarmConfigInfo.class)));

// 查询告警历史
CommandSupportManagerProviders
    .getCommandSupport(InternalSdkServices.ruleService, "alarmHistory")
    .flatMap(cmd -> cmd.execute(QueryPagerCommand.of(AlarmHistoryInfo.class, query)));

// 处理告警
CommandSupportManagerProviders
    .getCommandSupport(InternalSdkServices.ruleService, "alarmRecord")
    .flatMap(cmd -> cmd.execute(new HandleAlarmCommand(recordId, handleInfo)));
```

### 3.3 通知相关命令

| 服务ID | 命令支持ID | 说明 | 常用命令 |
|--------|-----------|------|---------|
| `notifyService` | ` ` | 通知发送 | SendNotify |

**使用示例**：

```java
// 发送通知
CommandSupportManagerProviders
    .getCommandSupport(SdkServices.notifyService)
    .flatMap(cmd -> cmd.execute(new SendNotifyCommand(notifierId, templateId, context)));
```

### 3.4 数据源相关命令

| 服务ID | 命令支持ID | 说明 | 常用命令 |
|--------|-----------|------|---------|
| `datasource:*` | 动态 | 自定义数据源 | QueryList, QueryPager, Execute |

**使用示例**：

```java
// 查询数据源数据
CommandSupportManagerProviders
    .getCommandSupport("datasource:" + datasourceId)
    .flatMap(cmd -> cmd.execute(new ExecuteCommand(sql, params)));
```

### 3.5 文件服务命令

| 服务ID | 命令支持ID | 说明 | 常用命令 |
|--------|-----------|------|---------|
| `fileService` | ` ` | 文件管理 | Upload, Download, Delete, GetInfo |

### 3.6 认证相关命令

| 服务ID | 命令支持ID | 说明 | 常用命令 |
|--------|-----------|------|---------|
| `authService` | `menu` | 菜单管理 | QueryList, QueryTree |
| `authService` | `user` | 用户管理 | QueryList, QueryPager, QueryById |
| `authService` | `dimension` | 维度管理 | QueryList, QueryPager |

---

## 四、命令定义

### 4.1 基础命令类

**查询列表命令**：

```java
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;

// 创建查询列表命令
QueryListCommand<DeviceInfo> command = QueryListCommand.of(DeviceInfo.class);

// 带条件查询
QueryParamEntity query = new QueryParamEntity();
query.and("productId", "product-001");
QueryListCommand<DeviceInfo> command = QueryListCommand.of(DeviceInfo.class, query);
```

**分页查询命令**：

```java
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;

// 创建分页查询命令
QueryParamEntity query = new QueryParamEntity();
query.setPageSize(20);
query.setPageIndex(0);
QueryPagerCommand<DeviceInfo> command = QueryPagerCommand.of(DeviceInfo.class, query);
```

**根据ID查询命令**：

```java
import org.jetlinks.sdk.server.commons.cmd.QueryByIdCommand;

// 根据ID查询
QueryByIdCommand<DeviceInfo> command = QueryByIdCommand.of(DeviceInfo.class, deviceId);
```

### 4.2 自定义命令类

```java
package org.jetlinks.pro.device.cmd;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.command.AbstractCommand;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;

/**
 * 获取设备配置命令
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetConfigCommand extends AbstractCommand<Mono<Map<String, Object>>, GetConfigCommand> {

    // 设备ID
    private String id;

    // 配置KEY
    private String configKey;

    /**
     * 定义命令元数据
     */
    public static FunctionMetadata metadata() {
        return FunctionMetadata
            .of("GetConfig", "获取设备配置")
            .input(SimplePropertyMetadata.of("id", "设备ID", StringType.GLOBAL))
            .input(SimplePropertyMetadata.of("configKey", "配置KEY", StringType.GLOBAL))
            .output(new ObjectType().toMetadata());
    }
}
```

---

## 五、服务定义最佳实践

### 5.0 服务定义规范

**✅ 强烈推荐：使用 @CommandService 注解**

```java
// ✅ 推荐：使用注解自动注册
@Component
@CommandService(id = "exampleService", name = "示例服务")
public class ExampleCommandService implements CrudCommandHandler<ExampleEntity, String> {
    private final ExampleService service;
    
    public ExampleCommandService(ExampleService service) {
        this.service = service;
    }
    
    @Override
    public ExampleService getService() {
        return service;
    }
    
    // 自定义命令
    @CommandHandler
    public Mono<Result> customCommand(CustomCommand command) {
        return service.doSomething(command);
    }
}

// ❌ 不推荐：手动注册（除非有特殊需求）
@Component
public class ExampleCommandSupportManager extends StaticCommandSupportManagerProvider {
    public ExampleCommandSupportManager(ExampleCommandSupport support) {
        super("exampleService");
        register("example", support);
    }
}
```

**注解方式的优势**：
- ✅ 代码简洁，无需额外配置类
- ✅ 自动扫描注册，减少样板代码
- ✅ 更符合 Spring 的声明式编程风格
- ✅ 易于理解和维护

### 5.1 选择调用方式

```java
// ✅ 推荐：响应式调用（非阻塞，高性能）
public Flux<DeviceInfo> getDevices() {
    return CommandSupportManagerProviders
        .getCommandSupport(SdkServices.deviceService, "device")
        .flatMapMany(cmd -> cmd.execute(QueryListCommand.of(DeviceInfo.class)));
}

// ⚠️ 谨慎使用：阻塞式调用（简单，但性能较低）
public List<DeviceInfo> getDevicesSync() {
    return CommandSupportManagerProviders
        .getCommandSupportNow(SdkServices.deviceService, "device")
        .executeToList(QueryListCommand.of(DeviceInfo.class));
}
```

### 5.2 错误处理

```java
public Mono<DeviceInfo> getDevice(String id) {
    return CommandSupportManagerProviders
        .getCommandSupport(SdkServices.deviceService, "device")
        .flatMap(cmd -> cmd.execute(QueryByIdCommand.of(DeviceInfo.class, id)))
        .switchIfEmpty(Mono.error(new NotFoundException("设备不存在")))
        .onErrorResume(BusinessException.class, e -> {
            log.error("查询设备失败", e);
            return Mono.error(new BusinessException("查询设备失败: " + e.getMessage()));
        });
}
```

### 5.3 性能优化

```java
// 批量查询优化
public Flux<DeviceInfo> batchGetDevices(List<String> deviceIds) {
    return CommandSupportManagerProviders
        .getCommandSupport(SdkServices.deviceService, "device")
        .flatMapMany(cmd -> {
            // 使用 IN 查询
            QueryParamEntity query = new QueryParamEntity();
            query.in("id", deviceIds);
            return cmd.execute(QueryListCommand.of(DeviceInfo.class, query));
        });
}
```

### 5.4 权限继承

```java
// 命令服务的权限注解会自动应用到调用方
@CommandService(id = "exampleService", name = "示例服务")
@Resource(id = "example-resource", name = "示例资源")
@Authorize // 需要认证
public class ExampleCommandService implements CrudCommandHandler<ExampleEntity, String> {

    // 查询操作需要 query 权限
    @QueryAction
    @CommandHandler
    public Mono<ExampleEntity> findByName(FindByNameCommand command) {
        // 调用此命令时会自动检查 query 权限
        return service.findByName(command.getName());
    }
}
```

---

## 六、常见问题

### Q1: 为什么不能直接注入其他服务的 Service？

**不推荐**：

```java
@Service
public class ExampleService {

    // ❌ 不推荐：直接注入其他模块的 Service
    @Autowired
    private DeviceInstanceService deviceService;

    public Mono<Device> getDevice(String id) {
        return deviceService.findById(id);
    }
}
```

**推荐**：

```java
@Service
public class ExampleService {

    // ✅ 推荐：使用命令模式调用
    public Mono<DeviceInfo> getDevice(String id) {
        return CommandSupportManagerProviders
            .getCommandSupport(SdkServices.deviceService, "device")
            .flatMap(cmd -> cmd.execute(QueryByIdCommand.of(DeviceInfo.class, id)));
    }
}
```

**原因**：
- 直接注入会产生强依赖，模块无法独立部署
- 微服务拆分时需要大量重构代码
- 命令模式支持集群路由和负载均衡
- 命令模式自动处理权限和资产过滤

### Q2: 如何知道有哪些可用的命令服务？

**方式1**：通过接口查询

```bash
# 查询所有命令服务
GET /api/command/_services

# 查询特定服务的命令
GET /api/command/deviceService/_supports
```

**方式2**：查看源码

- 查找 `@CommandService` 注解的类
- 查看 `*CommandSupport.java` 文件
- 查看 `*CommandSupportManager.java` 文件

### Q3: 命令服务的 ID 命名规范是什么？

**命名格式**：`{serviceId}:{supportId}`

- `serviceId`: 服务标识（如 `deviceService`, `ruleService`）
- `supportId`: 命令支持标识（如 `device`, `product`, `alarm`）

**示例**：
- `deviceService:device` - 设备服务的设备命令
- `deviceService:product` - 设备服务的产品命令
- `ruleService:alarm` - 规则引擎服务的告警命令
- `datasource:mydb` - 数据源服务的自定义数据源

### Q4: 如何在集群环境中调试命令调用？

```java
// 启用日志跟踪
@GetMapping("/devices")
public Flux<DeviceInfo> getDevices() {
    return CommandSupportManagerProviders
        .getCommandSupport(SdkServices.deviceService, "device")
        .doOnNext(cmd -> log.info("获取到命令支持: {}", cmd))
        .flatMapMany(cmd -> cmd.execute(QueryListCommand.of(DeviceInfo.class)))
        .doOnNext(device -> log.info("查询到设备: {}", device))
        .doOnError(err -> log.error("查询失败", err));
}
```

### Q5: 如何限制边缘端或插件可访问的命令？

在 `application.yml` 中配置：

```yaml
# 限制边缘端可访问的命令
agent:
  command:
    filter:
      enabled: true
      includes:
        "[deviceService:device]": ["QueryPager", "QueryList", "QueryById", "Count"]
        "[deviceService:product]": ["QueryPager", "QueryList"]
        "[fileService]": ["*"]  # 允许所有命令
```

---

## 七、完整示例

### 7.1 Provider 端：定义命令服务（使用注解）

```java
package org.jetlinks.pro.example.cmd;

import org.hswebframework.web.authorization.annotation.*;
import org.jetlinks.core.annotation.command.CommandHandler;
import org.jetlinks.pro.annotation.command.CommandService;
import org.jetlinks.pro.command.crud.CrudCommandHandler;
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.jetlinks.pro.example.service.ExampleService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 示例命令服务
 * 
 * 使用 @CommandService 注解声明命令服务，框架自动扫描注册
 * 无需手动创建 Provider 或 Manager 类
 */
@Component
@CommandService(
    id = "exampleService:example",  // 服务标识
    name = "示例命令服务",              // 服务名称
    description = "提供示例数据的CRUD操作"  // 服务描述（可选）
)
@Resource(id = "example-resource", name = "示例资源")
@Authorize
public class ExampleCommandSupport implements CrudCommandHandler<ExampleEntity, String> {

    private final ExampleService service;

    public ExampleCommandSupport(ExampleService service) {
        this.service = service;
    }

    @Override
    public ExampleService getService() {
        return service;
    }

    // 自定义命令：使用 @CommandHandler 标记
    @CommandHandler
    @QueryAction
    public Mono<ExampleEntity> findByName(FindByNameCommand command) {
        return service.findByName(command.getName());
    }
    
    // 更多自定义命令...
}

// 注意：使用 @CommandService 注解后，无需创建额外的 Manager 或 Provider 类
// 框架会自动扫描并注册该服务
```

### 7.2 Consumer 端：调用命令服务

```java
package org.jetlinks.pro.other.service;

import org.jetlinks.pro.command.CommandSupportManagerProviders;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OtherService {

    // 查询示例列表
    public Flux<ExampleInfo> getExamples() {
        return CommandSupportManagerProviders
            .getCommandSupport("exampleService", "example")
            .flatMapMany(cmd -> cmd.execute(QueryListCommand.of(ExampleInfo.class)));
    }

    // 调用自定义命令
    public Mono<ExampleInfo> findByName(String name) {
        return CommandSupportManagerProviders
            .getCommandSupport("exampleService", "example")
            .flatMap(cmd -> cmd.execute(new FindByNameCommand(name)));
    }
}
```

---

## 八、参考资料

- [命令模式官方文档](https://hanta.yuque.com/px7kg1/dev/ew1xvzmlgbzkc0hy)
- [JetLinks SDK API](https://hanta.yuque.com/px7kg1/dev/sdk)
- [微服务示例](../../microservices/examples/mvc-service)

---

## 总结

**关键要点**：

1. ✅ **使用命令模式进行跨服务调用**，而不是直接注入 Service
2. ✅ **Consumer 端**：使用 `CommandSupportManagerProviders.getCommandSupport()` 获取命令支持
3. ✅ **Provider 端**：**优先使用 @CommandService 注解定义服务**，自动扫描注册
4. ✅ **实现接口**：实现 `CrudCommandHandler` 接口自动获得标准 CRUD 命令
5. ✅ **常用服务**：设备服务（deviceService）、告警服务（ruleService）、通知服务（notifyService）
6. ✅ **权限控制**：命令服务自动继承权限注解，支持资产过滤
7. ✅ **优先使用响应式调用**，性能更好，支持流式处理
8. ✅ **避免手动注册**，除非有特殊需求，否则使用 @CommandService 注解即可

**服务定义最佳实践**：
```java
// ✅ 推荐：简洁的注解方式
@Component
@CommandService(id = "myService", name = "我的服务")
public class MyCommandService implements CrudCommandHandler<MyEntity, String> {
    private final MyService service;
    // 实现...
}

// ❌ 避免：不必要的手动注册
// 除非有特殊需求，否则不需要创建额外的 Manager 类
```

**遵循本规范可以确保服务间松耦合、易于扩展和维护。**

