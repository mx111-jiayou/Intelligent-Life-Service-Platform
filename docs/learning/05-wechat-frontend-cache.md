# 苍穹外卖进阶基础：微信登录、前端请求与 Spring 缓存

整理日期：2026-10-19。承接[Spring 对象管理与 Web 请求流程](04-spring-web-basics.md)，结合当前 Java 后端和本地前端练习配置整理。

后端代码中的行为以当前仓库为准。小程序 `app.json`、嵌套路由示例用于讲解通用概念；当前工作区未找到对应的小程序源码、路由配置或 `wendor.js`。本地 `frontend-learning` 练习目录目前未纳入 Git，本笔记只记录核对结果，不依赖其文件才能阅读。

## 1. 先认识两条请求链

管理端页面查询数据时：

```text
浏览器中的页面
  → fetch / Axios 等方式发送异步请求
  → 开发服务器代理或部署后的 Nginx（按实际配置）
  → Java 后端 Controller
  → 业务处理，按需要查缓存、查数据库
  → JSON 响应
  → 页面更新显示
```

微信小程序登录时：

```text
小程序调用 wx.login，取得临时 code
  → 把 code 发给自己的 Java 后端
  → Java 后端通过 HttpClient 请求微信服务器
  → 微信返回身份相关信息
  → 后端取得 openid，查找或创建本地用户
  → 后端签发自己的 JWT
  → 小程序携带 JWT 访问后续业务接口
```

这里的“客户端”是相对的：浏览器是 Java 后端的客户端，Java 后端调用微信时，又是微信接口的客户端。

## 2. WebMvcConfiguration：管理端与用户端文档分组

本项目在一个配置类中创建了两个 `Docket` Bean，分别选择不同包里的接口。

下面摘取关键配置，省略标题、版本等说明：

```java
// docket1() 中：管理端文档
new Docket(DocumentationType.SWAGGER_2)
        .groupName("管理端接口")
        .select()
        .apis(RequestHandlerSelectors.basePackage("com.sky.controller.admin"))
        .paths(PathSelectors.any())
        .build();

// docket2() 中：用户端文档
new Docket(DocumentationType.SWAGGER_2)
        .groupName("用户端接口")
        .select()
        .apis(RequestHandlerSelectors.basePackage("com.sky.controller.user"))
        .paths(PathSelectors.any())
        .build();
```

| 代码 | 作用 |
| --- | --- |
| `@Bean` | 把方法返回的文档配置对象交给 Spring |
| `groupName(...)` | 设置文档分组名称 |
| `basePackage(...)` | 选择纳入这一组文档的 Controller 包 |
| `paths(...)` | 对路径进一步筛选，当前为任意路径 |

这样在 Knife4j 中就可以分别查看用户端、管理端接口，方便不同角色查阅和调试。

这里的“扫描”是文档接口选择，不等同于 Spring 的 `@ComponentScan`。它也不负责修改实际接口地址、拆分部署或授予访问权限。

实际 URL 由 `@RequestMapping`、`@GetMapping` 等决定；能否访问由拦截器、认证和权限规则决定。例如本项目另行配置了 `/admin/**` 和 `/user/**` 的 JWT 拦截规则。

## 3. HttpClient：Java 后端主动请求其他服务

Controller 接收别人发来的 HTTP 请求，HttpClient 让自己的 Java 程序主动发出 HTTP 请求。

本项目 `HttpClientUtil` 使用 Apache HttpClient 4.x，关键包名是 `org.apache.http...`。不要和 JDK 的 `java.net.http.HttpClient` 或其他版本的库混为一谈。

微信登录中的实际调用是：

```java
String json = HttpClientUtil.doGet(WECHAT_LOGIN, map);
```

含义是：向微信登录接口发送 GET 请求，将 map 中的内容作为请求参数，取得响应文本。

这个工具类中还有不同发送方式：

| 方法 | 请求数据放在哪里 |
| --- | --- |
| `doGet(url, map)` | 拼到 URL 查询参数中 |
| `doPost(url, map)` | 作为表单请求体 |
| `doPost4Json(url, map)` | 作为 JSON 请求体 |

