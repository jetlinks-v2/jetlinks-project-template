# JetLinks 实时数据订阅指南

使用 `@Subscribe` 注解和 `EventBus` 实现实时数据订阅。

## 核心概念

**事件总线 (EventBus)**: 基于字典树的事件订阅发布模型，支持进程内和跨进程事件传递。

**特点**: 支持通配符(`*`/`**`)、集群订阅、优先级控制、消息持久化。

---

## Subscribe 注解

### 基本示例

```java
@Component
public class DeviceDataHandler {

    @Subscribe("/device/*/*/message/**")
    public Mono<Void> handleDeviceMessage(DeviceMessage message) {
        return doSomething(message);
    }
}
```

### 注解属性

| 属性 | 说明 | 默认值 |
|-----|------|-------|
| topics/value | 订阅主题，支持通配符 `*`(单层) `**`(多层) `,`(多选) | - |
| id | 订阅者ID | 方法名 |
| features | 订阅特性(local/broker/shared等) | local |
| priority | 优先级，值越小优先级越高 | Integer.MAX_VALUE |
| buffer | 持久化配置(2.11+) | 关闭 |

---

## Topic 规则

**格式**: `/device/{产品ID}/{设备ID}/message/{消息类型}`

**通配符**:
- `*` - 单层通配符: `/device/*/device-1/online`
- `**` - 多层通配符: `/device/**`
- `,` - 多选符: `/device/*/device-1,device-2/online`
- 表达式: `/device/${config.product-id}/**`

---

## 常见 Topic 速查表

### 设备基础消息

| 消息类型 | Topic | 输出类型 | 说明 |
|---------|-------|---------|------|
| **设备上线** | `/device/*/*/online` | `DeviceOnlineMessage` | 设备上线消息 |
| **设备离线** | `/device/*/*/offline` | `DeviceOfflineMessage` | 设备离线消息 |
| **属性上报** | `/device/*/*/message/property/report` | `ReportPropertyMessage` | 设备主动上报属性值 |
| **事件上报** | `/device/*/*/message/event/{事件ID}` | `EventMessage` | 设备上报事件，{事件ID}为物模型定义的事件标识 |
| **标签更新** | `/device/*/*/message/tags/update` | `UpdateTagMessage` | 设备标签变更消息 |
| **日志消息** | `/device/*/*/message/log` | `DeviceLogMessage` | 设备日志消息 |
| **透传消息** | `/device/*/*/message/direct` | `DirectDeviceMessage` | 设备透传消息 |
| **应答消息** | `/device/*/*/message/acknowledge` | `AcknowledgeDeviceMessage` | 设备消息应答 |
| **所有消息** | `/device/**` | `DeviceMessage` | 订阅该设备的所有消息 |

**说明**: 第一个 `*` 为产品ID，第二个 `*` 为设备ID。例如: `/device/light-product/device-001/online`

### 设备指令与回复

| 消息类型 | Topic | 输出类型 | 说明 |
|---------|-------|---------|------|
| **读取属性** | `/device/*/*/message/send/property/read` | `ReadPropertyMessage` | 平台向设备发送读取属性指令 |
| **读取属性回复** | `/device/*/*/message/property/read/reply` | `ReadPropertyMessageReply` | 设备响应读取属性指令 |
| **修改属性** | `/device/*/*/message/send/property/write` | `WritePropertyMessage` | 平台向设备发送修改属性指令 |
| **修改属性回复** | `/device/*/*/message/property/write/reply` | `WritePropertyMessageReply` | 设备响应修改属性指令 |
| **调用功能** | `/device/*/*/message/send/function` | `FunctionInvokeMessage` | 平台向设备发送调用功能指令 |
| **调用功能回复** | `/device/*/*/message/function/reply` | `FunctionInvokeMessageReply` | 设备响应功能调用指令 |
| **状态检查** | `/device/*/*/message/state_check` | `DeviceStateCheckMessage` | 平台发送状态检查消息 |
| **状态检查回复** | `/device/*/*/message/state_check_reply` | `DeviceStateCheckMessageReply` | 设备响应状态检查 |
| **断开连接** | `/device/*/*/disconnect` | `DisconnectDeviceMessage` | 平台发送断开连接指令 |
| **断开连接回复** | `/device/*/*/disconnect/reply` | `DisconnectDeviceMessageReply` | 设备响应断开连接 |

