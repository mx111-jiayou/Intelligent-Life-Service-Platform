# 苍穹外卖基础笔记：Spring 对象管理与 Web 请求流程

整理日期：2026-10-19。结合本仓库当前代码及本次学习中的问题整理。示例省略了部分 import 和业务代码，标有“示意”的内容用于说明原理，不代表项目的实际部署配置。

学习时先回答三个问题：这是什么、解决什么问题、在项目哪里用到。

## 1. 先把一次请求串起来

以使用 Nginx 转发的后端请求为例：

```text
浏览器发送请求
  → Nginx 根据配置转发
  → Spring MVC 拦截器检查登录凭证（登录等放行路径除外）
  → Controller 接收参数
  → Service 处理业务
  → Mapper / MyBatis 读写数据库
  → Controller 用 Result 包装结果
  → 消息转换器借助 Jackson 将对象转成 JSON
  → 浏览器收到响应
```

遇到匹配的业务异常时，执行流程会转到异常处理器，组织错误响应。拦截器也可以直接结束请求，例如本项目 JWT 校验失败时返回 HTTP 401。

| 知识点 | 先记住它负责什么 |
| --- | --- |
| Nginx | 提供静态资源、转发请求等 |
| Spring 容器 / Bean | 管理对象及对象之间的依赖 |
| Handler | 对某种处理职责的命名 |
| JWT | 本项目登录后使用的令牌 |
| Properties 类 | 把一组配置项绑定到 Java 对象 |
| 配置类 | 注册组件、设置框架行为 |
| Lombok / `@Builder` | 帮你生成创建对象等重复代码 |
| YApi / Swagger / Knife4j / Scalar | 接口管理、描述、展示与调试相关工具 |
| BeanUtils | 复制 Java 对象的匹配属性 |
| MyBatis 驼峰映射 | 把查询结果列名对应到 Java 属性 |
| Result 泛型 | 表达统一响应中 data 的类型 |
| Jackson / 消息转换器 | 在 HTTP 请求、响应中转换 Java 对象和 JSON |

## 2. Spring 项目中的对象不一定由 Spring 管理

“类写在 Spring 项目里”只代表项目包含这份代码。对象注册到 Spring 容器后，才是由 Spring 管理的 Bean。

```java
Employee employee = new Employee();
```

这个普通对象不会仅因为项目用了 Spring 就自动成为 Bean。

注册到容器的主要用途是：

- 由 Spring 按配置创建和管理对象生命周期。
- 让需要合作的对象通过依赖注入连接起来。
- 让满足相应配置的 Bean 使用事务、切面等框架能力；这些功能有各自的生效条件。

### 2.1 什么时候考虑加 `@Component`

当你写的是一个功能组件，希望 Spring 管理它，并把它注入其他组件或为它注入依赖时，可以考虑使用 `@Component`。

```java
@Component
public class SmsSender {
    public void send(String phone, String message) {
        // 调用短信平台
    }
}

@Service
public class LoginService {
    @Autowired
    private SmsSender smsSender;
}
```

前提是类处于组件扫描范围内。默认情况下，组件通常是容器内的单例，不需要每次业务调用都创建一个。

| 写的是什么类 | 通常怎么处理 |
| --- | --- |
| 通用处理器、短信发送器等功能组件 | 可以使用 `@Component` |
| Service 实现类 | 使用 `@Service`，不必重复加 `@Component` |
| REST Controller | 使用 `@RestController` |
| 全局异常处理器 | 使用 `@RestControllerAdvice` |
| 配置类 | 使用 `@Configuration` |
| 员工 Entity、请求 DTO、返回 VO | 通常按需要创建或由请求转换流程创建，不加 `@Component` |
| 仅通过静态方法调用的工具类 | 通常不需要注册为 Bean |

`@Service` 等注解已经包含组件注册能力，并表达更具体的职责。

即使类上有 `@Component`，另外手动 `new` 出来的对象也不会自动成为 Bean。普通手动创建的对象中的 `@Autowired` 字段，通常不会被 Spring 自动注入。

### 2.2 `@Bean` 是另一种注册方式

```java
@Configuration
public class SomeConfiguration {
    @Bean
    public SomeClient someClient() {
        return new SomeClient();
    }
}
```

