# Claude Code 高级开发规则

## 🔧 Claude Code 工具使用指南

### 1. 文件搜索工具使用策略

```bash
# 使用 Glob 查找文件模式
Glob: "modules/**/*Service.java"           # 查找所有服务类
Glob: "modules/**/*Controller.java"        # 查找所有控制器类
Glob: "modules/**/pom.xml"                 # 查找所有 pom.xml 文件

# 使用 Grep 进行内容搜索
Grep: pattern="GenericReactiveCrudService" glob="modules/**/*.java" output_mode="files_with_matches"
Grep: pattern="@Subscribe" type="java" output_mode="content" -C 3  # 显示上下文
Grep: pattern="CommandSupportManagerProviders" -A 5 -B 5  # 显示前后文
```

### 2. 必需的验证步骤

对于每个生成的类，必须验证：

```bash
# 验证类是否存在
find modules -name "ClassName.java"

# 验证包路径
grep -r "import org.jetlinks" modules/ | head -5

# 验证方法签名
grep -r "public.*Method" modules/
```

### 3. 常用代码模板定位

使用 Task 工具快速找到相关代码：

```markdown
搜索任务：查找现有模块中的 GenericReactiveCrudService 实现示例
搜索任务：查找 @Subscribe 注解的使用方式
搜索任务：查找 CommandSupportManagerProviders 的调用示例
```

## 🎯 场景化处理指南

### 场景1：创建新模块

1. **Task**: 搜索现有模块结构
2. **Read**: 读取 `.prompt/module-creation-rules.md`
3. **Glob**: 查找类似模块的 pom.xml
4. **Read**: 读取示例模块的配置文件
5. **生成**: 基于真实模板创建新模块

### 场景2：添加 CRUD 功能

1. **Grep**: 搜索模块中现有的 Entity/Service/Controller
2. **Read**: 读取 `.prompt/common-crud-rules.md`
3. **验证**: 确认所有使用的注解和类存在
4. **生成**: 创建完整的 CRUD 类

### 场景3：跨服务调用

1. **Grep**: 搜索 `CommandSupportManagerProviders` 使用示例
2. **Read**: 读取 `.prompt/cross-service-call-rules.md`
3. **验证**: 确认命令服务存在
4. **生成**: 创建命令调用代码

### 场景4：实时订阅

1. **Grep**: 搜索 `@Subscribe` 使用示例
2. **Read**: 读取 `.prompt/realtime-subscription-rules.md`
3. **验证**: 确认 Topic 格式正确
4. **生成**: 创建订阅处理代码

## 🔍 代码搜索最佳实践

### 优先级搜索顺序：

1. **当前模块内搜索**：先在目标模块内查找相似实现
2. **相似模块搜索**：在其他模块中查找类似功能
3. **框架源码搜索**：查找 JetLinks 框架提供的示例
4. **文档规则搜索**：最后查阅规则文档

### 搜索技巧：

```bash
# 按功能搜索
grep -r "设备管理" modules/ --include="*.java"
grep -r "device.*service" modules/ -i

# 按注解搜索
grep -r "@Service" modules/ | grep -v test
grep -r "@RestController" modules/

# 按类继承搜索
grep -r "extends.*CrudService" modules/
grep -r "implements.*CommandHandler" modules/
```

## ✅ 代码质量检查清单

生成代码后，逐一检查：

- [ ] 所有导入语句正确
- [ ] 所有注解完整且参数正确
- [ ] 类名和方法名符合命名规范
- [ ] 包名与模块结构一致
- [ ] 逻辑与现有代码风格一致
- [ ] 错误处理方式与项目一致
- [ ] 日志记录方式正确
- [ ] 权限控制注解正确
- [ ] API 文档注解完整

## 🚀 性能优化提醒

1. **响应式编程**：优先使用 Mono/Flux 操作符
2. **分页查询**：避免一次性加载大量数据
3. **缓存策略**：合理使用 Redis 缓存
4. **异步处理**：耗时操作使用异步方式
5. **资源管理**：确保资源正确释放

## 📚 常用速查表

### 命令服务速查：

| 服务类型 | 命令ID | 用途 |
|---------|--------|------|
| 设备服务 | deviceService | 设备信息查询 |
| 产品服务 | productService | 产品信息查询 |
| 告警服务 | alarmService | 告警触发和查询 |
| 通知服务 | notifyService | 发送通知消息 |

### 常用注解速查：

```java
// 实体类
@Table(name = "table_name")
@EnableEntityEvent
@Column(length = 32)

// 服务类
@Service
@Slf4j

// 控制器
@RestController
@RequestMapping("/api/v1/xxx")
@Authorize
@Tag(name = "API名称")
@QueryAction
@Operation(summary = "接口说明")

// 事件处理
@Component
@EventListener

// 命令服务
@Component
@CommandService(id = "serviceId", name = "服务名称")
```

记住：**搜索先行，验证为本，基于实际，生成代码**。