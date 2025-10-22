# JetLinks CRUD 进阶使用规范

## 概述

本规范专注于 JetLinks 项目中 CRUD 功能的进阶使用场景，包括事件驱动开发、复杂查询构建、响应式与阻塞式编程模式选择等高级功能。

---

## 一、事件驱动开发规范

### 1.1 场景识别

当用户提到以下需求时，应引导到事件驱动开发：

- **"当XXX保存时，同时处理YYY"**
- **"当XXX删除时，清理相关的ZZZ"**
- **"当XXX修改时，同步更新AAA"**
- **"当XXX创建时，自动创建BBB"**

### 1.2 引导到事件驱动规则

```markdown
根据您的需求，这属于事件驱动开发场景。请参考：

📖 **事件驱动开发规范**: [event-driven-rules.md](./event-driven-rules.md)

该文档包含：
- 实体增删改查事件监听
- 响应式与阻塞式事件处理
- 集群事件监听
- 实战示例和最佳实践
```

### 1.3 常见事件驱动场景

#### 1.3.1 资产同步场景
```java
@EventListener
public void handleEntityDeploy(EntityDeployedEvent event) {
    event.async(
        syncAssets(
            event.getEntities()
                .stream()
                .map(Entity::getId)
                .collect(Collectors.toList())
        )
    );
}
```

#### 1.3.2 数据收集器注册场景
```java
@EventListener
public void handleEntitySaved(EntitySavedEvent<YourEntity> event) {
    if (!targetEntityType().isAssignableFrom(event.getEntityType())) {
        return;
    }
    event.async(
        Flux.fromIterable(event.getEntity())
            .flatMap(this::registerCollector)
    );
}
```

#### 1.3.3 缓存更新场景
```java
@EventListener
public void handleEntityModified(EntityModifyEvent<YourEntity> event) {
    event.async(
        Flux.fromIterable(event.getAfter())
            .flatMap(this::updateCache)
    );
}
```

---

## 二、复杂查询构建规范

### 2.1 响应式复杂查询

#### 2.1.1 基础复杂查询模式

```java
// 使用 createQuery() 构建复杂查询
public Mono<Integer> processEntities(QueryParamEntity query, Supplier<Message> templateSupplier) {
    return entityService
        .createQuery()
        .setParam(query)
        .select(Entity::getId, Entity::getName, Entity::getType)
        .fetch()
        .map(entity -> {
            // 处理逻辑
            return processEntity(entity);
        })
        .buffer(200)
        .flatMap(list -> this.processBatch(list).thenReturn(list.size()))
        .reduce(Math::addExact);
}
```

#### 2.1.2 条件查询构建

```java
// 动态条件查询
private Flux<YourEntity> findEntitiesByCondition(long total) {
    return QueryParamEntity
        .newQuery()
        .when(total >= Integer.MAX_VALUE, Query::noPaging)
        .when(total < Integer.MAX_VALUE, query -> query.doPaging(0, (int) total))
        .where(YourEntity::getServerId, getCurrentServerId())
        .in(YourEntity::getState, EntityState.active)
        .execute(repository::query);
}
```

#### 2.1.3 关联查询

```java
// 使用 QueryHelper 进行关联查询
private Flux<YourInfo> queryWithJoin(QueryParamEntity param, BiFunction<String, QueryParamEntity, QueryParamEntity> queryFunc) {
    return Authentication
        .currentReactive()
        .map(auth -> auth.getUser().getId())
        .flatMapMany(userId -> queryHelper
            .select(YourInfo.class)
            .all(YourEntity.class)
            .as(RelatedEntity::getName, YourInfo::setRelatedName)
            .from(YourEntity.class)
            .leftJoin(RelatedEntity.class, cdt -> cdt.is(YourEntity::getRelatedId, RelatedEntity::getId))
            .where(queryFunc.apply(userId, param))
            .fetch()
        );
}
```

#### 2.1.4 原生SQL查询

##### 2.1.4.1 使用 QueryHelper 执行原生SQL

