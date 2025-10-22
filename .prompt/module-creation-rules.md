# JetLinks 模块创建指南

本指南专注于 JetLinks 项目中**模块结构创建**，不涉及具体业务实现。

## 一、模块结构概述

JetLinks 项目采用模块化架构，所有业务模块位于 `modules/` 目录下。根据模块是否需要提供跨服务调用，模块结构分为两种形式：

### 1.1 单模块结构 (除非明确指定,否则使用多模块结构)

适用于不需要跨服务调用的简单业务模块：

```
modules/
└── {模块名}-manager/              # 业务模块目录
    ├── pom.xml                    # Maven配置文件
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── org/jetlinks/pro/{模块名}/
        │   │       ├── entity/        # 实体类目录
        │   │       ├── enums/         # 枚举类目录
        │   │       ├── service/       # 服务类目录
        │   │       ├── web/           # 控制器目录
        │   │       ├── assets/        # 资产类型定义目录(可选)
        │   │       ├── events/        # 事件定义目录(可选)
        │   │       └── configuration/ # 配置类目录(可选)
        │   └── resources/
        │       ├── META-INF/
        │       │   └── spring/
        │       │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
        │       └── i18n/
        │           └── {模块名}-manager/
        │               ├── messages_zh.properties
        │               └── messages_en.properties
        └── test/
            └── java/
                └── org/jetlinks/pro/{模块名}/
                    ├── service/       # 服务测试目录
                    └── web/           # 控制器测试目录
```

### 1.2 多模块结构（跨服务调用场景，默认请使用此场景）

**适用场景**: 当模块需要提供跨服务调用时（默认推荐使用此结构），应创建两个子模块。

**命名规范**：

- 父模块：`{项目简写}-{模块名}`（如 `pms-report`）
- API 模块：`{模块名}-api`（如 `report-api`）
- Manager 模块：`{模块名}-manager`（如 `report-manager`，实现模块）

```
modules/
└── {项目简写}-{模块名}/              # 父模块目录（如 pms-report）
    ├── pom.xml                       # 父模块 POM（聚合子模块）
    │
    ├── {模块名}-api/                 # API 模块（对外接口定义，如 report-api）
    │   ├── pom.xml
    │   └── src/
    │       ├── main/
    │       │   └── java/
    │       │       └── org/jetlinks/pro/{模块名}/api/
    │       │           ├── entity/        # 实体类目录（DTO、VO）
    │       │           ├── enums/         # 枚举类目录
    │       │           ├── command/       # 命令接口目录
    │       │           ├── event/         # 事件定义目录
    │       │           └── constants/     # 常量定义目录
    │       └── test/
    │           └── java/
    │
    └── {模块名}-manager/            # Manager 实现模块（如 report-manager）
        ├── pom.xml
        └── src/
            ├── main/
            │   ├── java/
            │   │   └── org/jetlinks/pro/{模块名}/
            │   │       ├── entity/        # 实体实现类目录
            │   │       ├── service/       # 服务实现类目录
            │   │       ├── web/           # 控制器目录
            │   │       ├── command/       # 命令实现目录
            │   │       ├── assets/        # 资产类型定义目录(可选)
            │   │       └── configuration/ # 配置类目录
            │   └── resources/
            │       ├── META-INF/
            │       │   └── spring/
            │       │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
            │       └── i18n/
            │           └── {模块名}-manager/
            │               ├── messages_zh.properties
            │               └── messages_en.properties
            └── test/
                └── java/
                    └── org/jetlinks/pro/{模块名}/
                        ├── service/       # 服务测试目录
                        └── web/           # 控制器测试目录
```

#### 多模块结构说明：

1. **{模块名}-api 模块**：
   - 仅包含接口定义、实体类（DTO/VO）、枚举、常量等
   - 不包含具体实现逻辑
   - 可被其他服务依赖，用于跨服务调用
   - 依赖项尽可能少，只包含必要的基础依赖

2. **{模块名}-manager 模块**（实现模块）：
   - 包含所有业务逻辑实现
   - 依赖 `{模块名}-api` 模块
   - 包含控制器、服务实现、数据库操作等
   - 包含配置类、资源文件、测试代码等

