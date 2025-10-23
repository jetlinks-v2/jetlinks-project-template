# JetLinks 项目开发规范 - AI辅助规则

## 项目概述

JetLinks是基于Spring Boot的物联网平台，使用Reactive响应式编程模型，采用模块化架构设计。

## 核心技术栈

- Spring Boot + WebFlux (响应式编程) / Spring MVC (阻塞式编程)
- EasyORM (响应式ORM框架 / 阻塞式ORM)
- HSWebFramework (权限控制框架)
- Reactor (Mono/Flux) - 响应式编程
- PostgreSQL/MySQL (关系型数据库)

## 编程模式说明

JetLinks 支持两种编程模式：

1. **响应式编程模式(WebFlux)**: 适用于高并发、IO密集型场景，使用 Mono/Flux，非阻塞
2. **阻塞式编程模式(Spring MVC)**: 适用于传统业务场景，使用普通Java对象，代码简单直观

本规范同时包含两种模式的开发指南。

## ⚠️ 重要：AI 代码生成核心原则

### 🔴 最小化代码生成原则（必读！）

**在生成任何代码前，请务必遵守以下核心原则**：

1. **只生成用户明确要求的代码**
   - ❌ 不要创建用户没有明确说明的功能
   - ❌ 不要创建"示例"或"演示"代码
   - ❌ 不要创建"可能有用"的辅助方法
   - ✅ 严格按照用户提供的需求生成代码
   - ✅ 如果不确定，先询问用户

2. **保持代码简洁**
   - Service 层：空实现即可，所有 CRUD 功能通过父类提供
   - Controller 层：空实现即可，所有标准接口通过接口继承提供
   - 只在用户明确要求或确实需要时才添加自定义方法

3. **利用框架提供的能力**
   - 动态查询：通过 `POST /_query` 接口支持所有查询场景
   - 标准 CRUD：通过接口继承自动提供
   - 不需要为每个字段创建查询方法

4. **避免过度设计**
   - ❌ 不要"预测"用户需求
   - ❌ 不要创建"完整"的示例
   - ✅ 生成最小可用代码
   - ✅ 让用户按需扩展

---

## ⚠️ 重要：编程模式选择

**在开始生成代码前，必须先询问用户选择编程模式：**

### 询问模板

```
请选择您要使用的编程模式：

1. **响应式编程模式 (WebFlux)** - 推荐
   - 适用于：高并发、IO密集型场景
   - 特点：使用 Mono/Flux，非阻塞，性能更好
   - 返回类型：Mono<T>、Flux<T>
   - 学习成本：较高

2. **阻塞式编程模式 (Spring MVC)** - 传统
   - 适用于：传统业务场景、管理后台
   - 特点：使用普通Java对象，代码简单直观
   - 返回类型：Object、List<T>、Optional<T>
   - 学习成本：较低

请明确告诉我您选择哪种模式，我将根据您的选择生成相应的代码。
```

### 模式选择建议

- **默认推荐响应式模式**：JetLinks项目主要使用响应式编程
- **如果用户未明确说明**：询问用户选择
- **如果用户不确定**：建议响应式模式，并说明两种模式的特点

---

## 一、模块架构规范

### 1.1 模块职责划分

JetLinks项目采用模块化架构，不同模块有明确的职责分工：

#### API模块 (api)

- **职责**: 定义数据传输对象和对外接口契约
- **包含内容**:
    - VO类 (Value Object) - 用于API数据传输
    - DTO类 (Data Transfer Object) - 用于请求/响应
    - 接口定义和文档
- **禁止**: 不创建JPA实体类、Service类、Controller类，不直接操作数据库

#### Manager模块 (manager)

- **职责**: 核心业务逻辑，数据持久化，API实现
- **包含内容**:
    - Entity类 - JPA实体类，对应数据库表
    - Service类 - 业务逻辑服务
    - Controller类 - REST API控制器（重要：Controller放在manager模块中）
    - Repository类 - 数据访问层
    - 枚举类、工具类等

#### 模块间关系

```
外部调用
    ↓
Manager模块 (manager) - 包含Entity、Service、Controller
    ↓ 操作
数据库
```

### 1.2 命名规范

- **Entity类**: 在manager模块的entity包中，如 `DeviceEntity`
- **VO类**: 在api模块中，如 `DeviceInfo`、`DeviceDetail`
- **Service类**: 在manager模块的service包中，如 `DeviceService`
- **Controller类**: 在manager模块的web包中，如 `DeviceController`

---

## 二、实体层(Entity)开发规范

### 2.1 基础实体类开发规范

**注意**: 实体类应创建在 **manager模块** ，不在api模块创建。

#### 必需注解和配置

- 继承 `GenericEntity<String>` 或 `GenericTreeSortSupportEntity<String>`
- 实现 `RecordCreationEntity` 和 `RecordModifierEntity` 接口
- 添加 `@Table(name = "表名")` - **注意：使用 JPA 的注解 `javax.persistence.Table`**
- 添加 `@EnableEntityEvent` 启用实体事件支持
- 使用 `@Column` 标记字段，设置长度、是否可更新等属性 - **注意：使用 JPA 的注解 `javax.persistence.Column`**
- 使用 `@Schema` 提供API文档描述
- 使用 `@JsonCodec` 处理JSON字段
- 使用 `@EnumCodec` 处理枚举字段
- 使用 `@DefaultValue` 设置默认值
- 使用 `@GeneratedValue` 配置ID生成策略

#### 🔴 统一字段规范（重要！）

当创建的实体字段与JetLinks体系中的标准字段冲突时，**必须以JetLinks规范为准**，使用统一的抽象类和接口。具体字段规范如下：

**基础实体类继承模式**：
```java
// ✅ 正确：继承 GenericEntity 并实现 RecordCreationEntity, RecordModifierEntity
@Getter
@Setter
@Table(name = "example")
@EnableEntityEvent
public class ExampleEntity extends GenericEntity<String>
    implements RecordCreationEntity, RecordModifierEntity {

    // 业务字段
    @Column(length = 128, nullable = false)
    @Schema(description = "业务名称")
    private String businessName;

    @Column(length = 512)
    @Schema(description = "业务描述")
    private String description;

    // 🔴 重要说明：
    // - id: 主键ID (已通过GenericEntity提供，无需定义)
    // - RecordCreationEntity 和 RecordModifierEntity 是接口，需要手动定义相应字段
    // - creatorId, creatorName, createTime (RecordCreationEntity接口要求，必须定义)
    // - modifierId, modifierName, modifyTime (RecordModifierEntity接口要求，必须定义)

    // 接口要求的标准字段需要手动定义：
    @Column(length = 64, updatable = false)
    @Schema(description = "创建者ID(只读)", accessMode = Schema.AccessMode.READ_ONLY)
    private String creatorId;

    @Column(updatable = false)
    @Schema(description = "创建者名称(只读)", accessMode = Schema.AccessMode.READ_ONLY)
    private String creatorName;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间(只读)", accessMode = Schema.AccessMode.READ_ONLY)
    private Long createTime;

    @Column(length = 64)
    @Schema(description = "修改人ID")
    private String modifierId;

    @Column(length = 64)
    @Schema(description = "修改人名称")
    private String modifierName;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "修改时间")
    private Long modifyTime;
}
```

**树形实体类继承模式**：
```java
// ✅ 正确：继承 GenericTreeSortSupportEntity 并实现 RecordCreationEntity
public class ExampleTreeEntity extends GenericTreeSortSupportEntity<String>
    implements RecordCreationEntity {
    // 业务字段...

    // 🔴 重要说明：
    // - id, parentId, path, sortIndex ,level (树形结构字段已在抽象类中定义)
    // - children: 子节点列表字段需要手动定义（抽象类未提供），不需要@Column注解
    // - RecordCreationEntity 接口要求：creatorId, creatorName, createTime 必须手动定义

    // 标准字段（RecordCreationEntity接口要求）
    @Column(length = 64, updatable = false)
    private String creatorId;

    @Column(length = 128)
    @Upsert(insertOnly = true)
    private String creatorName;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    // 🔴 树形结构补充字段（需要手动定义，不需要注解）
    private List<ExampleTreeEntity> children;
}
```

**标准字段说明**：