### 设备注册与注销

| 消息类型 | Topic | 输出类型 | 说明 |
|---------|-------|---------|------|
| **设备注册** | `/device/*/*/register` | `DeviceRegisterMessage` | 设备注册消息 |
| **设备注销** | `/device/*/*/unregister` | `DeviceUnRegisterMessage` | 设备注销消息 |

### 子设备消息

| 消息类型 | Topic | 输出类型 | 说明 |
|---------|-------|---------|------|
| **子设备消息** | `/device/*/*/message/children/{子设备ID}/**` | `ChildDeviceMessage` | 网关代理的子设备消息 |
| **子设备消息回复** | `/device/*/*/message/children/reply/{子设备ID}/**` | `ChildDeviceMessageReply` | 子设备消息回复 |

**示例**: `/device/gateway-product/gateway-001/message/children/child-001/message/property/report`

### 固件升级消息

| 消息类型 | Topic | 输出类型 | 说明 |
|---------|-------|---------|------|
| **拉取固件** | `/device/*/*/firmware/pull` | `RequestFirmwareMessage` | 设备请求拉取固件 |
| **拉取固件回复** | `/device/*/*/firmware/pull/reply` | `RequestFirmwareMessageReply` | 平台响应固件拉取请求 |
| **上报固件信息** | `/device/*/*/firmware/report` | `ReportFirmwareMessage` | 设备上报当前固件信息 |
| **推送固件** | `/device/*/*/firmware/push` | `UpgradeFirmwareMessage` | 平台推送固件升级消息 |
| **推送固件回复** | `/device/*/*/firmware/push/reply` | `UpgradeFirmwareMessageReply` | 设备响应固件推送 |
| **固件安装进度** | `/device/*/*/firmware/progress` | `UpgradeFirmwareProgressMessage` | 设备上报固件升级进度 |

### 告警消息

| 消息类型 | Topic | 输出类型 | 说明 |
|---------|-------|---------|------|
| **告警记录** | `/alarm/{目标类型}/{目标ID}/{告警ID}/record` | `AlarmHistoryInfo` | 告警记录消息 |

**目标类型示例**: `device`(设备), `product`(产品), `org`(机构), `scene`(场景) 等

**示例**: `/alarm/device/device-001/fire-alarm/record`

### 系统消息

| 消息类型 | Topic | 输出类型 | 说明 |
|---------|-------|---------|------|
| **设备注册事件** | `/_sys/registry-device/{设备ID}/register` | - | 设备在注册中心注册 |
| **设备注销事件** | `/_sys/registry-device/{设备ID}/unregister` | - | 设备从注册中心注销 |
| **设备物模型变更** | `/_sys/registry-device/{设备ID}/metadata` | - | 设备物模型发生变更 |
| **产品注册事件** | `/_sys/registry-product/{产品ID}/register` | - | 产品在注册中心注册 |
| **产品注销事件** | `/_sys/registry-product/{产品ID}/unregister` | - | 产品从注册中心注销 |
| **产品物模型变更** | `/_sys/registry-product/{产品ID}/metadata` | - | 产品物模型发生变更 |

### 按维度订阅（带租户/机构信息）

平台支持按照组织等维度订阅消息，格式为: `/{维度1}/{维度值1}/device/{产品ID}/{设备ID}/**`

**示例**:
- 按组织订阅: `/org/org-001/device/*/*/message/property/report`

---

## 消息数据获取

### 从消息中获取常用信息