3. **父模块 `{项目简写}-{模块名}`**：
   - 聚合 api 和 manager 两个子模块
   - 统一管理版本号和公共配置
   - 项目简写如：pms（平台管理系统）、dms（设备管理系统）等

---

## 二、模块创建步骤

⚠️注意:
1. 请严格根据用户输入的信息生成,不要创建额外的内容.
2. 不要创建任何示例实体类等功能,仅创建模块即可.

### 2.1 明确模块信息

在创建模块前，需要明确以下基础信息：

#### 基础信息

- **模块名称**: 如 `report`, `notify`, `workflow`（使用短横线命名，如 `rule-engine`）
- **项目简写**: 如 `pms`（平台管理系统）、`dms`（设备管理系统）等
- **模块描述**: 简要说明模块用途
- **包名**: 通常为 `org.jetlinks.pro.{模块名}`
- **artifactId**:
  - 单模块：`{模块名}-manager`（如 `template-manager`）
  - 多模块：
      - 父模块：`{项目简写}-{模块名}`（如 `pms-report`）
      - API 子模块：`{模块名}-api`（如 `report-api`）
      - Manager 子模块：`{模块名}-manager`（如 `report-manager`）
- **模块结构选择**:
  - **单模块**: 简单业务，不需要跨服务调用
  - **多模块（推荐）**: 需要提供跨服务调用接口，或模块较复杂需要拆分

#### 项目简写示例

- `pms`: Platform Management System（平台管理系统）
- `dms`: Device Management System（设备管理系统）
- `ams`: Asset Management System（资产管理系统）
- `wms`: Workflow Management System（工作流管理系统）

---

### 2.2 选择模块结构

根据业务需求选择合适的模块结构：

#### 使用单模块结构的场景：

- ✅ 简单的 CRUD 业务
- ✅ 不需要被其他服务依赖
- ✅ 模块代码量较小（< 50 个类）
- ✅ 快速原型开发

#### 使用多模块结构的场景（推荐）：

- ✅ 需要提供跨服务调用接口
- ✅ 需要在微服务架构中被其他服务依赖
- ✅ 模块较复杂，需要清晰的接口定义
- ✅ 需要版本化管理 API
- ✅ 需要限制其他服务对内部实现的依赖

> **建议**: 对于新建模块，默认使用多模块结构，即使暂时不需要跨服务调用，这样可以为未来扩展留下空间。

---

### 2.3 创建 pom.xml

根据选择的模块结构，创建对应的 POM 文件。

#### 方式一：单模块 POM（简单场景）

适用于不需要跨服务调用的简单业务模块：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 父项目配置 -->
    <parent>
        <groupId>org.jetlinks.pro</groupId>
        <artifactId>jetlinks-parent</artifactId>
        <version>2.11.0-SNAPSHOT</version>
    </parent>

    <!-- 当前模块的 artifactId -->
    <artifactId>{模块名}-manager</artifactId>

    <dependencies>
        <!-- 通用组件: 包含基础工具类、实体类等 -->
        <dependency>
            <groupId>org.jetlinks.pro</groupId>
            <artifactId>common-component</artifactId>
        </dependency>

        <!-- HSWeb 通用 CRUD -->
        <dependency>
            <groupId>org.hswebframework.web</groupId>
            <artifactId>hsweb-commons-crud</artifactId>
        </dependency>

        <!-- 资产组件: 如需要资产权限控制，添加此依赖 -->
        <dependency>
            <groupId>org.jetlinks.pro</groupId>
            <artifactId>assets-component</artifactId>
            <scope>compile</scope>
        </dependency>

        <!-- 测试组件 -->
        <dependency>
            <groupId>org.jetlinks.pro</groupId>
            <artifactId>test-component</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

#### 方式二：多模块 POM（跨服务调用场景，推荐）

适用于需要提供跨服务调用的模块，将模块拆分为 API 和 Manager 两个子模块。