| 字段名 | 类型 | 说明 | 归属 |
|--------|------|------|------|
| `id` | String | 主键ID | GenericEntity |
| `creatorId` | String | 创建人ID | RecordCreationEntity |
| `creatorName` | String | 创建人名称 | RecordCreationEntity |
| `createTime` | Long | 创建时间（时间戳） | RecordCreationEntity |
| `modifierId` | String | 修改人ID | RecordModifierEntity |
| `modifierName` | String | 修改人名称 | RecordModifierEntity |
| `modifyTime` | Long | 修改时间（时间戳） | RecordModifierEntity |
| `parentId` | String | 父节点ID | GenericTreeSortSupportEntity |
| `name` | String | 节点名称 | GenericTreeSortSupportEntity |
| `path` | String | 节点路径 | GenericTreeSortSupportEntity |
| `sortIndex` | Integer | 排序索引 | GenericTreeSortSupportEntity |
| `children` | List<E> | 子节点列表 | GenericTreeSortSupportEntity |

**重要说明**：实现`RecordCreationEntity`和`RecordModifierEntity`接口时，
**必须在实体类中定义相应的字段**，接口只是规范，不提供字段实现。

**字段配置规范**：

```java
// 创建人相关字段 (RecordCreationEntity接口要求)
@Column(length = 64, updatable = false)
@Schema(description = "创建人ID")
private String creatorId;

@Column
@Schema(description = "创建人名称")
@Upsert(insertOnly = true)  // 只在插入时设置
private String creatorName;

@Column(updatable = false)
@DefaultValue(generator = Generators.CURRENT_TIME)
@Schema(description = "创建时间")
private Long createTime;

// 修改人相关字段 (RecordModifierEntity接口要求)
@Column(length = 64)
@Schema(description = "修改人ID")
private String modifierId;

@Column
@Schema(description = "修改人名称")
private String modifierName;

@Column
@DefaultValue(generator = Generators.CURRENT_TIME)
@Schema(description = "修改时间")
private Long modifyTime;
```

**❌ 错误示例**：
```java
// ❌ 错误：不要重复定义标准字段
public class ExampleEntity extends GenericEntity<String> {
    @Column
    private String id;  // 重复定义，GenericEntity已提供


}
```

**✅ 正确示例**：
```java
// ✅ 正确：定义业务字段和标准字段
@Getter
@Setter
@Table(name = "example")
@EnableEntityEvent
public class ExampleEntity extends GenericEntity<String>
    implements RecordCreationEntity, RecordModifierEntity {

    // 业务字段
    @Column(length = 128, nullable = false)
    @Schema(description = "业务名称")
    private String businessName;

    @Column(length = 512)
    @Schema(description = "业务描述")
    private String description;

    // 标准字段（接口要求实现）
    @Column(length = 64, updatable = false)
    @Schema(description = "创建人ID")
    private String creatorId;

    @Column(length = 128)
    @Schema(description = "创建人名称")
    @Upsert(insertOnly = true)
    private String creatorName;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间")
    private Long createTime;

    @Column(length = 64)
    @Schema(description = "修改人ID")
    private String modifierId;

    @Column(length = 128)
    @Schema(description = "修改人名称")
    private String modifierName;

    @Column
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "修改时间")
    private Long modifyTime;
}
```

#### 字段类型规范

- **基础字段**: String、Integer、Long、Boolean等
- **枚举字段**: 使用 `@EnumCodec` 和 `@ColumnType(javaType = String.class)`
- **JSON字段**: 使用 `Map<String, Object>` + `@JsonCodec`
- **大文本字段**: 使用 `@ColumnType(jdbcType = JDBCType.CLOB)`
- **时间字段**: 使用 `Long` 类型存储时间戳

#### 转换方法

- 添加 `toInfo()` 方法，使用 `FastBeanCopier.copy(this, VO::new)`
- 添加 `toDetail()` 方法，处理特殊字段转换
- 导入对应的VO类
- **注意：FastBeanCopier 的完整包名为 `org.hswebframework.web.bean.FastBeanCopier`**

### 2.2 树形实体类规范

如果实体需要支持树形结构：

- 继承 `GenericTreeSortSupportEntity<String>` 或 `ExtendableTreeSortSupportEntity<String>`
- 实现 `RecordCreationEntity` 接口（通常不需要 `RecordModifierEntity`，因为抽象类已提供）
- **重要**：不需要手动添加 `children` 字段，抽象类已提供
- 树形结构相关字段（`parentId`, `path`, `sortIndex`, `name`等）都已在抽象类中定义
- 其他业务字段配置与基础实体类相同

**树形实体标准示例**：
```java
// ✅ 正确：标准树形实体继承模式
@Table(name = "example_tree")
@Getter
@Setter
@EnableEntityEvent
public class ExampleTreeEntity extends GenericTreeSortSupportEntity<String>
    implements RecordCreationEntity {

    // 业务字段
    @Column(length = 64, nullable = false)
    @Schema(description = "业务编码")
    private String code;

    @Column(length = 512)
    @Schema(description = "业务描述")
    private String description;

    @Column
    @JsonCodec
    @Schema(description = "业务配置")
    private Map<String, Object> properties;

    // 标准字段（RecordCreationEntity接口要求）
    @Column(length = 64, updatable = false)
    @Schema(description = "创建人ID")
    private String creatorId;

    @Column(length = 128)
    @Schema(description = "创建人名称")
    @Upsert(insertOnly = true)
    private String creatorName;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间")
    private Long createTime;

    // 树形结构字段已通过GenericTreeSortSupportEntity继承获得：
    // - id: 主键ID
    // - parentId: 父节点ID
    // - path: 节点路径
    // - sortIndex: 排序索引

    // 子节点属性,无需定义Column,仅用于数据传输使用
    private List<ExampleTreeEntity> children;
}
```

**❌ 错误示例**：
```java
// ❌ 错误：不要重复定义树形结构字段
public class ExampleTreeEntity extends GenericTreeSortSupportEntity<String> {
    @Column
    private String parentId;  // 重复定义，抽象类已提供

    @Column
    private String name;      // 重复定义，抽象类已提供

    @Column
    private Integer sortIndex; // 重复定义，抽象类已提供

}
```

### 2.3 枚举类型规范

枚举类开发规范：

- 实现 `I18nEnumDict<String>` 接口支持国际化
- 添加 `@AllArgsConstructor` 和 `@Getter` 注解
- 实现 `getValue()` 方法返回枚举名称
- 通过 `GET /dictionary/{枚举类名}/items` 接口获取选项内容

在实体类中使用枚举：

- **单选枚举**: 使用 `@EnumCodec` + `@ColumnType(javaType = String.class)`
- **多选枚举**: 使用 `@EnumCodec(toMask = true)` + `@ColumnType(javaType = Long.class, jdbcType = JDBCType.BIGINT)`

### 2.4 实体注解说明

#### 核心注解

- `@Table(name = "表名")`: 指定数据库表名 - **使用 `javax.persistence.Table`**
- `@EnableEntityEvent`: 启用实体事件支持 - **使用 `org.hswebframework.web.crud.annotation.EnableEntityEvent`**

#### 字段注解

- `@Column`: 标记字段为数据库列 - **使用 `javax.persistence.Column`**
    - `length`: 字段长度
    - `updatable`: 是否可更新(默认true)
    - `nullable`: 是否允许为空(默认true)
- `@ColumnType`: 指定JDBC类型和Java类型 - **使用 `org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType`**
- `@JsonCodec`: 自动将Java对象序列化为JSON存储 - **使用 `org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec`**
- `@EnumCodec`: 枚举类型编解码 - **使用 `org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec`**
    - `toMask = true`: 使用位掩码存储多个枚举值(用于数组)
- `@DefaultValue`: 设置默认值 - **使用 `org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue`**
    - `generator`: 使用生成器(如 `Generators.CURRENT_TIME`)
- `@Schema`: Swagger文档注解
    - `description`: 字段描述
    - `accessMode`: 访问模式(READ_ONLY/READ_WRITE等)

### 2.5 🔴 @Column 定义规范（重要！）

#### 字段约束配置要求

**每个字段都应该有明确的约束配置**，AI在生成实体类时必须仔细审查并设置合适的@Column注解配置。

#### 🔴 字段长度规范（重要！）

| 字段类型 | 默认长度 | 说明 |
|----------|----------|------|
| **所有字符串ID字段** | **64** | creatorId, modifierId, userId, deviceId, productId等 |
| **名称字段** | 128 | name, title, displayName等 |
| **描述字段** | 512 | description, remark等 |
| **编码字段** | 64 | code, type, status等 |
| **大文本字段** | 不限制 | 使用 CLOB 类型 |
| **标识字段** | 32 | 短标识，如 category, level等 |

#### 基本字段约束模板

