# 事件驱动开发规范

## 概述
平台使用Spring Event实现功能解耦，支持实体类增删改查事件和自定义事件。

---

## 一、通用CRUD事件监听

### 1.1 基础用法（响应式环境）

```java
@Component
@Slf4j
@AllArgsConstructor
public class ExampleEventHandler {

    @EventListener
    public void handleSaved(EntitySavedEvent<ExampleEntity> event) {
        // 响应式操作必须使用 event.async() 包装
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::doSomething)
                .then()
        );
    }

    @EventListener
    public void handleCreated(EntityCreatedEvent<ExampleEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::doSomething)
                .then()
        );
    }

    @EventListener
    public void handleModified(EntityModifyEvent<ExampleEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())  // 注意：使用 getAfter() 获取修改后的数据
                .flatMap(this::doSomething)
                .then()
        );
    }

    @EventListener
    public void handleDeleted(EntityDeletedEvent<ExampleEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(this::doCleanup)
                .then()
        );
    }

    private Mono<Void> doSomething(ExampleEntity entity) {
        log.info("处理实体: {}", entity.getId());
        return Mono.empty();
    }
}
```

### 1.2 阻塞式环境（MVC）

```java
@Component
@Slf4j
public class ExampleEventHandler {

    @EventListener
    public void handleSaved(EntitySavedEvent<ExampleEntity> event) {
        // MVC环境直接处理，无需 event.async()
        for (ExampleEntity entity : event.getEntity()) {
            log.info("保存后: {}", entity);
            // 执行同步操作
        }
    }

    // 如需在事务提交后执行
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSavedAfterCommit(EntitySavedEvent<ExampleEntity> event) {
        // 事务提交后执行，不会回滚
        for (ExampleEntity entity : event.getEntity()) {
            log.info("事务提交后: {}", entity);
        }
    }
}
```

---

## 二、事件类型说明

| 事件类型 | 触发时机 | 数据获取方法 | 说明 |
|---------|---------|------------|------|
| `EntityPrepareCreateEvent` | 创建前预处理 | `getEntity()` | 可修改实体属性 |
| `EntityBeforeCreateEvent` | 创建前校验 | `getEntity()` | 用于参数校验 |
| `EntityCreatedEvent` | 创建后 | `getEntity()` | 事务提交后触发 |
| `EntityPrepareSaveEvent` | 保存前预处理 | `getEntity()` | 可修改实体属性 |
| `EntityBeforeSaveEvent` | 保存前校验 | `getEntity()` | 用于参数校验 |
| `EntitySavedEvent` | 保存后 | `getEntity()` | 事务提交后触发 |
| `EntityPrepareModifyEvent` | 修改前预处理 | `getAfter()` | 可修改实体属性 |
| `EntityBeforeModifyEvent` | 修改前校验 | `getBefore()` / `getAfter()` | 用于参数校验 |
| `EntityModifyEvent` | 修改后 | `getBefore()` / `getAfter()` | 事务提交后触发 |
| `EntityBeforeDeleteEvent` | 删除前校验 | `getEntity()` | 可校验能否删除 |
| `EntityDeletedEvent` | 删除后 | `getEntity()` | 事务提交后触发 |

---

## 三、实战示例

### 3.1 设备激活时同步资产

```java
@Component
@Slf4j
@AllArgsConstructor
public class DeviceAssetsSynchronizer {

    private final AssetsHolderCrudService assetsService;

    @EventListener
    public void handleDeviceDeploy(DeviceDeployedEvent event) {
        event.async(
            syncDeviceAssets(
                event.getDevices()
                    .stream()
                    .map(DeviceInstanceEntity::getId)
                    .collect(Collectors.toList())
            )
        );
    }

    private Mono<Void> syncDeviceAssets(List<String> deviceIds) {
        return Flux.fromIterable(deviceIds)
            .flatMap(assetsService::syncAssets)
            .then();
    }
}
```

### 3.2 实体保存时发布到EventBus

```java
@Component
@Slf4j
@AllArgsConstructor
public class ExampleEventHandler {

    private final EventBus eventBus;

    @EventListener
    public void handleCreated(EntityCreatedEvent<ExampleEntity> event) {
        event.async(handleRelationEvent(event.getEntity(), "created"));
    }

    private Mono<Void> handleRelationEvent(Collection<ExampleEntity> entities, String operation) {
        return Flux.fromIterable(entities)
            .flatMap(entity -> {
                String topic = "/example/" + entity.getId() + "/" + operation;
                return eventBus.publish(topic, entity);
            })
            .then();
    }
}
```

### 3.3 用户Token变更时发布通知

```java
@Component
@Slf4j
@AllArgsConstructor
public class UserTokenEventHandler {

    private final EventBus eventBus;

    @EventListener
    public void handleTokenRemoved(UserTokenRemovedEvent event) {
        event.async(doPublish(UserTokenEvent.ofRemove(event)));
    }

    @EventListener
    public void handleTokenCreated(UserTokenCreatedEvent event) {
        event.async(doPublish(UserTokenEvent.ofCreate(event)));
    }

    private Mono<Void> doPublish(UserTokenEvent tokenEvent) {
        return Authentication.currentReactive()
            .map(tokenEvent::with)
            .defaultIfEmpty(tokenEvent)
            .flatMap(evt -> eventBus.publish(topic(evt), evt))
            .then();
    }

    private String topic(UserTokenEvent event) {
        return "/user/token-changed/" + event.getUserId() + "/" + event.getToken();
    }
}
```