##### 1. 父模块 POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 父项目配置 -->
    <parent>
        <groupId>org.jetlinks.pro</groupId>
        <artifactId>jetlinks-parent</artifactId>
        <version>2.11.0-SNAPSHOT</version>
    </parent>

    <!-- 父模块 artifactId: {项目简写}-{模块名} -->
    <artifactId>{项目简写}-{模块名}</artifactId>
    <packaging>pom</packaging>
    <name>JetLinks Pro {模块名} Module</name>
    <description>{模块描述}</description>

    <!-- 聚合子模块 -->
    <modules>
        <module>{模块名}-api</module>
        <module>{模块名}-manager</module>
    </modules>
</project>
```

##### 2. API 模块 POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 父模块配置 -->
    <parent>
        <groupId>org.jetlinks.pro</groupId>
        <artifactId>{项目简写}-{模块名}</artifactId>
        <version>2.11.0-SNAPSHOT</version>
    </parent>

    <!-- API 模块 artifactId: {模块名}-api -->
    <artifactId>{模块名}-api</artifactId>
    <name>JetLinks Pro {模块名} API</name>
    <description>{模块描述} - API 接口定义</description>

    <dependencies>

        <!-- 最小化依赖（仅包含必要的基础依赖） -->
        <dependency>
            <groupId>org.jetlinks.pro</groupId>
            <artifactId>common-component</artifactId>
        </dependency>

        <dependency>
            <groupId>org.hswebframework.web</groupId>
            <artifactId>hsweb-commons-crud</artifactId>
        </dependency>

    </dependencies>
</project>
```

##### 3. Manager 实现模块 POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 父模块配置 -->
    <parent>
        <groupId>org.jetlinks.pro</groupId>
        <artifactId>{项目简写}-{模块名}</artifactId>
        <version>2.11.0-SNAPSHOT</version>
    </parent>

    <!-- Manager 模块 artifactId: {模块名}-manager -->
    <artifactId>{模块名}-manager</artifactId>
    <name>JetLinks Pro {模块名} Manager</name>
    <description>{模块描述} - 服务实现</description>

    <dependencies>
        <!-- 依赖本模块的 API -->
        <dependency>
            <groupId>org.jetlinks.pro</groupId>
            <artifactId>{模块名}-api</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- HSWeb 通用 CRUD -->
        <dependency>
            <groupId>org.hswebframework.web</groupId>
            <artifactId>hsweb-commons-crud</artifactId>
        </dependency>

        <!-- 资产组件: 资产权限控制 -->
        <dependency>
            <groupId>org.jetlinks.pro</groupId>
            <artifactId>assets-component</artifactId>
            <scope>compile</scope>
        </dependency>

        <!-- 网关组件: API 网关相关 -->
        <dependency>
            <groupId>org.jetlinks.pro</groupId>
            <artifactId>gateway-component</artifactId>
            <scope>compile</scope>
        </dependency>

        <!-- 测试组件 -->
        <dependency>
            <groupId>org.jetlinks.pro</groupId>
            <artifactId>test-component</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

---

### 2.4 创建目录结构

根据选择的模块结构创建对应的目录。

#### 方式一：单模块目录结构

```bash
# 创建单模块目录结构
mkdir -p modules/{模块名}-manager/src/main/java/org/jetlinks/pro/{模块名}/{entity,service,web,enums,configuration}
mkdir -p modules/{模块名}-manager/src/main/resources/META-INF/spring
mkdir -p modules/{模块名}-manager/src/main/resources/i18n/{模块名}-manager
mkdir -p modules/{模块名}-manager/src/test/java/org/jetlinks/pro/{模块名}/{service,web}
```

#### 方式二：多模块目录结构（推荐）

```bash
# 1. 创建父模块目录
mkdir -p modules/{项目简写}-{模块名}

# 2. 创建 API 模块目录结构
mkdir -p modules/{项目简写}-{模块名}/{模块名}-api/src/main/java/org/jetlinks/pro/{模块名}/api/{entity,enums,command,query,event,constants}
mkdir -p modules/{项目简写}-{模块名}/{模块名}-api/src/test/java/org/jetlinks/pro/{模块名}/api

# 3. 创建 Manager 模块目录结构
mkdir -p modules/{项目简写}-{模块名}/{模块名}-manager/src/main/java/org/jetlinks/pro/{模块名}/{entity,service,web,command,configuration}
mkdir -p modules/{项目简写}-{模块名}/{模块名}-manager/src/main/resources/META-INF/spring
mkdir -p modules/{项目简写}-{模块名}/{模块名}-manager/src/main/resources/i18n/{模块名}-manager
mkdir -p modules/{项目简写}-{模块名}/{模块名}-manager/src/test/java/org/jetlinks/pro/{模块名}/{service,web}
```