```java
// 使用 QueryHelper 执行原生SQL查询
@Autowired
private QueryHelper queryHelper;

// 基础原生SQL查询
public Flux<Record> executeNativeQuery(String sql, Object... args) {
    return queryHelper
        .select(sql, args)
        .fetch();
}

// 设备实例关联查询示例
public Flux<Record> queryDeviceWithProduct(String deviceId) {
    String sql = """
        select device.*, product.name as product_name, product.type as product_type
        from dev_device_instance device 
        left join dev_product product on device.product_id = product.id
        where device.id = ?
        """;
    return queryHelper
        .select(sql, deviceId)
        .fetch();
}

// 带动态条件的原生SQL查询
public Flux<Record> executeNativeQueryWithConditions(String sql, QueryParamEntity param, Object... args) {
    return queryHelper
        .select(sql, args)
        .where(param)
        .fetch();
}

// 带DSL条件的原生SQL查询
public Flux<Record> executeNativeQueryWithDSL(String sql, Object... args) {
    return queryHelper
        .select(sql, args)
        .where(dsl -> dsl.like("name", "%keyword%").and().gt("age", 18))
        .orderByAsc("create_time")
        .fetch();
}

// 复杂关联查询示例
public Flux<Record> queryDeviceWithTagsAndBinds(String productId) {
    String sql = """
        select device.id, device.name, device.state,
               tag.key as tag_key, tag.value as tag_value,
               bind.third_party_type, bind.third_party_id
        from dev_device_instance device 
        left join dev_device_tag tag on device.id = tag.device_id
        left join dev_device_bind bind on device.id = bind.device_id
        where device.product_id = ?
        """;
    return queryHelper
        .select(sql, productId)
        .fetch();
}

// 聚合查询示例
public Flux<Record> queryDeviceStatistics(String productId) {
    String sql = """
        select product_id, 
               count(*) as total_count,
               sum(case when state = 'online' then 1 else 0 end) as online_count,
               sum(case when state = 'offline' then 1 else 0 end) as offline_count
        from dev_device_instance 
        where product_id = ?
        group by product_id
        """;
    return queryHelper
        .select(sql, productId)
        .fetch();
}

// 分页查询
public Mono<PagerResult<Record>> executeNativeQueryPaged(String sql, QueryParamEntity param, Object... args) {
    return queryHelper
        .select(sql, args)
        .where(param)
        .fetchPaged();
}

// 转换为实体类的原生SQL查询
public Flux<DeviceInfo> executeNativeQueryToEntity(String sql, Object... args) {
    return queryHelper
        .select(sql, DeviceInfo::new, args)
        .fetch();
}

// 子查询示例
public Flux<Record> queryDeviceWithSubQuery(String productId) {
    String sql = """
        select device.*, 
               (select count(*) from dev_device_tag where device_id = device.id) as tag_count,
               (select count(*) from dev_device_bind where device_id = device.id) as bind_count
        from dev_device_instance device 
        where device.product_id = ?
        """;
    return queryHelper
        .select(sql, productId)
        .fetch();
}

// UNION查询示例
public Flux<Record> queryDeviceUnion(String deviceId) {
    String sql = """
        select id, name, 'device' as type from dev_device_instance where id = ?
        union all
        select id, name, 'product' as type from dev_product where id = ?
        """;
    return queryHelper
        .select(sql, deviceId, deviceId)
        .fetch();
}
```
 

### 2.2 阻塞式复杂查询

#### 2.2.1 基础查询模式

```java
// 参考: microservices/examples/mvc-service/src/main/java/org/jetlinks/pro/crud/event/ExampleEventHandler.java
@EventListener
public void handleEvent(EntitySavedEvent<ExampleEntity> event) {
    for (ExampleEntity exampleEntity : event.getEntity()) {
        // 同步处理逻辑
        log.info("保存后: {}", exampleEntity);
    }
}
```

#### 2.2.2 条件查询

```java
// 阻塞式环境中的查询
public List<YourEntity> findEntities(QueryParamEntity param) {
    return repository
        .createQuery()
        .setParam(param)
        .where(YourEntity::getStatus, EntityStatus.ACTIVE)
        .and(YourEntity::getCreateTime, TermType.gte, param.getStartTime())
        .and(YourEntity::getCreateTime, TermType.lte, param.getEndTime())
        .fetch();
}
```

#### 2.2.3 原生SQL查询（阻塞式）

##### 2.2.3.1 使用 QueryHelper 执行原生SQL（阻塞式）

