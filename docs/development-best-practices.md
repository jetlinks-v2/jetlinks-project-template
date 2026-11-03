# 二次开发与多租户最佳实践

本文约定应用业务、租户适配和 SaaS 接入的职责，供同一份业务代码在私有化、多租户、平台单体和微服务环境中复用。模块名 `my-business-*` 为应用自定义名称示意；平台 API 和依赖以配套的 `2.20` 制品为准。

## 部署组合

先选择身份、隔离与运营模式，再选择部署形态。私有化描述部署归属，不等于单租户；单体描述进程装配，不等于关闭数据权限。私有化环境也可以运行多租户，专有云运行时也可以接入 SaaS 运营中心。

| 业务模式 | 业务与租户装配 | SaaS 接入 |
| --- | --- | --- |
| 不接入运营中心的普通私有化 | 业务模块、统一认证及 `assets-component`；无需租户能力时不引入业务租户适配模块 | 关闭本模板的 SaaS profile，保留功能权限与资产权限 |
| 不接入运营中心的普通多租户 | 业务模块和独立的业务租户适配模块，后者引入 `tenant-component`；认证端按需装配 `tenant-authentication` 等能力 | 不需要 `saas-runtime-agent`；租户管理、初始化与限额仍需按平台方式装配 |
| 接入运营中心的 SaaS 运行时 | 复用业务和租户适配模块，按服务职责增加 `saas-runtime-component` 等运行时能力 | 认证端装配 `saas-runtime-auth`、`saas-runtime-agent` 并配置 agent；运营控制端单独部署 |

`saas-runtime-component` 已依赖 `tenant-component`，但业务专属指标、拦截器和初始化逻辑仍需由自己的适配模块提供。不要在普通业务模块中引入运营控制端的 `saas-manager`。

| 平台部署形态 | 应用边界和装配方式 |
| --- | --- |
| 平台单体 | 二开应用仍默认作为独立业务服务，通过公开命令访问平台；同进程装配时也保持公共契约，不借机依赖标准模块实体或内部 Service |
| 平台微服务或多副本 | 在业务所属服务装配业务和租户适配模块；跨服务使用命令、事件及平台上下文传播，不共享数据库作为调用协议 |

本模板的 `standalone` / `microservice` Maven profile 选择平台启动参考，`saas.enabled` 控制 SaaS 构建依赖；它们不是运行时租户识别开关，也不表示单租户或多租户。关闭 SaaS 不会自动装配普通多租户所需模块。前端的独立模式脚本同样不能替代后端的部署与权限配置。

## 业务字段扩展

### 使用自己的关联表

为设备等标准对象增加业务字段时，在业务服务内维护扩展表或中间表，例如 `biz_device_extension(device_id, business_code, ...)`。标准 ID 只作为业务关联键；不修改标准设备表，不继承标准设备实体，也不为取字段引入 `device-manager`。

- 一对一扩展应有相应唯一约束，一对多关系应有明确的关系键、关联索引和清理规则。多租户下，业务键的唯一性按实际租户归属设计；租户列及其索引通过平台元数据与部署迁移统一处理，兼容未装配租户模块的表结构。
- 保存、换绑和解绑必须校验所引用标准资产的权限及归属，不能仅校验业务表或相信调用方传来的 ID。标准资产删除后的关系清理通过平台已公开的生命周期事件或业务补偿流程完成，不跨服务声明数据库外键。
- 业务关系表与平台资产授权绑定不是同一用途，不向权限绑定表附加业务字段。

### 按查询边界使用 QueryHelper

`QueryHelper` 来自 `org.hswebframework.web.crud.query`，用于本地查询、分页结果转换及批量结果组合，不是跨服务访问标准表的许可，也不会自动补齐自定义查询的资产权限。