---

### 2.5 创建基础配置文件

#### AutoConfiguration.imports

创建文件: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
org.jetlinks.pro.{模块名}.configuration.{模块名首字母大写}ManagerConfiguration
```

#### Configuration 类

创建文件: `src/main/java/org/jetlinks/pro/{模块名}/configuration/{模块名首字母大写}ManagerConfiguration.java`

```java
package org.jetlinks.pro.{模块名}.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;

/**
 * {模块名称}管理模块配置
 *
 * @author {作者}
 * @since {版本}
 */
@Configuration
@EnableEasyormRepository("org.jetlinks.pro.{模块名}.entity")
@Slf4j
public class {模块名首字母大写}ManagerConfiguration {

    @Bean
    public {模块名首字母大写}Properties {模块名}Properties() {
        return new {模块名首字母大写}Properties();
    }
}
```

#### Properties 类

创建文件: `src/main/java/org/jetlinks/pro/{模块名}/configuration/{模块名首字母大写}Properties.java`

```java
package org.jetlinks.pro.{模块名}.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {模块名称}配置属性
 *
 * @author {作者}
 * @since {版本}
 */
@ConfigurationProperties(prefix = "jetlinks.{模块名}")
public class {模块名首字母大写}Properties {
    // 配置属性
}
```

### 2.6 创建国际化文件

#### 中文消息文件

创建: `src/main/resources/i18n/{模块名}-manager/messages_zh.properties`

```properties
# 错误消息
error.{模块名}_not_found={模块名称}数据不存在
error.{模块名}_name_duplicate={模块名称}名称已存在

# 成功消息
success.{模块名}_save={模块名称}保存成功
success.{模块名}_delete={模块名称}删除成功
success.{模块名}_update={模块名称}更新成功

# 验证消息
validation.{模块名}_name_required={模块名称}名称不能为空
validation.{模块名}_id_invalid={模块名称}ID格式无效
```

#### 英文消息文件

创建: `src/main/resources/i18n/{模块名}-manager/messages_en.properties`

```properties
# Error messages
error.{模块名}_not_found={Module name} data not found
error.{模块名}_name_duplicate={Module name} name already exists

# Success messages
success.{模块名}_save={Module name} saved successfully
success.{模块名}_delete={Module name} deleted successfully
success.{模块名}_update={Module name} updated successfully

# Validation messages
validation.{模块名}_name_required={Module name} name is required
validation.{模块名}_id_invalid=Invalid {module name} ID format
```

### 2.7 将模块添加到父 POM

编辑根目录的 `pom.xml`，在 `<modules>` 标签中添加:

**单模块示例**：

```xml
<modules>
    <!-- 其他模块 -->
    <module>modules/{模块名}-manager</module>
</modules>
```

**多模块示例**：

```xml
<modules>
    <!-- 其他模块 -->
    <module>modules/{项目简写}-{模块名}</module>  <!-- 只添加父模块 -->