```java
// 使用 QueryHelper 执行原生SQL查询（阻塞式环境）
@Autowired
private QueryHelper queryHelper;

// 基础原生SQL查询
public List<Record> executeNativeQuery(String sql, Object... args) {
    return queryHelper
        .select(sql, args)
        .fetch()
        .collectList()
        .block();
}

// 设备统计查询示例（阻塞式）
public List<Record> queryDeviceStatisticsBlocking(String productId) {
    String sql = """
        select product_id, 
               count(*) as total_count,
               sum(case when state = 'online' then 1 else 0 end) as online_count,
               sum(case when state = 'offline' then 1 else 0 end) as offline_count
        from dev_device_instance 
        where product_id = ?
        group by product_id
        """;
    return queryHelper
        .select(sql, productId)
        .fetch()
        .collectList()
        .block();
}

// 带动态条件的原生SQL查询
public List<Record> executeNativeQueryWithConditions(String sql, QueryParamEntity param, Object... args) {
    return queryHelper
        .select(sql, args)
        .where(param)
        .fetch()
        .collectList()
        .block();
}

// 设备详情查询示例（阻塞式）
public List<Record> queryDeviceDetailBlocking(String deviceId) {
    String sql = """
        select device.*, product.name as product_name, product.type as product_type,
               tag.key as tag_key, tag.value as tag_value
        from dev_device_instance device 
        left join dev_product product on device.product_id = product.id
        left join dev_device_tag tag on device.id = tag.device_id
        where device.id = ?
        """;
    return queryHelper
        .select(sql, deviceId)
        .fetch()
        .collectList()
        .block();
}

// 分页查询
public PagerResult<Record> executeNativeQueryPaged(String sql, QueryParamEntity param, Object... args) {
    return queryHelper
        .select(sql, args)
        .where(param)
        .fetchPaged()
        .block();
}
```

#### 2.2.4 事务中的原生SQL

```java
@Transactional
public void executeTransactionalSql(String sql, Map<String, Object> parameters) {
    try {
        jdbcExecutor.update(SqlRequests.template(sql, parameters));
        // 其他业务逻辑
    } catch (Exception e) {
        // 事务会自动回滚
        throw new BusinessException("SQL执行失败", e);
    }
}
```

### 2.3 复杂条件构建

#### 2.3.1 嵌套条件查询

```java
// 使用 NestConditional 构建复杂嵌套条件
NestConditional<Query<Object, QueryParamEntity>> nest = param.toNestQuery().nest();

// 构建复杂嵌套条件: field1 = ? and ( ( field2 = ? and (field3 in (?,?,?) or field4 in(?,?)) ) or ( .... ) )
return Authentication
    .currentReactive()
    .flatMap(auth -> Flux
        .fromIterable(getTargetList())
        .flatMap(target -> checkPermission(target)
            .flatMap(result -> {
                if (result) {
                    return injectConditional(nest
                        .orNest()
                        .and("targetType", target.getType())
                        .nest(), target.getAssetType(), "targetId")
                        .then();
                } else {
                    nest.orNest().and("targetType", target.getType());
                    return Mono.empty();
                }
            }))
        .then(Mono.fromSupplier(() -> param))
    );
```

#### 2.3.2 动态条件构建

```java
// 复杂条件组合示例
public Flux<YourEntity> queryWithComplexConditions(QueryParamEntity param) {
    return repository
        .query(QueryParamEntity
            .of()
            .toNestQuery(q -> q.gt("score", 3))
            .nest()
            .like("name", "%keyword%")
            .or()
            .is("status", "active")
            .end()
            .getParam())
        .doOnNext(System.out::println);
}
```

#### 2.3.3 原生SQL条件构建

##### 2.3.3.1 使用QueryParamEntity自动构建动态条件

```java
// QueryHelper 支持通过 QueryParamEntity 自动构建动态条件，无需手动拼接SQL
@Autowired
private QueryHelper queryHelper;

// 设备查询示例 - 使用QueryParamEntity自动构建条件
public Flux<Record> queryDevicesWithDynamicConditions(QueryParamEntity param) {
    String sql = """
        select device.id, device.name, device.state, device.product_id,
               product.name as product_name, product.type as product_type
        from dev_device_instance device 
        left join dev_product product on device.product_id = product.id
        """;
    return queryHelper
        .select(sql)
        .where(param)  // QueryParamEntity会自动构建WHERE条件
        .fetch();
}

// 设备统计查询示例 - 使用QueryParamEntity自动构建条件
public Flux<Record> queryDeviceStatisticsWithDynamicConditions(QueryParamEntity param) {
    String sql = """
        select device.product_id, product.name as product_name,
               count(*) as total_count,
               sum(case when device.state = 'online' then 1 else 0 end) as online_count,
               sum(case when device.state = 'offline' then 1 else 0 end) as offline_count
        from dev_device_instance device 
        left join dev_product product on device.product_id = product.id
        """;
    return queryHelper
        .select(sql)
        .where(param)  // QueryParamEntity会自动构建WHERE条件
        .fetch();
}
```