| 查询需求 | 推荐方式与边界 |
| --- | --- |
| 只补充标准对象的展示字段 | 先通过公开查询命令取得已授权的标准对象分页；使用 `QueryHelper.transformPageResult(...)` 保留分页元信息，按本页 ID 批量查询业务扩展并组装 DTO |
| 本业务内的多表查询 | 用 `QueryHelper.select(...).from(...).leftJoin(...).where(...).fetchPaged()` 等 DSL 关联本服务拥有的表；查询参数进入执行链前按对应资产边界注入权限 |
| 本页需要一对多业务明细 | 使用 `QueryHelper.combineOneToMany(...)` 等批量组合能力，避免逐行远程调用或逐行查库 |
| 按业务扩展字段筛选标准对象 | 先在业务权限范围内筛选关联 ID，再将约束作为公开命令支持的查询条件参与完整筛选和分页；ID 集合较大时设计业务自己的查询投影，不无限制扩大 `IN` 参数 |
| 混合标准字段与扩展字段排序、聚合 | 先确定查询的归属与完整排序口径。复杂场景建立业务自己的读模型或查询接口；不能先取标准对象一页再在内存筛选、排序，却继续返回原来的总数 |

业务扩展无独立授权和生命周期时，按父资产保护，参考关联资产控制器及 `AssetsHolder` 的关联资产检查。真正独立的业务资产才定义自己的 `AssetType`、`@Asset` 与资产提供方；不能给每张关系表机械地注册一个独立资产。

独立业务资产的实体继承 `ExtendableEntity` 并启用平台实体事件，租户适配由元数据和事件机制补充租户字段与归属，不在通用业务实体中增加一个强制必填的租户字段。自定义资产提供方构造 `DefaultAsset` 时使用 `withEntity(entity)` 带入真实实体及扩展属性，避免丢失租户归属，导致查询和单条资产校验不一致。

列表、详情、导出和批量操作使用一致的权限边界。查询投影、缓存和 DTO 只承载数据，不成为另一套授权来源。即使平台当前恰好同进程或同数据库，也不联表查询标准模块私有表，否则迁移微服务后无法保留同一业务实现。

具体 API 及示例见 [查询 DSL 与 QueryHelper 规范][query-guide]、[统一数据权限接入手册][assets-guide]。

## 租户指标扩展

这里的 metric 指租户资源的计量与配额指标，例如实体数量、调用额度、并发数和速率；通用运行统计或监控指标不应因此依赖租户模块。是否开通某项功能使用平台 `Capability` 及资源 Provider 机制表达，不用计数器替代能力授权。

### 使用独立的租户适配模块

```text
modules/my-business/
├── my-business-core       业务模型、资产类型、命令和中性扩展契约
├── my-business-manager    实体、业务服务、接口、事件和通用统计
└── my-business-tenant     租户指标、限制器、业务 SPI 适配和租户生命周期
```

依赖方向为 `my-business-manager → my-business-core`，`my-business-tenant → tenant-component + 本业务公开的扩展能力`。租户适配模块可按本地扩展点依赖本业务 manager；manager 和 core 不反向依赖 tenant 或 SaaS。需要新增通用回调时，在业务侧定义中性 SPI，由适配模块实现，避免业务流程反复判断部署模式。

适配模块使用平台版本引入租户组件，业务模块继续使用应用自己的版本：

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>tenant-component</artifactId>
    <version>${jetlinks.pro.version}</version>