它不是 Spring MVC 的消息转换器。前者负责向外发请求；后者负责当前应用的 HTTP 请求体、响应体转换。

当前 `doGet` 只在 HTTP 状态为 200 时读取结果正文。HTTP 200 也可能携带业务错误信息，因此使用外部接口时还要判断响应内容。现有方法在异常、资源关闭等方面属于较简化的实现，不能把示例理解为已覆盖所有失败情况。

## 4. JSONObject：让 Java 方便地读取 JSON 字段

先区分三样东西：

| 名称 | 是什么 |
| --- | --- |
| JSON | 表达数据的文本格式 |
| JSON 字符串 | Java 中保存这段文本的 String |
| JSONObject | 某个 JSON 库提供的 Java 对象，可按字段名取值 |

本项目微信登录中使用的是 Fastjson 的 `com.alibaba.fastjson.JSONObject`：

```java
String json = "{\"openid\":\"demo-openid\"}";
JSONObject object = JSON.parseObject(json);
String openid = object.getString("openid");
```

逐行理解：先拿到文本，再解析成可操作的 JSON 对象，最后读取 openid 字段。

```text
JSON.parseObject(...)：JSON 文本 → Java 对象，反序列化
JSON.toJSONString(...)：Java 对象 → JSON 文本，序列化
```

不同库中有名称相近的类型，例如 Gson 的 `JsonObject`。判断用法时先看 import，不要只看类名。

当前项目对外响应主要通过 Jackson 转换，微信响应又使用 Fastjson 手动解析。这是两处不同的处理流程，并不要求整个项目只能出现一种 JSON 库。

## 5. 微信登录：appid、secret、code、openid、JWT

### 5.1 每个值分别代表什么

| 值 | 含义 | 本项目中的用途 |
| --- | --- | --- |
| `appid` | 小程序的应用标识 | 告诉微信是哪个小程序发起登录交换 |
| `secret` | 小程序的应用密钥 | 后端调用微信登录交换接口时使用 |
| `code` | `wx.login` 获取的临时登录凭证 | 小程序交给后端，后端向微信交换身份信息 |
| `openid` | 用户在相应小程序下的身份标识 | 在本地用户表中查找用户 |
| `session_key` | 微信会话密钥 | 微信登录接口可能返回；当前登录业务未使用它生成 JWT |
| 项目的 JWT | 自己的后端签发的业务登录令牌 | 后续访问苍穹外卖接口时携带 |

`code` 有效期有限，不能当作长期令牌反复使用；需要重新登录时应重新获取。`openid` 不等于本地用户表主键，也不能简单当作所有小程序共用的同一个用户编号。

`secret` 放在后端配置中，不写进小程序 `app.json` 或浏览器打包文件。appid 与 secret 的用途和保密要求不同。

### 5.2 本项目实际流程

用户端登录接口由以下映射组合成：

```text
类上的 @RequestMapping("/user/user")
方法上的 @PostMapping("/login")
完整接口：POST /user/user/login
```

请求体示意：

```json
{
  "code": "wx.login取得的临时凭证"
}
```

`UserServiceImpl` 会构造这些参数：

```java
Map<String, String> map = new HashMap<>();
map.put("appid", wxChatProperties.getAppid());
map.put("secret", wxChatProperties.getSecret());
map.put("js_code", code);
map.put("grant_type", "authorization_code");

String json = HttpClientUtil.doGet(WECHAT_LOGIN, map);
JSONObject jsonObject = JSON.parseObject(json);
String openid = jsonObject.getString("openid");
```

注意：前端提交的字段叫 `code`，向微信发送的参数叫 `js_code`，这里是代码手动建立对应关系。

后续逻辑：

1. 没有取得 openid，抛出登录失败异常。
2. 根据 openid 查询本地用户。
3. 没查到，创建一个新用户并保存到数据库。
4. Controller 取本地用户 ID，生成 JWT。
5. 返回包含 id、openid、token 的 `UserLoginVO`。

当前代码主要检查 openid 是否为空，未完整区分微信响应中的各种业务错误；网络或 JSON 解析失败也可能发生在这个检查之前。