##### 2.3.3.2 使用 QueryHelper 进行复杂查询

```java
// 使用 QueryHelper 进行复杂查询构建
@Autowired
private QueryHelper queryHelper;

// 复杂关联查询
public Flux<YourResult> executeComplexQuery(QueryParamEntity param) {
    return queryHelper
        .select(YourResult.class)
        .as(MainEntity::getId, YourResult::setId)
        .as(MainEntity::getName, YourResult::setName)
        .as(RelatedEntity::getDetail, YourResult::setDetail)
        .from(MainEntity.class)
        .leftJoin(RelatedEntity.class, spec -> spec.is(MainEntity::getId, RelatedEntity::getMainId))
        .where(param)
        .orderByDesc(MainEntity::getCreateTime)
        .fetch();
}

// 一对多数据组合查询
public Flux<YourEntity> executeOneToManyQuery(QueryParamEntity param) {
    return queryHelper
        .select(YourEntity.class)
        .all(MainEntity.class)
        .from(MainEntity.class)
        .where(param)
        .fetch()
        .flatMap(main -> {
            // 查询关联数据
            return queryHelper
                .select(DetailEntity.class)
                .where(dsl -> dsl.is(DetailEntity::getMainId, main.getId()))
                .fetch()
                .collectList()
                .map(details -> {
                    main.setDetails(details);
                    return main;
                });
        });
}

// 使用静态方法进行一对多数据组合
public Flux<YourEntity> executeOneToManyWithHelper(QueryParamEntity param) {
    return QueryHelper.combineOneToMany(
        mainService.createQuery().setParam(param).fetch(),
        YourEntity::getId,
        detailService.createQuery(),
        DetailEntity::getMainId,
        YourEntity::setDetails
    );
}
```

---

## 三、QueryParamEntity 最佳实践

### 3.1 推荐使用方式

#### 3.1.1 使用 QueryParamEntity.newQuery() 构建查询

```java
// ✅ 推荐：使用链式调用构建查询条件
public Flux<YourEntity> queryEntities(String name, String status, Date startTime, Date endTime) {
    return QueryParamEntity
        .newQuery()
        .where("name", name)                    // 等于条件
        .like("description", "%keyword%")      // 模糊查询
        .in("status", Arrays.asList("active", "pending"))  // IN查询
        .gte("createTime", startTime)          // 大于等于
        .lte("createTime", endTime)            // 小于等于
        .orderByDesc("createTime")             // 排序
        .doPaging(0, 20)                       // 分页
        .execute(repository::query);
}

// ✅ 推荐：使用Lambda表达式构建条件
public Flux<YourEntity> queryEntitiesWithLambda(String name, String status) {
    return QueryParamEntity
        .newQuery()
        .where(YourEntity::getName, name)
        .where(YourEntity::getStatus, status)
        .orderByAsc(YourEntity::getCreateTime)
        .execute(repository::query);
}
```

#### 3.1.2 嵌套条件查询

```java
// ✅ 推荐：使用nest()构建嵌套条件
public Flux<YourEntity> queryWithNestedConditions(String name, String status) {
    return QueryParamEntity
        .newQuery()
        .where("name", name)
        .nest()                                // 开始嵌套条件
        .is("status", status)
        .or("type", "special")                 // OR条件
        .end()                                 // 结束嵌套条件
        .execute(repository::query);
}

// ✅ 推荐：使用orNest()构建OR嵌套条件
public Flux<YourEntity> queryWithOrNestedConditions(String name, String status) {
    return QueryParamEntity
        .newQuery()
        .where("name", name)
        .orNest()                              // OR嵌套条件
        .is("status", status)
        .is("type", "special")
        .end()
        .execute(repository::query);
}
```