</modules>
```

> **注意**:
> - 单模块：直接添加模块目录，如 `modules/{模块名}-manager`
> - 多模块：只添加父模块目录，如 `modules/{项目简写}-{模块名}`，子模块（api、manager）由父模块的 POM 自动管理

---

## 三、模块创建检查清单

在创建模块后，使用以下检查清单确保模块结构完整性:

### 3.1 文件结构检查

- [ ] `pom.xml` 已创建，包含必要的依赖
- [ ] `Configuration` 类已创建，启用了 `@EnableEasyormRepository`
- [ ] `AutoConfiguration.imports` 文件已创建
- [ ] `Properties` 类已创建
- [ ] 国际化文件已创建 (messages_zh.properties, messages_en.properties)
- [ ] 目录结构完整 (entity, service, web, enums, configuration)

### 3.2 模块集成检查

- [ ] 模块已添加到父 POM 的 modules 列表
- [ ] 模块可以成功编译 (`mvn clean compile`)
- [ ] 模块可以成功打包 (`mvn clean package`)
- [ ] 模块结构符合 JetLinks 规范

### 3.3 多模块结构检查（如适用）

- [ ] 父模块 POM 正确聚合子模块
- [ ] API 模块依赖最小化
- [ ] Manager 模块正确依赖 API 模块
- [ ] 包名结构正确 (org.jetlinks.pro.{模块名}.api 和 org.jetlinks.pro.{模块名})

---

## 四、模块创建示例

### 4.1 创建单模块示例

**场景**: 创建一个"配置模板"管理模块

```bash
# 1. 创建目录结构
mkdir -p modules/template-manager/src/main/java/org/jetlinks/pro/template/{entity,service,web,enums,configuration}
mkdir -p modules/template-manager/src/main/resources/{META-INF/spring,i18n/template-manager}
mkdir -p modules/template-manager/src/test/java/org/jetlinks/pro/template/{service,web}

# 2. 创建 pom.xml (使用单模块基础模板)
# 3. 创建配置类
# 4. 创建国际化文件
# 5. 添加到父 POM
```

### 4.2 创建多模块示例（推荐）

**场景**: 创建一个"任务调度"管理模块，需要被其他服务调用

**命名示例**：
- 父模块：`pms-scheduler`
- API 模块：`scheduler-api`
- Manager 模块：`scheduler-manager`

```bash
# 1. 创建父模块目录
mkdir -p modules/pms-scheduler

# 2. 创建 API 模块目录结构
mkdir -p modules/pms-scheduler/scheduler-api/src/main/java/org/jetlinks/pro/scheduler/api/{entity,enums,command,query,event,constants}
mkdir -p modules/pms-scheduler/scheduler-api/src/test/java/org/jetlinks/pro/scheduler/api

# 3. 创建 Manager 模块目录结构
mkdir -p modules/pms-scheduler/scheduler-manager/src/main/java/org/jetlinks/pro/scheduler/{entity,service,web,command,configuration}
mkdir -p modules/pms-scheduler/scheduler-manager/src/main/resources/{META-INF/spring,i18n/scheduler-manager}
mkdir -p modules/pms-scheduler/scheduler-manager/src/test/java/org/jetlinks/pro/scheduler/{service,web}

# 4. 创建三个 POM 文件
# 5. 创建配置类
# 6. 创建国际化文件
# 7. 添加到父 POM
```

---

## 五、总结

### 模块创建核心步骤：

1. ✅ **明确模块信息**: 模块名称、项目简写、模块描述
2. ✅ **选择模块结构**: 单模块 vs 多模块（推荐多模块）
3. ✅ **创建 pom.xml**: 根据结构创建对应的 POM 文件
4. ✅ **创建目录结构**: 使用 mkdir 命令创建完整目录
5. ✅ **创建基础配置**: Configuration + Properties + AutoConfiguration.imports
6. ✅ **创建国际化文件**: messages_zh.properties + messages_en.properties
7. ✅ **添加到父 POM**: 在根 POM 中添加模块引用
8. ✅ **验证模块**: 编译、打包测试

### 多模块结构优势：

- ✅ **解耦合**: API 和实现分离，其他服务只依赖接口
- ✅ **可扩展**: 便于版本管理和演进
- ✅ **轻依赖**: API 模块依赖少，不会引入过多传递依赖
- ✅ **跨服务**: 天然支持微服务架构中的服务间调用
- ✅ **职责清晰**: API 定义契约，Manager 提供实现

### 建议：

> 🎯 **新建模块默认使用多模块结构**，即使暂时不需要跨服务调用，这样可以为未来扩展留下空间。

> 📝 **模块创建专注于结构搭建**，具体业务实现请参考其他开发规范文档。

遵循本指南可以快速、规范地创建模块结构，为后续业务开发奠定基础。