| 字段类型 | 必填字段配置 | 可选字段配置 | 说明 |
|----------|--------------|--------------|------|
| `String` | `length` + `nullable = false` | `length` | 文本字段必须指定长度 |
| `Integer` | `nullable = false` | - | 数值字段 |
| `Long` | `nullable = false` | - | 长整数字段 |
| `Boolean` | `nullable = false` | - | 布尔字段 |
| `LocalDateTime` | `nullable = false` | - | 日期时间字段 |
| `Map<String,Object>` | `@JsonCodec` | `@JsonCodec` | JSON对象字段 |

#### 🔴 标准字段约束模板

根据实际JetLinks项目代码分析，标准字段约束模式如下：

```java
// 🔴 创建人相关字段（不可更新）
@Column(length = 64, updatable = false)
@Schema(description = "创建者ID(只读)", accessMode = Schema.AccessMode.READ_ONLY)
private String creatorId;

@Column(updatable = false)
@Schema(description = "创建者名称(只读)", accessMode = Schema.AccessMode.READ_ONLY)
private String creatorName;

@Column(updatable = false)
@DefaultValue(generator = Generators.CURRENT_TIME)
@Schema(description = "创建时间(只读)", accessMode = Schema.AccessMode.READ_ONLY)
private Long createTime;

// 🔴 修改人相关字段（可更新）
@Column(length = 64)
@Schema(description = "修改人ID")
private String modifierId;

@Column(length = 64)
@Schema(description = "修改人名称")
private String modifierName;

@Column
@DefaultValue(generator = Generators.CURRENT_TIME)
@Schema(description = "修改时间")
private Long modifyTime;
```

#### 业务字段约束模板

```java
// 🔴 必填文本字段
@Column(length = 128, nullable = false)
@Schema(description = "业务名称")
@NotBlank // 使用jsr303校验
private String businessName;

// 🔴 可选文本字段
@Column(length = 512)
@Schema(description = "业务描述")
private String businessDescription;

// 🔴 ID字段（关联其他实体）
@Column(length = 64, nullable = false)
@Schema(description = "关联设备ID")
@NotBlank
private String deviceId;

// 🔴 枚举字段
@Column(length = 32, nullable = false)
@EnumCodec
@ColumnType(javaType = String.class)
@Schema(description = "业务状态")
private BusinessStatus status;

// 🔴 数值字段
@Column(nullable = false)
@Schema(description = "排序索引")
private Integer sortIndex;

// 🔴 布尔字段
@Column
@Schema(description = "是否启用")
private Boolean enabled;

// 🔴 JSON字段
@Column
@JsonCodec
@Schema(description = "扩展配置")
private Map<String, Object> properties;
```

#### 特殊约束配置

```java
@Column(length = 64, nullable = false)
@Schema(description = "业务编码")
private String businessCode;

// 🔴 大文本字段
@Column
@ColumnType(jdbcType = JDBCType.LONGVARCHAR)
@Schema(description = "详细内容")
private String content;

// 🔴 时间戳字段（自动更新）
@Column
@DefaultValue(generator = Generators.CURRENT_TIME)
@Schema(description = "创建时间")
private Long createTime;

// 🔴 定长字段
@Column(length = 32)
@Schema(description = "分类标识")
private String category;
```

#### 🔴 AI 审查清单

在生成实体类时，AI必须按以下顺序审查每个字段：

1. **字段类型分析**
   - 字段是什么类型？
   - 是否为ID字段（必须长度64）？
   - 是否为必填字段？
   - 是否有特殊业务需求？

2. **约束配置审查**
   - `String` 类型：是否设置了 `length`？
   - **ID字段**：是否设置 `length = 64`？
   - **名称字段**：是否设置合适的长度（通常128）？
   - **描述字段**：是否设置合适的长度（通常512）？
   - 必填字段：是否设置了 `nullable = false`？
   - 创建时间字段：是否设置了 `updatable = false`？
   - JSON字段：是否添加了 `@JsonCodec`？
   - 枚举字段：是否添加了 `@EnumCodec` 和 `@ColumnType`？

3. **注解完整性**
   - 是否有 `@Column` 注解？
   - 是否有 `@Schema` 注解？
   - 是否有相关的编解码注解？

4. **业务逻辑检查**
   - 字段配置是否符合业务需求？
   - 约束是否合理？
   - 是否遗漏了必要的配置？

#### ❌ 错误示例

```java
// ❌ 错误：缺少长度约束
@Column
private String name;  // String字段必须指定length

// ❌ 错误：ID字段长度错误
@Column(length = 32)
private String deviceId;  // ID字段应该使用长度64

// ❌ 错误：必填字段未设置约束
@Column
private String requiredField;  // 应该设置 nullable = false

// ❌ 错误：JSON字段缺少编解码
@Column
private Map<String, Object> config;  // 应该添加 @JsonCodec

// ❌ 错误：枚举字段缺少编解码
@Column
private StatusEnum status;  // 应该添加 @EnumCodec 和 @ColumnType
```

#### ✅ 正确示例

```java
import java.sql.JDBCType;

// ✅ 正确：完整的字段约束配置
@Getter
@Setter
@Table(name = "example")
@EnableEntityEvent
public class ExampleEntity extends GenericEntity<String>
    implements RecordCreationEntity, RecordModifierEntity {

    // 必填文本字段
    @Column(length = 128, nullable = false)
    @Schema(description = "业务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessName;

    // 可选文本字段
    @Column(length = 512)
    @Schema(description = "业务描述")
    private String businessDescription;

    // ID关联字段
    @Column(length = 64, nullable = false)
    @Schema(description = "关联设备ID")
    private String deviceId;

    // 枚举字段
    @Column(length = 32, nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "业务状态")
    private BusinessStatus status;

    // JSON字段
    @Column
    @JsonCodec
    //JsonCodec需配合ColumnType
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR,javaType=String.class)
    @Schema(description = "扩展配置")
    private Map<String, Object> properties;

    // 标准字段（接口要求实现）
    @Column(length = 64, updatable = false)
    @Schema(description = "创建人ID")
    private String creatorId;

    // ... 其他字段
}
```

---

## 三、VO类(Value Object)开发规范

### 3.1 基础VO类开发规范

**注意**: VO类应创建在 **api模块** 中，用于API数据传输，不包含JPA注解。

#### 必需配置

- 添加 `@Getter` 和 `@Setter` 注解
- 添加 `@Schema(description = "描述")` 注解
- 不包含JPA相关注解

#### 常用字段类型

- **基础字段**: String、Integer、Long、Boolean等
- **时间字段**: 使用 `Long` 表示时间戳.
- **关联字段**: 添加关联对象的ID和名称字段

### 3.2 详情VO类规范

用于返回详细信息：

- 继承基础VO类
- 添加更多详细字段
- 包含扩展属性、关联数据、统计信息等

### 3.3 树形VO类规范

用于树形结构数据：

- 继承基础VO类
- 添加 `parentId`、`sortIndex`、`children` 字段
- 添加 `level`、`leaf` 等树形相关字段

### 3.4 查询VO类规范

用于查询条件：

- 添加查询条件字段
- 使用 `@JsonFormat` 格式化时间范围查询
- 添加关键词搜索、排序等字段

### 3.5 创建/更新VO类规范

用于请求参数：

- 创建请求VO：包含必填字段验证
- 更新请求VO：字段通常为可选
- 使用 `@NotBlank` 等验证注解

### 3.6 VO类最佳实践

#### 命名规范

- 基础信息: `{实体名}Info`
- 详细信息: `{实体名}Detail`
- 树形结构: `{实体名}Tree`
- 查询条件: `{实体名}Query`
- 创建请求: `Create{实体名}Request`
- 更新请求: `Update{实体名}Request`

#### 字段设计原则

- 只包含前端需要的字段
- 添加状态文本等显示字段
- 使用合适的日期时间格式
- 避免暴露敏感信息

#### 注解使用规范

- 使用 `@Schema` 提供API文档
- 使用 `@JsonFormat` 格式化日期时间
- 使用 `@NotBlank` 等验证注解
- 不使用JPA相关注解

#### 继承关系

- 基础VO类作为父类
- 详情VO继承基础VO
- 树形VO继承基础VO

#### Entity到VO的转换

- **推荐方式**: 在Entity类中添加 `toInfo()` 方法
- 使用 `FastBeanCopier` 进行字段拷贝(性能优秀)
- 对于特殊字段(如枚举文本、时间格式)可手动处理
- 在Controller中直接调用转换方法

---

## 四、服务层(Service)开发规范

### 4.1 响应式服务类开发规范

#### 基础配置

- 继承 `GenericReactiveCrudService<Entity, String>`
- 添加 `@Service` 和 `@Slf4j` 注解
- 所有方法返回 `Mono<T>` 或 `Flux<T>`