### 5.3 小程序端的调用示意

以下代码只帮助理解先后关系，省略了完整错误处理：

```javascript
wx.login({
  success(res) {
    if (!res.code) return;
    wx.request({
      url: 'https://api.example.com/user/user/login',
      method: 'POST',
      data: { code: res.code },
      success(response) {
        if (response.statusCode === 200 && response.data.code === 1) {
          wx.setStorageSync('token', response.data.data.token);
        }
      }
    });
  }
});
```

示意域名需要替换为实际服务地址，并满足小程序平台的请求域名等配置要求。

这里有两个含义不同的 code：`res.code` 是微信临时凭证，`response.data.code` 是本项目 Result 的业务结果码。当前项目用 1 表示成功。

## 6. app.json、package.json 和其他配置文件

配置文件就是把某些规则或参数交给对应的工具读取。不同文件被不同的工具读取，不是只要叫“配置文件”就能互换。

| 文件或类 | 谁读取 | 典型内容 |
| --- | --- | --- |
| 原生微信小程序 `app.json` | 微信小程序框架 | 页面列表、窗口样式、底部导航等 |
| 前端 `package.json` | npm 和相关构建工具 | 依赖、脚本、项目元信息 |
| `vite.config.js` | Vite | 开发端口、代理、插件等 |
| `vue.config.js` | Vue CLI | 开发服务器、代理、打包选项等 |
| `application.yml` | Spring Boot | 后端端口、数据库、Redis、业务配置等 |
| `WeChatProperties` | Spring Boot 配置绑定机制 | 接收 `sky.wechat` 下的配置值 |
| `WebMvcConfiguration` | Spring 配置机制 | 设置 MVC 相关组件及行为 |

原生小程序 `app.json` 示意：

```json
{
  "pages": [
    "pages/index/index",
    "pages/orders/orders"
  ],
  "window": {
    "navigationBarTitleText": "苍穹外卖"
  }
}
```

这个示例配置了页面和导航栏标题，并没有配置后端 URL。后端地址通常在请求封装或环境配置中定义；微信开发者工具的项目配置又可能包含 appid 等信息，应按文件职责查找。

小程序 `app.js` 是 JavaScript 入口逻辑，`app.json` 是全局配置；网页里的 `App.vue` 则是另一套 Vue 应用中的根组件。

## 7. AJAX：页面不整体刷新也能请求数据

AJAX 通常指网页通过 JavaScript 异步获取数据，再更新部分页面的做法。虽然名字包含 XML，实际项目常传输 JSON。

例如点击“查询员工”，页面可以保持菜单和布局不变，只更新表格数据。

浏览器中的请求示意：

```javascript
async function loadEmployees(token) {
  const response = await fetch('/api/employee/page?page=1&pageSize=10', {
    headers: { token }
  });
  if (!response.ok) throw new Error('HTTP 请求失败');
  const result = await response.json();
  if (result.code !== 1) throw new Error(result.msg || '查询失败');
  return result.data;
}
```

使用相应代理配置时，请求才会到达正确的后端地址。`await` 等待异步结果，不代表刷新整个页面。

| 名称 | 所在位置及用途 |
| --- | --- |
| AJAX | 浏览器异步请求并更新页面的常见做法 |
| fetch | 浏览器提供的 HTTP 请求 API |
| Axios | 可在浏览器、Node.js 等环境使用的 HTTP 客户端库 |
| `wx.request` | 微信小程序提供的请求 API |
| Apache HttpClient | Java 后端发起 HTTP 请求的库 |

小程序的 `wx.request` 与浏览器 AJAX 都能请求后端，但运行环境与平台限制不同，不能把浏览器 CORS 规则直接等同于小程序的请求域名规则。

## 8. wendor.js 可能是 vendor.js

当前未找到名为 `wendor.js` 的文件。它可能指课程中的 `vendor.js`、`vendors.js` 或 `chunk-vendors...js`，下面按常见打包产物解释。

vendor 常用来表示第三方依赖代码。构建工具可以把 Vue、请求库等公共依赖放进独立文件，便于复用和缓存，但实际内容取决于打包配置。