</dependency>
```

启动模块决定是否装配 `my-business-tenant`。普通私有化不引入该适配模块时，业务和通用统计应能正常使用；普通多租户与 SaaS 复用同一适配模块，SaaS 接入层只增加运营中心协作。Spring Boot 自动配置通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册，并按实际业务扩展点控制装配条件。

### 指标接入步骤

1. **确定资源归属。** 资产指标复用本业务已注册的资产类型；没有实体表的资产资源按平台虚拟资产机制接入，不能仅为配额伪造实体表。需要新的资产资源入口时实现并注册对应的 `AssetResourceProvider`；非资产资源使用 `TenantResourceProvider` / `ResourceMetric` 等公共契约。
2. **声明稳定指标。** 实现 `AssetResourceMetric` 并声明为 Spring Bean，使用 `MetricId.of(resourceId, metricId)` 标识；明确单位、总量或周期口径、`ResourceType` 和 `ResourceLimitType`，不要根据租户或节点动态生成指标 ID。`TenantResourceManager` 会收集注册的指标和资源 Provider。
3. **在实际业务边界执行限制。** 仅注册指标不会自动限制业务。实体数量优先复用 `EntityQuotaLimiter` / `EntityEventQuotaLimiter` 体系；请求总量使用 `forQuota()`，速率使用 `forRate()`，并发使用 `forConcurrency().execute(...)` 包住完整业务 Publisher，使完成、失败和取消都能释放资源。
4. **配置并同步限额。** 普通多租户使用平台租户限额配置；SaaS 还需将资源及指标纳入服务规格、项目授权与限额同步链路。检查运营中心能发现运行时指标，并验证配置到运行时实际执行的闭环。

| 限制类型 | 应用选择 |
| --- | --- |
| `quotaLimit` | 全局累计总量，不以附加周期参数充当周期配额 |
| `quotaTimeLimit` | 按明确时间周期累计，单位及周期口径保持一致 |
| `rateLimit` | 一定速率的业务资源申请 |
| `concurrency` | 同时进行的任务，必须覆盖任务整个生命周期 |

### 计量与兼容约束

- **一处业务事实只记账一次。** 明确网关、业务服务、事件消费者中谁是计量所有者；重试、重复事件和请求取消要有对应处理。实体配额不要同时走通用表级统计和自定义事件扣减。
- **关联实体按真实归属统计。** 没有独立租户字段的子实体，通过关联主资产解析租户并批量分组；后台补偿、导入、删除与配额重建也遵守同一口径，不能一律使用处理线程的当前租户。
- **失败、回滚与并发不是同一问题。** 明确失败操作是否退还额度，数据库事务与外部调用是否一致。数据库统计、Redis 原子扣减和异步记账按平台已有实现选择，不用“先查余量、再执行业务”替代原子控制。
- **多副本共享限额。** 平台通用实现使用 Redis 等共享能力；内存限制器只适合明确的单实例或测试环境，不能作为集群全局限额。无需为单体与微服务各实现一套计量逻辑。
- **模式回退与异常分开。** 私有化未装配适配模块应沿业务默认路径执行；要求租户归属的请求必须在可信入口验证租户选择、成员关系及启用状态。指标未开通、无限制、额度耗尽和服务调用失败要明确区分，不把认证或网络异常吞成无限额。

完整扩展方式见 [租户模块开发文档][tenant-guide] 的“自定义配额指标”“扩展点”和“完整示例”。[tenant-ai-agent 实例][tenant-ai-guide] 展示了中性业务 SPI 与租户成本、并发、会话配额分离的做法；指标定义和限制器放在租户适配模块，通用计费统计留在业务模块。

## 接入 SaaS 的 agent 配置

### 配置位置与参数

微服务部署时，在 [认证服务](../microservices/authentication-service/pom.xml) 装配 `saas-runtime-auth`、`saas-runtime-agent`，并在该服务的部署配置或 `application-local.yml` 中提供：

```yaml
jetlinks:
  saas:
    agent:
      server-uri: ${SAAS_AGENT_SERVER_URI}
      access-id: ${SAAS_AGENT_ACCESS_ID}
      access-key: ${SAAS_AGENT_ACCESS_KEY}
```

| 参数 | 来源与含义 |
| --- | --- |
| `server-uri` | 认证服务可访问的运营中心 RSocket/TCP 地址，例如 `tcp://saas-control.example.internal:19101`；端口以运营中心实际配置及网络映射为准 |
| `access-id` | 运营中心注册的**服务运行时记录 ID**，对应 `SaasServiceRuntimeEntity.id`；不是项目 ID、租户 ID、运行时 `code` 或 Spring 应用名 |
| `access-key` | 同一服务运行时记录的 `configuration.accessKey`，必须与运营中心一致；由部署环境或 Secret 注入 |