#### 继承的 CRUD 方法

- `save(Publisher<E>)`: 批量保存
- `insert(Publisher<E>)`: 批量新增
- `updateById(K, Mono<E>)`: 根据ID更新
- `deleteById(Publisher<K>)`: 根据ID删除
- `findById(K)`: 根据ID查询
- `query(QueryParamEntity)`: 动态查询（支持复杂条件）
- `queryPager(QueryParamEntity)`: 分页查询
- `count(QueryParamEntity)`: 统计数量

#### 🚨 重要：Service 层简化原则

**不要生成大量重复的查询方法**，例如：

```java
// ❌ 错误：不要生成这类方法
public Flux<Entity> findByField1(String field1) { ...}

public Flux<Entity> findByField2(String field2) { ...}

public Flux<Entity> findByField3(String field3) { ...}
```

**推荐使用动态查询**：

```java
// ✅ 正确：通过 QueryParamEntity 实现灵活查询
// 前端可动态构建查询条件，无需后端定义大量方法
```

#### 自定义业务方法原则

- **只在真正有特殊业务逻辑时**才添加自定义方法
- **不要**为简单查询创建方法，使用动态查询替代
- 使用 `@Transactional` 注解管理事务
- 复杂业务逻辑可组合使用 `createQuery()`、`createUpdate()`、`createDelete()`

#### ⚠️ 代码生成最小化原则

**AI 在生成代码时，请严格遵守以下原则**：

1. **只生成用户明确要求的功能**
   - ❌ 不要创建示例查询方法（如 `findByName`、`findByType` 等）
   - ❌ 不要创建用户未要求的业务逻辑方法
   - ✅ 只生成基础的 Service 类，继承 `GenericReactiveCrudService` 或 `GenericCrudService` 即可

2. **Service 层应该尽可能简洁**
   ```java
   // ✅ 推荐：简洁的 Service 类
   @Service
   @Slf4j
   public class CategoryPointService extends GenericCrudService<CategoryPointEntity, String> {
       // 空实现即可，所有 CRUD 功能已通过父类提供
   }
   ```

3. **只在以下情况添加自定义方法**
   - 用户明确要求某个特定查询方法
   - 存在复杂的业务逻辑（如级联删除、数据验证等）
   - 需要事务控制的复合操作
   - 需要调用其他服务的业务逻辑

4. **避免过度设计**
   - ❌ 不要"猜测"用户可能需要什么功能
   - ❌ 不要创建"可能有用"的辅助方法
   - ✅ 保持代码简洁，让用户按需添加

### 4.2 树形服务类开发规范

如果实体需要支持树形结构：

- 继承 `GenericReactiveTreeSupportCrudService<Entity, String>`
- 重写 `setChildren()` 方法设置子节点
- 重写 `getIDGenerator()` 方法配置ID生成器

### 4.3 响应式服务层最佳实践

#### 编程规范

- 所有方法返回 `Mono` 或 `Flux`
- 使用 `createQuery()` 构建查询条件
- 使用 `@Slf4j` 注解，在关键位置记录日志
- 使用 `onErrorResume`、`onErrorReturn` 处理异常
- 使用 `@Transactional` 注解管理事务

---

## 二.A、非响应式(Spring MVC)服务层开发规范

### 4.A.1 阻塞式服务类开发规范

#### 基础配置

- 继承 `GenericCrudService<Entity, String>`
- 添加 `@Service` 和 `@Slf4j` 注解
- 所有方法返回普通Java对象、List、Optional等

#### 继承的方法

- `save(Collection<E>)`: 批量保存
- `insert(Collection<E>)`: 批量新增
- `updateById(K, E)`: 根据ID更新
- `deleteById(Collection<K>)`: 根据ID删除
- `findById(K)`: 根据ID查询，返回Optional
- `createQuery()`: 创建查询对象
- `createUpdate()`: 创建更新对象
- `createDelete()`: 创建删除对象

#### 自定义业务方法

- 使用 `createQuery()` 构建查询条件
- 使用 `createUpdate()` 构建更新条件
- 使用 `@Transactional` 注解管理事务
- 返回类型使用 `List`、`Optional`、基本类型等

### 4.A.2 响应式与阻塞式混合使用

在 Spring MVC 环境中，可以混合使用两种模式：

#### 调用方式

- **阻塞式调用响应式**: 使用 `.block()` 方法
- **响应式调用阻塞式**: 使用 `Mono.fromCallable()` 包装
- **工具类转换**: 使用 `Reactors` 工具类

### 4.A.3 阻塞式查询规范

#### 常用查询模式

- **单条件查询**: 返回 `Optional<Entity>`
- **多条件查询**: 返回 `List<Entity>`
- **IN查询**: 使用 `Arrays.asList()` 构建条件
- **分页查询**: 使用 `fetchPaged()` 方法
- **统计数量**: 使用 `count()` 方法

### 4.A.4 阻塞式最佳实践

#### 编程规范

- 返回类型使用 `List`、`Optional`、基本类型等
- 使用 try-catch 处理异常
- 使用 `@Transactional` 注解管理事务
- 注意线程池配置，避免线程阻塞
- 可以在需要时调用响应式Service，使用 `.block()` 转换

---

## 三、控制器层(Controller)开发规范

### 5.1 响应式控制器开发规范

#### ⚠️ 重要：Controller 位置规范

**Controller 必须放在 manager 模块的 web 包中**，不是 api 模块！

#### 基础配置

- 实现 `ReactiveServiceCrudController<Entity, String>` 接口（推荐）
- 或实现 `AssetsHolderCrudController<Entity, String>` 接口（需要资产权限时）
- 添加 `@RestController` 和 `@RequestMapping` 注解
- 添加权限控制注解：`@Authorize`、`@Resource`
- 添加Swagger文档注解：`@Tag`、`@Operation`
- 添加 `@AllArgsConstructor`、`@Getter` 注解

#### 继承的标准 CRUD 接口

通过实现接口，自动获得以下 RESTful 接口：

- `GET /{id}`: 根据ID查询
- `GET /_query`: 查询列表
- `POST /_query`: 分页查询（支持动态查询条件）
- `GET /_count`: 统计数量
- `POST /`: 新增/批量保存
- `PUT /{id}`: 更新
- `DELETE /{id}`: 删除

#### 🚨 重要：Controller 层简化原则

**不要重复定义继承接口已有的方法**，例如：

```java
// ❌ 错误：这些方法已经通过继承获得
@GetMapping("/{id}")
public Mono<Entity> getById(@PathVariable String id) { ...}

@PostMapping("/_query")
public Mono<PagerResult<Entity>> query(@RequestBody QueryParamEntity param) { ...}

// ❌ 错误：不要为简单查询创建接口
@GetMapping("/field/{value}")
public Flux<Entity> findByField(@PathVariable String value) { ...}
```

**正确的做法**：

```java
// ✅ 正确：只需要定义必要的自定义接口
@RestController
public class ExampleController implements ReactiveServiceCrudController<ExampleEntity, String> {

    private final ExampleService service;

    // 只添加真正必要的自定义接口，如导出功能
    @GetMapping("/_export/{name}.xlsx")
    public Mono<Void> exportData(/* 参数 */) {
        // 导出逻辑
    }
}
```

#### 自定义接口规范

- **只在真正需要时**添加自定义接口
- 使用 `@QueryAction` 标记查询操作
- 使用 `@SaveAction` 标记保存操作
- 使用 `@DeleteAction` 标记删除操作
- 使用 `@Operation` 提供接口描述
- 返回类型使用 `Mono<T>` 或 `Flux<T>`

#### ⚠️ Controller 代码生成最小化原则

**AI 在生成 Controller 代码时，请严格遵守以下原则**：

1. **只生成用户明确要求的接口**
   - ❌ 不要创建示例查询接口（如 `findByCategoryId`、`findByType` 等）
   - ❌ 不要创建用户未要求的业务接口
   - ❌ 不要创建"可能有用"的辅助接口
   - ✅ 只生成基础的 Controller 类，实现对应的 CRUD 接口即可

2. **Controller 层应该尽可能简洁**
   ```java
   // ✅ 推荐：简洁的 Controller 类
   @RestController
   @RequestMapping("/cloud-patrol/category-point")
   @AllArgsConstructor
   @Getter
   @Slf4j
   @Resource(id = "cloud-patrol-category-point", name = "分类点位管理")
   @Tag(name = "分类点位管理")
   @AssetsController(type = "cloud-patrol")
   public class CategoryPointController implements BlockingAssetsHolderCrudController<CategoryPointEntity, String> {

       private final CategoryPointService service;

       // 空实现即可，所有标准 CRUD 接口已通过接口提供
       // 如：GET /{id}, POST /_query, POST /, PUT /{id}, DELETE /{id} 等
   }
   ```