如果浏览器网络面板显示请求由 `vendor.js` 发起，可能只是业务代码调用的请求库被打包进了这个文件，不能据此认定后端地址一定写在 vendor 文件里。

排查后端请求时，优先看业务接口文件、请求封装、baseURL、环境变量和代理配置。打包文件通常应由源码重新构建生成；文件名本身不是“请求后端”的固定配置开关。

## 9. npm run serve、npm run dev 与 package.json

`npm run xxx` 表示执行当前项目 `package.json` 的 `scripts.xxx`。命令叫什么取决于脚本配置。

Vue CLI 项目中的常见示例：

```json
{
  "scripts": {
    "serve": "vue-cli-service serve",
    "build": "vue-cli-service build"
  }
}
```

这时 `npm run serve` 就会执行 `vue-cli-service serve`。

你本地的 `frontend-learning/05-components` 练习实际使用：

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build"
  }
}
```

所以这里的开发启动命令是 `npm run dev`，没有配置 serve 脚本就不能直接套用 `npm run serve`。

这些命令要在相应前端项目目录执行，并且依赖已安装。开发服务器提供页面、热更新和代理等能力，默认不会替你启动 Java 后端。`npm run build` 在上述配置中负责生成部署用文件。

`dev` 常是 development 的缩写，但要看位置：

- `scripts.dev` 是脚本名称。
- `devDependencies` 是开发、构建等工具依赖声明。
- `devServer` 是某些工具的开发服务器配置。
- Spring 的 `application-dev.yml` 是名为 dev 的后端环境配置。

它们名称相近，但不会自动变成同一套配置。

## 10. 开发代理：前端地址如何转到后端地址

本地练习的 Vite 配置包含：

```javascript
server: {
  host: '127.0.0.1',
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://127.0.0.1:18082',
      rewrite: path => path.replace(/^\/api/, '/admin')
    },
    '/user': {
      target: 'http://127.0.0.1:18082'
    }
  }
}
```

对管理端接口，路径变化为：

```text
页面发出 /api/employee/page?page=1&pageSize=10
  → 浏览器请求 http://127.0.0.1:5173/api/employee/page?...
  → Vite 转发并改写前缀
  → 后端收到 http://127.0.0.1:18082/admin/employee/page?...
```

`target` 决定目标服务，`rewrite` 决定路径如何改写。用户端 `/user` 在这个配置中只转发，不改前缀。

浏览器请求的是页面同源的开发服务器，开发服务器再从服务端转发到后端。这有助于处理开发期间的跨源请求，而不是关闭浏览器安全机制。

开发代理通常只在开发服务器运行时生效。部署后需要另行设置 Nginx 等反向代理或后端 CORS；Vite 的开发代理配置不会自动写进打包后的页面并继续充当服务器。

前端代理、Nginx 代理和 Spring AOP 代理都叫“代理”，但职责不同：前两者转发 HTTP 请求，Spring AOP 代理在方法调用前后执行缓存、事务等逻辑。

## 11. 嵌套路由：在固定布局内部切换页面

后台管理页面常保留左侧菜单和顶部栏，只切换右侧内容。Vue Router 可以通过父子路由表达这种关系。

下面是通用示例，假设相应组件已导入：

```javascript
const routes = [
  {
    path: '/manage',
    component: Layout,
    children: [
      { path: 'employee', component: EmployeePage },
      { path: 'dish', component: DishPage }
    ]
  }
];
```

Layout 组件中留出子页面位置：

```vue
<template>
  <div>
    <aside>菜单</aside>
    <main><router-view /></main>
  </div>
