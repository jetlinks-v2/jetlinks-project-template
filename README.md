# JetLinks 项目脚手架

`2.20` 分支用于基于 JetLinks 平台的二次开发：Java 17、JetLinks `2.20.0-SNAPSHOT`、Spring Boot `3.4.13`。平台依赖统一由根 `pom.xml` 的 `jetlinks.pro.version` 和 `jetlinks-parent` BOM 管理，应用自己的版本与平台依赖版本分别维护。

> [!WARNING]
> **生产环境必须更换默认 JWT 密钥对。** 模板为兼容开发与演示环境保留了已公开的默认密钥，不能用于生产。部署时须注入独立密钥对，并同步所有验签服务的公钥，详见 [JWT 密钥与生产环境](#jwt-密钥与生产环境)。

## 模块说明

```text
jetlinks-project-template
├── bootstrap                         平台单体启动参考
├── runtime-ui                        当前项目前端工作区
│   ├── jetlinks-web-core             平台前端壳与共享能力
│   └── modules                       标准 UI、SaaS 扩展及独立业务 UI
├── microservices                     平台及独立应用的微服务启动模块
│   ├── api-gateway-service           API 网关
│   ├── authentication-service        认证服务
│   ├── demo-service                  独立应用示例启动服务
│   ├── file-service                  文件服务
│   ├── iot-service                   物联网服务
│   ├── notify-service                通知服务
│   ├── rule-engine-service           规则引擎服务
│   ├── visualization-service         可视化服务
│   └── ui-service                    前端资源服务
└── modules                           应用业务模块
    └── examples                      CRUD、命令、规则引擎等示例
        ├── mvc-service               独立 MVC 服务启动示例
        └── demo-manager              独立应用业务示例
```

`bootstrap` 和已有平台微服务用于了解平台装配方式。二开应用的业务实现放在 `modules/`，独立启动服务可在 `microservices/` 下建立自己的目录，装配所需业务模块；不向既有平台服务的内部业务添加实现。

## 使用

### 创建项目

在 [GitHub 模板创建页](https://github.com/jetlinks-v2/jetlinks-project-template/generate) 创建仓库时，确保包含 `2.20` 分支；也可以直接克隆指定分支：

```bash
git clone --branch 2.20 --single-branch --recurse-submodules -o template git@github.com:jetlinks-v2/jetlinks-project-template.git {新的项目名称}
cd {新的项目名称}
git remote add origin {项目远程仓库地址}
```

在 Gitee 中创建仓库时，选择相应版本的 `jetlinks-project-template` 模板。

### 初始化与开发前端

当前项目在 `runtime-ui/` 安装依赖、启动和开发前端，复用其中已有的前端壳、认证、设备、规则引擎、边缘网关及 SaaS 扩展。依赖、脚本与环境配置以该工作区为准：

```bash
# 首次克隆后，在项目根目录按锁定提交初始化标准前端子模块
git submodule update --init --recursive

# Node.js >= 22.18.0，pnpm >= 10
cd runtime-ui
pnpm install
pnpm dev

# 开发时指定实际后端网关
pnpm dev:proxy http://localhost:8844

# 生产构建
pnpm build

# 仅构建指定业务模块，将 my-business-ui 替换为实际模块名称
pnpm build:modules my-business-ui
```

前端环境配置位于 `runtime-ui/.env*`，开发端口由 `VITE_PORT` 决定；按目标项目配置 `VITE_APP_PROJECT_CODE`、`VITE_APP_DEV_PROXY_URL` 和 `VITE_PUBLIC_PATH`。`VITE_APP_NAME=iot` 用于存储命名及默认系统菜单查询，不替换为本地工程名；进入业务应用后，平台按应用实例作用域查询 `owner=app` 菜单。构建产物位于 `runtime-ui/dist/`，可独立部署。前端运行环境与后端 Maven 的 `saas.enabled` 依赖开关分别配置。

新增业务页面放在 `runtime-ui/modules/` 的独立模块，通过 `index.ts`、`baseMenu.ts` 等平台入口接入；实际需要公共扩展时再声明 `register.ts`，复用现有路由、菜单、权限和公共组件。

`.gitmodules` 直接登记 `runtime-ui/jetlinks-web-core` 及认证、设备、边缘网关、规则引擎标准 UI 子模块，跟踪 `2.20` 并锁定提交；SaaS 扩展与示例 UI 由当前工作区维护。初始化使用锁定提交，不通过 `git submodule update --remote` 推进全部上游提交。

### 配置 Maven 私服

在 Maven `settings.xml` 中配置平台私服凭据。Snapshot 依赖需要对应的仓库下载权限：

```xml
<servers>
    <server>
        <id>jetlinks</id>
        <username>{用户名}</username>
        <password>{密码}</password>
    </server>
</servers>
```

### 构建与 profile

```bash
# 默认构建平台单体启动参考，SaaS 默认启用
mvn -U clean compile

# 构建平台微服务，SaaS 同样默认启用
mvn -U -Pmicroservice clean compile

# 构建独立 MVC 服务及其依赖示例
mvn -U -Pexample -pl modules/examples/mvc-service -am clean compile

# 构建并验证独立应用示例
mvn -Pdemo -pl microservices/demo-service -am verify

# 显式关闭 SaaS；可与 standalone、microservice、example 等 profile 组合
mvn -Pmicroservice -Dsaas.enabled=false clean compile

# Elasticsearch 默认使用 7.x；切换为 8.x 时使用统一选择属性
mvn -Pmicroservice -Delasticsearch.major=8 clean compile
```

`standalone` 是根 POM 的默认 profile。显式启用 `example` 或其他根 profile 时，如需同时构建单体模块，应使用 `-Pstandalone,example`。

独立应用示例由 `demo` profile 选择，聚合 `modules/examples/demo-manager/` 业务模块与 `microservices/demo-service/` 启动服务；测试前端位于 `runtime-ui/modules/jetlinks-demo-ui/`。示例通过现有应用中心、模板和应用菜单接入，操作步骤见 [新增应用本地接入 SaaS 测试环境操作指南](docs/新增应用本地接入SaaS测试环境操作指南.pdf)。

SaaS 项目需先在 cloud 端订阅包含示例菜单的服务并同步到 runtime，再配置应用模板、实例与角色。应用角色授权不能代替项目的服务菜单订阅。

按示例说明注入数据库、Redis、集群及验签配置后，可在仓库根目录使用 `java -jar microservices/demo-service/target/application.jar` 运行构建产物；服务加入既有集群，前端通过现有网关访问 `/demo/record`。

Elasticsearch 选择使用 `elasticsearch.major` 属性，缺省为 7.x，值为 `8` 时选择 8.x。这样启用 SaaS 或数据库 profile 时不会意外关闭默认存储依赖。若手动使用存储 profile，应显式排除另一项，例如 `-Pmicroservice,es8x,!es7x`，避免同时引入两个版本。

`runtime-ui` 是当前项目的前端源码开发入口。现有 `bootstrap` 的 `ui` profile 和 `ui-service` 保留为 Maven 静态资源装配参考；这些资源独立发布，当前使用已发布的 `2.12.0-SNAPSHOT` 前端壳、数据源及可视化资源，其余模块为 `2.11.0-SNAPSHOT`，通过根 POM 的 `jetlinks.ui.*` 属性维护，不将其替换为尚未发布的 `2.20.0-SNAPSHOT`。

`2.20` 的 OpenAI 智能体提供者使用 `ai-agent-openai`，旧坐标 `ai-openai` 不再使用。

### JWT 密钥与生产环境

> [!WARNING]
> **保留默认密钥只为兼容现有开发环境，不代表生产环境可使用。** 未配置或传入空值时，Compose 仍回退到原有默认密钥；启用 `prod` profile 也不会自动生成或更换密钥。生产部署必须显式覆盖下面两个参数。

| 参数 | 含义与配置范围 |
| --- | --- |
| `JWT_PRIMARY_KEY` | 用于签发 JWT 的私钥，保持原有变量名；仅注入认证服务，使用 PKCS#8 DER 的单行 Base64 内容 |
| `JWT_PUBLIC_KEY` | 用于校验 JWT 的配套公钥，使用 X.509/SPKI DER 的单行 Base64 内容；同步到所有 JWT 验签服务 |

每个部署环境使用独立的 RSA 密钥对，生产密钥长度至少为 2048 bit。可在仓库外的受限目录执行以下命令：

```bash
umask 077
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
openssl pkcs8 -topk8 -nocrypt -in jwt-private.pem -outform DER | openssl base64 -A > jwt-private.base64
openssl pkey -in jwt-private.pem -pubout -outform DER | openssl base64 -A > jwt-public.base64
```

将两个 `.base64` 文件的完整内容分别作为对应参数的值，不传入文件路径或带 PEM 头尾的多行文本。真实私钥及包含私钥的环境文件不提交到仓库，通过部署环境或 Secret 管理。

使用 [微服务 Compose](microservices/docker-compose.yml) 时，可在宿主机环境中设置这两个变量，或使用仓库外的受限环境文件，例如 `/secure/jetlinks/jwt.env`：

```dotenv
# 将占位内容替换为上面生成的单行 Base64 值
JWT_PRIMARY_KEY=<私钥文件的完整内容>
JWT_PUBLIC_KEY=<公钥文件的完整内容>
```

在项目根目录通过 `docker compose --env-file /secure/jetlinks/jwt.env -f microservices/docker-compose.yml up -d` 加载。Compose 会将公钥同步到公共服务环境，将私钥仅注入认证服务；直接运行微服务时，在相应进程的环境中注入同名变量。宿主机环境中的同名变量优先于 `--env-file`，部署时应检查实际来源，避免沿用其他环境的密钥。

密钥更换会影响已签发 JWT 的校验，应协调认证服务与全部验签服务切换，并安排用户重新登录。JWT 密钥与 SaaS agent 的 `access-key` 分别配置，不能互相替代。

### SaaS 接入

相关启动模块通过 `saas` profile 引入 SaaS/租户运行时依赖，缺省启用；使用 `-Dsaas.enabled=false` 或 `-P'!saas'` 关闭。该开关控制构建依赖，部署时仍需按目标平台配置服务发现、认证、数据库及 SaaS 运行环境。

| 启动模块 | SaaS/租户依赖 |
| --- | --- |
| `bootstrap` | 组合下列认证、设备、业务及租户运行时能力 |
| `authentication-service` | `saas-runtime-auth`、`saas-runtime-agent`、`tenant-domain` |
| `iot-service` | `saas-runtime-device`、`tenant-device` |
| `file-service`、`mvc-service` | `saas-runtime-component` |
| `notify-service` | `tenant-notify` |
| `rule-engine-service` | `tenant-rule-engine` |
| `visualization-service` | `tenant-visualization` |

新增业务微服务时，在自己的启动 POM 中按同样方式接入公共 SaaS 运行时；不要引入运营控制端的 `saas-manager`：

```xml
<profile>
    <id>saas</id>
    <activation>
        <property>
            <name>saas.enabled</name>
            <value>!false</value>
        </property>
    </activation>
    <dependencies>
        <dependency>
            <groupId>org.jetlinks.pro</groupId>
            <artifactId>saas-runtime-component</artifactId>
            <version>${jetlinks.pro.version}</version>
        </dependency>
    </dependencies>
</profile>
```

属性缺省激活可以与其他 profile 组合。不要仅使用 `activeByDefault` 实现 SaaS 默认启用，否则同一 POM 激活其他 profile 时可能将其关闭。关闭 SaaS 也不代表关闭应用的功能权限和资产数据权限。

接入运营中心还需要在认证服务配置 `jetlinks.saas.agent.server-uri`、`access-id` 和 `access-key`；单体装配时配置在实际引入 agent 的启动进程中。配置示例、运行时凭据来源、RSocket/TCP 端口和接入验证见 [SaaS agent 接入](docs/development-best-practices.md#接入-saas-的-agent-配置)。

## 开发边界

### 1. 应用作为独立微服务运行

业务模块采用 Maven 模块化管理，由应用自己的启动微服务装配并独立部署。复用平台公开的 API、命令契约及基础组件，通过平台服务发现和命令执行能力访问标准模块。

不要为了扩展标准模块的字段而直接引入 `device-manager` 等实现模块，也不要继承或修改标准设备实体、注入标准模块内部 Service，或直接读写其数据库表。现有 `bootstrap`、`iot-service` 等平台装配参考中的标准模块依赖，不是二开业务服务的依赖清单。

需要扩展设备等标准对象的业务信息时，在应用自己的表中保存对象 ID 和业务字段，通过中间表建立关联。标准对象的信息查询和操作通过公开命令执行，并保留当前身份、应用和权限上下文。例如：业务设备扩展表保存 `deviceId` 与业务字段，设备名称、状态等通过设备公开查询命令获取；不依赖跨服务数据库联表，也不在标准设备表上增加业务列。

### 2. 遵守平台统一的数据权限规范

业务实体继承 `org.hswebframework.web.api.crud.entity.ExtendableEntity`，并使用 `org.jetlinks.pro.assets.annotation.Asset` 声明资产类型。例如：

```java
import org.hswebframework.web.api.crud.entity.ExtendableEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.jetlinks.pro.assets.annotation.Asset;

import javax.persistence.Table;

@Table(name = "biz_record")
@EnableEntityEvent
@Asset(type = "business-record")
public class BusinessRecordEntity extends ExtendableEntity<String> {
}
```

`business-record` 是示例标识；应用需定义并注册自己的 `AssetType`、资产提供方及权限动作，确保实体、接口和命令使用同一资产类型。关联资产应按真实归属声明资产 ID 字段，不能机械地把每张关联表都视为独立资产。

实体声明与接口保护共同组成数据权限边界：标准 CRUD 优先复用 `AssetsHolderCrudController` / `BlockingAssetsHolderCrudController` 及 `@AssetsController`；关联资产使用对应的关联资产控制器。自定义查询、导出、批量操作和命令入口使用平台 `AssetsHolder` 能力注入查询范围或校验操作权限，由实际暴露入口负责校验。

不要手写租户、部门、创建人等通用过滤规则来替代平台数据权限。`@Asset` 声明不能替代接口检查，功能权限通过也不代表拥有对应资产的数据权限。已有框架入口完成校验后，内部 Service 不重复维护同一套权限判断。

### 3. 菜单与权限声明使用平台统一规范

前端业务模块遵守平台的模块、菜单与路由注册约定：通过模块 `index.ts` 声明页面路由，使用 `baseMenu.ts`（可读取 `baseMenu.json`）提供菜单，公共扩展通过平台模块注册机制接入；不在独立页面中维护另一套路由、菜单或权限状态。

菜单 code、页面路由、模块及应用归属需保持一致，菜单名称、按钮名称遵守平台国际化约定。页面和按钮使用平台统一权限组件与能力，权限声明使用平台的 `permissions` 结构：

```json
{
  "permissions": [
    {
      "permission": "business-record",
      "actions": ["query", "save", "delete"]
    }
  ]
}
```

`permission` 必须与后端 `@Resource` 的资源标识一致，`actions` 必须与后端实际声明的查询、保存、删除等操作一致；每个按钮只声明所需动作。后端仍须执行功能权限和资产数据权限检查，不能从菜单名称、URL 或按钮显隐推断授权，也不能用前端控制代替后端鉴权。

## 多租户与多种部署模式的最佳实践

完整指南见 [二次开发与多租户最佳实践](docs/development-best-practices.md)。私有化可以使用多租户，单体也可以接入 SaaS；业务代码通过公共契约与独立租户适配模块兼容这些组合，不为每种部署模式分别实现一套业务。

| 场景 | 实践与文档入口 |
| --- | --- |
| 选择私有化、普通多租户或 SaaS 装配 | [部署组合与模块职责](docs/development-best-practices.md#部署组合)：区分业务、租户适配与运营中心，明确 Maven profile 的作用 |
| 为标准对象扩展业务字段 | [关联表与查询边界](docs/development-best-practices.md#业务字段扩展)：自有表保存关联与业务字段，公开命令获取标准数据，QueryHelper 做本地查询和批量分页结果组合 |
| 接入统一数据权限 | [业务模块统一数据权限手册](https://github.com/jetlinks-v2/jetlinks-components/blob/2.20/assets-component/docs/assets-permission-guide.md)：资产、关联资产、自定义接口、命令及私有化/租户范围兼容 |
| 扩展自定义 metric、配额与能力 | [独立租户适配模块与指标接入](docs/development-best-practices.md#租户指标扩展)、[平台租户开发文档](https://github.com/jetlinks-v2/jetlinks-tenant/blob/2.20/README.md)、[tenant-ai-agent 实例](https://github.com/jetlinks-v2/jetlinks-tenant/blob/2.20/tenant-ai-agent/README.md) |
| 认证服务接入 SaaS | [agent 配置、凭据来源与接入验收](docs/development-best-practices.md#接入-saas-的-agent-配置)：微服务在认证端配置，单体在实际装配进程配置 |
| 微服务、后台任务及多副本 | [上下文与后台任务](docs/development-best-practices.md#上下文与后台任务)：统一身份传播、资源归属、共享限额与幂等计量 |
| 租户开通、初始化和数据清理 | [租户数据生命周期](docs/development-best-practices.md#租户数据生命周期)：复用 TenantDataProvider / TenantDataManager，不跨服务直连数据库 |
| 验证同一业务的多模式兼容性 | [兼容性验收](docs/development-best-practices.md#兼容性验收)：按实际改动覆盖私有化、租户隔离、单体/微服务、多副本及 SaaS 接入 |

## AI 开发入口

[AGENTS.md](AGENTS.md) 统一维护本仓库的开发边界和协作规则。Codex 使用该入口；Claude Code 通过 [CLAUDE.md](CLAUDE.md) 导入；Cursor 通过 `.cursor/rules/develop.mdc` 引用；[ai-prompt.md](ai-prompt.md) 仅保留兼容说明。

JetLinks 通用方法统一使用 [jetlinks-develop-skills](https://github.com/jetlinks/jetlinks-develop-skills)，不在仓库复制维护 `.prompt/` 规则库或本地 JetLinks skills。先使用已安装的 focused skill；任务边界不清楚时使用 `jetlinks-router`。未安装时，可在支持该能力的 Agent 中执行：

```text
Use $skill-installer to install the required JetLinks skills from https://github.com/jetlinks/jetlinks-develop-skills, starting with jetlinks-router.
```