3. **标准 CRUD 接口已自动提供**

   通过实现 `AssetsHolderCrudController` 或 `BlockingAssetsHolderCrudController`，以下接口自动可用：
   - `GET /{id}` - 根据ID查询
   - `POST /_query` - 分页查询（支持动态条件）
   - `GET /_query/no-paging` - 不分页查询
   - `GET /_count` - 统计数量
   - `POST /` 或 `PATCH /` - 保存/批量保存
   - `PUT /{id}` - 更新
   - `DELETE /{id}` - 删除

   **这些接口不需要在代码中重复定义！**

4. **只在以下情况添加自定义接口**
   - 用户明确要求某个特定接口
   - 需要导出、导入等特殊功能
   - 需要复杂的业务逻辑（不是简单查询）
   - 需要调用其他服务的接口

5. **避免过度设计**
   - ❌ 不要"猜测"用户可能需要什么接口
   - ❌ 不要为每个字段创建查询接口
   - ❌ 不要创建"便捷"的辅助接口
   - ✅ 保持代码简洁，依赖动态查询功能
   - ✅ 让用户通过 `POST /_query` 接口灵活查询

6. **动态查询已经足够强大**

   用户可以通过 `POST /_query` 接口实现各种查询：
   ```json
   {
     "filter": {
      "categoryId": "123",
      "code$gt": 10
     },
     "pageIndex": 0,
     "pageSize": 20
   }
   ```

   **不需要为每种查询创建单独的接口！**

### 5.2 树形结构控制器规范

用于树形结构数据：

- 使用 `@GetMapping("/_tree")` 映射树形查询接口
- 使用 `TreeSupportEntity.list2tree()` 构建树形结构

### 5.3 关联资产权限控制规范

当实体与其他资产关联时：

- 实现 `CorrelatesAssetsHolderCrudController<Entity, String>` 接口
- 重写 `getAssetType()` 方法返回关联的资产类型
- 重写 `getAssetIdMapper()` 方法返回资产ID映射函数
- 重写 `getAssetProperty()` 方法返回关联资产ID字段名

### 5.4 多资产权限控制规范

支持多个资产类型：

- 使用 `@MultiAssetsController` 注解
- 配置多个 `@AssetsController` 注解
- 设置不同的 `assetIdIndex` 参数

---

## 三.A、非响应式(Spring MVC)控制器层开发规范

### 5.A.1 阻塞式控制器开发规范

#### 基础配置

- 实现 `BlockingAssetsHolderCrudController<Entity, String>` 接口
- 添加 `@RestController` 和 `@RequestMapping` 注解
- 添加权限控制注解：`@Authorize`、`@Resource`、`@AssetsController`
- 添加Swagger文档注解：`@Tag`、`@Operation`
- 添加 `@Slf4j`、`@AllArgsConstructor`、`@Getter` 注解

#### 继承的RESTful接口

- `GET /{id}`: 根据ID查询
- `GET /_query`: 查询列表
- `POST /_query`: 分页查询
- `GET /_count`: 统计数量
- `POST /`: 新增/批量保存
- `PUT /{id}`: 更新
- `PATCH /_batch`: 批量修改
- `DELETE /{id}`: 删除

#### 自定义接口规范

- 使用 `@QueryAction` 标记查询操作
- 使用 `@SaveAction` 标记保存操作
- 使用 `@Operation` 提供接口描述
- 返回类型使用普通Java对象、List、boolean等
- 调用Service方法并转换为VO对象

### 5.A.2 阻塞式与响应式混合使用

在 Spring MVC Controller 中可以混合使用两种模式：

#### 调用方式

- **阻塞式接口**: 返回普通Java对象
- **响应式接口**: 返回 `Mono<T>` 或 `Flux<T>`
- **混合调用**: 在阻塞式接口中调用响应式Service，使用 `.block()` 转换

### 5.A.3 关联资产权限控制(阻塞式)

当实体与其他资产关联时：

- 实现 `BlockingCorrelatesAssetsHolderCrudController<Entity, String>` 接口
- 重写 `getAssetType()` 方法返回关联的资产类型
- 重写 `getAssetIdMapper()` 方法返回资产ID映射函数
- 重写 `getAssetProperty()` 方法返回关联资产ID字段名

### 5.A.4 阻塞式最佳实践

#### 编程规范

- 返回类型使用普通Java对象、List、Optional等
- 使用标准的 try-catch 或抛出异常
- 注意线程占用，适合低并发场景
- 可以返回 Flux/Mono 实现流式响应
- Java 21+ 可开启虚拟线程提升性能

---

### 5.5 控制器注解说明

#### 权限控制注解

- `@Authorize`: 启用权限控制(类或方法级别)
    - `merge = false`: 不合并权限检查
- `@Resource`: 定义资源
    - `id`: 资源ID(必填)
    - `name`: 资源名称(必填)
    - `group`: 资源分组
- `@QueryAction`: 查询操作，需要 `query` 权限
- `@SaveAction`: 保存操作，需要 `save` 权限
- `@DeleteAction`: 删除操作，需要 `delete` 权限

#### 资产权限注解

- `@AssetsController`: 资产权限控制
    - `type`: 资产类型
    - `assetIdIndex`: 资产ID在方法参数中的索引(默认0)
    - `assetObjectIndex`: 资产对象在方法参数中的索引(默认-1)
    - `property`: 资产ID属性名(默认"id")
    - `required`: 是否必须有资产权限(默认false)
    - `autoBind`: 保存时自动绑定资产(默认false)
    - `autoUnbind`: 删除时自动解绑资产(默认false)
    - `ignore`: 忽略资产权限控制(默认false)
    - `ignoreQuery`: 忽略查询时的资产权限控制(默认false)
    - `validate`: 是否验证资产权限(默认true)
    - `allowAssetNotExist`: 允许资产不存在(默认false)
    - `permission`: 自定义权限标识

- `@MultiAssetsController`: 多资产权限控制

#### Swagger文档注解

- `@Tag`: API分组标签
- `@Operation`: 接口描述
    - `summary`: 简要描述
    - `description`: 详细描述
- `@Parameter`: 参数描述
    - `description`: 参数说明
    - `hidden`: 是否隐藏(默认false)

---

## 六、权限控制最佳实践

### 6.1 权限层级

```
资源级别 (@Resource)
    ↓
操作级别 (@QueryAction, @SaveAction, @DeleteAction)
    ↓
资产级别 (@AssetsController, @DeviceAsset, @ProductAsset)
```

### 6.2 常见权限配置

#### 标准CRUD权限

- 使用 `@Authorize`、`@Resource`、`@AssetsController` 注解
- 实现对应的Controller接口自动获得权限控制

#### 公开接口(无需权限)

- 使用 `@Authorize(ignore = true)` 忽略权限检查

#### 忽略资产权限

- 使用 `@AssetsController(ignore = true)` 忽略资产权限
- 但仍需基础query权限

#### 仅需要登录

- 使用 `@Authorize(merge = false)` 只验证登录状态

### 6.3 自定义资产类型

创建自定义资产类型注解：

- 使用 `@AssetsController(type = "自定义资产类型")` 作为元注解
- 使用 `@AliasFor` 映射配置项
- 支持所有 `@AssetsController` 的配置选项

---

## 七、常用查询模式

### 7.1 基础查询规范

#### 常用查询模式

- **单条件查询**: 使用 `where(Entity::getField, value)`
- **多条件查询(AND)**: 使用 `and()` 连接条件
- **多条件查询(OR)**: 使用 `or()` 连接条件
- **IN查询**: 使用 `in(Entity::getId, Arrays.asList())`
- **模糊查询**: 使用 `like$(Entity::getName, "keyword")`
- **范围查询**: 使用 `gte()` 和 `lte()` 方法

### 7.2 分页查询规范

使用 `QueryParamEntity` 进行分页查询：

- 使用 `setParam(query)` 设置查询参数
- 使用 `fetchPaged()` 方法获取分页结果

### 7.3 关联查询规范

使用 `leftJoin()` 或 `innerJoin()` 进行关联查询：

- 指定关联的实体类
- 使用Lambda表达式构建关联条件

### 7.4 分组统计规范

使用 `groupBy()` 和 `select()` 进行分组统计：

- 使用 `groupBy(Entity::getField)` 分组
- 使用 `select()` 选择字段
- 使用 `count()` 统计数量

---

## 八、响应式编程规范

### 8.1 Mono和Flux的选择