#### 3.1.3 动态条件构建

```java
// ✅ 推荐：使用when()进行条件判断
public Flux<YourEntity> queryWithDynamicConditions(String name, String status, boolean includeInactive) {
    return QueryParamEntity
        .newQuery()
        .where("name", name)
        .when(StringUtils.hasText(status), query -> query.where("status", status))
        .when(!includeInactive, query -> query.where("status", "!=", "inactive"))
        .orderByDesc("createTime")
        .execute(repository::query);
}

// ✅ 推荐：使用Consumer进行复杂条件构建
public Flux<YourEntity> queryWithComplexConditions(String name, List<String> statusList, Date startTime) {
    return QueryParamEntity
        .newQuery()
        .where("name", name)
        .when(CollectionUtils.isNotEmpty(statusList), 
              query -> query.in("status", statusList))
        .when(startTime != null, 
              query -> query.gte("createTime", startTime))
        .execute(repository::query);
}
```

### 3.2 避免的使用方式

#### 3.2.1 避免直接new Term()

```java
// ❌ 不推荐：直接创建Term对象
public Flux<YourEntity> badQueryExample() {
    Term term1 = new Term();
    term1.setColumn("name");
    term1.setValue("test");
    term1.setType(TermType.eq);
    
    Term term2 = new Term();
    term2.setColumn("status");
    term2.setValue("active");
    term2.setType(TermType.eq);
    
    QueryParamEntity param = new QueryParamEntity();
    param.setTerms(Arrays.asList(term1, term2));
    
    return repository.query(param);
}

// ✅ 推荐：使用链式调用
public Flux<YourEntity> goodQueryExample() {
    return QueryParamEntity
        .newQuery()
        .where("name", "test")
        .where("status", "active")
        .execute(repository::query);
}
```

#### 3.2.2 避免手动设置查询参数

```java
// ❌ 不推荐：手动设置查询参数
public Flux<YourEntity> badManualQuery() {
    QueryParamEntity param = new QueryParamEntity();
    param.setPageIndex(0);
    param.setPageSize(20);
    param.setSorts(Arrays.asList(
        new Sort("createTime", "desc")
    ));
    
    List<Term> terms = new ArrayList<>();
    Term term = new Term();
    term.setColumn("name");
    term.setValue("test");
    term.setType(TermType.like);
    terms.add(term);
    param.setTerms(terms);
    
    return repository.query(param);
}

// ✅ 推荐：使用链式调用
public Flux<YourEntity> goodManualQuery() {
    return QueryParamEntity
        .newQuery()
        .like("name", "%test%")
        .orderByDesc("createTime")
        .doPaging(0, 20)
        .execute(repository::query);
}
```

### 3.3 高级用法

#### 3.3.1 条件组合

```java
// ✅ 推荐：使用and()和or()进行条件组合
public Flux<YourEntity> queryWithConditionCombination(String name, String status, String type) {
    return QueryParamEntity
        .newQuery()
        .where("name", name)
        .and()                                 // AND条件
        .where("status", status)
        .or()                                  // OR条件
        .where("type", type)
        .execute(repository::query);
}
```

#### 3.3.2 分页和排序

```java
// ✅ 推荐：使用doPaging()和orderBy()进行分页排序
public Mono<PagerResult<YourEntity>> queryWithPaging(String name, int pageIndex, int pageSize) {
    return QueryParamEntity
        .newQuery()
        .where("name", name)
        .orderByDesc("createTime")
        .orderByAsc("id")
        .doPaging(pageIndex, pageSize)
        .execute(repository::queryPager);
}
```

#### 3.3.3 字段选择

```java
// ✅ 推荐：使用select()进行字段选择
public Flux<YourEntity> queryWithFieldSelection(String name) {
    return QueryParamEntity
        .newQuery()
        .where("name", name)
        .select("id", "name", "status", "createTime")  // 只查询指定字段
        .execute(repository::query);
}
```

### 3.4 最佳实践总结

#### 3.4.1 推荐做法

1. **使用链式调用**：`QueryParamEntity.newQuery().where().like().execute()`
2. **使用Lambda表达式**：`where(Entity::getField, value)`
3. **使用条件方法**：`when()`, `nest()`, `orNest()`
4. **使用分页排序**：`doPaging()`, `orderBy()`
5. **使用字段选择**：`select()`

