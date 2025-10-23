# JetLinks 注解和导入语句参考手册

## 🔴 重要提醒

在生成代码时，必须使用正确的注解和包名。请严格参考本文档和现有项目代码。

---

## 一、实体类注解

### 1.1 JPA 注解（使用 javax.persistence）

```java
import javax.persistence.Column;
import javax.persistence.Table;
```

#### @Table
- **包名**: `javax.persistence.Table`
- **用途**: 指定数据库表名
- **示例**:
  ```java
  @Table(name = "cloud_patrol_category_point")
  public class CategoryPointEntity extends GenericEntity<String> {
  }
  ```

#### @Column
- **包名**: `javax.persistence.Column`
- **用途**: 标记字段为数据库列
- **常用属性**:
  - `length`: 字段长度
  - `nullable`: 是否允许为空
  - `updatable`: 是否可更新
- **示例**:
  ```java
  @Column(length = 100, nullable = false)
  private String categoryName;
  ```

---

### 1.2 HSWebFramework 注解

```java
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.GeneratedValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
```

#### @EnableEntityEvent
- **包名**: `org.hswebframework.web.crud.annotation.EnableEntityEvent`
- **用途**: 启用实体事件支持
- **示例**:
  ```java
  @EnableEntityEvent
  public class CategoryPointEntity extends GenericEntity<String> {
  }
  ```

#### @Comment
- **包名**: `org.hswebframework.ezorm.rdb.mapping.annotation.Comment`
- **用途**: 添加表或字段注释
- **示例**:
  ```java
  @Table(name = "cloud_patrol_category_point")
  @Comment("分类点位表")
  public class CategoryPointEntity {
  }
  ```

#### @ColumnType
- **包名**: `org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType`
- **用途**: 指定 JDBC 类型和 Java 类型
- **示例**:
  ```java
  // 枚举字段
  @ColumnType(javaType = String.class)
  @EnumCodec
  private StatusEnum status;

  // 大文本字段
  @ColumnType(jdbcType = JDBCType.CLOB)
  private String content;

  // 多选枚举（位掩码）
  @ColumnType(javaType = Long.class, jdbcType = JDBCType.BIGINT)
  @EnumCodec(toMask = true)
  private StatusEnum[] statuses;
  ```

#### @JsonCodec
- **包名**: `org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec`
- **用途**: 自动将 Java 对象序列化为 JSON 存储
- **示例**:
  ```java
  @Column
  @JsonCodec
  @ColumnType(jdbcType = JDBCType.CLOB)
  private Map<String, Object> configuration;
  ```

#### @EnumCodec
- **包名**: `org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec`
- **用途**: 枚举类型编解码
- **示例**:
  ```java
  // 单选枚举
  @EnumCodec
  @ColumnType(javaType = String.class)
  private StatusEnum status;

  // 多选枚举（位掩码）
  @EnumCodec(toMask = true)
  @ColumnType(javaType = Long.class, jdbcType = JDBCType.BIGINT)
  private StatusEnum[] statuses;
  ```

#### @DefaultValue
- **包名**: `org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue`
- **用途**: 设置字段默认值
- **示例**:
  ```java
  @Column
  @DefaultValue(generator = Generators.CURRENT_TIME)
  private Long createTime;
  ```

#### @GeneratedValue
- **包名**: `org.hswebframework.ezorm.rdb.mapping.annotation.GeneratedValue`
- **用途**: 配置 ID 生成策略
- **示例**:
  ```java
  @Column(length = 64)
  @GeneratedValue(generator = Generators.SNOW_FLAKE)
  private String id;
  ```

---

### 1.3 Swagger 注解

```java
import io.swagger.v3.oas.annotations.media.Schema;
```

#### @Schema
- **包名**: `io.swagger.v3.oas.annotations.media.Schema`
- **用途**: 提供 API 文档描述
- **常用属性**:
  - `description`: 字段描述
  - `accessMode`: 访问模式（READ_ONLY, READ_WRITE 等）