在 Spring 处理这个配置类时，`@Bean` 方法返回的对象会注册到容器。它适合需要手动组装对象、尤其是第三方库对象的情况。

因此，不能简单记成“所有 new 出来的对象都不属于 Spring”。关键在于对象有没有通过注册机制交给容器。

## 3. Handler、处理器和全局异常处理器

`handler` 意思是“处理器”，不是 Java 关键字。类名中写 `Handler` 只是表示职责，具体能力由代码实现。

| 处理器 | 处理什么 |
| --- | --- |
| 消息处理器 | 接收、解析、保存消息 |
| 文件上传处理器 | 检查、保存上传文件 |
| 异常处理器 | 记录异常、组织错误响应 |

```java
@Component
public class MessageHandler {
    public void handle(String message) {
        System.out.println(message);
    }
}
```

这里 `Handler` 表达“处理消息”的职责，`@Component` 让对象由 Spring 管理。它们可以同时出现。注册到容器本身不会让 `handle()` 自动执行，仍需要业务代码或框架机制调用。

### 3.1 全局异常处理器为什么有用

多个接口都可能失败。把记录异常、生成统一错误响应的代码集中处理，可以减少重复，并保持响应格式一致。

本项目密码错误时会抛出异常：

```java
throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
```

正常执行流程中断，异常沿调用链向外传播。`PasswordErrorException` 继承 `BaseException`，可匹配下面的处理方法：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex) {
        return Result.error(ex.getMessage());
    }
}
```

- `@RestControllerAdvice`：提供可作用于多个 Controller 的增强，并把结果写入响应体。
- `@ExceptionHandler`：标记异常处理方法；此处由参数类型推断匹配的异常。
- `ex.getMessage()`：读取异常携带的错误说明。
- `Result.error(...)`：组织本项目的错误结果。

“全局”不代表可以捕获所有线程、所有位置的所有异常。项目当前还单独处理 `DataIntegrityViolationException`，JWT 拦截器则在自己的 catch 中处理校验失败。

`Result` 中的业务码 `code: 0` 与 HTTP 状态码是两回事，调用 `Result.error()` 本身不会自动把 HTTP 状态变成 500。

## 4. JWT：登录后的凭证

JWT 是 JSON Web Token。本项目将它用作登录凭证：

```text
提交账号密码 → 后端校验通过 → 生成包含员工 ID、过期时间的 JWT
后续请求带上 JWT → 拦截器验证签名和有效期 → 获取员工 ID → 放行
```

项目使用 HS256 签名，其令牌通常由三段组成：头部、载荷、签名，段之间用点分隔。

签名用于验证内容是否被篡改。这里的载荷通常可以被读取，因此不能把 JWT 理解为“把所有内容加密后别人就看不到”。项目 `JwtUtil` 中的“Token 解密”注释，更准确的说法是“解析并验证签名令牌”。

`JwtProperties` 提供密钥、有效时长、请求头名称等配置。`adminTtl` 在当前工具类中的单位是毫秒；JWT 中的过期声明由库负责生成。

JWT 认证通过也不代表用户可以执行所有操作，具体权限仍需要对应业务规则。

## 5. Properties、配置属性类与配置类

`properties` 在这里表示属性、配置项。它既可能出现在类名中，也可能是配置文件的后缀；配置属性类同样可以绑定 YAML 中的配置。

下面是不包含密钥的简化示例：

```yaml
sky:
  jwt:
    admin-ttl: 7200000
    admin-token-name: token
```

```java
@Component
@ConfigurationProperties(prefix = "sky.jwt")
@Data
public class JwtProperties {
    private long adminTtl;
    private String adminTokenName;
}
```

| 部分 | 作用 |
| --- | --- |
| `@Component` | 在扫描范围内注册这个配置对象 |
| `@ConfigurationProperties` | 把指定前缀的配置绑定到属性 |
| `@Data` | Lombok 生成 getter、setter 等方法 |
| `sky.jwt.admin-ttl` | 绑定到 `adminTtl` |
| `jwtProperties.getAdminTtl()` | 让业务代码读取已绑定的值 |

配置属性类装载“值”，配置类设置“行为”。例如 `JwtProperties` 保存有效时长，而 `WebMvcConfiguration` 设置哪些请求要被拦截。

类名中的 `Properties`、包名中的 `config` 都是命名习惯，生效依赖相应注解和框架机制。

## 6. Lombok 与 `@Builder`

Lombok 是帮助生成重复 Java 代码的库。`@Data`、`@Builder`、`@Slf4j` 都来自 Lombok，`@Component` 则来自 Spring。

`@Builder` 生成构建器相关代码，让你这样创建对象：

```java
EmployeeLoginVO vo = EmployeeLoginVO.builder()
        .id(employee.getId())
        .userName(employee.getUsername())
        .name(employee.getName())
        .token(token)
        .build();