#### 3.4.2 避免做法

1. **避免直接new Term()**：使用链式调用替代
2. **避免手动设置参数**：使用链式调用替代
3. **避免复杂的条件拼接**：使用nest()和orNest()
4. **避免硬编码分页**：使用doPaging()方法

#### 3.4.3 性能优化

1. **合理使用索引字段**：在where条件中使用有索引的字段
2. **避免全表扫描**：使用合适的查询条件
3. **使用分页查询**：避免一次性查询大量数据
4. **使用字段选择**：只查询需要的字段

---

## 四、编程模式选择指南

### 4.1 响应式编程适用场景

#### 4.1.1 高并发查询场景

```java
// 适合高并发、非阻塞的查询场景
@Override
public Flux<YourData> query(String typeId, QueryParamEntity param) {
    return getRepository(typeId)
        .createQuery()
        .setParam(param)
        .fetch()
        .map(YourData::new);
}

@Override
public Mono<YourData> querySingleData(String typeId, String entityId) {
    return getRepository(typeId)
        .findById(entityId)
        .map(YourData::new);
}
```

#### 4.1.2 流式数据处理

```java
// 适合流式数据处理场景
public void init() {
    Flux
        .<YourTask>create(sink -> {
            AtomicBoolean requesting = new AtomicBoolean();
            sink.onRequest(total -> {
                if (requesting.compareAndSet(false, true)) {
                    findPendingTasks(total)
                        .doOnNext(sink::next)
                        .doFinally(s -> requesting.set(false))
                        .subscribe();
                }
            });
        })
        .subscribe(subscriber);
}
```

### 3.2 阻塞式编程适用场景

#### 3.2.1 简单业务逻辑

```java
// 适合简单业务逻辑的阻塞式处理
@Component
public class YourEventHandler {

    @EventListener
    public void handleEvent(EntityBeforeSaveEvent<YourEntity> event) {
        for (YourEntity entity : event.getEntity()) {
            System.out.println("保存前:" + entity);
        }
    }

    @EventListener
    public void handleEvent(EntitySavedEvent<YourEntity> event) {
        for (YourEntity entity : event.getEntity()) {
            System.out.println("保存后:" + entity);
        }
    }
}
```

#### 3.2.2 传统管理后台

```java
// 阻塞式环境中的简单查询
public List<YourEntity> findActiveEntities() {
    return repository
        .createQuery()
        .where(YourEntity::getStatus, EntityStatus.ACTIVE)
        .fetch();
}
```

### 3.3 混合使用场景

#### 3.3.1 在响应式环境中调用阻塞式服务

```java
// 使用 Mono.fromCallable 包装阻塞式操作
public Mono<String> processData(String id) {
    return Mono.fromCallable(() -> {
        // 阻塞式操作
        return blockingService.process(id);
    })
    .subscribeOn(Schedulers.boundedElastic());
}
```

#### 3.3.2 在阻塞式环境中调用响应式服务

```java
// 使用 .block() 方法转换
public List<ExampleEntity> getEntities() {
    return reactiveService
        .createQuery()
        .fetch()
        .collectList()
        .block();
}
```

---

## 四、查询优化最佳实践

### 4.1 分页查询优化

```java
// 参考: modules/device-manager/src/main/java/org/jetlinks/pro/device/service/task/DeviceMessageTaskService.java
private Flux<DeviceMessageTask> findWaitSendTask(long total) {
    return QueryParamEntity
        .newQuery()
        .when(total >= Integer.MAX_VALUE, Query::noPaging)
        .when(total < Integer.MAX_VALUE, query -> query.doPaging(0, (int) total))
        .where(DeviceMessageTask::getServerId, clusterManager.getCurrentServerId())
        .in(DeviceMessageTask::getState, TaskState.wait)
        .execute(taskRepository::query);
}
```

### 4.2 字段选择优化

```java
// 只选择需要的字段，避免查询过多数据
return deviceInstanceService
    .createQuery()
    .setParam(deviceQuery)
    .select(DeviceInstanceEntity::getId, DeviceInstanceEntity::getName, DeviceInstanceEntity::getProductId)
    .fetch();
```

### 4.3 批量处理优化