- `Mono<T>`: 返回0或1个元素
    - 单个对象查询
    - 保存/更新/删除操作
    - 统计操作

- `Flux<T>`: 返回0到N个元素
    - 列表查询
    - 批量操作
    - 流式处理

### 8.2 常用操作符

#### 转换操作符

- `flatMap`: 转换并扁平化
- `map`: 简单转换
- `flatMapMany`: Mono转Flux

#### 空值处理

- `switchIfEmpty`: 空值处理
- `defaultIfEmpty`: 提供默认值

#### 组合操作符

- `zipWith`: 组合两个Mono
- `then`: 忽略结果，返回Mono<Void>
- `thenReturn`: 返回固定值

#### 副作用操作

- `doOnNext`: 副作用操作(不修改流)
- `collectList`: Flux转List

#### 异常处理

- `onErrorResume`: 异常处理
- `onErrorReturn`: 异常时返回默认值

### 8.3 响应式最佳实践

#### 编程规范

- 避免阻塞操作：不要在Reactor链中使用 `.block()`
- 异常处理：使用 `onErrorResume`、`onErrorReturn` 而不是 try-catch
- 日志记录：使用 `doOnNext`、`doOnError` 而不是直接log
- 链式调用：保持链式调用的可读性，适当换行
- 延迟执行：使用 `Mono.defer()` 或 `Flux.defer()` 延迟执行

---

## 九、动态查询使用指南

### 9.1 🚨 为什么使用动态查询

**传统查询方式的问题**：

```java
// ❌ 糟糕的方式：为每个查询条件都创建方法
public Flux<Entity> findByType(Integer type) { ...}

public Flux<Entity> findByStatus(String status) { ...}

public Flux<Entity> findByTypeAndStatus(Integer type, String status) { ...}

public Flux<Entity> findByNameLike(String name) { ...}
// ... 无限的组合
```

**动态查询的优势**：

- ✅ 一个接口支持所有查询组合
- ✅ 前端可以灵活构建查询条件
- ✅ 后端代码简洁，易于维护
- ✅ 支持复杂查询条件（AND、OR、IN、LIKE等）

### 9.2 QueryParamEntity 查询参数结构

前端发送的查询参数示例：

```json
{
    "terms": [
        {
            "column": "type",
            "value": 1
        },
        {
            "column": "status",
            "value": "active"
        },
        {
            "column": "name",
            "value": "test",
            "termType": "like"
        },
        {
            "column": "createTime",
            "value": [
                "2023-01-01",
                "2023-12-31"
            ],
            "termType": "between"
        }
    ],
    "pageIndex": 0,
    "pageSize": 20,
    "sorts": [
        {
            "name": "createTime",
            "order": "desc"
        }
    ]
}
```

### 9.3 支持的查询操作符

| termType   | 说明    | 示例                                                                                       |
|------------|-------|------------------------------------------------------------------------------------------|
| `eq` (默认)  | 等于    | `{"column": "type", "value": 1}`                                                         |
| `like`     | 模糊查询  | `{"column": "name", "value": "test", "termType": "like"}`                                |
| `not`      | 不等于   | `{"column": "status", "value": "deleted", "termType": "not"}`                            |
| `gt`       | 大于    | `{"column": "age", "value": 18, "termType": "gt"}`                                       |
| `gte`      | 大于等于  | `{"column": "age", "value": 18, "termType": "gte"}`                                      |
| `lt`       | 小于    | `{"column": "age", "value": 65, "termType": "lt"}`                                       |
| `lte`      | 小于等于  | `{"column": "age", "value": 65, "termType": "lte"}`                                      |
| `between`  | 区间查询  | `{"column": "createTime", "value": ["2023-01-01", "2023-12-31"], "termType": "between"}` |
| `in`       | 包含查询  | `{"column": "status", "value": ["active", "pending"], "termType": "in"}`                 |
| `not_in`   | 不包含查询 | `{"column": "status", "value": ["deleted"], "termType": "not_in"}`                       |
| `is_null`  | 为空    | `{"column": "deleteTime", "termType": "is_null"}`                                        |
| `not_null` | 不为空   | `{"column": "name", "termType": "not_null"}`                                             |

### 9.4 复杂查询示例

#### 条件组合（AND、OR）

```json
{
    "terms": [
        {
            "type": "and",
            "terms": [
                {
                    "column": "type",
                    "value": 1
                },
                {
                    "column": "status",
                    "value": "active"
                }
            ]
        },
        {
            "type": "or",
            "terms": [
                {
                    "column": "priority",
                    "value": "high"
                },
                {
                    "column": "urgent",
                    "value": true
                }
            ]
        }
    ]
}
```

#### 嵌套查询

```json
{
    "terms": [
        {
            "type": "and",
            "terms": [
                {
                    "column": "projectId",
                    "value": "proj123"
                },
                {
                    "type": "or",
                    "terms": [
                        {
                            "column": "type",
                            "value": 1
                        },
                        {
                            "column": "assignedTo",
                            "value": "user456"
                        }
                    ]
                }
            ]
        }
    ]
}
```

### 9.5 前端使用示例

#### React/Vue 示例

```javascript
// 构建查询参数
const queryParams = {
    where: [],
    pageIndex: 0,
    pageSize: 20,
    sorts: [{name: 'createTime', order: 'desc'}]
};

// 添加查询条件
if (searchForm.type) {
    queryParams.where.push({column: 'type', value: searchForm.type});
}

if (searchForm.name) {
    queryParams.where.push({
        column: 'name',
        value: searchForm.name,
        termType: 'like'
    });
}

if (searchForm.dateRange && searchForm.dateRange.length === 2) {
    queryParams.where.push({
        column: 'createTime',
        value: searchForm.dateRange,
        termType: 'between'
    });
}

// 发送请求
const response = await fetch('/api/entity/_query', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(queryParams)
});
```

### 9.6 后端无需额外配置

**重要的是**：动态查询功能已经内置在 `GenericReactiveCrudService` 中，后端无需做任何额外配置：

```java
// ✅ Service 层什么都不用写，直接使用继承的方法
public class ExampleService extends GenericReactiveCrudService<ExampleEntity, String> {
    // 空实现即可，动态查询已经可用
}

// ✅ Controller 层也无需定义查询接口
@RestController
public class ExampleController implements ReactiveServiceCrudController<ExampleEntity, String> {
    // POST /_query 接口自动可用，支持动态查询
}
```

---

## 十、国际化支持

### 10.1 消息文件规范

在 `src/main/resources/i18n/{模块名}/` 目录下创建:

- `messages_zh.properties`: 中文
- `messages_en.properties`: 英文

### 10.2 使用国际化消息

使用 `LocaleUtils.resolveMessageReactive()` 方法：

- 支持响应式消息解析
- 支持带参数的消息
- 在Service或Controller中使用

### 10.3 实体国际化

实现 `MultipleI18nSupportEntity` 接口：

- 添加 `i18nMessages` 字段存储国际化信息
- 使用 `@JsonCodec` 和 `@ColumnType` 注解
- 实现 `getI18nMessage()` 方法获取国际化文本

---

## 十一、异常处理规范

### 11.1 常用异常类型

#### 异常类型选择

- **业务异常**: 使用 `BusinessException`
- **资源不存在**: 使用 `NotFoundException`
- **验证异常**: 使用 `ValidationException`
- **权限拒绝**: 使用 `AccessDenyException`

### 10.2 响应式异常处理

#### 异常处理方式

- 使用 `switchIfEmpty()` 处理空值异常
- 使用 `onErrorResume()` 处理特定异常类型
- 使用 `onErrorReturn()` 返回默认值
- 在异常处理中记录日志并继续抛出或返回默认值

---

## 十二、测试规范

### 12.1 测试类开发规范

#### 基础配置

- 使用 `@SpringBootTest` 注解
- 使用 `@Autowired` 注入Service
- 使用 `StepVerifier` 进行响应式测试

#### 测试方法规范

- 使用 `@Test` 注解标记测试方法
- 使用 `StepVerifier.create()` 创建测试
- 使用 `expectNextMatches()` 验证结果
- 使用 `verifyComplete()` 完成测试

### 12.2 Controller测试规范

#### 基础配置

- 使用 `@SpringBootTest` 和 `@AutoConfigureWebTestClient` 注解
- 使用 `WebTestClient` 进行HTTP测试

#### 测试方法规范

- 使用 `client.get()` 等方法发送请求
- 使用 `exchange()` 执行请求
- 使用 `expectStatus()` 验证状态码
- 使用 `expectBodyList()` 验证响应体

---

