# JetLinks 模块列表速查

本文档提供 JetLinks 项目中所有可用模块的快速检索，包含 Maven 引入方式和简要说明。

---

## 📋 目录

- [一、业务管理模块 (10个)](#一业务管理模块-manager-modules)
- [二、基础组件 (19个)](#二基础组件-component-modules)
- [三、网络组件 (8个)](#三网络组件-network-components)
- [四、数据存储组件 (10个)](#四数据存储组件-storage-components)
- [五、消息中间件组件 (3个)](#五消息中间件组件-messaging-components)
- [六、通知组件 (7个)](#六通知组件-notify-components)
- [七、微服务组件 (1个)](#七微服务组件-microservice-components)
- [八、其他组件 (2个)](#八其他组件)
- [使用建议](#-使用建议)
- [相关文档](#-相关文档)

---

## 📋 如何使用本文档

1. **快速查找**：使用 Ctrl/Cmd+F 搜索关键词（如：设备、MQTT、Elasticsearch）
2. **Maven 引入**：复制对应的 `<dependency>` 配置到 pom.xml
3. **详细说明**：参考 [module-reference.md](./module-reference.md) 获取详细信息和使用示例
4. **搜索源码**：使用 `grep -r "关键词" modules/` 查找实际用例

---

## 一、业务管理模块 (Manager Modules)

位于 `modules/` 目录，提供完整的业务功能。

### 1.1 device-manager（设备管理）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>device-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：设备实例、产品、分组、分类、固件、透传消息、虚拟属性、设备数据存储与查询

**核心服务**：DeviceRegistry、DeviceInstanceService、DeviceProductService、DeviceDataService

---

### 1.2 authentication-manager（认证权限管理）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>authentication-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：用户、角色、权限、菜单、组织架构、维度、第三方认证（OAuth2/SSO/LDAP）、Token管理

**核心服务**：UserService、RoleService、MenuService、OrganizationService、DimensionService

---

### 1.3 rule-engine-manager（规则引擎）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>rule-engine-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：场景联动、告警配置、告警历史、告警记录、规则实例、流程编排

**核心服务**：SceneService、AlarmConfigService、AlarmHistoryService、AlarmHandler

---

### 1.4 notify-manager（通知管理）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>notify-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：邮件、短信、钉钉、企业微信、Webhook、语音通知；通知模板、通知历史

**核心服务**：NotifierService、NotifyTemplateService、NotifierManager

---

### 1.5 network-manager（网络组件管理）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>network-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：MQTT、TCP、UDP、HTTP、WebSocket、CoAP 网络组件配置；证书管理

**核心服务**：NetworkConfigService、CertificateService、NetworkManager

---

### 1.6 datasource-manager（数据源管理）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>datasource-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：MySQL、PostgreSQL、Oracle、SQL Server、ClickHouse、TDengine、InfluxDB 数据源配置；SQL执行

**核心服务**：DataSourceConfigService、DynamicDataSourceService

---

### 1.7 visualization-manager（可视化管理）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>visualization-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：可视化项目、模板、大屏配置、数据源集成、实时数据展示

**核心服务**：VisProjectService、VisTemplateService

---

### 1.8 logging-manager（日志管理）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>logging-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：系统日志、操作日志、访问日志记录、查询、导出

**核心服务**：AccessLoggerService、SystemLoggerService

---

### 1.9 jetlinks-function（函数管理）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>jetlinks-function</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：脚本函数（Groovy/JavaScript）、HTTP调用函数、设备属性计算函数

**核心服务**：FunctionService、FunctionExecutor

---

### 1.10 jetlinks-media（视频流媒体）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>media-manager</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：GB28181、ONVIF、RTSP、RTMP 视频设备接入；实时点播、历史回放、云台控制

**核心服务**：MediaServerService、ChannelService、PlaybackService

---

## 二、基础组件 (Component Modules)

位于 `jetlinks-components/` 目录，提供底层能力支撑。

### 2.1 common-component（通用组件）★

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>common-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：缓冲批量处理、集群聚合计算、序列化、事件总线、熔断器、资源管理、实时数学计算

**核心工具**：PersistenceBuffer、FluxCluster、EventBus、CircuitBreaker、Serializers

---

### 2.2 assets-component（资产权限组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>assets-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：通用非侵入式资产数据权限控制

**核心类**：AssetsHolderCrudController、AssetType、AssetPermissionService

---

### 2.3 api-component（API组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>api-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：API定义、版本管理、接口注册

---

### 2.4 application-component（应用管理组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>application-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：应用配置、应用生命周期管理、应用间通信

---

### 2.5 configure-component（配置管理组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>configure-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：动态配置管理、配置版本控制、配置热更新

---

### 2.6 dashboard-component（仪表板组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>dashboard-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：仪表板定义、指标统计、数据可视化

---

### 2.7 datasource-component（数据源组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>datasource-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：动态数据源支持、多数据源切换、数据源连接池管理

---

### 2.8 gateway-component（网关组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>gateway-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：API网关、路由管理、限流熔断、认证鉴权

---

### 2.9 geo-component（地理位置组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>geo-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：地理围栏、位置计算、轨迹管理、GIS功能

---

### 2.10 io-component（IO组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>io-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：文件上传下载、Excel导入导出、PDF生成、文件存储

---

### 2.11 logging-component（日志组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>logging-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：日志拦截、日志存储、日志查询、日志分析

---

### 2.12 plugin-component（插件组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>plugin-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：插件加载、插件生命周期管理、插件隔离

---

### 2.13 protocol-component（协议组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>protocol-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：设备协议定义、协议解析、协议编解码

---

### 2.14 relation-component（关系管理组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>relation-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：实体关系管理、关系图谱、关系查询

---

### 2.15 rule-engine-component（规则引擎组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>rule-engine-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：规则引擎核心、规则节点、规则执行器、规则调度

---

### 2.16 script-component（脚本组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>script-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：脚本执行（JavaScript/Groovy/Python）、脚本沙箱、脚本调试

---

### 2.17 things-component（物模型组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>things-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：物模型定义、物实例管理、属性事件服务管理

---

### 2.18 timeseries-component（时序数据组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>timeseries-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：时序数据存储、时序数据查询、聚合统计

---

### 2.19 collector-component（采集器组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>collector-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：数据采集、采集器管理、采集任务调度

---

## 三、网络组件 (Network Components)

### 3.1 network-component（网络核心）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>network-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：网络组件核心抽象、网络客户端/服务端接口定义

---

### 3.2 mqtt-component（MQTT组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>mqtt-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：MQTT服务端、MQTT客户端、MQTT消息路由

---

### 3.3 tcp-component（TCP组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>tcp-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：TCP服务端、TCP客户端、粘包拆包处理

---

### 3.4 udp-component（UDP组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>udp-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：UDP服务端、UDP广播、UDP组播

---

### 3.5 http-component（HTTP组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>http-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：HTTP客户端、HTTP服务端、RESTful支持

---

### 3.6 websocket-component（WebSocket组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>websocket-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：WebSocket服务端、WebSocket客户端、实时推送

---

### 3.7 coap-component（CoAP组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>coap-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：CoAP服务端、CoAP客户端、CoAP资源管理

---

### 3.8 serialport-component（串口组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>serialport-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：串口通信、串口设备管理

---

## 四、数据存储组件 (Storage Components)

### 4.1 elasticsearch-component（Elasticsearch组件）

```xml
<!-- Elasticsearch 7.x -->
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>elasticsearch-7x</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- Elasticsearch 8.x -->
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>elasticsearch-8x</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：全文检索、日志存储、时序数据存储、聚合查询

---

### 4.2 influxdb-component（InfluxDB组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>influxdb-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：时序数据存储、InfluxDB查询、数据保留策略

---

### 4.3 tdengine-component（TDengine组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>tdengine-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：时序数据存储、TDengine查询、超级表管理

---

### 4.4 timescaledb-component（TimescaleDB组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>timescaledb-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：时序数据存储、TimescaleDB查询、连续聚合

---

### 4.5 clickhouse-component（ClickHouse组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>clickhouse-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：列式存储、OLAP分析、大数据查询

---

### 4.6 cassandra-component（Cassandra组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>cassandra-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：分布式存储、高可用、大数据量存储

---

### 4.7 iotdb-component（IoTDB组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>iotdb-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：时序数据存储、IoTDB查询、存储组管理

---

### 4.8 mongodb-component（MongoDB组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>mongodb-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：文档存储、MongoDB查询、聚合管道

---

### 4.9 doris-component（Doris组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>doris-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：MPP分析、实时数据仓库、OLAP查询

---

### 4.10 starrocks-component（StarRocks组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>starrocks-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：极速统一分析、实时数仓、向量化查询

---

## 五、消息中间件组件 (Messaging Components)

### 5.1 kafka-component（Kafka组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>kafka-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：Kafka生产者、Kafka消费者、消息队列

---

### 5.2 rabbitmq-component（RabbitMQ组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>rabbitmq-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：RabbitMQ生产者、RabbitMQ消费者、交换机路由

---

### 5.3 rocketmq-component（RocketMQ组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>rocketmq-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：RocketMQ生产者、RocketMQ消费者、消息追踪

---

## 六、通知组件 (Notify Components)

### 6.1 notify-core（通知核心）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>notify-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：通知抽象、通知模板、通知历史

---

### 6.2 notify-email（邮件通知）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>notify-email</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：邮件发送、附件支持、HTML邮件

---

### 6.3 notify-sms（短信通知）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>notify-sms</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：短信发送、短信模板、多平台支持

---

### 6.4 notify-dingtalk（钉钉通知）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>notify-dingtalk</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：钉钉机器人、钉钉消息推送

---

### 6.5 notify-wechat（企业微信通知）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>notify-wechat</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：企业微信机器人、企业微信消息推送

---

### 6.6 notify-webhook（Webhook通知）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>notify-webhook</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：HTTP回调、自定义Webhook

---

### 6.7 notify-voice（语音通知）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>notify-voice</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：语音电话、语音播报

---

## 七、微服务组件 (Microservice Components)

### 7.1 microservice-autoconfigure（微服务自动配置）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>microservice-autoconfigure</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：微服务自动配置、服务发现、配置中心集成

---

## 八、其他组件

### 8.1 test-component（测试组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>test-component</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

**功能**：测试工具类、Mock工具、集成测试支持

---

### 8.2 streaming-component（流式处理组件）

```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>streaming-component</artifactId>
    <version>${project.version}</version>
</dependency>
```

**功能**：流式数据处理、实时计算

---

## 📌 使用建议

### 1. 常用组件组合

**基础 CRUD 功能**：
```xml
<!-- 必需 -->
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>common-component</artifactId>
</dependency>

<!-- 如需权限控制 -->
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>assets-component</artifactId>
</dependency>
```

**设备接入相关**：
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>device-manager</artifactId>
</dependency>
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>network-component</artifactId>
</dependency>
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>protocol-component</artifactId>
</dependency>
```

**规则引擎相关**：
```xml
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>rule-engine-manager</artifactId>
</dependency>
<dependency>
    <groupId>org.jetlinks.pro</groupId>
    <artifactId>rule-engine-component</artifactId>
</dependency>
```

### 2. 查找技巧

```bash
# 搜索模块中的具体实现
grep -r "类名或功能关键词" modules/
grep -r "类名或功能关键词" jetlinks-components/

# 查找模块的 pom.xml
find . -name "pom.xml" | grep "模块名"

# 查看模块的依赖关系
cd modules/模块名 && mvn dependency:tree
```

### 3. 注意事项

- ⚠️ 引入模块前，先查看 [module-reference.md](./module-reference.md) 确定是否真的需要模块引入
- ⚠️ 优先使用跨服务调用（命令模式）而不是直接引入模块
- ⚠️ 只引入必需的模块，避免不必要的依赖
- ⚠️ 注意 `${project.version}` 需要与项目版本保持一致

---

## 📚 相关文档

- [module-reference.md](./module-reference.md) - 模块详细说明和使用示例
- [common-crud-rules.md](./common-crud-rules.md) - CRUD 开发规范
- [cross-service-call-rules.md](./cross-service-call-rules.md) - 跨服务调用规范
- [README.md](../README.md) - 开发辅助工具总览

---

**最后更新**：2025-01-22