</template>
```

访问 `/manage/employee`，在 Layout 的 `<router-view>` 显示员工页面；访问 `/manage/dish`，显示菜品页面。外层应用也要有路由出口来显示 Layout。

`children` 声明子路由，`router-view` 提供渲染位置。示例子路径不以 `/` 开头，所以拼接父路径；以 `/` 开头时按根路径匹配，不能再简单相加。

前端路由决定显示哪个页面，后端路由决定执行哪个接口方法。访问员工页面后，页面还需要单独请求员工数据，并非“切换路由就自动查询数据库”。

## 12. Spring 缓存框架：少写重复的缓存代码

缓存是保存可复用的查询结果，让之后的相同查询尽量少重复访问数据库。Spring Cache 提供统一的缓存调用方式和注解，具体保存在哪里由缓存实现及 CacheManager 决定。

Redis 是一种可以作为缓存存储的服务；Spring Cache 是缓存抽象；RedisTemplate 是直接操作 Redis 的工具。三者分工不同。

### 12.1 常见注解

| 注解 | 作用 |
| --- | --- |
| `@EnableCaching` | 启用注解驱动缓存支持 |
| `@Cacheable` | 查缓存；命中时通常跳过方法体，未命中则执行并缓存返回结果 |
| `@CachePut` | 执行方法，并用结果更新缓存 |
| `@CacheEvict` | 删除指定缓存项或缓存区域中的条目 |

注解不需要你在每个查询方法里重复编写“读缓存、未命中再查库、保存结果”的流程，但需要正确的配置与调用方式。

### 12.2 本项目的套餐缓存

启动类 `SkyApplication` 已标注 `@EnableCaching`。用户端套餐查询方法是：

```java
@GetMapping("/list")
@Cacheable(value = "setmealCache", key = "#categoryId")
public Result<List<Setmeal>> list(Long categoryId) {
    Setmeal setmeal = new Setmeal();
    setmeal.setCategoryId(categoryId);
    setmeal.setStatus(StatusConstant.ENABLE);
    List<Setmeal> list = setmealService.list(setmeal);
    return Result.success(list);
}
```

`setmealCache` 是缓存名称，`#categoryId` 是 SpEL 表达式，表示使用参数 categoryId 的值作为 key。缓存的是整个方法返回的 `Result<List<Setmeal>>`，并非只缓存其中的列表。

以 categoryId 为 10 为例：

```text
第一次：缓存未命中 → 执行查询 → 缓存返回结果 → 返回
下一次：相同缓存名和 key 命中 → 直接取得缓存结果 → 返回
```

即使缓存命中，请求通常仍要先经过配置的登录拦截器；跳过的是被缓存方法的方法体，不是整个 HTTP 处理流程。

### 12.3 数据变动后为什么清缓存

管理端新增套餐时，项目清除对应分类的缓存：

```java
@CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")
```

修改、删除、起售停售等操作中，项目使用：

```java
@CacheEvict(cacheNames = "setmealCache", allEntries = true)
```

这表示清除 setmealCache 这个缓存区域中的所有条目，不是清空所有 Redis 数据。默认在方法正常完成后执行清理。

如果数据库已经改了、旧缓存却还在，用户可能持续看到旧套餐数据。失效策略就是为了让后续请求重新查询最新数据。

### 12.4 生效条件与当前配置边界

- 在默认代理模式下，需要调用经过 Spring 代理的 Bean 方法；普通手动 new 对象、同一个类中直接自调用等情况，通常不会触发缓存拦截。
- 需要可用的缓存管理器。项目同时引入了 cache 与 data-redis starter；实际使用哪一个 CacheManager，应结合运行环境和自动配置结果确认，本次未启动应用验证。
- 当前源码中没有找到自行声明的 CacheManager，也没有看到这里为套餐缓存指定过期时间，不能假设所有缓存都自动保存 30 分钟。
- `RedisTemplate` 上设置的序列化器，不等于自动设置了 RedisCacheManager 的序列化规则。
- key 必须反映影响结果的条件。用户专属数据不能只按公共分类编号缓存给所有用户。

## 13. 配置和数据如何串起来

| 你要排查的问题 | 从哪里开始看 |
| --- | --- |
| 管理端与用户端文档混在一起 | 两个 Docket 的 groupName 与包选择 |
| 小程序登录失败 | 前端 code、后端微信配置、微信响应、本地用户处理 |
| JSON 里某字段取不到 | 原始响应结构、字段名、JSON 库 import |
| 不知道用 serve 还是 dev | 当前目录 package.json 的 scripts |
| 浏览器请求地址与后端不一样 | 请求封装、开发代理、Nginx 路径改写 |
| 看不到子页面 | children 配置与父组件 router-view |
| 更新套餐后页面还是旧数据 | 缓存名、key 和清理逻辑 |
| 缓存注解没生效 | EnableCaching、Bean 代理、调用方式、CacheManager |