## 十、代码生成指南(供AI使用)

### 10.1 快速生成CRUD模块步骤

当用户要求创建一个新的CRUD模块时，按照以下步骤生成代码:

**⚠️ 重要提醒：在生成代码前，请重新阅读"AI 代码生成核心原则"部分！**

1. **⚠️ 首先询问编程模式**
   ```
   请选择您要使用的编程模式：

   1. **响应式编程模式 (WebFlux)** - 推荐
      - 适用于：高并发、IO密集型场景
      - 特点：使用 Mono/Flux，非阻塞，性能更好
      - 返回类型：Mono<T>、Flux<T>
      - 学习成本：较高

   2. **阻塞式编程模式 (Spring MVC)** - 传统
      - 适用于：传统业务场景、管理后台
      - 特点：使用普通Java对象，代码简单直观
      - 返回类型：Object、List<T>、Optional<T>
      - 学习成本：较低

   请明确告诉我您选择哪种模式，我将根据您的选择生成相应的代码。
   ```

2. **明确需求**
    - 模块名称(如: notify-manager, device-manager)
    - 实体名称(如: NotifyTemplate, DeviceCategory)
    - 表名
    - 字段列表(名称、类型、是否必填、描述)
    - 是否需要树形结构
    - 资产类型(如果需要资产权限控制)
    - **编程模式**: 响应式(WebFlux) 或 阻塞式(Spring MVC) - 已通过询问确定

2. **创建目录结构**

```
modules/{模块名}/
├── api/                           # API模块
│   └── src/main/java/org/jetlinks/pro/{模块名}/api/
│       ├── vo/                    # VO类
│       └── dto/                    # DTO类
└── manager/                       # Manager模块
    └── src/main/java/org/jetlinks/pro/{模块名}/
        ├── entity/                # 实体类
        ├── service/               # 服务类
        ├── enums/                 # 枚举类
        ├── assets/                # 资产类型定义(如需要)
        └── web/                    # 控制器
```

3. **生成Manager模块代码**
    - **实体类**: 在manager模块中生成Entity类
        - 根据字段列表生成完整的Entity类
        - 添加必要的注解(@Table, @Column, @Schema等)
        - 实现RecordCreationEntity和RecordModifierEntity接口
        - 如果是树形结构，继承GenericTreeSortSupportEntity

    - **服务类**: 在manager模块中生成Service类
        - **响应式**: 继承GenericReactiveCrudService或GenericReactiveTreeSupportCrudService
        - **阻塞式**: 继承GenericCrudService
        - 添加@Service和@Slf4j注解
        - ⚠️ **默认生成空实现**，不添加任何自定义方法
        - ⚠️ 只有在用户明确要求时才添加自定义方法

4. **生成API模块代码**
    - **VO类**: 在api模块中生成VO类
        - 基础信息VO: `{实体名}Info`
        - 详细信息VO: `{实体名}Detail` (如需要)
        - 树形结构VO: `{实体名}Tree` (如需要)
        - 查询条件VO: `{实体名}Query`
        - 创建请求VO: `Create{实体名}Request`
        - 更新请求VO: `Update{实体名}Request`

    - **控制器类**: 在manager模块的web包中生成Controller类
        - **响应式**: 实现AssetsHolderCrudController接口
        - **阻塞式**: 实现BlockingAssetsHolderCrudController接口
        - 添加完整的权限注解(@Resource, @Authorize, @AssetsController)
        - 添加Swagger注解(@Tag, @Operation)
        - ⚠️ **默认生成空实现**，不添加任何自定义接口
        - ⚠️ 只有在用户明确要求时才添加自定义接口
        - **重要**: Controller中调用Service，返回Entity对象（框架会自动处理）

5. **生成国际化文件**
    - 在resources/i18n/{模块名}/目录下创建消息文件
    - 添加常用的错误消息和提示信息

6. **生成测试类**
    - 为Service和Controller生成基础测试类

### 10.2 代码生成检查清单

#### Manager模块检查项

- [ ] Entity类包含所有必需注解
- [ ] Entity类实现了RecordCreationEntity和RecordModifierEntity
- [ ] Entity类创建在manager模块中
- [ ] Entity类添加了toInfo()和toDetail()转换方法
- [ ] Entity类导入了FastBeanCopier和对应的VO类
- [ ] Service类继承了正确的基类
    - [ ] 响应式: GenericReactiveCrudService
    - [ ] 阻塞式: GenericCrudService
- [ ] Service类创建在manager模块中

#### API模块检查项

- [ ] VO类创建在api模块中
- [ ] VO类不包含JPA注解
- [ ] VO类包含完整的@Schema注解
- [ ] VO类使用合适的日期时间格式(@JsonFormat)
- [ ] 创建了必要的VO类型:
    - [ ] 基础信息VO: `{实体名}Info`
    - [ ] 详细信息VO: `{实体名}Detail` (如需要)
    - [ ] 树形结构VO: `{实体名}Tree` (如需要)
    - [ ] 查询条件VO: `{实体名}Query`
    - [ ] 创建请求VO: `Create{实体名}Request`
    - [ ] 更新请求VO: `Update{实体名}Request`
- [ ] Controller实现了正确的接口
    - [ ] 响应式: AssetsHolderCrudController
    - [ ] 阻塞式: BlockingAssetsHolderCrudController
- [ ] Controller创建在manager模块的web包中
- [ ] Controller调用manager模块的Service
- [ ] Controller返回类型按需选择：默认返回Entity（复用内置CRUD能力），业务需要时可返回VO

#### 通用检查项

- [ ] 权限注解配置完整(@Resource, @Authorize, @AssetsController)
- [ ] Swagger文档注解完整(@Tag, @Operation, @Parameter)
- [ ] 返回类型正确
    - [ ] 响应式: 所有方法返回Mono或Flux
    - [ ] 阻塞式: 返回普通Java对象、List、Optional等
- [ ] 异常处理完整
    - [ ] 响应式: 使用onErrorResume/onErrorReturn
    - [ ] 阻塞式: 使用try-catch或抛出异常
- [ ] 包含基础的测试用例

### 12.3 常见问题处理

#### 枚举类型处理

- 创建枚举类实现 `I18nEnumDict` 接口
- 在Entity中使用 `@EnumCodec` 和 `@ColumnType` 注解
- 多选枚举使用 `@EnumCodec(toMask = true)` 和位掩码存储

#### JSON对象存储

- 使用 `Map<String, Object>` 类型
- 添加 `@JsonCodec` 和 `@ColumnType` 注解

#### 关联查询实现

- 使用 `leftJoin` 或 `innerJoin` 方法
- 在Service中分别查询后组装

#### 大文本字段处理

- 使用 `@ColumnType(jdbcType = JDBCType.CLOB)` 注解

#### 级联删除实现

- 在Service中重写delete方法
- 先删除关联数据，再删除主数据

#### 响应式与阻塞式混合使用

- 阻塞式调用响应式：使用 `.block()` 或 `Reactors` 工具类
- 响应式调用阻塞式：使用 `Mono.fromCallable()` 包装

#### Entity到VO转换

- 在Entity类中添加 `toInfo()` 方法
- 使用 `FastBeanCopier.copy()` 进行字段拷贝
- 对于特殊字段可手动处理

#### API模块调用Manager模块

- 通过依赖注入调用Manager模块的Service
- 在Controller中转换为VO对象

#### VO类字段映射

- 使用不同的VO类处理不同场景
- 基础信息VO、详细信息VO、树形VO等

#### 推荐使用FastBeanCopier

- JetLinks项目内置工具类，无需额外依赖
- 基于字节码生成，性能接近手写代码
- 使用简单，支持Supplier方式创建目标对象
- 缓存生成的拷贝器，性能优秀
- **完整包名**: `org.hswebframework.web.bean.FastBeanCopier`

---

## 十三、性能优化建议

### 13.1 数据库优化

- 为常用查询字段添加索引
- 避免查询过多字段，使用select指定字段
- 批量操作使用bufferSize控制批次大小

### 13.2 响应式优化

- 使用Flux.buffer()控制批处理大小
- 避免在Reactor链中使用.block()
- 使用.cache()缓存可重复使用的Mono/Flux

### 13.3 缓存策略

- 使用 `.cache()` 缓存可重复使用的Mono/Flux
- 使用Spring Cache注解进行方法级缓存

## 十四、安全规范

### 14.1 输入验证

- 使用jakarta.validation注解验证输入
- 在Entity中定义验证规则
- 使用CreateGroup和UpdateGroup区分创建和更新验证

### 14.2 SQL注入防护

- 使用参数化查询，避免字符串拼接
- EasyORM自动提供参数化查询支持