单体装配时，将相同配置放在实际引入 agent 的启动进程中，本模板对应 [bootstrap](../bootstrap/pom.xml)。同一逻辑运行时的认证服务副本使用配套的运行时记录和凭据；不要给每个业务微服务复制一套 agent 连接。业务服务仍通过公开命令和平台服务发现访问已发布能力。

agent 的属性前缀与字段以 [SaasAgentConfiguration][agent-configuration] 和 [RSocketSassAgentProperties][agent-properties] 为准。当前实现的连接通道是 RSocket/TCP，不把浏览器 API 网关的 HTTP 地址、路径或端口当作该通道。配置中不提交真实凭据。

### 接入步骤与验收

1. 在运营中心创建或配置服务运行时；当前远程连接实现的 Provider ID 为 `default`。配置该运行时的 `accessKey`，记录生成的运行时 ID，并使运营中心加载该记录。
2. 确认运营中心装配 `saas-runtime-master`，`jetlinks.saas.server` 的服务端配置有效。默认监听端口为 `19101`；认证服务所在网络必须能访问实际对外映射的 TCP 端口。
3. 在认证服务注入上述三个 agent 参数，启用 SaaS 依赖后启动。认证、业务服务和运营中心按平台配置各自的服务发现与命令路由；agent 通道不代替业务微服务间的发现机制。
4. 从运营中心读取一次运行时已公开的服务、菜单或资源指标，验证双向命令链路，而不只检查进程存活、启动日志或 TCP 端口连通。
5. 开通一个测试项目或同步一组权限和限额，验证用户登录、菜单权限、租户数据范围和指标限制均在正确运行时生效。随后使用受限租户账号验证跨租户访问被拒绝，不能把 agent 连接成功视为业务授权成功。

运行时 ID 的来源见 [DefaultServiceRuntimeManager][runtime-manager]；凭据校验与服务端端口见 [RSocketSaasMasterProvider][agent-server]，服务端开关绑定见 [SaasMasterConfiguration][master-configuration]。排查时按“TCP 可达 → 运行时记录已加载 → ID/Key 匹配 → 命令和权限 → 项目配置同步”顺序定位；签名校验还要求双方时钟正常。

不接入运营中心时，通过本模板的 `-Dsaas.enabled=false` 等装配方式关闭 SaaS 依赖；如果仍需普通多租户，应显式保留相应 `tenant-*` 模块。当前 agent 属性类没有 `enabled` 字段，不虚构 `jetlinks.saas.agent.enabled=false` 开关，也不以空凭据报错冒充关闭接入。

## 上下文与后台任务

- 租户 Header、域名和项目选择只表达选择上下文，不能单独作为授权凭据；由平台认证与可信入口确定成员关系、功能权限及资产范围。业务代码不再维护一套租户/部门/创建人 SQL 过滤。
- 响应式业务沿 Reactor 链路传递身份和范围，跨服务复用平台命令上下文编解码；不使用普通 ThreadLocal、脱链 `subscribe()` 或阻塞调用拼接上下文。
- 定时任务、消息消费和异步计量应按真实任务/资源归属建立受信上下文。跨租户批处理按归属分组，不能套用发起者当前租户处理所有资源；面向用户的命令由暴露入口继续执行权限校验。
- 自建业务缓存、任务去重和幂等键应包含与数据归属一致的租户/应用范围，明确平台公共数据的独立命名空间；不要使用一个隐式默认租户承接缺失的身份。

权限组合见 [统一数据权限接入手册][assets-guide]，跨服务调用与非阻塞约束见 [跨服务调用规范][command-guide] 和 [响应式开发规范][reactive-guide]。

## 租户数据生命周期