```java
@Subscribe("/device/*/*/message/property/report")
public Mono<Void> handlePropertyReport(ReportPropertyMessage message) {
    // 获取设备ID
    String deviceId = message.getDeviceId();

    // 获取产品ID（从header）
    String productId = message.getHeaderOrDefault(PropertyConstants.productId);

    // 获取时间戳
    long timestamp = message.getTimestamp();

    // 获取属性值（方式1：使用工具类）
    Map<String, Object> properties = DeviceMessageUtils
        .tryGetProperties(message)
        .orElse(Collections.emptyMap());

    // 获取属性值（方式2：直接获取）
    Map<String, Object> properties2 = message.getProperties();

    // 获取单个属性值
    Object temperature = properties.get("temperature");

    // 获取消息ID
    String messageId = message.getMessageId();

    return Mono.empty();
}
```

### 从事件消息中获取数据

```java
@Subscribe("/device/*/*/message/event/fire_alarm")
public Mono<Void> handleFireAlarm(EventMessage message) {
    // 获取事件标识
    String event = message.getEvent();

    // 获取事件数据
    Object data = message.getData();

    // 如果事件数据是 Map 类型
    if (data instanceof Map) {
        Map<String, Object> eventData = (Map<String, Object>) data;
        String location = (String) eventData.get("location");
        Integer level = (Integer) eventData.get("level");
    }

    return Mono.empty();
}
```

### 从告警消息中获取数据

```java
@Subscribe("/alarm/device/*/*/record")
public Mono<Void> handleAlarm(AlarmHistoryInfo alarm) {
    // 告警配置ID
    String alarmConfigId = alarm.getAlarmConfigId();

    // 告警目标类型（device、product、org等）
    String targetType = alarm.getTargetType();

    // 告警目标ID
    String targetId = alarm.getTargetId();

    // 告警级别
    Integer level = alarm.getAlarmLevel();

    // 告警说明
    String description = alarm.getAlarmDescription();

    // 告警时间
    Long alarmTime = alarm.getAlarmTime();

    // 告警数据
    Map<String, Object> alarmData = alarm.getAlarmData();

    return Mono.empty();
}
```

### 从固件消息中获取数据

```java
@Subscribe("/device/*/*/firmware/report")
public Mono<Void> handleFirmwareReport(ReportFirmwareMessage message) {
    // 获取固件版本
    String version = message.getVersion();

    // 获取其他固件信息（如果有）
    Map<String, Object> properties = message.getProperties();

    return Mono.empty();
}

@Subscribe("/device/*/*/firmware/progress")
public Mono<Void> handleFirmwareProgress(UpgradeFirmwareProgressMessage message) {
    // 获取升级进度（0-100）
    int progress = message.getProgress();

    // 获取是否完成
    boolean complete = message.isComplete();

    // 获取升级状态描述
    String description = message.getProgressText();

    return Mono.empty();
}
```

---

## 常见应用场景

### 场景 1: 订阅属性上报并更新 Redis

```java
@Component
public class DevicePropertyToRedisHandler {

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Subscribe(
        topics = "/device/*/*/message/property/report",
        features = {Subscription.Feature.local, Subscription.Feature.broker}
    )
    public Mono<Void> handlePropertyReport(DeviceMessage message) {
        Map<String, Object> properties = DeviceMessageUtils
            .tryGetProperties(message)
            .orElse(null);

        if (properties == null || properties.isEmpty()) {
            return Mono.empty();
        }

        String key = "device:latest:" + message.getDeviceId();
        return redisTemplate.opsForHash()
            .putAll(key, properties)
            .then(redisTemplate.expire(key, Duration.ofDays(7)))
            .then();
    }
}
```

### 场景 2: 订阅设备上下线

```java
@Component
public class DeviceStateHandler {

    @Subscribe(topics = "/device/*/*/online", priority = 100)
    public Mono<Void> handleOnline(DeviceOnlineMessage message) {
        log.info("设备[{}]上线", message.getDeviceId());
        return notifyService.send("设备上线", message.getDeviceId());
    }

    @Subscribe(topics = "/device/*/*/offline", priority = 100)
    public Mono<Void> handleOffline(DeviceOfflineMessage message) {
        log.warn("设备[{}]离线", message.getDeviceId());
        return notifyService.send("设备离线", message.getDeviceId());
    }
}
```

### 场景 3: 订阅设备事件并处理