- **示例**:
  ```java
  @Schema(description = "分类名称")
  private String categoryName;

  @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
  private Date createTime;
  ```

---

### 1.4 实体类接口

```java
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
```

#### 常用接口
- `GenericEntity<String>`: 基础实体接口，提供 ID 字段
- `GenericTreeSortSupportEntity<String>`: 树形实体接口，提供父节点和排序字段
- `RecordCreationEntity`: 记录创建人接口，提供 creatorId、creatorName、createTime 字段
- `RecordModifierEntity`: 记录修改人接口，提供 modifierId、modifierName、modifyTime 字段

---

## 二、工具类

### 2.1 FastBeanCopier

```java
import org.hswebframework.web.bean.FastBeanCopier;
```

#### 用途
- 高性能的对象属性拷贝工具
- 基于字节码生成，性能接近手写代码
- 支持 Supplier 方式创建目标对象

#### 示例
```java
public class CategoryPointEntity extends GenericEntity<String> {

    /**
     * 转换为基础信息VO
     */
    public CategoryPointInfo toInfo() {
        return FastBeanCopier.copy(this, CategoryPointInfo::new);
    }

    /**
     * 转换为详细信息VO
     */
    public CategoryPointDetail toDetail() {
        CategoryPointDetail detail = FastBeanCopier.copy(this, CategoryPointDetail::new);
        // 处理特殊字段
        detail.setStatusText(this.status != null ? this.status.getText() : null);
        return detail;
    }
}
```

---

## 三、服务层

### 3.1 响应式服务

```java
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
```

#### 基础服务
```java
@Service
@Slf4j
public class CategoryPointService extends GenericReactiveCrudService<CategoryPointEntity, String> {
    // 继承提供所有 CRUD 功能
}
```

#### 树形服务
```java
@Service
@Slf4j
public class CategoryService extends GenericReactiveTreeSupportCrudService<CategoryEntity, String> {
    // 继承提供树形结构支持
}
```

---

### 3.2 阻塞式服务

```java
import org.hswebframework.web.crud.service.GenericCrudService;
```

#### 基础服务
```java
@Service
@Slf4j
public class CategoryPointService extends GenericCrudService<CategoryPointEntity, String> {
    // 继承提供所有 CRUD 功能
}
```

---

## 四、控制器层

### 4.1 响应式控制器

```java
import org.jetlinks.pro.assets.crud.AssetsHolderCrudController;
import org.jetlinks.pro.assets.crud.CorrelatesAssetsHolderCrudController;
import org.jetlinks.pro.assets.annotation.AssetsController;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.authorization.annotation.DeleteAction;
```

#### 基础控制器
```java
@RestController
@RequestMapping("/category-point")
@AllArgsConstructor
@Getter
@Slf4j
@Resource(id = "category-point", name = "分类点位管理")
@Tag(name = "分类点位管理")
@AssetsController(type = "cloud-patrol")
@Authorize
public class CategoryPointController implements AssetsHolderCrudController<CategoryPointEntity, String> {

    private final CategoryPointService service;

    // 自动提供标准 CRUD 接口
}
```

---

### 4.2 阻塞式控制器

```java
import org.jetlinks.pro.assets.crud.BlockingAssetsHolderCrudController;
import org.jetlinks.pro.assets.crud.BlockingCorrelatesAssetsHolderCrudController;
```

#### 基础控制器
```java
@RestController
@RequestMapping("/category-point")
@AllArgsConstructor
@Getter
@Slf4j
@Resource(id = "category-point", name = "分类点位管理")
@Tag(name = "分类点位管理")
@AssetsController(type = "cloud-patrol")
@Authorize
public class CategoryPointController implements BlockingAssetsHolderCrudController<CategoryPointEntity, String> {

    private final CategoryPointService service;

    // 自动提供标准 CRUD 接口
}
```