### 14.3 XSS防护

- 前端输出时进行HTML转义
- 后端存储时不做转义，保持原始数据

## 附录: 快速参考

### 常用注解速查表

| 注解                | 用途      | 位置                    |
|-------------------|---------|-----------------------|
| @Table            | 指定表名    | Entity类               |
| @Column           | 标记字段    | Entity字段              |
| @Schema           | API文档   | Entity字段/Controller方法 |
| @Service          | 服务类     | Service类              |
| @RestController   | REST控制器 | Controller类           |
| @Resource         | 定义资源    | Controller类           |
| @Authorize        | 启用权限    | Controller类/方法        |
| @AssetsController | 资产权限    | Controller类/方法        |
| @QueryAction      | 查询操作    | Controller方法          |
| @SaveAction       | 保存操作    | Controller方法          |

### 常用类速查表

| 类名                                           | 用途         | 模式  |
|----------------------------------------------|------------|-----|
| GenericEntity                                | 基础实体类      | 通用  |
| GenericTreeSortSupportEntity                 | 树形实体类      | 通用  |
| RecordCreationEntity                         | 记录创建人接口    | 通用  |
| RecordModifierEntity                         | 记录修改人接口    | 通用  |
| I18nEnumDict                                 | 枚举字典接口     | 通用  |
| **服务层**                                      |            |     |
| GenericReactiveCrudService                   | 响应式服务基类    | 响应式 |
| GenericReactiveTreeSupportCrudService        | 响应式树形服务基类  | 响应式 |
| GenericCrudService                           | 阻塞式服务基类    | 阻塞式 |
| **控制器层**                                     |            |     |
| AssetsHolderCrudController                   | 响应式控制器接口   | 响应式 |
| CorrelatesAssetsHolderCrudController         | 响应式关联资产控制器 | 响应式 |
| BlockingAssetsHolderCrudController           | 阻塞式控制器接口   | 阻塞式 |
| BlockingCorrelatesAssetsHolderCrudController | 阻塞式关联资产控制器 | 阻塞式 |
| **查询与结果**                                    |            |     |
| QueryParamEntity                             | 查询参数       | 通用  |
| PagerResult                                  | 分页结果       | 通用  |
| **响应式类型**                                    |            |     |
| Mono                                         | 0-1个元素的发布者 | 响应式 |
| Flux                                         | 0-N个元素的发布者 | 响应式 |
| **工具类**                                      |            |     |
| Reactors                                     | 响应式工具类     | 通用  |

---

## 十三、编程模式选择建议

### 13.1 响应式编程(WebFlux)适用场景

- ✅ 高并发、IO密集型应用
- ✅ 需要背压(backpressure)控制
- ✅ 长连接、流式数据处理
- ✅ 微服务间通信密集
- ✅ 资源利用率要求高

### 13.2 阻塞式编程(Spring MVC)适用场景

- ✅ 传统业务系统、管理后台
- ✅ 并发量不高的场景
- ✅ 团队不熟悉响应式编程
- ✅ 大量使用阻塞式第三方库
- ✅ 需要快速开发，代码简单直观
- ✅ Java 21+ 可使用虚拟线程提升性能

### 13.3 模式对比

| 特性    | 响应式(WebFlux) | 阻塞式(Spring MVC)      |
|-------|--------------|----------------------|
| 并发模型  | 非阻塞、事件驱动     | 线程池模型                |
| 线程占用  | 少量线程处理大量请求   | 每个请求占用一个线程           |
| 性能    | 高并发下性能更好     | 低并发下性能足够             |
| 代码复杂度 | 较高，需要理解响应式概念 | 较低，传统编程模式            |
| 学习成本  | 高            | 低                    |
| 调试难度  | 较难，异步调用栈     | 简单，同步调用栈             |
| 返回类型  | Mono/Flux    | Object/List/Optional |
| 适用场景  | IO密集型、高并发    | CPU密集型、低并发           |

---

## 总结

本规范涵盖了JetLinks项目中CRUD模块开发的完整流程，包括:

1. **模块架构规范** - API模块和Manager模块的职责划分
2. **实体类开发** - 在Manager模块中创建JPA实体类
3. **VO类开发** - 在API模块中创建数据传输对象
4. **响应式和阻塞式**两种服务层实现方式
5. **响应式和阻塞式**两种控制器实现方式
6. 权限控制和资产管理
7. 响应式编程最佳实践
8. 阻塞式编程最佳实践
9. 测试和文档编写
10. 编程模式选择建议

**重要原则**:

- **API模块**: 只创建VO类，不创建JPA实体类
- **Manager模块**: 创建Entity类和Service类
- **模块分离**: 确保API模块和Manager模块职责清晰分离
- **Entity转换**: 在Entity类中添加 `toInfo()` 方法，使用 `FastBeanCopier` 进行转换

遵循本规范可以快速、规范地开发出高质量的业务代码。

**AI使用建议**:

- **⚠️ 必须首先询问用户选择编程模式**，使用标准询问模板
- 如果用户未明确说明，**默认使用响应式模式**（项目主流）
- **严格按照模块分离原则**:
    - API模块: 创建VO类
    - Manager模块: 创建Entity类 + Service类 + Controller类
- 根据用户选择的编程模式，使用对应的代码模板：
    - **响应式模式**: 使用响应式Service和Controller模板
    - **阻塞式模式**: 使用阻塞式Service和Controller模板
- 确保生成的代码包含所有必需的注解和配置
- 生成代码时考虑完整性: Entity + Service + VO + Controller + 国际化文件
- 提供清晰的代码说明和使用示例
- 必要时说明两种模式的差异和选择建议

---

## 🔴 AI 最后检查清单（生成代码前必读！）

在生成任何代码之前，请确认以下事项：

### ✅ 需求确认
- [ ] 我已经完全理解用户的需求
- [ ] 用户已明确选择了编程模式（响应式/阻塞式）
- [ ] 我知道需要创建哪些类（Entity、Service、Controller、VO）
- [ ] 我知道用户明确要求的自定义功能（如果有）

### ✅ 代码简洁性
- [ ] Service 类：我只生成空实现（除非用户明确要求自定义方法）
- [ ] Controller 类：我只生成空实现（除非用户明确要求自定义接口）
- [ ] 我没有创建"示例"或"演示"方法
- [ ] 我没有创建用户未明确要求的辅助方法

### ✅ 框架能力利用
- [ ] 我知道动态查询可以满足大部分查询需求
- [ ] 我知道标准 CRUD 接口已通过接口继承提供
- [ ] 我没有为每个字段创建查询方法
- [ ] 我没有重复定义已有的接口

### ✅ 避免过度设计
- [ ] 我没有"猜测"用户可能需要什么
- [ ] 我没有创建"完整"的功能示例
- [ ] 我生成的是最小可用代码
- [ ] 我让用户按需扩展

**如果以上所有项都确认无误，才可以开始生成代码！**

---

## 📋 标准代码模板示例

### Service 层标准模板（推荐）

```java
package org.jetlinks.pro.example.service;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.service.GenericCrudService; // 或 GenericReactiveCrudService
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.springframework.stereotype.Service;

/**
 * 示例服务类
 *
 * @author JetLinks
 * @since 2.11.0
 */
@Service
@Slf4j
public class ExampleService extends GenericCrudService<ExampleEntity, String> {
    // 空实现即可，所有 CRUD 功能已通过父类提供
    // 包括：save, insert, updateById, deleteById, findById, createQuery 等
    //
    // 只有在用户明确要求时才添加自定义方法
}
```

### Controller 层标准模板（推荐）

```java
package org.jetlinks.pro.example.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Resource;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.jetlinks.pro.assets.crud.BlockingAssetsHolderCrudController; // 或 AssetsHolderCrudController
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.jetlinks.pro.example.service.ExampleService;
import org.springframework.web.bind.annotation.*;

/**
 * 示例控制器
 *
 * @author JetLinks
 * @since 2.11.0
 */
@RestController
@RequestMapping("/example")
@AllArgsConstructor
@Getter
@Slf4j
@Resource(id = "example", name = "示例管理")
@Tag(name = "示例管理")
@AssetsController(type = "example")
public class ExampleController implements BlockingAssetsHolderCrudController<ExampleEntity, String> {

    private final ExampleService service;

    // 空实现即可，所有标准 CRUD 接口已通过接口提供
    // 包括：GET /{id}, POST /_query, POST /, PUT /{id}, DELETE /{id} 等
    //
    // 只有在用户明确要求时才添加自定义接口
}
```

**记住：简洁就是美！让框架为你做事！**