```java
@Component
@Slf4j
public class DeviceEventHandler {

    @Autowired
    private DeviceRegistry deviceRegistry;

    @Autowired
    private EventRecordService eventRecordService;

    /**
     * 订阅特定事件：火灾告警
     */
    @Subscribe(
        topics = "/device/*/*/message/event/fire_alarm",
        features = {Subscription.Feature.local, Subscription.Feature.broker},
        priority = 1
    )
    public Mono<Void> handleFireAlarm(EventMessage message) {
        log.error("火灾告警: deviceId={}, event={}, data={}",
            message.getDeviceId(),
            message.getEvent(),
            message.getData());

        return deviceRegistry
            .getDevice(message.getDeviceId())
            .flatMap(device -> {
                // 保存事件记录
                EventRecord record = new EventRecord();
                record.setDeviceId(message.getDeviceId());
                record.setEvent(message.getEvent());
                record.setData(message.getData());
                record.setTimestamp(message.getTimestamp());

                return eventRecordService.save(record);
            })
            .then();
    }

    /**
     * 订阅所有设备的所有事件
     */
    @Subscribe(
        topics = "/device/*/*/message/event/*",
        features = {Subscription.Feature.local, Subscription.Feature.broker}
    )
    public Mono<Void> handleAllEvents(EventMessage message) {
        log.info("收到设备事件: deviceId={}, event={}",
            message.getDeviceId(),
            message.getEvent());

        // 处理事件逻辑
        return Mono.empty();
    }
}
```

### 场景 4: 订阅告警消息

```java
@Component
@Slf4j
public class AlarmNotificationHandler {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AlarmConfigService alarmConfigService;

    /**
     * 订阅所有设备告警
     */
    @Subscribe(
        topics = "/alarm/device/*/*/record",
        features = {Subscription.Feature.local, Subscription.Feature.broker},
        priority = 1
    )
    public Mono<Void> handleDeviceAlarm(AlarmHistoryInfo alarm) {
        log.warn("收到设备告警: targetId={}, alarmId={}, level={}, description={}",
            alarm.getTargetId(),
            alarm.getAlarmConfigId(),
            alarm.getAlarmLevel(),
            alarm.getAlarmDescription());

        // 获取告警配置
        return alarmConfigService
            .findById(alarm.getAlarmConfigId())
            .flatMap(config -> {
                // 根据告警级别发送不同类型的通知
                if (alarm.getAlarmLevel() >= 3) {
                    // 高级别告警：发送短信和邮件
                    return Mono.when(
                        notificationService.sendSms(alarm),
                        notificationService.sendEmail(alarm)
                    );
                } else {
                    // 低级别告警：仅发送站内消息
                    return notificationService.sendMessage(alarm);
                }
            })
            .then();
    }

    /**
     * 订阅所有类型的告警（设备、产品、机构、场景等）
     */
    @Subscribe(
        topics = "/alarm/*/*/*/record",
        features = {Subscription.Feature.local, Subscription.Feature.broker}
    )
    public Mono<Void> handleAllAlarms(AlarmHistoryInfo alarm) {
        log.info("收到告警: type={}, targetId={}, alarmId={}",
            alarm.getTargetType(),
            alarm.getTargetId(),
            alarm.getAlarmConfigId());

        // 统一告警处理逻辑
        return processAlarm(alarm);
    }

    /**
     * 订阅特定告警配置的告警
     */
    @Subscribe(
        topics = "/alarm/*/*/high-temperature-alarm/record",
        features = {Subscription.Feature.local, Subscription.Feature.broker}
    )
    public Mono<Void> handleHighTemperatureAlarm(AlarmHistoryInfo alarm) {
        log.error("高温告警: targetType={}, targetId={}",
            alarm.getTargetType(),
            alarm.getTargetId());

        // 处理高温告警的特殊逻辑
        return handleHighTemperature(alarm);
    }
}
```

### 场景 5: 订阅固件升级消息