### 3.4 实体修改时获取修改前后的值

```java
@Component
@Slf4j
public class ExampleEventHandler {

    @EventListener
    public void handleModified(EntityModifyEvent<ExampleEntity> event) {
        event.async(
            Flux.range(0, event.getBefore().size())
                .flatMap(index -> {
                    ExampleEntity before = event.getBefore().get(index);
                    ExampleEntity after = event.getAfter().get(index);

                    log.info("修改前: {}, 修改后: {}", before, after);

                    // 比对变化并处理
                    if (!before.getName().equals(after.getName())) {
                        return handleNameChanged(before, after);
                    }
                    return Mono.empty();
                })
                .then()
        );
    }

    private Mono<Void> handleNameChanged(ExampleEntity before, ExampleEntity after) {
        // 处理名称变更逻辑
        return Mono.empty();
    }
}
```

---

## 四、集群事件监听

### 4.1 实现ClusterEntityEventListener

用于监听整个集群下的实体事件（企业版特性）

```java
@Component
public class ExampleClusterEventListener implements ClusterEntityEventListener<ExampleEntity> {

    @Override
    public Mono<Void> doHandleCreated(ExampleEntity entity, boolean fromAnotherNode) {
        if (fromAnotherNode) {
            // 来自集群其他节点的事件
        }
        // 处理创建事件
        return Mono.empty();
    }

    @Override
    public Mono<Void> doHandleSaved(ExampleEntity entity, boolean fromAnotherNode) {
        // 处理保存事件
        return Mono.empty();
    }

    @Override
    public Mono<Void> doHandleDeleted(ExampleEntity entity, boolean fromAnotherNode) {
        // 处理删除事件
        return Mono.empty();
    }

    @Override
    public Mono<Void> doHandleModified(ExampleEntity before, ExampleEntity after, boolean fromAnotherNode) {
        // 处理修改事件
        return Mono.empty();
    }

    @Override
    public boolean isParallel() {
        return true; // true=并行处理, false=串行处理
    }
}
```

---

## 五、开启实体事件

### 5.1 使用@EnableEntityEvent注解

```java
@Table(name = "example_entity")
@Comment("示例实体")
@EnableEntityEvent  // 开启实体事件
public class ExampleEntity extends GenericEntity<String> {
    // 实体定义
}
```

### 5.2 自定义实体事件

```java
@Component
public class ExampleEntityEventCustomizer implements EntityEventListenerCustomizer {

    @Override
    public void customize(EntityEventListenerRegistry registry) {
        // 为指定实体类型注册事件监听
        registry.register(ExampleEntity.class);
    }
}
```

---

## 六、注意事项

### 6.1 响应式环境必须使用event.async()

```java
// ✅ 正确
@EventListener
public void handleEvent(EntitySavedEvent<ExampleEntity> event) {
    event.async(
        doSomethingReactive()
    );
}

// ❌ 错误 - 响应式操作不会执行
@EventListener
public void handleEvent(EntitySavedEvent<ExampleEntity> event) {
    doSomethingReactive().subscribe();  // 不要这样做
}
```

### 6.2 泛型指定实体类型

```java
// 只监听 ExampleEntity 的事件
@EventListener
public void handleEvent(EntitySavedEvent<ExampleEntity> event) { }

// 监听所有实体的事件
@EventListener
public void handleEvent(EntitySavedEvent event) { }
```

### 6.3 事件在事务提交后触发

- `EntityCreatedEvent`、`EntitySavedEvent`、`EntityModifyEvent`、`EntityDeletedEvent` 在事务提交后触发
- 事件处理中发生错误不会回滚数据库事务
- MVC环境可使用 `@TransactionalEventListener` 精确控制触发时机

### 6.4 避免事件循环

```java
// ❌ 可能导致无限循环
@EventListener
public void handleSaved(EntitySavedEvent<ExampleEntity> event) {
    event.async(
        repository.save(event.getEntity())  // 又会触发 EntitySavedEvent
    );
}
```

---

## 七、快速参考

### 7.1 响应式环境模板

```java
@Component
@Slf4j
@AllArgsConstructor
public class YourEntityEventHandler {

    @EventListener
    public void handleSaved(EntitySavedEvent<YourEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(entity -> {
                    // 你的响应式逻辑
                    return Mono.empty();
                })
                .then()
        );
    }
}
```

### 7.2 阻塞式环境模板

```java
@Component
@Slf4j
public class YourEntityEventHandler {

    @EventListener
    public void handleSaved(EntitySavedEvent<YourEntity> event) {
        for (YourEntity entity : event.getEntity()) {
            // 你的同步逻辑
        }
    }
}
```

---