```

`builder()` 获取构建器，中间方法设置待使用的值，`build()` 创建最终对象。就这段赋值效果而言，可以对照下面的方式理解：

```java
EmployeeLoginVO vo = new EmployeeLoginVO();
vo.setId(employee.getId());
vo.setUserName(employee.getUsername());
vo.setName(employee.getName());
vo.setToken(token);
```

这不表示中间方法实际调用了 VO 的 setter，也不表示创建出的 VO 自动成为 Spring Bean。

## 7. Nginx 的 server 与虚拟主机

Nginx 能提供静态文件，也能作为反向代理把请求转发给 Java 后端。

HTTP 配置中的 `server { ... }` 是一组虚拟主机规则。可以把它理解为“一套站点接待规则”：监听哪个地址和端口、匹配哪个主机名、怎样处理访问路径。

下面只是理解概念的配置片段，放在 `http` 配置上下文中：

```nginx
server {
    listen 80;
    server_name shop.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
    }
}

server {
    listen 80;
    server_name docs.example.com;

    location / {
        proxy_pass http://127.0.0.1:8082;
    }
}
```

当两个域名均解析到这台 Nginx，并且后端服务可用时：

```text
访问 shop.example.com → 匹配第一组 server → 转发到 8080
访问 docs.example.com → 匹配第二组 server → 转发到 8082
```

“虚拟”的意思是一个 Nginx 可以用多组规则服务多个逻辑站点。一个 `server` 块不会创建一台虚拟机，也不必对应一个独立 Nginx 进程。

Nginx 会先结合请求到达的地址、端口选择候选配置，再依据 HTTP Host 等信息匹配主机名。主机名不匹配时，通常由该监听地址、端口的默认 server 处理。`server_name` 也不会自动替你创建 DNS 解析。

`location` 进一步决定路径的处理方式，例如返回文件或代理请求。上面的 `proxy_pass` 没有附加 URI，示例意图是保留请求路径，并不是项目里 `/api/` 到 `/admin/` 的路径改写配置。

## 8. YApi、Swagger、Knife4j、Scalar 与文档注解

| 名称 | 用途 |
| --- | --- |
| YApi | 接口管理、团队协作、Mock 和测试 |
| OpenAPI | 用结构化文档描述接口的规范 |
| Swagger | 与接口描述、文档展示等相关的工具体系；课程也常用这个词泛指接口文档方案 |
| Knife4j | 提供增强的接口文档展示、调试等能力 |
| Scalar | 提供基于 OpenAPI 等规范的交互式文档和 API 调试能力 |

此前写到的 `sclalar` 应为 `Scalar`。这里讨论的是接口工具；数学中的 scalar 则表示标量。

本项目使用 Springfox 的 `Docket` 配置和 Swagger 2 注解，由 Knife4j 展示文档。阅读其他项目时可能遇到 OpenAPI 3 的不同注解，不要直接混用。

```java
@Api(tags = "员工相关接口")
public class EmployeeController {
    @ApiOperation("新增员工")
    @PostMapping
    public Result save(@RequestBody EmployeeDTO employeeDTO) {
        // 示例省略业务调用
        return Result.success();
    }
}
```

| 注解 | 影响什么 |
| --- | --- |
| `@Api` | 文档里的接口组说明 |
| `@ApiOperation` | 文档里的接口说明 |
| `@ApiModel` | 文档里的数据模型说明 |
| `@ApiModelProperty` | 文档里的属性说明 |
| `@PostMapping` | 实际请求映射 |
| `@RequestBody` | 从请求体读取数据并转换为参数对象 |

把文档文字“新增员工”改为“添加员工”不会改变业务处理；修改实际请求映射才可能改变接口调用方式。

## 9. WebMvcConfiguration 与静态资源映射

本项目的 `WebMvcConfiguration` 位于 `sky-server` 的 `config` 包，继承 `WebMvcConfigurationSupport`，通过扩展方法设置 MVC 行为。

| 方法 | 当前职责 |
| --- | --- |
| `addInterceptors` | 配置管理端、用户端 JWT 拦截路径与放行路径 |
| `docket1()`、`docket2()` | 配置接口扫描范围和文档分组，返回 `@Bean` 对象 |
| `addResourceHandlers` | 设置文档页面及相关静态资源的访问映射 |
| `extendMessageConverters` | 添加使用自定义 ObjectMapper 的 JSON 消息转换器 |

静态资源通常指 HTML、CSS、JS、图片等文件。映射就是告诉框架“访问这个 URL 时去哪里找文件”。

```java
registry.addResourceHandler("/doc.html")
        .addResourceLocations("classpath:/META-INF/resources/");