```java
// 使用 buffer 进行批量处理
.fetch()
.map(deviceInstance -> {
    // 处理逻辑
    return DeviceMessageTask.newTask(template);
})
.buffer(200)  // 批量处理，每批200条
.flatMap(list -> this.createTask(list).thenReturn(list.size()))
.reduce(Math::addExact);
```

---

## 五、实际项目案例速查

### 5.1 事件驱动案例

| 场景 | 实现方式 | 说明 |
|------|---------|------|
| 资产同步 | `@EventListener` + `event.async()` | 实体保存时同步相关资产信息 |
| 数据收集器注册 | `@EventListener` + 批量处理 | 实体保存时注册数据收集器 |
| 缓存更新 | `@EventListener` + 异步更新 | 实体变更时更新相关缓存 |

### 5.2 复杂查询案例

| 场景 | 实现方式 | 说明 |
|------|---------|------|
| 条件查询 | `createQuery()` + 链式调用 | 复杂条件查询和批量处理 |
| 嵌套条件 | `NestConditional` + 权限控制 | 嵌套条件查询构建 |
| 关联查询 | `QueryHelper` + `leftJoin` | 多表关联查询和权限控制 |

### 5.3 原生SQL查询案例

| 场景 | 实现方式 | 说明 |
|------|---------|------|
| QueryHelper SQL | `QueryHelper.select()` + 链式调用 | 使用QueryHelper执行原生SQL |
| 动态条件 | `QueryParamEntity` + `where()` | 使用QueryParamEntity自动构建动态条件 |
| 复杂关联 | `QueryHelper` + `leftJoin/rightJoin/innerJoin` | 使用QueryHelper进行复杂关联查询 |
| 一对多查询 | `QueryHelper.combineOneToMany()` | 使用QueryHelper进行一对多数据组合 |

### 5.4 响应式编程案例

| 场景 | 实现方式 | 说明 |
|------|---------|------|
| 数据查询 | `createQuery()` + `Flux` | 响应式数据查询 |
| 流式处理 | `Flux.create()` + 背压控制 | 流式数据处理 |
| 事件处理 | `@EventListener` + 异步处理 | 响应式事件处理 |

---

## 六、AI 使用指南

### 6.1 场景识别模板

当用户提出以下需求时，使用相应的引导：

#### 6.1.1 事件驱动场景
```
用户说："当XXX保存时，同时处理YYY"
AI回复：
根据您的需求，这属于事件驱动开发场景。请参考：

📖 **事件驱动开发规范**: [event-driven-rules.md](./event-driven-rules.md)

该文档包含完整的实体事件监听、响应式与阻塞式事件处理等指南。
```

#### 6.1.2 复杂查询场景
```
用户说："需要复杂的查询条件" / "多表关联查询" / "动态条件构建"
AI回复：
这属于复杂查询构建场景。根据您选择的编程模式，我将为您生成相应的查询代码：

**响应式模式**：使用 createQuery() + 链式调用
**阻塞式模式**：使用 createQuery() + 同步方法调用
**QueryHelper模式**：使用 QueryHelper.select() + 链式调用
```

#### 6.1.3 原生SQL查询场景
```
用户说："需要执行原生SQL" / "复杂SQL查询"
AI回复：
这属于原生SQL查询场景。根据您选择的编程模式，我将为您生成相应的SQL执行代码：

**QueryHelper模式**：使用 QueryHelper.select() + 链式调用
**动态条件**：使用 QueryParamEntity 自动构建WHERE条件
```

#### 6.1.4 复杂关联查询场景
```
用户说："多表关联查询" / "一对多查询" / "复杂JOIN查询"
AI回复：
这属于复杂关联查询场景。根据您的需求，我将为您生成相应的查询代码：

**QueryHelper关联查询**：使用 leftJoin/rightJoin/innerJoin
**一对多数据组合**：使用 QueryHelper.combineOneToMany()
**原生SQL关联**：使用 QueryHelper.select() + 原生SQL
```

#### 6.1.3 编程模式选择
```
用户说："不确定用响应式还是阻塞式"
AI回复：
请选择您要使用的编程模式：

1. **响应式编程模式 (WebFlux)** - 推荐
   - 适用于：高并发、IO密集型场景
   - 特点：使用 Mono/Flux，非阻塞，性能更好
   - 返回类型：Mono<T>、Flux<T>

2. **阻塞式编程模式 (Spring MVC)** - 传统
   - 适用于：传统业务场景、管理后台
   - 特点：使用普通Java对象，代码简单直观
   - 返回类型：Object、List<T>、Optional<T>

请明确告诉我您选择哪种模式，我将根据您的选择生成相应的代码。
```