---

## 五、常见错误

### ❌ 错误示例

```java
// 错误1：使用错误的 @Table 注解
import org.hswebframework.ezorm.rdb.mapping.annotation.Table; // ❌ 错误

// 错误2：使用错误的 @Column 注解
import org.hswebframework.ezorm.rdb.mapping.annotation.Column; // ❌ 错误

// 错误3：使用错误的 FastBeanCopier 包名
import org.jetlinks.pro.fastjson.FastBeanCopier; // ❌ 错误
```

### ✅ 正确示例

```java
// 正确1：使用 JPA 的 @Table 注解
import javax.persistence.Table; // ✅ 正确

// 正确2：使用 JPA 的 @Column 注解
import javax.persistence.Column; // ✅ 正确

// 正确3：使用正确的 FastBeanCopier 包名
import org.hswebframework.web.bean.FastBeanCopier; // ✅ 正确
```

---

## 六、完整实体类示例

```java
package org.jetlinks.pro.cloud.patrol.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.GeneratedValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.jetlinks.pro.cloud.patrol.api.entity.CategoryPointInfo;

import javax.persistence.Column;
import javax.persistence.Table;
import java.util.Date;

/**
 * 分类点位实体
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cloud_patrol_category_point")
@Comment("分类点位表")
@EnableEntityEvent
public class CategoryPointEntity extends GenericEntity<String>
    implements RecordCreationEntity, RecordModifierEntity {

    @Column(length = 1, nullable = false)
    @Schema(description = "指定类型1:全员 2:指定人")
    private Integer type;

    @Column(length = 100, nullable = false)
    @Schema(description = "分类名称")
    private String categoryName;

    /**
     * 转换为基础信息VO
     */
    public CategoryPointInfo toInfo() {
        return FastBeanCopier.copy(this, CategoryPointInfo::new);
    }
}
```

---

## 七、快速检查清单

在生成实体类代码前，请确认：

- [ ] `@Table` 和 `@Column` 使用的是 `javax.persistence` 包
- [ ] `@EnableEntityEvent` 使用的是 `org.hswebframework.web.crud.annotation` 包
- [ ] `@ColumnType`、`@JsonCodec`、`@EnumCodec` 等使用的是 `org.hswebframework.ezorm.rdb.mapping.annotation` 包
- [ ] `FastBeanCopier` 使用的是 `org.hswebframework.web.bean` 包
- [ ] 实体类继承 `GenericEntity<String>`
- [ ] 实体类实现 `RecordCreationEntity` 和 `RecordModifierEntity` 接口
- [ ] 添加了 `toInfo()` 和 `toDetail()` 转换方法

---

## 八、AI 代码生成建议

### 生成代码前的步骤

1. **搜索现有代码**
   ```bash
   grep -r "import javax.persistence.Table" modules/
   grep -r "import org.hswebframework.web.bean.FastBeanCopier" modules/
   ```

2. **验证包名和注解**
   - 查看项目中其他实体类的导入语句
   - 确认使用的是 JPA 注解还是其他注解

3. **参考现有实现**
   - 找到相似的实体类作为参考
   - 保持代码风格一致

4. **生成代码后验证**
   - 检查所有导入语句是否正确
   - 确认注解使用是否正确
   - 验证转换方法是否使用了正确的 FastBeanCopier

---

## 总结

- ✅ **@Table 和 @Column**: 使用 JPA 的 `javax.persistence` 包
- ✅ **@EnableEntityEvent**: 使用 HSWeb 的 `org.hswebframework.web.crud.annotation` 包
- ✅ **FastBeanCopier**: 使用 `org.hswebframework.web.bean.FastBeanCopier`
- ✅ **始终参考现有代码**：搜索项目中的实际用例，确保使用正确的包名和注解

遵循本文档可以避免常见的导入错误，生成规范的代码！