```java
@Component
@Slf4j
public class FirmwareUpgradeHandler {

    @Autowired
    private DeviceRegistry deviceRegistry;

    @Autowired
    private FirmwareUpgradeService upgradeService;

    /**
     * 订阅固件信息上报
     */
    @Subscribe(
        topics = "/device/*/*/firmware/report",
        features = {Subscription.Feature.local, Subscription.Feature.broker}
    )
    public Mono<Void> handleFirmwareReport(ReportFirmwareMessage message) {
        log.info("设备上报固件信息: deviceId={}, version={}",
            message.getDeviceId(),
            message.getVersion());

        // 保存固件信息到数据库
        return upgradeService.saveFirmwareInfo(
            message.getDeviceId(),
            message.getVersion(),
            message.getProperties()
        ).then();
    }

    /**
     * 订阅固件升级进度
     */
    @Subscribe(
        topics = "/device/*/*/firmware/progress",
        features = {Subscription.Feature.local, Subscription.Feature.broker}
    )
    public Mono<Void> handleUpgradeProgress(UpgradeFirmwareProgressMessage message) {
        int progress = message.getProgress();
        boolean complete = message.isComplete();

        log.info("固件升级进度: deviceId={}, progress={}%, complete={}",
            message.getDeviceId(),
            progress,
            complete);

        // 更新升级进度
        return upgradeService.updateProgress(
            message.getDeviceId(),
            progress,
            complete
        ).then();
    }
}
```

### 场景 6: 使用 Buffer 处理耗时操作

```java
@Component
@Slf4j
public class HeavyOperationHandler {

    @Autowired
    private DatabaseOperator databaseOperator;

    /**
     * 订阅消息并批量缓冲处理
     * 当 buffer.size > 1 时，方法参数必须使用 List<DeviceMessage>
     */
    @Subscribe(
        topics = "/device/*/*/message/property/report",
        features = Subscription.Feature.local,
        buffer = @Buffer(
            size = 200 // 缓冲区大小：每200条触发一次批量处理
        )
    )
    public Mono<Void> handleBatchMessages(List<DeviceMessage> messages) {
        log.info("批量处理 {} 条消息", messages.size());

        // 转换为数据库记录
        List<Map<String, Object>> dataList = messages.stream()
            .map(message -> {
                Map<String, Object> properties = DeviceMessageUtils
                    .tryGetProperties(message)
                    .orElse(Collections.emptyMap());

                Map<String, Object> data = new HashMap<>(properties);
                data.put("device_id", message.getDeviceId());
                data.put("timestamp", message.getTimestamp());
                return data;
            })
            .filter(data -> data.size() > 2)  // 过滤掉没有属性的数据
            .collect(Collectors.toList());

        if (dataList.isEmpty()) {
            return Mono.empty();
        }

        // 批量写入数据库（耗时操作）
        return databaseOperator
            .insert("device_data")
            .values(dataList)
            .execute()
            .then()
            .doOnSuccess(v -> log.debug("批量写入成功: {} 条", dataList.size()))
            .onErrorResume(err -> {
                log.error("批量写入失败: {} 条", dataList.size(), err);
                return Mono.empty();
            });
    }
}
```

**关键说明**:
- ⚠️ **当 `size > 1` 时，方法参数必须使用 `List<DeviceMessage>`**
- 如果 `size = 1`，可以使用单个 `DeviceMessage` 参数
- `size`: 缓冲区大小，达到此数量触发一次批量处理
- 框架自动批量缓冲消息，然后调用处理方法

---

## 订阅特性

| 特性 | 说明 |
|-----|------|
| local | 只订阅当前进程的消息 (默认) |
| broker | 订阅集群其他节点的消息 |
| shared | 共享订阅，多个订阅者只有一个收到 (负载均衡) |
| sharedLocalFirst | 共享订阅-本地优先 |
| safetySerialization | 安全序列化，避免集群序列化错误 |

**常用组合**:
- 单机: `local`
- 集群: `{local, broker}`
- 集群负载均衡: `{local, broker, shared}`

### 跨微服务订阅说明

⚠️ **重要**: 当跨微服务订阅消息时，如果订阅方和发布方**没有使用相同的模块**（即订阅方无法识别发布方的消息类型类），必须使用 `safetySerialization` 特性。