### 6.2 代码生成检查清单

#### 6.2.1 事件驱动代码检查
- [ ] 使用 `@EventListener` 注解
- [ ] 响应式环境使用 `event.async()` 包装
- [ ] 阻塞式环境直接处理
- [ ] 正确获取事件数据（`getEntity()`, `getAfter()`, `getBefore()`）
- [ ] 添加适当的异常处理

#### 6.2.2 复杂查询代码检查
- [ ] 使用 `createQuery()` 创建查询对象
- [ ] 正确设置查询条件（`where`, `and`, `or`）
- [ ] 响应式：返回 `Mono<T>` 或 `Flux<T>`
- [ ] 阻塞式：返回 `List<T>` 或 `Optional<T>`
- [ ] 添加分页和排序支持
- [ ] 优化字段选择，避免查询过多数据

#### 6.2.3 原生SQL查询代码检查
- [ ] 使用 `QueryHelper.select()` + 链式调用
- [ ] 响应式：返回 `Mono<T>` 或 `Flux<T>`
- [ ] 阻塞式：使用 `.block()` 获取结果
- [ ] 使用 `QueryParamEntity` 自动构建动态条件
- [ ] 避免SQL注入，使用预编译参数
- [ ] 添加适当的异常处理

#### 6.2.4 复杂关联查询代码检查
- [ ] 使用 `QueryHelper` 进行关联查询
- [ ] 正确设置关联条件（`leftJoin`, `rightJoin`, `innerJoin`）
- [ ] 一对多查询：使用 `QueryHelper.combineOneToMany()`
- [ ] 字段映射：使用 `as()` 方法进行字段映射
- [ ] 性能优化：避免N+1查询问题

#### 6.2.3 编程模式一致性检查
- [ ] 响应式：所有方法返回 `Mono` 或 `Flux`
- [ ] 阻塞式：返回普通Java对象
- [ ] 异常处理方式正确
- [ ] 事务管理方式正确

---

## 七、快速参考

### 7.1 常用查询模式速查

| 查询类型 | 响应式写法 | 阻塞式写法 |
|---------|-----------|-----------|
| 基础查询 | `createQuery().fetch()` | `createQuery().fetch()` |
| 条件查询 | `createQuery().where().fetch()` | `createQuery().where().fetch()` |
| 分页查询 | `createQuery().setParam().fetchPaged()` | `createQuery().setParam().fetchPaged()` |
| 关联查询 | `queryHelper.select().from().leftJoin().fetch()` | `queryHelper.select().from().leftJoin().fetch()` |

### 7.2 事件处理模式速查

| 事件类型 | 响应式处理 | 阻塞式处理 |
|---------|-----------|-----------|
| 保存事件 | `event.async(Flux.fromIterable(event.getEntity()).flatMap(...))` | `for (Entity entity : event.getEntity()) { ... }` |
| 修改事件 | `event.async(Flux.fromIterable(event.getAfter()).flatMap(...))` | `for (Entity entity : event.getAfter()) { ... }` |
| 删除事件 | `event.async(Flux.fromIterable(event.getEntity()).flatMap(...))` | `for (Entity entity : event.getEntity()) { ... }` |

### 7.3 性能优化建议

- **分页查询**：使用 `doPaging()` 方法
- **字段选择**：使用 `select()` 只查询需要的字段
- **批量处理**：使用 `buffer()` 进行批量操作
- **条件优化**：合理使用索引字段作为查询条件
- **缓存策略**：对频繁查询的数据使用缓存

---

## 总结

本规范涵盖了 JetLinks 项目中 CRUD 进阶使用的核心场景：

1. **事件驱动开发** - 基于实体事件的业务解耦
2. **复杂查询构建** - 支持各种复杂查询场景
3. **编程模式选择** - 响应式与阻塞式的适用场景
4. **性能优化** - 查询和处理的性能优化建议
5. **实际案例** - 基于项目真实代码的最佳实践

遵循本规范可以高效地实现复杂的业务逻辑和查询需求。