初始化、清理或卸载业务数据时，由租户适配模块实现并注册 `TenantDataProvider`，统一经 `TenantDataManager.init` / `purge` 调用。本地 Provider 与远端 Provider 使用同一入口，不在运营中心直接连接业务数据库。

Provider ID 应跨服务全局唯一，同一逻辑 Provider 的副本使用同一 ID。只清理本模块拥有的数据，显式按操作指定的租户定位数据，并设计幂等、失败重试和补偿；不要依赖多个 Provider 的遍历顺序或把一次调用当作跨服务事务。清理范围还包括本业务缓存、查询投影和外部资源，标准资源通过所属模块的公开能力处理。

完整契约见 [租户模块开发文档][tenant-guide] 的“租户数据初始化与清理”。

## 兼容性验收

业务新增或改变相关行为时，在受影响的组合上验证；仅修改文档无需重复构建整个平台。

| 场景 | 核心验收 |
| --- | --- |
| 无租户的私有化 | 不装配业务租户适配或 SaaS agent 仍可启动；业务、通用统计和资产权限正常，业务实体不依赖必填租户字段 |
| 普通多租户 | A/B 租户覆盖列表、详情、导出、修改、批量与关联操作；越权拒绝，缺失/冲突/禁用的租户上下文按入口契约处理 |
| 单体与微服务 | 相同业务接口、公开命令和错误语义；微服务不跨库联表、不远程逐行补字段，分页总数与筛选排序一致 |
| 多副本与异步任务 | 共享限额、一次计量、幂等与任务取消；后台初始化/清理不串租户，不丢失身份及范围 |
| SaaS 接入 | 认证端 agent 调用链、菜单和指标发现、项目授权与限额同步；凭据错误、运营中心不可达和额度耗尽不会被当作无限制 |

按改动风险选择已有单元、契约或集成验证，不为每个场景复制一套实现。环境与组件版本必须配套，不能用本机未发布源码代替可获取的平台依赖。

[query-guide]: https://github.com/jetlinks/jetlinks-develop-skills/blob/master/jetlinks-crud/references/query-dsl-rules.md
[assets-guide]: https://github.com/jetlinks-v2/jetlinks-components/blob/2.20/assets-component/docs/assets-permission-guide.md
[tenant-guide]: https://github.com/jetlinks-v2/jetlinks-tenant/blob/2.20/README.md
[tenant-ai-guide]: https://github.com/jetlinks-v2/jetlinks-tenant/blob/2.20/tenant-ai-agent/README.md
[agent-configuration]: https://github.com/jetlinks-v2/jetlinks-saas/blob/2.20/saas-runtime/saas-runtime-agent/src/main/java/org/jetlinks/pro/saas/agent/SaasAgentConfiguration.java
[agent-properties]: https://github.com/jetlinks-v2/jetlinks-saas/blob/2.20/saas-runtime/saas-runtime-core/src/main/java/org/jetlinks/pro/saas/runtime/client/RSocketSassAgentProperties.java
[runtime-manager]: https://github.com/jetlinks-v2/jetlinks-saas/blob/2.20/saas-manager/src/main/java/org/jetlinks/pro/saas/manager/runtime/DefaultServiceRuntimeManager.java
[agent-server]: https://github.com/jetlinks-v2/jetlinks-saas/blob/2.20/saas-runtime/saas-runtime-core/src/main/java/org/jetlinks/pro/saas/runtime/server/RSocketSaasMasterProvider.java
[master-configuration]: https://github.com/jetlinks-v2/jetlinks-saas/blob/2.20/saas-runtime/saas-runtime-master/src/main/java/org/jetlinks/pro/saas/runtime/master/SaasMasterConfiguration.java
[command-guide]: https://github.com/jetlinks/jetlinks-develop-skills/blob/master/jetlinks-boundary/references/cross-service-call-rules.md
[reactive-guide]: https://github.com/jetlinks/jetlinks-develop-skills/blob/master/jetlinks-reactive/SKILL.md