**使用场景**:
- 微服务 A 发布了自定义消息类型（如 `CustomDeviceMessage`）
- 微服务 B 需要订阅这个消息，但没有引入 A 的模块依赖
- 此时 B 无法反序列化为 `CustomDeviceMessage` 类型

**解决方案**:

```java
@Component
public class CrossServiceMessageHandler {

    /**
     * 跨微服务订阅，使用安全序列化
     * 接收到的消息会被反序列化为 Map<String, Object>
     */
    @Subscribe(
        topics = "/device/*/*/message/custom",
        features = {
            Subscription.Feature.local, 
            Subscription.Feature.broker,
            Subscription.Feature.safetySerialization  // 关键：安全序列化
        }
    )
    public Mono<Void> handleCustomMessage(Map<String, Object> messageMap) {
        // 消息会被反序列化为 Map，可以安全获取各个字段
        String deviceId = (String) messageMap.get("deviceId");
        Long timestamp = (Long) messageMap.get("timestamp");
        Map<String, Object> data = (Map<String, Object>) messageMap.get("data");
        
        log.info("收到跨服务消息: deviceId={}, timestamp={}, data={}", 
            deviceId, timestamp, data);
        
        return processMessage(messageMap).then();
    }
    
    /**
     * 或者使用 TopicPayload 获取原始数据
     */
    @Subscribe(
        topics = "/device/*/*/message/custom",
        features = {
            Subscription.Feature.local, 
            Subscription.Feature.broker,
            Subscription.Feature.safetySerialization
        }
    )
    public Mono<Void> handleWithPayload(TopicPayload payload) {
        // 手动解码为 Map
        Map<String, Object> messageMap = payload.decode(Map.class);
        
        // 或者获取 JSON 字符串
        String json = payload.bodyToString();
        
        return Mono.empty();
    }
}
```

**最佳实践**:
1. **同微服务内**：直接使用具体消息类型，无需 `safetySerialization`
2. **跨微服务**：优先共享消息类型定义（通过公共模块），如果无法共享，使用 `safetySerialization` + `Map` 接收
3. **参数类型**：使用 `safetySerialization` 时，方法参数应为 `Map<String, Object>` 或 `TopicPayload`

---

## 最佳实践

1. **使用精确 Topic**: 直接订阅需要的消息类型，避免订阅 `/device/**` 后再过滤
2. **避免阻塞**: 使用响应式 API，避免 `Thread.sleep()` 等阻塞操作
3. **错误处理**: 使用 `onErrorResume` 捕获异常，返回 `Mono.empty()` 避免订阅被取消
4. **批量处理**: 使用 `Flux.buffer()` 批量处理数据
5. **合理选择特性**: 单机用 `local`，集群用 `{local, broker}`

```java
// 正确的错误处理
@Subscribe(topics = "/device/**")
public Mono<Void> handle(DeviceMessage message) {
    return processMessage(message)
        .onErrorResume(err -> {
            log.error("处理失败", err);
            return Mono.empty();  // 重要：避免订阅被取消
        });
}
```

---

## 常见问题

**Q: 方法支持哪些参数?**
- `DeviceMessage` - 具体消息类型
- `Map<String, Object>` - 使用 `safetySerialization` 时接收的通用类型
- `TopicPayload` - 原始数据
- 无参数

**Q: 返回值要求?**
- 推荐: `Mono<Void>`
- 支持: `void`, `Mono<?>`

**Q: 如何取消订阅?**
- @Subscribe 注解: 自动管理
- 编程式: 调用 `disposable.dispose()`

**Q: 如何调试?**
```yaml
logging.level:
  org.jetlinks.core.event: DEBUG
```

**Q: 异常处理?**
⚠️ 通过eventbus.subscribe()订阅的消息，必须捕获异常并返回 `Mono.empty()`，否则订阅会被取消！

```java
eventbus
.subscribe(...)
.flatMap(data->
   process(data)
  .onErrorResume(err -> {
    log.error("处理失败", err);
    return Mono.empty();  // 关键
  })
.subscribe();
```

**Q: 跨微服务订阅时收到序列化错误怎么办?**

如果看到类似 `ClassNotFoundException` 或序列化异常，说明订阅方无法识别发布方的消息类型。