## 14. 对照后端源码

下列路径相对于仓库根目录，均已核对存在：

| 文件 | 阅读重点 |
| --- | --- |
| `sky-server/src/main/java/com/sky/config/WebMvcConfiguration.java` | 文档分组、接口包选择、拦截规则 |
| `sky-common/src/main/java/com/sky/utils/HttpClientUtil.java` | GET、表单 POST、JSON POST |
| `sky-common/src/main/java/com/sky/properties/WeChatProperties.java` | 微信配置属性 |
| `sky-server/src/main/java/com/sky/controller/user/UserController.java` | 接收 code、返回业务 JWT |
| `sky-server/src/main/java/com/sky/service/impl/UserServiceImpl.java` | 交换 openid、查找或创建用户 |
| `sky-pojo/src/main/java/com/sky/dto/UserLoginDTO.java` | 前端提交的 code |
| `sky-server/src/main/java/com/sky/controller/user/SetmealController.java` | Cacheable 查询缓存 |
| `sky-server/src/main/java/com/sky/controller/admin/SetmealController.java` | CacheEvict 清理缓存 |
| `sky-server/src/main/java/com/sky/SkyApplication.java` | EnableCaching 开关 |
| `sky-server/src/main/java/com/sky/config/RedisConfiguration.java` | RedisTemplate 配置 |

## 15. 自测

| 问题 | 答案 |
| --- | --- |
| 文档分组是否能阻止用户访问管理接口？ | 不能，需要实际访问控制 |
| Java 后端能不能当 HTTP 客户端？ | 能，调用微信接口就是例子 |
| JSONObject 是 JSON 文件吗？ | 不是，是 JSON 库提供的 Java 对象 |
| code 和 JWT 能互换吗？ | 不能，前者用于登录交换，后者用于本项目后续认证 |
| appid 是用户 ID 吗？ | 不是，是小程序的应用标识 |
| secret 应放在 app.json 里吗？ | 不应，应用密钥用于后端调用 |
| npm run dev 是否固定等于 vite？ | 不固定，要看 scripts.dev 的定义 |
| 开发代理会自动随静态文件部署吗？ | 不会，部署环境要配置自己的转发方式 |
| vendor.js 是否一定存放后端地址？ | 不一定，要看源码和构建配置 |
| 嵌套路由切换是否自动查数据库？ | 不会，页面需要发起相应数据请求 |
| Cacheable 命中会执行被注解的方法体吗？ | 通常不会，但请求其他处理环节仍会运行 |
| allEntries=true 是否清空整个 Redis？ | 不是，只针对指定缓存区域 |

## 16. 参考资料

以下官方资料用于核对通用机制，具体版本以项目依赖为准：

- [Apache HttpClient 4.5 快速入门](https://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html)
- [Fastjson 入门](https://github.com/alibaba/fastjson/wiki/Quick-Start-CN)
- [MDN：AJAX](https://developer.mozilla.org/en-US/docs/Glossary/AJAX)
- [npm run-script](https://docs.npmjs.com/cli/v10/commands/npm-run-script/)
- [Vue CLI 配置与开发代理](https://cli.vuejs.org/config/#devserver-proxy)
- [Vite 开发服务器配置](https://vite.dev/config/server-options.html)
- [Vue Router 嵌套路由](https://router.vuejs.org/guide/essentials/nested-routes.html)
- [webpack 代码拆分](https://webpack.js.org/guides/code-splitting/)
- [Spring 缓存注解](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)

微信官方文档入口如下。本次工具未能读取这些页面，微信调用部分结合本地实现说明，未据此确认具体有效时长、配额等平台细则；实际接入时以官方当前说明为准。

- [小程序登录流程](https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/login.html)
- [wx.login](https://developers.weixin.qq.com/miniprogram/dev/api/open-api/login/wx.login.html)
- [小程序配置](https://developers.weixin.qq.com/miniprogram/dev/framework/config.html)