registry.addResourceHandler("/webjars/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/");
```

访问 `/doc.html` 时，在类路径的 `META-INF/resources/` 下查找相应资源；`/webjars/**` 用于匹配文档页面需要的脚本、样式等资源路径。

类路径包含项目资源和运行时依赖 JAR 中的资源，所以文件不一定出现在自己编写的源码目录中。

这些是 Spring MVC 的资源规则，与 Nginx 的资源规则分属不同服务。请求到达哪一层、由谁处理，要看代理和应用配置。

## 10. MD5 与 DigestUtils

项目使用 Spring 的 `org.springframework.util.DigestUtils`：

```java
password = DigestUtils.md5DigestAsHex(password.getBytes());
```

流程是：字符串转为字节数组，计算 MD5 摘要，再用十六进制字符串表示结果。相同字节输入会产生相同摘要。

```text
输入密码 → 计算摘要 → 与数据库保存的摘要比较
```

MD5 是摘要算法，不是可以解密还原的加密算法。登录过程没有“把数据库里的 MD5 解密为密码”这一步。

这是静态工具方法，调用它不需要注册 `DigestUtils` Bean。新写代码时通常会显式指定 UTF-8，避免依赖默认字符集。

当前课程代码帮助理解比较流程；实际密码存储不应单独使用 MD5，应使用 BCrypt、Argon2 等专门的密码哈希方案。

## 11. BeanUtils.copyProperties：对象之间复制属性

本项目使用 Spring 的 `org.springframework.beans.BeanUtils`，参数顺序是来源在前、目标在后：

```java
Employee employee = new Employee();
BeanUtils.copyProperties(employeeDTO, employee);
```

它通过属性访问方法，把名称相同、类型兼容的可读属性复制给目标中的可写属性。例如相当于少写一批：

```java
employee.setName(employeeDTO.getName());
employee.setPhone(employeeDTO.getPhone());
```

需要记住：

- `userName` 不会自动对应到 `name`，不同名称要手动处理。
- 创建时间、状态等 DTO 没有的字段，要另外补充。
- 它是浅拷贝，嵌套可变对象通常复制的是引用。
- 来源属性为 null 时，也可能覆盖目标对应属性，并非自动忽略空值。
- 两个对象不需要是 Spring Bean，普通 `new` 出来的对象也能使用。

## 12. LocalDateTime.now() 与当前时间

```java
LocalDateTime now = LocalDateTime.now();
employee.setCreateTime(now);
employee.setUpdateTime(now);
```

`now()` 按运行环境的系统时钟和默认时区获取当前本地日期时间。返回的是时间对象，不是已经格式化好的字符串，而且对象本身不携带时区。

先获取一次 `now`，再赋给两个属性，可以让创建时间与修改时间使用同一个值。

当前项目把部分公共时间字段填充放在 `AutoFillAspect` 中，在匹配的 Mapper 调用前为对象赋值。因此 Service 中早期手动赋值代码已被注释。给对象赋值后，还需要执行数据库操作才会入库。

## 13. 数据库下划线与 Java 驼峰命名

```text
数据库列名       Java 属性
create_time  →  createTime
update_time  →  updateTime
id_number    →  idNumber
```

本项目已设置：

```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true
```

它帮助 MyBatis 在自动映射查询结果时完成对应，不会修改数据库列名，也不会自动改写手写 SQL。

这与配置属性绑定、JSON 格式化是三个不同机制：

| 场景 | 负责的机制 |
| --- | --- |
| `sky.jwt.admin-ttl` → `adminTtl` | Spring Boot 配置属性绑定 |
| 查询结果 `create_time` → `createTime` | MyBatis 结果映射 |
| `createTime` → JSON 中的时间字符串 | Jackson 序列化 |

## 14. Result<T>：统一返回结构与泛型

项目中的核心结构：

```java
public class Result<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;
}
```

`T` 是类型占位符，用来表达 data 装的具体数据类型。

| 写法 | data 类型 |
| --- | --- |
| `Result<String>` | 字符串 |
| `Result<EmployeeLoginVO>` | 登录结果对象 |
| `Result<List<EmployeeVO>>` | 员工展示对象列表；EmployeeVO 为示意类型 |

```java
Result<EmployeeLoginVO> result = Result.success(employeeLoginVO);
```

`Result` 保持 code、msg、data 的结构一致，泛型帮助编译器检查类型。实际 JSON 只包含数据，不会多出一个名为 T 的字段。

项目中 `public static <T> Result<T> success(T object)` 的第一个 `<T>` 声明这个静态方法自己的类型参数，后面的 `Result<T>` 表示返回类型。

## 15. 序列化、消息转换器与 ObjectMapper

这里讨论 JSON 转换：

```text
Java 对象 → JSON：序列化
JSON → Java 对象：反序列化
```

请求体中的 JSON 转为 `@RequestBody EmployeeDTO` 是反序列化；REST Controller 返回的 `Result` 转为 JSON 是序列化。

Java 的 `Serializable` 接口与 Jackson 的 JSON 转换不是同一套机制。对象通常不需要实现 `Serializable` 才能被 Jackson 转为 JSON。

| 角色 | 分工 |
| --- | --- |
| `MappingJackson2HttpMessageConverter` | 在 Spring MVC 中读取、写入 HTTP 请求和响应体 |
| Jackson `ObjectMapper` | 具体执行 Java 对象与 JSON 的转换 |
| 项目的 `JacksonObjectMapper` | 继承 ObjectMapper，设置项目的日期时间等转换规则 |

项目实际配置：

```java
MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter();
converter.setObjectMapper(new JacksonObjectMapper());
converters.add(0, converter);
```

先创建消息转换器，再设置对象映射器，最后放到转换器列表前面。Spring 会结合数据类型、媒体类型等条件选择可用转换器；排第一不代表它处理所有响应类型。

这里也说明：一个对象即使没有独立注册成 Bean，也能被框架组件持有和使用。`new JacksonObjectMapper()` 被设置给转换器，转换器又被添加到 MVC 的转换器列表中。

## 16. 日期格式：@JsonFormat 与统一配置

### 16.1 指定一个属性的格式

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createTime;
```

在当前支持该注解的 Jackson 日期时间处理流程中，可以为这个属性指定 JSON 格式。

### 16.2 为一类时间值配置统一规则

本项目在 `JacksonObjectMapper` 中为 LocalDateTime、LocalDate、LocalTime 注册了序列化器和反序列化器，再由 `WebMvcConfiguration` 把该映射器交给消息转换器。

当前 LocalDateTime 默认格式实际是：

```java
public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
```

采用默认规则时，示例输出为 `2026-10-19 15:30`。没有秒是因为格式中没有 `ss`。当前 Employee 的两个 `@JsonFormat` 已注释，因此通常采用统一规则；其他字段可以单独指定格式。

| 格式片段 | 含义 |
| --- | --- |
| `yyyy-MM-dd` | 年月日 |
| `HH:mm:ss` | 24 小时制时分秒 |
| `MM` | 月份 |
| `mm` | 分钟 |

统一配置适合一致的默认格式，字段注解适合个别例外。这改变 JSON 的表示或解析方式，不会自动改变数据库列类型或数据库中的值。接口接收时间字符串时，也要与实际反序列化规则匹配。

## 17. 对照项目代码复习

下列路径均相对于仓库根目录：

| 文件 | 阅读重点 |
| --- | --- |
| `sky-server/src/main/java/com/sky/handler/GlobalExceptionHandler.java` | 异常如何变成 Result |
| `sky-server/src/main/java/com/sky/controller/admin/EmployeeController.java` | 登录、JWT、Builder、返回值 |
| `sky-server/src/main/java/com/sky/service/impl/EmployeeServiceImpl.java` | 密码摘要、属性复制、业务异常 |
| `sky-server/src/main/java/com/sky/interceptor/JwtTokenAdminInterceptor.java` | 读取令牌、校验、放行或返回 401 |
| `sky-common/src/main/java/com/sky/utils/JwtUtil.java` | JWT 创建与验证 |
| `sky-common/src/main/java/com/sky/properties/JwtProperties.java` | 配置属性绑定 |
| `sky-pojo/src/main/java/com/sky/vo/EmployeeLoginVO.java` | Lombok Builder、文档属性说明 |
| `sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java` | 拦截器、文档、静态资源、消息转换器 |
| `sky-common/src/main/java/com/sky/json/JacksonObjectMapper.java` | 日期时间转换规则 |
| `sky-common/src/main/java/com/sky/result/Result.java` | 统一响应和泛型 |
| `sky-server/src/main/resources/application.yml` | MyBatis 驼峰配置 |
| `sky-server/src/main/java/com/sky/aspect/AutoFillAspect.java` | 公共时间字段填充 |

## 18. 自测与答案

先遮住答案，尝试用自己的话回答。

| 问题 | 答案 |
| --- | --- |
| 所有写在 Spring 项目中的对象都由 Spring 管理吗？ | 不是，要通过注册机制成为 Bean |
| 名字叫 XxxHandler 会自动执行吗？ | 不会，需要业务调用或框架调用机制 |
| 有 `@Component` 的类，手动 new 的对象会自动注入吗？ | 通常不会，那个对象没有因此自动成为 Bean |
| `@Builder` 与 `@Component` 都是 Spring 注解吗？ | 不是，前者来自 Lombok，后者来自 Spring |
| 一个 Nginx server 块就是一台虚拟机吗？ | 不是，它是一组逻辑站点配置 |
| 改 `@ApiOperation` 的说明会修改接口地址吗？ | 不会，地址由请求映射决定 |
| JWT 签名是否隐藏载荷内容？ | 不会，签名主要用于验证完整性和来源 |
| MD5 登录校验需要解密吗？ | 不需要，计算输入摘要后进行比较 |
| `copyProperties(a, b)` 从谁复制给谁？ | Spring BeanUtils 从 a 复制到 b |
| MyBatis 驼峰配置会更改 JSON 日期格式吗？ | 不会，两者是不同机制 |
| `Result<EmployeeLoginVO>` 的 data 是什么？ | EmployeeLoginVO 类型的对象 |
| `@JsonFormat` 会改变数据库的时间值吗？ | 不会，它影响 JSON 转换 |
| 消息转换器和 ObjectMapper 是同一个东西吗？ | 不是，前者接入 HTTP 流程，后者执行具体对象映射 |

## 19. 官方资料

以下资料用于核对概念；在线文档可能对应较新版本，API 用法以本项目依赖版本为准。

- [Nginx 请求处理与虚拟主机](https://nginx.org/en/docs/http/request_processing.html)
- [Spring Controller Advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)
- [Spring Boot 外部配置](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html)
- [Lombok Builder](https://projectlombok.org/features/Builder)
- [JWT 介绍](https://www.jwt.io/introduction)
- [YApi 项目](https://github.com/YMFE/yapi)
- [Knife4j 文档](https://doc.xiaominfo.com/)
- [Scalar API Reference](https://github.com/scalar/scalar/tree/main/packages/api-reference)
- [Spring BeanUtils](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/BeanUtils.html)
- [Spring DigestUtils](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/util/DigestUtils.html)
- [Spring Security 密码存储](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [Java LocalDateTime](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/LocalDateTime.html)
- [MyBatis 配置](https://mybatis.org/mybatis-3/zh_CN/configuration.html)
- [Spring HTTP 消息转换](https://docs.spring.io/spring-framework/reference/web/webmvc/message-converters.html)