**解决方案**:
1. **推荐**: 将消息类型定义提取到公共模块，双方都引入
2. **临时方案**: 使用 `safetySerialization` 特性，参数改为 `Map<String, Object>`

```java
// 错误示例 - 会抛出序列化异常
@Subscribe(topics = "/custom/message", features = {Feature.local, Feature.broker})
public Mono<Void> handle(CustomMessage message) { // CustomMessage 类在本服务中不存在
    return Mono.empty();
}

// 正确示例 - 使用安全序列化
@Subscribe(
    topics = "/custom/message", 
    features = {Feature.local, Feature.broker, Feature.safetySerialization}
)
public Mono<Void> handle(Map<String, Object> messageMap) { // 使用 Map 接收
    return Mono.empty();
}
```

---

## 快速参考

### 常用订阅场景

| 场景 | Topic | Features | 参数类型 |
|-----|-------|----------|---------|
| 属性上报 | `/device/*/*/message/property/report` | `local, broker` | `ReportPropertyMessage` |
| 设备上线 | `/device/*/*/online` | `local, broker` | `DeviceOnlineMessage` |
| 设备离线 | `/device/*/*/offline` | `local, broker` | `DeviceOfflineMessage` |
| 事件上报 | `/device/*/*/message/event/*` | `local, broker` | `EventMessage` |
| 特定事件 | `/device/*/*/message/event/fire_alarm` | `local, broker` | `EventMessage` |
| 标签更新 | `/device/*/*/message/tags/update` | `local, broker` | `UpdateTagMessage` |
| 所有消息 | `/device/**` | `local` | `DeviceMessage` |
| 固件上报 | `/device/*/*/firmware/report` | `local, broker` | `ReportFirmwareMessage` |
| 固件进度 | `/device/*/*/firmware/progress` | `local, broker` | `UpgradeFirmwareProgressMessage` |
| 告警记录 | `/alarm/*/*/*/record` | `local, broker` | `AlarmHistoryInfo` |
| 集群订阅 | 任意 | `local, broker` | - |
| 负载均衡 | 任意 | `local, broker, shared` | - |
| 批量缓冲 | 任意 + `@Buffer(size>1)` | 任意 | `List<消息类型>` |

### 通配符示例

| 需求 | Topic 示例 |
|-----|----------|
| 订阅所有设备的属性上报 | `/device/*/*/message/property/report` |
| 订阅某产品所有设备上线 | `/device/product-001/*/online` |
| 订阅某设备所有消息 | `/device/*/device-001/**` |
| 订阅某设备的所有事件 | `/device/*/device-001/message/event/*` |
| 订阅某产品某设备的特定事件 | `/device/product-001/device-001/message/event/fire_alarm` |
| 订阅多个设备的上线消息 | `/device/*/device-001,device-002,device-003/online` |
| 订阅所有设备告警 | `/alarm/device/*/*/record` |
| 订阅某个告警配置的所有告警 | `/alarm/*/*/alarm-config-001/record` |

### 消息类型与参数对照

根据订阅的 Topic，方法参数类型应与消息类型匹配：

| Topic 模式 | 推荐参数类型 | 说明 |
|-----------|------------|------|
| `/device/*/*/online` | `DeviceOnlineMessage` | 设备上线消息 |
| `/device/*/*/offline` | `DeviceOfflineMessage` | 设备离线消息 |
| `/device/*/*/message/property/report` | `ReportPropertyMessage` | 属性上报消息 |
| `/device/*/*/message/event/*` | `EventMessage` | 事件消息 |
| `/device/*/*/message/tags/update` | `UpdateTagMessage` | 标签更新消息 |
| `/device/*/*/message/log` | `DeviceLogMessage` | 设备日志消息 |
| `/device/*/*/firmware/**` | `DeviceMessage` | 固件相关消息 |
| `/alarm/*/*/*/record` | `AlarmHistoryInfo` | 告警记录 |
| `/device/**` | `DeviceMessage` | 所有设备消息 |
| 不确定类型时 | `TopicPayload` | 通用数据载体 |

