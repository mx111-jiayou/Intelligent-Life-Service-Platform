# 苍穹外卖：按黑马顺序学习 AI 辅助全栈开发

首次整理：2026-10-18；最近更新：2026-10-19。

这份笔记整理本次聊天已经讲过的内容，重点保留容易混淆的概念。示例中的员工、分类编号和记录数量是教学数据，不代表当前数据库中的实际内容。

学习目标：看懂数据流和业务规则，能够向 AI 说明需求、阅读关键代码、验证结果。框架源码和反射语法暂时不要求背诵。

## 复习导航与当前进度

| 章节 | 复习内容 |
| --- | --- |
| 1 | 项目结构、登录、接口和数据对象 |
| 2 | 分页参数、SQL、PageHelper、前端翻页 |
| 3 | MyBatis、Mapper、XML 和参数绑定 |
| 4 | 动态 SQL |
| 5 | 启用、禁用员工 |
| 6 | 编辑员工：查询、回显、提交修改 |
| 7 | 分类、套餐、表关联和删除检查 |
| 8 | 公共字段自动填充、BaseContext、AOP |
| 9 | 图片上传：文件、OSS、图片地址与菜品保存 |
| 10 | 新增菜品：菜品与口味分表保存 |
| 11 | 菜品分页查询：LEFT JOIN、分类名称与 DishVO |
| 12 | 批量删除菜品：状态检查、套餐关联、口味清理与事务 |
| 13 | 编辑菜品：回显口味、替换口味列表与事务边界 |
| 14 | Day 4 套餐管理：套餐、菜品与关联表 |
| 15 | Day 5 店铺营业状态与 Redis 入门 |
| 16 | 使用 AI 开发时的验收方法 |
| 17 | 自测题与答案 |

当前进度：2026-10-19，Day 5已讲店铺状态键值读写；本节补充15.7、15.8，区分 Redis服务、Java中的RedisTemplate工具、SHOP_STATUS键，并理解链式调用、Spring创建与注入模板、类型转换及连接失败的定位。下一小节可继续Redis数据类型或过期时间，缓存菜品按后续课程展开。尚未运行Redis命令或修改真实状态。

Day 1 的结构和登录已讲解，但环境搭建没有逐项实操；Day 2 已讲分页、状态修改、编辑与分类删除，新增员工的完整流程尚未单独展开。不要把课程顺序误认为所有章节都已完成。

学习约定：每学完一个小节就更新这份笔记，记录核心概念、项目示例、追问过的易混点和当前进度。以重新讲清楚后的版本为准，不仅记录最初讲解。

同步约定：用户已要求每次更新学习笔记后，都提交并推送到 GitHub 当前学习分支，无需重复询问是否推送。提交范围限于本次笔记更新，不夹带无关代码改动；推送成功后告知结果。若推送失败，明确说明原因和笔记已保存到哪一步，不把本地提交说成远程同步成功。保留远程更新，不强制覆盖远程历史。

## 1. 项目和登录：快速复习

### 1.1 一次请求的路径

```text
前端发送请求
  → 代理转发（按运行配置）
  → Controller 接收参数
  → Service 执行业务规则
  → Mapper / MyBatis 操作数据库
  → 结果返回前端
```

| 部分 | 职责 |
| --- | --- |
| 前端 | 展示数据、收集输入、发送请求、显示结果 |
| Controller | 接收请求，调用业务方法，组织响应 |
| Service | 判断业务条件、组织处理步骤 |
| Mapper | 声明数据库操作方法 |
| MyBatis | 根据 SQL 配置和参数协调执行，转换查询结果 |
| MySQL | 保存数据、执行 SQL |

后端模块：`sky-common` 放公共工具、异常和返回对象；`sky-pojo` 放 DTO、Entity、VO；`sky-server` 放 Controller、Service、Mapper、配置等。

### 1.2 DTO、Entity、VO

| 名称 | 用途 | 登录中的例子 |
| --- | --- | --- |
| DTO | 接收本次操作的输入 | 用户名、密码 |
| Entity | 表示业务实体，通常对应数据库记录 | 员工记录 |
| VO | 组织返回给前端的数据 | 员工信息、token |

数据库保存的字段，不代表都应该返回前端。接收成功也不等于参数合法，必填、长度、允许值等需要另外校验。

### 1.3 登录流程

后端接口：`POST /admin/employee/login`。已有前端练习请求 `/api/employee/login`，通过代理配置对应到后端。

```text
提交账号密码
  → 根据用户名查询员工
  → 检查账号是否存在、密码是否匹配、账号是否禁用
  → 通过后生成 token
  → 前端后续请求携带 token
```

`@RequestMapping("/admin/employee")` 和 `@PostMapping("/login")` 共同确定接口路径。

项目响应使用 `{ code, msg, data }`，其中 `code=1` 表示业务成功。HTTP 状态码与这里的 `code` 不同：收到 HTTP 200，也要检查业务结果。

## 2. 分页查询：重点复习

### 2.1 四个变量

假设符合条件的员工共有 23 名，每页最多显示 10 名。

| 变量 | 含义 | 第 3 页的值 |
| --- | --- | --- |
| `page` | 查询第几页 | 3 |
| `pageSize` | 每页最多多少条 | 10 |
| `total` | 符合条件的总记录数 | 23 |
| `records` | 当前页的记录列表 | 最后 3 名员工 |

前端提供 `page`、`pageSize` 和搜索条件；后端返回 `total`、`records`。

```text
第1页：排序后的第1～10条
第2页：排序后的第11～20条
第3页：排序后的第21～23条
```

“第 11 条”是排序后的位置，不是员工 ID 为 11。`pageSize=10` 不保证每页都有 10 条。

### 2.2 LIMIT 和偏移量

```text
offset = (page - 1) × pageSize
总页数 = total ÷ pageSize，向上取整
```

| 查询页码 | 跳过条数 | SQL 写法 |
| --- | --- | --- |
| 1 | 0 | `LIMIT 0, 10` |
| 2 | 10 | `LIMIT 10, 10` |
| 3 | 20 | `LIMIT 20, 10` |

```sql
SELECT *
FROM employee
ORDER BY create_time DESC
LIMIT 10, 10;
```

含义：按创建时间从新到旧排列，跳过前 10 条，最多取 10 条。

### 2.3 为什么还要查总数

本页返回 10 条，不能据此知道总共有多少条，因此分页通常需要统计总数和查询本页两项工作。

```sql
-- 统计符合条件的总数
SELECT COUNT(*) FROM employee WHERE name LIKE '%张%';

-- 查询符合条件的数据中的第2页
SELECT * FROM employee
WHERE name LIKE '%张%'
ORDER BY create_time DESC
LIMIT 10, 10;
```

以上为教学 SQL，实际项目通过参数绑定提供姓名。总数查询与列表查询必须使用一致的筛选条件。

### 2.4 PageHelper 是什么

PageHelper 是分页插件库，帮助处理分页 SQL、总数统计和分页结果。它不保存业务数据，也不需要先把全部员工查到 Java 内存再截取。

下面的代码在 **EmployeeServiceImpl 的 pageQuery 方法里**：

```java
PageHelper.startPage(
    employeePageQueryDTO.getPage(),
    employeePageQueryDTO.getPageSize()
);

Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

long total = page.getTotal();
List<Employee> records = page.getResult();
return new PageResult(total, records);
```

逐句理解：

1. `startPage` 设置这次分页要求，此时还没有查询员工。
2. 调用 Mapper 触发查询，插件在执行过程中处理分页。
3. `getTotal()` 取出符合条件的总数。
4. `getResult()` 取出当前页数据。
5. `new PageResult(...)` 包装接口结果，不会再查询数据库。

这三个名字要区分：

| 名称 | 含义 |
| --- | --- |
| `PageHelper` | 提供分页能力的工具及其配套插件机制 |
| `Page<Employee>` | 插件提供的分页结果类型，包含员工数据和分页信息 |
| `PageResult` | 项目自定义的结果类，装 `total`、`records` |

`Page<Employee> page` 中的 `page` 是结果对象，不是数字页码。它也可以改名为 `employeePageResult`，不影响功能。

### 2.5 XML 没写 LIMIT，为什么仍然分页

```text
startPage 设置页码和每页条数
  → Mapper 提供基础 SQL
  → PageHelper 在查询过程中处理统计与分页
  → MySQL 执行 SQL
  → 返回分页结果
```

在当前这种调用方式下，`startPage` 应紧挨目标 MyBatis 查询，分页作用于同一线程中紧随其后的第一个查询。不要在两者之间插入无关查询，也不要误以为一次设置会让之后所有查询永久分页。正常执行完成后插件会清理分页设置。[PageHelper 官方说明](https://pagehelper.github.io/docs/howtouse/)

仅把带有 `page`、`pageSize` 的 DTO 传给 Mapper，并不意味着当前实现自动完成分页；这里依靠前面的 `startPage`。

### 2.6 返回前端的结构

Controller 用 `Result.success(pageResult)` 再包装一层，得到下面的结构示意：

```text
Result
├─ code：1
├─ msg：null
└─ data：PageResult
   ├─ total：23
   └─ records：当前页员工列表
```

前端读取：

```js
employees.value = result.data.records;
employeeTotal.value = result.data.total;
```

表格使用 `records`；总页数通过 `Math.ceil(total / pageSize)` 计算。

### 2.7 翻页与搜索

```text
点击第2页
  → 页码改为2
  → 重新发送请求
  → 用新 records 替换表格数据
```

改变 Vue 页码变量本身，不会自动决定调用接口，需要翻页事件或监听逻辑触发查询。

| 操作 | 参数变化 |
| --- | --- |
| 翻页 | 改变 page，保留搜索条件 |
| 搜索新姓名 | 改变 name，通常把 page 重置为 1 |
| 修改每页条数 | 改变 pageSize，通常把 page 重置为 1 |
| 清空搜索 | 清空 name，把 page 重置为 1 |

例如搜索“张”后翻到第 2 页，仍应发送 `page=2&pageSize=10&name=张`。漏掉姓名条件就变成查询全部员工。

如果新搜索只有 7 条结果，沿用旧的第 3 页会要求跳过 20 条，可能显示空列表，因此搜索时应重置页码。

截至整理时，已有 `LoginPanel.vue` 练习的请求固定为 `page=1&pageSize=10`，没有完整翻页交互；聊天中的动态页码代码是教学示例，不是已经完成的改动。

### 2.8 需要验收的边界

- 页码从 1 开始，每页条数应有合理上限。当前分页 DTO 的 `int` 字段没有显式业务默认值或参数校验，不能假定缺省就是 1 和 10。
- 没有匹配记录是成功的空结果：`total=0`、`records=[]`。
- 当前 XML 仅按创建时间倒序；可增加唯一 ID 作为同时间记录的排序依据，例如 `ORDER BY create_time DESC, id DESC`。这能明确相同时间下的顺序，但不解决并发数据变化导致的翻页偏移。
- 快速连续搜索时，较早请求可能较晚返回。可取消旧请求或只接受最新请求结果；仅加防抖不保证消除已发出请求的竞态。
- 检查接口字段是否过多，不能仅因为表格不展示密码字段，就认为响应没有返回它。分页直接返回实体，需要核对实际响应和序列化配置。

## 3. MyBatis、Mapper、XML：重点答疑

### 3.1 MyBatis 不是 XML 文件

| 名称 | 是什么 |
| --- | --- |
| MyBatis | 项目引入的数据库访问框架 |
| EmployeeMapper.java | Java 接口，声明查询和修改方法 |
| EmployeeMapper.xml | 写 SQL 和映射配置的文件 |
| MySQL | 真正保存数据、执行 SQL 的数据库软件 |

XML 提供配置和 SQL，MyBatis 读取并处理它们。XML 文件本身不会执行查询。

SQL 也可以写在注解里：

```java
@Select("select * from employee where username = #{username}")
Employee getByUsername(String username);
```

这里仍然使用 MyBatis，只是 SQL 的来源不是 XML。

### 3.2 Java 方法如何对应 XML

Java 接口方法：

```java
Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);
```

XML：

```xml
<mapper namespace="com.sky.mapper.EmployeeMapper">
    <select id="pageQuery" resultType="com.sky.entity.Employee">
        select * from employee
    </select>
</mapper>
```

- `namespace` 对应接口的完整名称。
- `id="pageQuery"` 对应接口方法名。
- `resultType` 表示查询中每条记录转换为 `Employee`，不是表示整个分页容器。

在 XML 被正确加载的前提下，通过这些配置建立对应关系，不是只凭 XML 文件名。

Service 的 `pageQuery` 与 Mapper 的 `pageQuery` 是不同对象的方法，名字相同不代表同一个方法：

```java
employeeService.pageQuery(dto); // Controller 调用业务
employeeMapper.pageQuery(dto);  // Service 调用数据访问
```

### 3.3 #{name} 是参数占位

```sql
SELECT * FROM employee WHERE name = #{name}
```

左边的 `name` 是数据库列；右边 `#{name}` 表示传入参数中的姓名值。

可以简化理解为 SQL 和参数分开提供：

```sql
SELECT * FROM employee WHERE name = ?
```

```text
第1个参数：张三
```

MyBatis 配合数据库驱动完成绑定，不需要自己把用户输入拼接进 SQL。

## 4. 动态 SQL

动态 SQL：根据传入参数，决定 SQL 包含哪些条件或更新字段。

### 4.1 动态查询条件

```xml
select * from employee
<where>
    <if test="name != null and name != ''">
        and name like concat('%', #{name}, '%')
    </if>
</where>
order by create_time desc
```

- `name` 是 DTO 中的属性；当前是单个 DTO 参数，可以直接访问其属性。
- 未提供姓名：不加姓名条件。
- 提供姓名“张”：加入包含“张”的筛选。
- `concat('%', 参数, '%')` 组合匹配模式；`LIKE` 中 `%` 表示任意数量的字符。
- `<where>` 在有条件时生成 `WHERE`，处理开头多余的 `AND`。

判断姓名是否为空，是检查传入参数，不需要先查询数据库。

### 4.2 动态更新字段

```xml
update employee
<set>
    <if test="name != null">
        name = #{name},
    </if>
    <if test="phone != null">
        phone = #{phone},
    </if>
    <if test="status != null">
        status = #{status},
    </if>
</set>
where id = #{id}
```

`<set>` 生成 `SET`，处理末尾多余的逗号。只提供手机号时，业务字段更新效果就是修改手机号；提供状态时，就更新状态。

**不是 null，不等于用户改过。** 前端把原来的姓名也提交回来，当前判断仍会把姓名加入更新，并不会自动比较新旧值。

| 值 | 当前 `!= null` 判断下的含义 |
| --- | --- |
| `null` | 跳过这个字段 |
| `""` | 空字符串，但不是 null，仍可能写入 |
| `0` | 明确数值，不是 null，例如禁用状态 |

`#{...}` 负责参数值；`<if>` 决定 SQL 片段是否出现。MyBatis 处理这些标签，数据库最终收到普通 SQL，不会收到 XML 标签。

## 5. 启用、禁用员工

`status=1` 表示启用，`status=0` 表示禁用。禁用是修改字段，不是删除员工。

后端请求示例：

```text
POST /admin/employee/status/0?id=8
```

- 路径中的 `0`：要改成的目标状态。
- 查询参数 `id=8`：要修改谁。
- POST 也可以带查询参数。

Controller 使用 `@PathVariable` 读取路径状态，Service 构造数据：

```java
Employee employee = Employee.builder()
        .status(status)
        .id(id)
        .build();
employeeMapper.update(employee);
```

创建 Java 对象不等于新增数据库记录；这里调用 `update` 执行修改。主要业务效果为：

```sql
UPDATE employee SET status = 0 WHERE id = 8;
```

`WHERE` 决定修改哪些记录，遗漏条件可能更新整张表。公共字段自动填充还会补充修改时间和修改人。

前端应检查保存结果，成功后刷新列表；失败不能假装成功。账号禁用会被当前登录逻辑检查，但已经签发的 token 是否立即失效，取决于后续身份校验机制。

## 6. 编辑员工：查询、回显、保存

### 6.1 两次请求

```text
点击编辑，取得员工ID
  → GET 查询详情
  → 回显到表单
  → 用户修改输入
  → PUT 提交表单
  → 后端更新数据库
```

后端接口：查询 `GET /admin/employee/8`，保存 `PUT /admin/employee`。

回显就是把已有信息填回输入框。ID 通常不作为可编辑输入框展示，但要保留并随保存提交，供后端确定修改哪一行。

只修改 Vue 表单数据，还没有修改数据库；点击保存并成功执行后端更新后，数据库才改变。

### 6.2 属性复制

```java
Employee employee = new Employee();
BeanUtils.copyProperties(employeeDTO, employee);
employeeMapper.update(employee);
```

`copyProperties(来源对象, 目标对象)` 把名称相同、类型兼容的属性复制过去。可以近似理解为多次 `employee.setName(dto.getName())` 等赋值。

它不会查询或更新数据库，也不会自动验证姓名、手机号是否合法。真正触发更新的是 Mapper 调用。

当前员工 DTO 包含 id、username、name、phone、sex、idNumber，不包含密码与账号状态；状态通过专门接口修改。

### 6.3 和状态修改复用同一个 Mapper

编辑提交姓名、手机号等属性；禁用提交 ID、状态。两者都调用 `employeeMapper.update(employee)`，由动态 SQL 决定更新哪些字段。

查询回显是为了让用户看到旧内容。当前保存代码并没有自动比较新旧数据。需要校验必填字段，不能只依靠 XML 的非 null 判断。

验收：修改后保存，重新查询，确认新值确实持久化；只在页面上看到新值不够。

## 7. 分类、套餐、表关联与删除

### 7.1 分类不是套餐

| 概念 | 例子 |
| --- | --- |
| 菜品分类 | 热菜、饮品 |
| 菜品 | 宫保鸡丁、柠檬茶 |
| 套餐分类 | 单人套餐、双人套餐 |
| 套餐 | 宫保鸡丁＋米饭＋柠檬茶组合售卖 |

`category.type=1` 是菜品分类，`type=2` 是套餐分类。商品属于哪个分类，与套餐中包含哪些菜品，是两种关系。

### 7.2 用编号关联

分类表示例：

| id | name |
| --- | --- |
| 1 | 热菜 |
| 2 | 饮品 |

菜品表示例：

| id | name | category_id |
| --- | --- | --- |
| 101 | 宫保鸡丁 | 1 |
| 102 | 鱼香肉丝 | 1 |
| 103 | 柠檬茶 | 2 |

宫保鸡丁自己的编号是 101，所属分类编号是 1。一个分类对应多道菜，是一对多关系。

有 `category_id` 这个关联字段，不代表数据库一定配置了外键约束，需要另查表结构确认。

### 7.3 删除前先检查

删除“热菜”分类不是直接删除其中所有菜品。当前项目的规则：有任何关联菜品或套餐，拒绝删除分类。

```java
// 简化示例：变量名和错误文字为了便于阅读而调整
public void deleteById(Long id) {
    Integer dishCount = dishMapper.countByCategoryId(id);
    if (dishCount > 0) {
        throw new DeletionNotAllowedException("分类下还有菜品");
    }

    Integer setmealCount = setmealMapper.countByCategoryId(id);
    if (setmealCount > 0) {
        throw new DeletionNotAllowedException("分类下还有套餐");
    }

    categoryMapper.deleteById(id);
}
```

检查菜品的 SQL：

```sql
SELECT COUNT(id) FROM dish WHERE category_id = 1;
```

`count=2` 表示还有两道菜使用这个分类。`throw` 中断当前正常流程，后面的删除不再执行。项目的全局异常处理器把业务异常转换为 `Result.error(...)`，前端据此显示原因。

两项检查都为 0，才执行：

```sql
DELETE FROM category WHERE id = 1;
```

这解释的是当前实现的基本业务流程，不代表仅靠“先检查、后删除”就已解决并发修改下的一切一致性问题。

### 7.4 这是不是多表查询

当前删除流程涉及多张表，但分别执行单表统计和删除，没有使用 JOIN 联表查询。

“表有关联”描述数据关系；“JOIN 联表查询”描述查询方式。例如同时获取菜名和分类名：

```sql
SELECT dish.name, category.name AS category_name
FROM dish
JOIN category ON dish.category_id = category.id;
```

这里一条 SQL 使用两张表的数据，才是本次讲解中所说的联表查询。

## 8. 公共字段自动填充：当前学习位置

### 8.1 四个公共字段

| Java 属性 | 数据库列 | 含义 |
| --- | --- | --- |
| createTime | create_time | 创建时间 |
| createUser | create_user | 创建人 |
| updateTime | update_time | 最近修改时间 |
| updateUser | update_user | 最近修改人 |

新增时初始化四个字段，修改时只更新后两个。不能每次修改都覆盖原来的创建信息。

管理员 1 修改员工 8 时：`employee.id=8`，`employee.updateUser=1`。被修改的人与操作人不是同一个概念。

### 8.2 BaseContext 是什么

BaseContext 是项目自定义工具类，临时保存和读取当前处理线程中的登录人 ID，不是数据库表。

```java
BaseContext.setCurrentId(empId);       // 保存登录人ID
Long currentId = BaseContext.getCurrentId(); // 读取
```

```text
后端校验 token
  → 取出登录员工ID
  → 存入 BaseContext
  → 业务或自动填充代码读取
```

内部使用 `ThreadLocal<Long>`，不同线程各自保存值。它不是按用户永久保存，也不是天然的跨线程会话存储。

请求完成后应清理，线程复用时不能遗留旧身份；异步线程也不会默认获得原线程的值。当前工具类提供 `removeCurrentId()`，但已查看的管理端 JWT 拦截器没有展示请求结束时调用清理的代码，不能仅凭方法存在就认为清理已落实。

### 8.3 自动填充如何执行

更新 Mapper 方法有标记：

```java
@AutoFill(value = OperationType.UPDATE)
void update(Employee employee);
```

新增对应 `OperationType.INSERT`。注解只是说明要做什么，真正处理的是 `AutoFillAspect`。

项目用 Spring AOP 在选定方法执行前加入统一步骤。当前选择条件是 `com.sky.mapper` 包中的方法且带有 `@AutoFill` 注解。

```java
@Before("autoFillPointCut()")
public void autoFill(JoinPoint joinPoint) {
    // 执行公共字段填充
}
```

`@Before` 表示先执行填充，再继续目标 Mapper 调用。

### 8.4 怎么拿到要填充的对象

```java
Object[] args = joinPoint.getArgs();
Object entity = args[0];
```

`getArgs()` 取得本次方法调用的参数；`args[0]` 是第一个参数。

例如 `employeeMapper.update(employee)` 的第一个参数就是 `employee`。这里拿到同一个对象，不是重新查数据库或复制出的对象。

当前设计约定：标记的方法第一个参数是待填充的实体，并提供相应 setter。不能把标记随便加在参数不符合约定的方法上。

### 8.5 反射在这里的用途

填充代码通过反射，在运行时找到实体的 setter 并调用它。对于更新员工，执行效果相当于：

```java
employee.setUpdateTime(LocalDateTime.now());
employee.setUpdateUser(BaseContext.getCurrentId());
```

传入分类或菜品对象时，使用同一套公共逻辑找到对应方法。下面 8.7 只解释本项目中的查找与调用，不要求背诵反射 API。

### 8.6 完整执行链

```text
Service 准备对象：id=8，phone=新手机号
  → 调用带 @AutoFill(UPDATE) 的 Mapper 方法
  → AOP 前置处理取得这个对象
  → 读取当前时间和 BaseContext 中的操作人ID
  → 给同一个对象补上 updateTime、updateUser
  → Mapper / MyBatis 执行 SQL
  → 手机号、修改时间、修改人一起入库
```

自动填充发生在 Java 对象上。SQL 也需要包含这些字段才能保存到数据库；本项目更新 XML 有相应字段处理。不是只加一个注解，数据库就会自行知道该存什么。

### 8.7 小节补充：反射如何完成赋值

教学场景：管理员1修改员工8的手机号。Service 准备的对象主要包含 `id=8` 和新手机号，随后调用 `employeeMapper.update(employee)`。

前置处理通过 `args[0]` 得到同一个对象，并用变量 `entity` 引用它。`employee` 和 `entity` 是不同位置的变量名，此时指向同一个对象，不是两份独立数据。

项目实际使用方法名常量，下面把常量展开为字符串，便于阅读：

```java
Method setUpdateTime = entity.getClass()
        .getDeclaredMethod("setUpdateTime", LocalDateTime.class);

setUpdateTime.invoke(entity, now);
```

分两步理解：

1. `entity.getClass()` 获取对象的实际类型，本次是 Employee。
2. `getDeclaredMethod(...)` 在该类型中查找名为 `setUpdateTime`、参数类型为 LocalDateTime 的方法。这里只查找，还没有赋值。
3. `invoke(entity, now)` 在这个对象上调用找到的方法，把 now 作为参数传入；此时完成对象属性赋值。

对于 Employee 对象，这两句执行效果相当于 `employee.setUpdateTime(now)`。`Method` 对象可以理解为对“找到的那个方法”的描述，不能误认为已经更新了时间。

```java
Method setUpdateUser = entity.getClass()
        .getDeclaredMethod("setUpdateUser", Long.class);

setUpdateUser.invoke(entity, currentId);
```

第二组赋值效果相当于 `employee.setUpdateUser(currentId)`。当前操作人是1，因此填入1，而不是被修改员工的8。

之所以使用反射，是为了让同一段代码根据实际对象找到对应 setter，服务员工、分类、菜品等不同类型。当前方案要求这些实体提供约定的方法；不是任意对象都能自动填充，也不是所有自动填充方案都必须使用反射。

填充前后：

```text
填充前：id=8，phone=新手机号
填充后：id=8，phone=新手机号，updateTime=当前时间，updateUser=1
```

### 8.8 小节补充：赋值和入库是两件事

```text
Service 组织业务数据
  → AOP 给同一对象补充属性
  → Mapper 的动态 SQL 读取这些属性
  → 数据库执行 UPDATE
```

`invoke` 调用的是 Java setter，只修改内存对象；真正写入数据库需要后续 SQL 执行。对开启事务的操作，最终持久化还取决于事务成功提交，事务会在菜品业务中进一步讲解。

本次是 UPDATE，填充代码只更新 updateTime、updateUser，保留数据库中的创建信息。需要同时具备匹配的方法标记、符合约定的实体、当前操作人来源，以及包含公共字段的 SQL，才能实现预期效果。

小练习：

- 只执行 `getDeclaredMethod`，修改时间改变了吗？答：没有，只找到了方法。
- 执行 `invoke` 后，数据库就更新了吗？答：没有，只修改了 Java 对象，还需执行更新 SQL。
- 管理员1修改员工8，`WHERE id` 和 `update_user` 分别是什么？答：8 和1。

## 9. 图片上传：文件和地址分开保存

### 9.1 从新增菜品页面理解

商家新增“宫保鸡丁”时填写菜名、分类、价格，并选择一张图片。这个项目的常见流程有两次不同的业务操作：

```text
选择图片 → 上传图片文件 → 得到访问地址 → 前端保存地址并展示图片
点击保存菜品 → 提交菜名、价格、分类ID、图片地址等 → 数据库保存菜品
```

两次操作不等于整个页面只有两次网络请求，分类查询、图片加载等也可能发请求。选择文件本身不等于已经上传，前端需要发起上传操作。

### 9.2 图片存在哪里

| 内容 | 当前方案的存放位置 |
| --- | --- |
| JPEG、PNG 等图片文件 | 阿里云 OSS 对象存储 |
| 菜品名称、价格、所属分类等 | MySQL 菜品表 |
| 图片访问地址字符串 | 菜品表的 image 字段 |

OSS 可以先理解成在线文件仓库，Bucket 是其中一个存储容器。本项目通常不把图片文件内容直接写进 MySQL，也不是所有项目都必须使用 OSS；其他项目可能使用本地文件目录或其他存储服务。

数据库中的图片地址帮助浏览器找到文件；地址字符串本身不是图片内容。能否访问还取决于文件是否存在、访问权限等。

### 9.3 后端如何接收文件

项目后端接口：`POST /admin/common/upload`，对应 CommonController：

```java
@PostMapping("/upload")
public Result<String> upload(MultipartFile file) {
    // 生成文件名、上传并返回地址
}
```

普通员工编辑提交 JSON 字段；这里提交文件，通常使用 `multipart/form-data`，字段名为 `file`。MultipartFile 是 Spring 提供的接收上传文件的类型。

前端教学示例：

```js
const formData = new FormData();
formData.append("file", selectedFile);
```

selectedFile 表示用户已经选择的 File 对象。FormData 可以理解为能装文件和普通字段的表单数据。以上两行只准备数据，还未发送请求。通过浏览器 fetch 发送 FormData 时，让浏览器生成包含 boundary 的 Content-Type，不要把它写成 application/json；受保护接口仍需携带登录凭证。

### 9.4 文件内容、原文件名和新文件名

Controller 中主要步骤：

```java
String originalFilename = file.getOriginalFilename();
// 实际代码从原文件名最后一个点开始截取扩展名
String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
String objectName = UUID.randomUUID().toString() + extension;
String filePath = aliOssUtil.upload(file.getBytes(), objectName);
return Result.success(filePath);
```

这是对实际代码的整理，省略日志和异常捕获，并复用了原代码已经取得的 originalFilename 变量。

| 名称 | 作用 |
| --- | --- |
| originalFilename | 用户上传的原始文件名，例如 photo.jpg |
| extension | 后缀，例如 .jpg |
| objectName | 上传后使用的对象名，例如 UUID 加 .jpg |
| file.getBytes() | 取得真正的文件内容 |
| filePath | 工具类返回的图片访问地址 |

UUID 用于降低同名碰撞风险，避免不同用户都上传 photo.jpg 时使用同一个存储名称。它不是把图片内容改掉，也不是文件类型验证。当前取后缀写法没有完整处理文件名为空或没有点的情况，实际功能需要相应验证。

### 9.5 OSS 上传和返回结果

AliOssUtil 中真正发起存储的调用：

```java
ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
```

可以读成：把这份文件内容，以 objectName 为名称，放进指定 Bucket。

在成功路径中，工具类构造地址，Controller 返回 `Result.success(filePath)`。因此这次响应中的 data 是地址字符串，不是分页对象或员工对象。

前端使用这个字符串给菜品表单的 image 赋值，随后通过图片元素加载地址以展示预览。例如 Vue 模板 `<img :src="form.image">`；这里只是教学示例，不代表已有管理端组件采用同样变量名。

点击保存菜品时，提交的 JSON 携带 image 地址。DishDTO 中 image 的类型是 String；菜品 Mapper 的插入 SQL 将 image 写入数据库。只上传图片但取消新增，菜品记录不会因为上传自动创建，图片可能已经留在 OSS 中。

### 9.6 当前代码需要区分的成功与失败

阅读发现：AliOssUtil 捕获 OSSException、ClientException 后仍继续构造并返回地址，没有把这两类失败继续抛给 Controller。因此某些上传失败也可能得到包含地址的成功响应。

这是代码阅读发现，没有实际发起失败实验，本节也没有修改实现。不能把“接口返回了地址”直接当成“文件一定上传成功”。正常设计应在存储失败时向调用方报告失败，并验证上传的文件确实可访问。

验收图片上传时，检查：有效图片上传后可正常显示；失败时不显示成功；文件大小与实际类型有合理校验；新建菜品保存后重新查询仍能取得图片地址。文件名后缀和前端 accept 限制不能替代后端文件验证。

### 9.7 小练习

1. 图片文件和 image 字段有什么区别？文件是实际图片内容；image 在当前项目中保存访问地址字符串。
2. 拿到图片 URL 后，菜品一定创建了吗？没有，仍需提交保存菜品请求。
3. 两个人都上传 photo.jpg，为什么重新生成文件名？降低对象名碰撞和意外覆盖的风险。
4. 地址字符串返回成功，但图片打不开，要检查哪里？请求结果、OSS 上传是否成功、对象是否存在、访问地址与权限，不能只看字符串。

### 9.8 追问补充：先把 OSS 理解成供程序使用的在线文件仓库

暂时不看上传代码，也不要求记住 SDK、Bucket、UUID 等术语。

假设电脑里有一张 `宫保鸡丁.jpg`。仅有你电脑上的文件路径，顾客的手机不能直接读取这张本地图片。需要把图片放到网站能够提供访问的位置。本项目选择把图片上传到阿里云 OSS。

OSS 是阿里云提供的一项文件存储服务，入门时可类比为“给程序使用的网盘”：程序把文件上传进去，之后按相应访问规则获取文件。这里的类比用于理解用途，不表示它与个人网盘完全相同，也不表示所有文件都默认公开。

```text
你的电脑：宫保鸡丁.jpg
    → 后端把图片文件上传到 OSS
    → 得到/构造图片的访问地址
    → 保存菜品时，把这个地址写进数据库
```

三件事要分开：

| 东西 | 可以怎样理解 |
| --- | --- |
| 图片文件 | 真正的照片内容 |
| OSS | 保存照片的在线仓库 |
| 图片地址 | 告诉浏览器去哪里获取照片的地址 |

菜品数据库记录可以理解为一张信息卡：菜名是宫保鸡丁，价格是28元，image 字段写着“这张照片的访问地址”。照片本身存放在 OSS 中。

顾客打开菜品页面时：

1. 前端从后端取得菜品信息，其中包含图片地址。
2. 浏览器使用图片地址发起请求，获取图片内容。
3. 浏览器把图片显示到页面上。

在文件允许直接读取的情况下，浏览器可根据地址直接向 OSS 请求图片。地址不等于文件内容：即使删除 OSS 中的照片，数据库里的地址字符串也不会自动消失，但它可能已无法加载照片。

图片也可以放在自己的服务器或其他存储服务中，不是只能使用 OSS。当前项目选择 OSS，是让专门的存储服务保存图片。数据库技术上可以保存二进制内容，但本项目采用“数据库存地址、OSS 存文件”的设计。

本节自测：只在数据库 image 字段随便写一个图片地址，没有实际上传文件，顾客就一定能看到照片吗？答：不能，还必须保证对应文件真实存在且允许访问。

### 9.9 代码重讲：从 photo.jpg 到图片访问地址

使用用户上传 photo.jpg 的成功流程演示。以下整理版本复用了 originalFilename 变量，并省略日志、异常捕获和接口文档注解；不是新增或修改实际业务实现。

```java
@PostMapping("/upload")
public Result<String> upload(MultipartFile file) {
    String originalFilename = file.getOriginalFilename();
    String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    String objectName = UUID.randomUUID().toString() + extension;
    String filePath = aliOssUtil.upload(file.getBytes(), objectName);
    return Result.success(filePath);
}
```

这段是为了说明主干流程的节选，真实方法保留了异常处理。MultipartFile.getBytes() 涉及异常处理，不应把节选当成无需补充即可单独编译的完整方法。

**步骤1：接收上传文件。** `MultipartFile` 是类型，`file` 是变量名，代表本次请求上传的文件。类上的 `/admin/common` 与方法上的 `/upload` 组合成后端接口路径。`Result<String>` 表示成功响应的 data 是字符串。

**步骤2：读取原文件名。** `file.getOriginalFilename()` 得到字符串 photo.jpg，并没有读取整张图片内容，也没有上传到 OSS。

**步骤3：取得后缀。** 将原来的长表达式拆开看：

```java
int dotIndex = originalFilename.lastIndexOf(".");
String extension = originalFilename.substring(dotIndex);
```

对 photo.jpg，最后一个点的下标是5。substring(5) 从这个点开始取到结尾，结果是 .jpg。这里操作的是文件名字符串，不是裁剪图片。

**步骤4：生成存储名称。** `UUID.randomUUID()` 生成 UUID，`toString()` 转成字符串，再与 .jpg 拼接。存储时使用新名字，降低同名碰撞风险。UUID 的格式不需要背诵，扩展名也不能代替真实类型校验。

**步骤5：取得内容并调用上传工具。**

```java
byte[] bytes = file.getBytes();
String filePath = aliOssUtil.upload(bytes, objectName);
```

这两行等价于 Controller 中的一行调用。第一个参数提供图片内容，第二个参数说明在 OSS 中用什么名字保存。byte[] 是字节数组，可以先理解为文件内容在 Java 中的一种表示。

这里是 Controller 直接调用项目的 AliOssUtil 工具类；没有经过员工分页中那种 Service → Mapper → MySQL 流程。保存文件与保存菜品记录是不同的操作。

工具类中创建 OSS 客户端：

```java
OSS ossClient = new OSSClientBuilder()
        .build(endpoint, accessKeyId, accessKeySecret);
```

endpoint 是要连接的 OSS 服务地址；accessKeyId 与 accessKeySecret 是程序使用的访问凭证。客户端对象是程序调用 OSS 的工具，不是图片文件。

真正执行存储请求：

```java
ossClient.putObject(
    bucketName,
    objectName,
    new ByteArrayInputStream(bytes)
);
```

三个参数分别回答“放进哪个容器、叫什么名字、文件内容是什么”。ByteArrayInputStream 把字节数组包装成可供读取的输入流，便于 SDK 读取文件内容，不是重新生成一张图片。

工具类最后关闭客户端以释放资源；shutdown 不表示删除已上传文件。

**步骤6：构造地址并返回给前端。** 当前工具使用 StringBuilder 把协议、Bucket、endpoint 和 objectName 拼成访问地址。可读成 `https://<bucketName>.<endpoint>/<objectName>` 的示意结构。

```java
return Result.success(filePath);
```

filePath 在这里保存网络访问地址，不是电脑本地文件路径。前端从 result.data 取得字符串，可用于图片显示以及后续保存菜品时提交 image 字段。

整个变量变化：

```text
file：请求上传的文件
  → originalFilename：photo.jpg
  → extension：.jpg
  → objectName：UUID字符串.jpg
  → file.getBytes()：图片内容
  → filePath：访问地址
  → Result.data：返回给前端的访问地址
```

文件上传的成功路径不涉及往菜品表插入记录。代码中原来的 try/catch 用于异常处理，但 AliOssUtil 吞掉部分 OSS 异常后仍返回地址的问题仍存在，详见 9.6；本节没有修复或实测上传。

小练习：`file.getOriginalFilename()` 与 `file.getBytes()` 的区别是什么？前者取得名字字符串，后者取得实际文件内容。仅发送文件名不足以保存图片。

### 9.10 再简化：暂时只看三步和赋值顺序

先忽略后缀截取、UUID、OSS 客户端创建、输入流等细节。下面使用固定名称仅用于解释数据流，不作为实际重复上传的命名方案：

```java
byte[] bytes = file.getBytes();
String url = aliOssUtil.upload(bytes, "photo.jpg");
return Result.success(url);
```

这三句是方法内部的教学节选，省略了原有异常处理。

1. file 是后端收到的文件对象。getBytes() 取得照片内容，保存到变量 bytes。
2. 调用上传工具，把内容 bytes 和保存名称 photo.jpg 交给它。按上传成功的路径，工具完成上传并返回图片访问地址。
3. 把地址包装成接口响应，交给前端。

第二句先执行等号右边的 upload，再把返回结果赋给左边的 url。url 的名字可以改，真正重要的是里面保存的是地址字符串；图片内容没有变成地址，而是工具返回了用于找到这份图片的地址。

```text
传进去：图片内容 + 保存名称
工具做：把文件存入 OSS
返回来：图片访问地址
```

此时没有创建菜品记录。上传函数返回值的含义与其内部实现分开理解；当前工具吞掉部分上传异常的问题仍参见 9.6。

学习反馈：用户在这版解释后回复“听懂了”。核心是区分传入的图片内容与返回的地址，按“先执行等号右边，再把返回值赋给左边变量”阅读方法调用。下一节继续菜品保存，不要求先背诵完整上传工具代码。

## 10. 新增菜品：菜品和口味分开保存

### 10.1 本节只看表结构与保存顺序

商家配置“宫保鸡丁”：所属分类热菜、价格28元、图片为已上传得到的地址，还可以配置辣度选项“不辣、微辣、中辣”，以及忌口选项“不要葱、不要蒜”。这里是在给商家配置可选项，不是记录某位顾客本次点单选了哪个口味。

菜品表 dish 保存基本信息，菜品口味表 dish_flavor 保存这道菜的口味配置。一个菜品可以有多条口味配置，因此是一对多关系。

菜品表示例：

| id | name | category_id | price | image |
| --- | --- | --- | --- | --- |
| 100 | 宫保鸡丁 | 1 | 28 | 已上传图片的访问地址 |

口味表示例：

| id | dish_id | name | value |
| --- | --- | --- | --- |
| 501 | 100 | 辣度 | `["不辣","微辣","中辣"]` |
| 502 | 100 | 忌口 | `["不要葱","不要蒜"]` |

编号均为教学假设。dish_id=100 表示口味配置属于菜品100；口味记录自己的编号是501或502。菜品的 category_id=1 则指向分类，不能把 category_id 与 dish_id 混为一谈。

当前 DishFlavor.value 的 Java 类型是 String，表中的选项以 JSON 数组形式的文本展示，不应误认为这个字段在 Java 中是 List。两行表示两个口味组，不是每个选项各插入一条记录。

#### 追问：为什么菜品和口味要分开保存

这里分开的是同一个数据库中的两张表，不是要求前端点两次保存。设计原因：一道菜的基本资料通常一份，但口味配置可以有零组、一组或多组，不同菜品的组数和名称也不同。

例如宫保鸡丁有辣度、忌口两组，奶茶有甜度、温度两组，矿泉水可以没有配置。若在菜品表预先固定“口味1、口味2、口味3”或“辣度、甜度、温度”等列，新增口味维度时可能要改表结构，并出现许多无用的空字段。

另一个方案是在同一张表中每个口味组占一行，重复写菜名和价格：

| 菜名 | 价格 | 口味组 |
| --- | --- | --- |
| 宫保鸡丁 | 28 | 辣度 |
| 宫保鸡丁 | 28 | 忌口 |

这样同一道菜的基本信息重复。修改价格若只改其中一行，就会出现不一致。

本项目选择：dish 里一行保存一份菜品基本资料；dish_flavor 里每个口味组一行，通过 dish_id 指向同一道菜。需要增加一个口味组时，增加一条关联记录即可，不需要为这个组添加菜品表的新列，也不需要重复存菜名、价格。

```text
dish：100，宫保鸡丁，28元
dish_flavor：100 → 辣度
             100 → 忌口
             100 → 后续新增的口味组
```

这是项目为一对多关系选择的设计方式，不是唯一合法方案。也可以将全部口味配置放在菜品记录的一个 JSON 字段中，但读写、查询和约束方式会不同。不能把“分表更符合当前模型”讲成“数据库绝对不能一列存多个选项”；本项目口味表的 value 本身就使用文本保存一组可选值。

复习要点：菜品基本信息保存一份，数量可变的口味组另存多条，用菜品ID关联。关联字段重复表示多条配置指向同一道菜，不是重复保存整份菜品资料。

### 10.2 前端提交一份表单，后端分别写两张表

DishDTO 同时接收菜品字段和口味列表：

```java
private String name;
private Long categoryId;
private BigDecimal price;
private String image;
private List<DishFlavor> flavors = new ArrayList<>();
```

flavors 可理解为装多个口味配置对象的列表。本例里有“辣度”和“忌口”两个对象。它不是图片，也不是菜品分类。

新增菜品接口将 DTO 交给 `dishService.saveWithFlavor(dishDTO)`。一次新增菜品请求可以组织多次数据库操作，不是每张表必须分别让前端发请求。

#### 追问：一次请求为什么能保存两张表

这里容易把“前端请求后端一次”和“后端执行一条 SQL”混为一谈。两者数量没有一一对应关系。

前端可以把整份表单一起交给后端，就像递交一张完整的申请表。表单里同时有菜名、价格、图片地址，以及辣度、忌口配置。

```text
用户点击一次“保存菜品”
    ↓
前端把整份表单发送给后端：一次新增菜品请求
    ↓
后端第1步：往 dish 表保存菜品基本信息
    ↓
后端第2步：往 dish_flavor 表保存关联口味配置
    ↓
后端完成处理后返回一次响应
```

前端请求的对象是后端接口，不是直接请求数据库表。后端根据业务需要执行多个 Mapper 方法，所以一个接口方法可以组织多条 SQL。

对应到当前代码，Controller 调用一次 `dishService.saveWithFlavor(dishDTO)`；这个 Service 方法内部先调用 `dishMapper.insert(dish)`，再在口味非空时调用 `dishFlavorMapper.insertBatch(flavors)`。多步操作集中在后端完成，不需要前端指挥每张表怎么存。

教学类比：在餐厅点一次套餐，服务员收到一份订单，厨房内部可以分别准备米饭、炒菜。对外的一次提交，可以对应内部多个步骤。

“一次请求”限定为这次点击保存菜品的请求，不包括之前已经发生的图片上传、分类列表加载等请求。它也不意味着多条 SQL 自动具有原子性；全部成功或失败时回滚需要后续的事务机制解释。

小练习：点击一次保存，后端执行两条 INSERT，是否矛盾？不矛盾。一次是前端到后端的提交次数，两条是后端执行的数据库操作数量。

### 10.3 为什么先保存菜品

按当前项目中由数据库生成新增菜品ID的流程，保存前不知道最终菜品ID，就无法给口味记录设置正确的 dish_id。

```text
提交菜品信息和口味配置
  → 向 dish 表插入菜品
  → 得到新菜品ID，假设为100
  → 把每个口味对象的 dishId 设置为100
  → 插入口味表
```

Service 中先执行：

```java
Dish dish = new Dish();
BeanUtils.copyProperties(dishDTO, dish);
dishMapper.insert(dish);
Long dishId = dish.getId();
```

属性复制准备菜品基本信息；口味列表不会因此自动存入数据库。insert 执行新增，随后 getId 读取回填到 dish 对象里的ID，并不是再执行查询。

Mapper XML 的配置是：

```xml
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    <!-- 实际 INSERT SQL -->
</insert>
```

它配合数据库生成的主键，将生成值写回传入对象的 id 属性。若没有相应主键生成与回填机制，不能仅凭 new Dish() 就认为已经有了数据库ID。

#### 本节展开：主键回填到底填了什么

主键可以先理解为一条记录的唯一编号。菜品名称可能重复或修改，用菜品ID关联口味更明确。以下按正常新增请求不提供 id、数据库负责生成主键的流程解释，不代表本次已经检查或修改了实际数据库表结构。

新建并复制好基本字段后，Java 对象可以是：

```text
dish.id = null
dish.name = 宫保鸡丁
dish.price = 28
```

调用 `dishMapper.insert(dish)`，数据库保存记录并生成ID，假设是100。Mapper 的 INSERT 列表没有主动写入 id 字段。

XML 中几个看起来相似的名字要分开：

| 配置 | 含义 |
| --- | --- |
| `id="insert"` | 这条映射语句对应哪个 Mapper 方法，不是菜品编号 |
| `useGeneratedKeys="true"` | 要求使用数据库/驱动提供的生成主键信息 |
| `keyProperty="id"` | 把取得的生成值写回传入 Java 对象的 id 属性 |

其中生成ID是数据库的工作；这个配置负责取回并填入对象，不是给数据库凭空开启自增功能。

```text
数据库：新增菜品，生成 id=100
    ↓
MyBatis：通过驱动取得生成值，写回原来传入的 dish 对象
    ↓
Java对象：dish.id 从 null 变为100
```

因此代码按顺序写：

```java
dishMapper.insert(dish);
Long dishId = dish.getId();
```

第一句执行完成后，第二句从内存对象读取100并保存到变量 dishId。getId 既不生成ID，也不再次查询数据库；没有回填成功时，仅调用 getter 不会把数据库ID自动查回来。

对比顺序：

```java
Long before = dish.getId(); // 正常新增且未赋ID时为null
dishMapper.insert(dish);    // 插入并回填，示例ID为100
Long after = dish.getId();  // 此时取得100
```

before 不会因为后来回填了 dish.id 而自动变成100，它是之前读取到的值。应当在插入后读取需要使用的生成ID。

随后给每个口味对象设置 dishId=100，就能将口味与刚新增的菜品关联。不应通过“按菜名查询最新一条”或“查询最大ID”猜测此次生成的编号。

小练习：口味应该关联菜品对象插入前还是插入后取得的ID？答案：插入成功、主键回填后读取的ID。若拿到的是 null，应检查数据库生成策略、Mapper 回填配置和调用是否成功，而不是把 null 当成有效关联编号。

#### 追问：数据库会自动生成 ID 吗

会，但取决于表结构和使用的生成方式。MySQL 常见方式是在主键列上设置 AUTO_INCREMENT（自增）。不是只要字段名叫 id，或者只声明了主键，就自动生成。

```sql
-- 仅为讲解用的建表示例，没有实际执行
CREATE TABLE dish_example (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50)
);
```

| 部分 | 作用 |
| --- | --- |
| id | 列名，程序用它表示编号 |
| BIGINT | 整数类型 |
| PRIMARY KEY | 标识记录，要求主键值唯一且非空 |
| AUTO_INCREMENT | 插入时由数据库按自增规则分配编号 |

```sql
INSERT INTO dish_example (name) VALUES ('宫保鸡丁');
INSERT INTO dish_example (name) VALUES ('鱼香肉丝');
```

插入语句没有提供 id，数据库可以自动填写。在新表、默认起始值与步长的简单示例中，通常得到1、2。不要把自增ID当作记录总数，也不要要求永远连续无缺口；删除、失败或回滚等都可能导致空缺。

只有 PRIMARY KEY 而没有自动生成机制时，应用通常需要自己提供合法ID。应用也可以采用其他ID生成方式，不能推断所有系统都依赖数据库自增。

本项目菜品 INSERT 不写 id，并配置了 useGeneratedKeys 与 keyProperty，代码按使用数据库生成主键的方式设计。当前没有读取运行中的 dish 表结构，因此不声称已经验证它实际存在 AUTO_INCREMENT。可以在数据库中执行只读的 `SHOW CREATE TABLE dish;`，查看 id 列定义；本节没有执行该命令。

完整分工：数据库根据表结构生成ID → MyBatis 获取生成值并填入 dish.id → Java 用 dish.getId() 读取。主键回填配置不负责为数据库字段开启自增。

### 10.4 再给口味设置所属菜品并批量保存

项目取得 `dishDTO.getFlavors()`，当列表不为 null 且非空时，给其中每个口味对象设置 dishId，再调用 `dishFlavorMapper.insertBatch(flavors)`。

先用普通循环理解原项目的 forEach 写法：

```java
for (DishFlavor flavor : flavors) {
    flavor.setDishId(dishId);
}
dishFlavorMapper.insertBatch(flavors);
```

这段只展示非空分支。循环中的 setDishId 只是修改 Java 对象；insertBatch 才触发口味表的插入。当前 XML 用 foreach 生成一条带多组 VALUES 的 INSERT，下面进一步展开。

没有口味时只保存菜品，不需要强行插入口味记录。一次新增请求是否全部成功，涉及事务；项目 saveWithFlavor 上有 @Transactional，留到后续结合失败场景讲解，不把它理解为任何外部操作都会自动回滚。

小练习：新菜品ID为200，它的“辣度”和“忌口”两条配置的 dish_id 应分别是多少？答案：都为200，表示同属于这道菜；各自的口味记录ID仍独立。

#### 本节展开：Java 循环与 XML 循环各做什么

教学前提：宫保鸡丁已经插入，主键回填得到 dishId=100。前端提供两条口味配置：辣度与忌口；一个口味组对应一个 DishFlavor 对象。

```java
List<DishFlavor> flavors = dishDTO.getFlavors();
```

这句读取请求 DTO 中的口味列表，不是在数据库里查询口味。此时新增口味的所属菜品ID尚待后端设置：

```text
flavors
├─ 辣度：dishId待设置，value为不辣、微辣、中辣的选项文本
└─ 忌口：dishId待设置，value为不要葱、不要蒜的选项文本
```

项目判断：

```java
if (flavors != null && flavors.size() > 0) {
    // 有口味配置时才处理
}
```

flavors != null 检查列表对象存在，size() > 0 检查里面有记录。&& 表示两个条件都满足；第一个条件不成立时不再执行后面的 size()，避免对 null 调用方法。

项目实际赋值代码：

```java
flavors.forEach(dishFlavor -> {
    dishFlavor.setDishId(dishId);
});
```

forEach 对列表中的每个对象执行括号里的操作。dishFlavor 是本轮拿到的对象的变量名，箭头右边是对它做的操作；它不是新增了一份口味对象。

当 dishId=100 时，相当于先给“辣度”对象设置100，再给“忌口”对象设置100。这两次 setter 都只修改内存对象，并没有执行两次数据库保存。

循环结束后执行：

```java
dishFlavorMapper.insertBatch(flavors);
```

insertBatch 是项目定义的方法名，真正执行什么由 XML 决定。当前映射是：

```xml
<insert id="insertBatch">
    insert into dish_flavor (dish_id, name, value)
    values
    <foreach collection="flavors" item="df" separator=",">
        (#{df.dishId}, #{df.name}, #{df.value})
    </foreach>
</insert>
```

| 写法 | 含义 |
| --- | --- |
| collection="flavors" | 遍历传入的口味列表 |
| item="df" | 给当前遍历到的口味对象起一个临时名字 |
| #{df.dishId} | 读取当前口味的所属菜品ID并绑定参数 |
| #{df.name} | 读取当前口味组名称 |
| #{df.value} | 读取当前口味组选项文本 |
| separator="," | 多组括号之间用逗号分隔 |

MyBatis 将两条配置组织为带两组 VALUES 的一条 SQL。下面使用具体值说明效果，实际执行使用参数绑定：

```sql
INSERT INTO dish_flavor (dish_id, name, value)
VALUES
    (100, '辣度', '["不辣","微辣","中辣"]'),
    (100, '忌口', '["不要葱","不要蒜"]');
```

对应插入两条记录。数据库执行的是 SQL，不执行 XML 标签。当前做法是单条 INSERT 插入多行，不要把所有叫 insertBatch 的方法都推断为同样实现。

区别速记：Java forEach 给每个对象补上所属菜品ID；XML foreach 用这些对象的值组织 SQL 的多组数据；MySQL 最后执行插入。虽然两个地方都在循环，但职责不同。

本节自测：有3个口味组时，Java setter 执行几次、当前 XML 生成几组 VALUES？答案：setter执行3次，一条 INSERT 中生成3组 VALUES。

### 10.5 事务：避免菜品和口味只保存一半

#### 先从失败场景理解

新增一份包含口味的菜品，需要先写 dish 表，再写 dish_flavor 表。如果没有共同事务，且每条语句独立提交，那么菜品保存成功后，口味插入失败可能留下不完整的业务数据：菜品存在，但本来应有的口味配置没保存。

事务把需要共同成功的数据库操作放在一个工作单元中。在事务正常生效、数据库支持事务、失败符合回滚规则等前提下：全部完成才提交；中途失败则回滚本次事务中的修改。

| 术语 | 本例中的意思 |
| --- | --- |
| 事务 | 将新增菜品与新增其口味作为同一组数据库操作 |
| 提交（commit） | 正式保留这组操作的结果 |
| 回滚（rollback） | 撤销本次事务中尚未提交的数据库修改 |

```text
开始事务
  → 插入菜品：SQL已执行，但整个事务还没有提交
  → 插入口味
      ├─ 正常完成 → 提交 → 菜品和口味都保存
      └─ 符合回滚规则的异常 → 回滚 → 本次新增的菜品也不保留
```

“前一条 SQL 执行成功”和“整个事务提交成功”不是一回事。数据不必全部等到最后才开始执行；可以先执行多条 SQL，最后决定提交还是回滚。

#### 项目代码如何声明

实际 `DishServiceImpl.saveWithFlavor` 方法上已有 `@Transactional`。简化主干如下：

```java
@Transactional
public void saveWithFlavor(DishDTO dishDTO) {
    Dish dish = new Dish();
    BeanUtils.copyProperties(dishDTO, dish);
    dishMapper.insert(dish);

    Long dishId = dish.getId();
    List<DishFlavor> flavors = dishDTO.getFlavors();
    if (flavors != null && flavors.size() > 0) {
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishId);
        }
        dishFlavorMapper.insertBatch(flavors);
    }
}
```

普通 for 循环是对实际 forEach 写法的等价教学表达。本节没有改动业务代码。

这个注解让 Spring 在事务配置和代理调用生效时管理该方法的事务。当前方法从 Controller 通过注入的 Service 调用，并把两个 Mapper 写入组织在同一业务方法中。单独在其中一个 INSERT 周围开启事务，不足以覆盖完整的两步业务。

#### 回滚不等于再手写 DELETE

如果口味插入失败导致回滚，数据库撤销同一事务里此前未提交的菜品插入，不要求业务代码再手写删除该菜品的补偿 SQL。它不会删除数据库中其他原有菜品，也不表示恢复整个数据库的所有历史操作。

Java 对象中已经回填的ID也不等于数据库最终一定存在这条记录：事务可能随后回滚。自增编号出现空缺也不一定代表回滚失败。

#### 需要知道的边界

默认声明式事务规则通常对 RuntimeException 和 Error 回滚，受检异常默认不回滚，可通过 rollbackFor 等配置调整。并非每一种错误提示都会自动回滚。如果在业务方法中自行 catch 后忽略异常、正常返回，又没有其他回滚标记，不能期待 Spring 仅凭打印日志判断业务失败。[Spring 官方回滚说明](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)

这个说明以当前普通同步方法为背景，不引入新版本框架的异步返回值规则。这里先掌握“事务必须生效，失败要正确传播并满足回滚规则”，暂不展开传播级别和隔离级别。

图片上传是之前单独的请求，文件存于 OSS。菜品数据库事务回滚，不会自动删除 OSS 图片；也不会自动撤销任何任意的外部操作。当前 Controller 在 Service 返回后另有缓存清理，数据库事务不能因此被理解成覆盖整个接口所有外部效果。

#### 追问：什么叫“失败正确报告给事务机制”

事务基础设施不会靠中文日志或返回 JSON 的 code=0 来判断业务是否需要回滚。常见声明式事务流程是在方法执行过程中，观察是否正常返回，或者是否有异常离开方法，再根据回滚规则决定处理。

```text
Controller 调用 Spring 管理的 Service
    → Spring 事务处理包围这次调用
    → Service 方法执行数据库操作
    → 方法正常结束：通常提交
      方法抛出符合规则的异常：回滚
```

以上假定没有其他回滚标记和提交失败等情况。当前项目是普通同步方法，不需要先掌握底层代理实现。

教学场景：菜品已插入，随后业务校验发现口味不合法。以下都是方法内部的说明片段，不是本节实际写入业务代码的改动。

```java
throw new IllegalArgumentException("口味配置不合法");
```

如果这类运行时异常从正常生效的事务方法抛出，Spring 的默认规则会使事务回滚。不是错误消息文字决定回滚，而是异常类型和配置规则。

如果在同一个事务方法内部捕获后忽略：

```java
try {
    throw new IllegalArgumentException("口味配置不合法");
} catch (IllegalArgumentException ex) {
    log.error("口味配置失败", ex);
}
// 方法接着正常结束
```

这里虽然日志写着失败，但异常没有离开方法。若没有其他回滚标记，方法可能正常提交，导致已插入的菜品留下来。这个示例使用业务异常说明控制流，不能推断所有数据库异常被 catch 后都能提交；底层或内部事务也可能已经标记回滚。

需要记录日志时，可以记录后继续抛出异常。关键是不能把本来要求整组失败的情况伪装为正常完成。仅返回 Result.error(...) 也不等同于抛出异常或标记回滚。

项目 Controller 外部的统一异常处理器捕获异常并返回前端提示，是另一回事：异常先离开 Service 的事务边界，Spring 可以先完成回滚，再由外层处理器组织响应。不要误以为任何位置的 catch 都会阻止回滚。

“事务正常生效”还要求数据库与事务配置支持、调用经过 Spring 的事务处理等条件；并非任何对象的方法贴上注解就自动生效。例如自己 new 出来的对象调用，或默认代理模式下同类内部直接调用该事务方法，都不能仅凭注解认定新事务已开启。本节先理解前面的调用边界，不展开代理语法。

复习句：失败需要从事务方法中以符合回滚规则的异常等方式传递出去；打印日志和页面显示失败不是回滚的证据。

#### 如何验证：教学方案，尚未执行

在隔离的测试环境中让口味插入抛出符合回滚规则的异常，确认请求失败后，本次新增菜品与口味都没有被保留；再验证正常情况两张表都有预期记录。仅看到页面提示失败，不能证明数据库已经回滚。不要为演示直接在真实业务数据上随意制造故障。

本节自测：新增宫保鸡丁时，菜品 INSERT 完成，口味 INSERT 抛出未捕获的运行时异常。若两步处于同一正常生效的事务中，菜品应留下吗？答案：不应留下，此次菜品插入随事务回滚。之前上传到 OSS 的图片会自动消失吗？不会，它不属于本次数据库事务。

## 11. 菜品分页查询：怎样显示分类名称

### 11.1 页面需要的信息来自两张表

页面希望显示“宫保鸡丁、热菜、28元”。菜品表只记录 category_id，分类名称来自 category 表。

菜品表 dish，教学示例：

| id | name | category_id | price |
| --- | --- | --- | --- |
| 100 | 宫保鸡丁 | 1 | 28 |
| 101 | 柠檬茶 | 2 | 8 |

分类表 category：

| id | name |
| --- | --- |
| 1 | 热菜 |
| 2 | 饮品 |

通过 dish.category_id = category.id 匹配，查询结果中可以同时取得菜名和分类名。不是拿菜品自己的 id 与分类 id 相等来匹配。

### 11.2 当前项目的联表 SQL

DishMapper.xml 中的基础查询整理为：

```sql
SELECT d.*, c.name AS categoryName
FROM dish d
LEFT JOIN category c
    ON d.category_id = c.id;
```

| 片段 | 含义 |
| --- | --- |
| FROM dish d | 查询菜品表，在这条 SQL 中用 d 作为表的简称 |
| LEFT JOIN category c | 把分类表关联进来，用 c 作为简称 |
| ON d.category_id = c.id | 规定按哪个字段关系匹配 |
| d.* | 取菜品表的所有列 |
| c.name AS categoryName | 取分类名称，在结果中称为 categoryName |

d 和 c 仅是本次 SQL 的表别名，没有新建或改名数据库表。d.name 是菜名，c.name 是分类名，两个表都有 name 列，前缀用于说明来源。

逐行理解关联：宫保鸡丁的 category_id=1，匹配到分类 id=1 的“热菜”；柠檬茶的 category_id=2，匹配到“饮品”。查询结果示意：

| id | name | category_id | price | categoryName |
| --- | --- | --- | --- | --- |
| 100 | 宫保鸡丁 | 1 | 28 | 热菜 |
| 101 | 柠檬茶 | 2 | 8 | 饮品 |

这是结果的组合，不会向 dish 表新增 categoryName 列，也不会把分类名称自动写进菜品表。

### 11.3 LEFT JOIN 为什么叫左连接

在这条查询中，左侧是 FROM 后的 dish，右侧是 JOIN 后的 category。对保留下来的菜品行，LEFT JOIN 尝试匹配分类；如果找不到，仍可保留菜品行，分类侧字段为 null。

例如某道菜 category_id=99，但分类表没有99，这条查询仍可返回菜品，categoryName 为 null。这个例子仅解释 SQL 行为，不表示业务允许保存无效分类。当前查询的动态筛选针对 d 表；其他查询如果在 WHERE 中添加分类侧条件，保留结果还会受这些条件影响。

### 11.4 为什么结果类型是 DishVO

当前 XML 使用 `resultType="com.sky.vo.DishVO"`，DishVO 中有：

```java
private String name;         // 菜名
private Long categoryId;     // 分类编号
private String categoryName; // 分类名称
```

SQL 的 `AS categoryName` 使分类名称结果列与这个 Java 属性对应。VO 用于组织页面所需的数据，能够接收由多个表组合出的结果，不要求每个属性都是同一张表的列。

DTO 与 VO 在本功能中对应：DishPageQueryDTO 接收查询要求（页码、每页条数、菜名、分类ID、状态）；DishVO 表示查出的单条菜品展示信息。

DishVO 虽然还定义了 flavors 属性，但本次分页 SQL 没有查询口味表；声明字段本身不会自动把口味查出来。

### 11.5 接回已经学过的分页

实际 Service 代码：

```java
PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
return new PageResult(page.getTotal(), page.getResult());
```

联表负责“每行包含哪些信息”，筛选负责“哪些菜品符合条件”，分页负责“返回其中哪一页”。项目的 XML 还根据菜名、分类ID、状态追加条件，并按 d.update_time 倒序排列。

关系链：dish.category_id → category.id → category.name → 查询结果 categoryName → DishVO.categoryName → 前端展示分类名。

小练习：dish.id=100、dish.category_id=2，分类表 id=2 的名称为“饮品”，查询得到的 categoryName 是什么？答案：饮品。匹配依据是 category_id=2，不是菜品编号100。

## 12. 批量删除菜品

### 12.1 先决定能不能删

项目的规则是：起售中的菜品不能直接删除；被套餐使用的菜品也不能删除。两项检查都通过，才删除菜品及其附属口味配置。

假设删除宫保鸡丁，编号100。它可能存在三种情况：

| 情况 | 当前业务处理 |
| --- | --- |
| 正在起售 | 拒绝删除，需要先停售 |
| 已停售，但被某个套餐包含 | 拒绝删除，需要先处理套餐关联 |
| 已停售，且不被套餐使用 | 可以进入删除步骤 |

不是把状态改成0就已经删除了。停售仍保留菜品记录；删除才从菜品表移除记录。

### 12.2 套餐关联保存在哪里

套餐可能包含多道菜，同一道菜也可能被多个套餐使用。项目用 setmeal_dish 表记录这种包含关系，先理解两个编号即可：

| setmeal_id：套餐编号 | dish_id：菜品编号 |
| --- | --- |
| 20 | 100 |
| 20 | 101 |

示例表示套餐20包含菜品100和101。菜品100若被直接删除，套餐中就缺少对应商品，因此当前项目先拒绝删除。

检查代码：

```java
List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
if (setmealIds != null && setmealIds.size() > 0) {
    throw new DeletionNotAllowedException(
        MessageConstant.DISH_BE_RELATED_BY_SETMEAL
    );
}
```

查到任何套餐编号，就表示待删除菜品中存在被套餐使用的菜品。这里不只检查套餐当前是否起售，查询的是包含关系是否存在。

### 12.3 为什么口味跟着删除，套餐却阻止删除

口味配置是菜品的附属数据。例如宫保鸡丁已删除，它的辣度配置没有独立保留的用途，所以一并清理。

套餐是另一个独立售卖的商品。当前规则不允许因为删除其中一道菜就顺便删除整个套餐，而是先拒绝操作，让商家处理套餐关联。

因此，同样是关联数据，处理方式取决于业务关系，不能一概认为“有关联就全删”或“有关联就全部不能删”。

### 12.4 ids 与 IN

Controller 接收 List<Long> ids，可包含多个菜品编号。后端请求示例：

```text
DELETE /admin/dish?ids=100,101
```

SQL 使用 IN 表达匹配其中任意编号：

```sql
DELETE FROM dish WHERE id IN (100, 101);
DELETE FROM dish_flavor WHERE dish_id IN (100, 101);
```

第一条按菜品自己的 id 删除；第二条按口味所属菜品的 dish_id 删除，不能错用口味记录自己的 id。上述 SQL 为具体值示意，项目 XML 用 foreach 与参数绑定生成编号列表。

### 12.5 对照实际 Service 流程

```text
收到 ids
  → 循环查询每道菜，发现起售状态则抛出异常
  → 查询是否存在套餐包含这些菜品，存在则抛出异常
  → dishMapper.deleteByIds(ids)
  → dishFlavorMapper.deleteByDishIds(ids)
```

当前方法先完成全部状态检查，再执行删除。若100已停售但101仍在售，本次批量删除会被拒绝，不会先删100再跳过101。

两次删除的实际代码：

```java
dishMapper.deleteByIds(ids);
dishFlavorMapper.deleteByDishIds(ids);
```

`deleteBatch` 方法上已有 `@Transactional`。在事务正常生效且异常符合回滚规则时，两步删除共同提交；如果第二步失败，本事务中的第一步删除也回滚，避免只删掉菜品、遗留口味记录。

这里按当前代码解释先删菜品再删口味的顺序；若其他项目配置了限制性外键，删除顺序需配合数据库约束，不能把当前顺序直接当作所有项目的固定规则。当前代码还需要根据完整需求核对参数非空、菜品不存在等边界，本节没有修改业务实现。

Controller 在 Service 返回后清理相关缓存；缓存的用途与更新策略在后续 Redis 课程中讲解。删除数据库记录也不意味着 OSS 中的图片文件会自动删除。

小练习：菜品100已停售且没有套餐关联，删除它时口味表应该用 `id=100` 还是 `dish_id=100` 定位？答案：dish_id=100，因为口味记录自己的主键是另一个编号。

## 13. 编辑菜品：回显和替换口味列表

### 13.1 先查询详情，回显两部分信息

与编辑员工一样，先查询原数据，再修改并提交。但菜品详情还包含多条口味配置。当前接口为 `GET /admin/dish/{id}`。

Service 方法 getByIdWithFlavor 中：

```java
Dish dish = dishMapper.getById(id);
List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

DishVO dishVO = new DishVO();
BeanUtils.copyProperties(dish, dishVO);
dishVO.setFlavors(dishFlavors);
return dishVO;
```

第一条 Mapper 调用查菜品基本信息；第二条查 `dish_flavor WHERE dish_id = ...`，取得该菜品的口味列表。复制基本信息并把列表放进 VO，再作为一次接口响应返回。

这里是两次单表查询后由 Java 组装，不是上一节列表中那条 JOIN SQL。VO 可以承接联表结果，也可以由 Service 主动组织数据。单声明一个 flavors 属性不会自动查询口味，实际调用 Mapper 并设置属性才有数据。

### 13.2 保存时把口味作为完整列表替换

假设菜品100编辑前有“辣度、忌口”两组；用户编辑后希望有“辣度、分量”两组。前端提交当前完整配置，包括保留或改动后的辣度，以及新增的分量，不再提交已经删除的忌口。

当前更新接口为 `PUT /admin/dish`。DishDTO 提供原菜品 id 和基本字段，以及编辑后的 flavors 列表。

项目的 updateWithFlavor 主要执行：

```java
// 1. 修改原菜品的基本资料
dishMapper.update(dish);

// 2. 删除这道菜原来的所有口味配置
dishFlavorMapper.deleteByDishId(dishDTO.getId());

// 3. 给新的口味列表关联原菜品ID，非空时批量插入
```

第1步之前由 BeanUtils.copyProperties 把 DTO 对应属性复制到 Dish 对象；第3步与新增口味时相同，对每个对象设置 dishId，再 insertBatch。

本次菜品ID仍是100，不会删除并重新新增菜品，也不需要重新生成菜品ID。被重建的是它的口味配置记录。

```text
原口味：100 → 辣度、忌口
    ↓ 删除 dish_id=100 的旧口味
新口味：100 → 新的辣度、分量
```

#### 没看懂时，先只看这一份表单

暂时忽略 VO、SQL、事务。假设菜品100是宫保鸡丁，编辑前有“辣度、忌口”两组配置。

用户打开编辑页看到的是原来的菜品资料与这两组口味。用户把“忌口”移除，再加一组“分量”，最后编辑页上剩“辣度、分量”。前端应把这份完整的最终口味列表发给后端。

```text
编辑前：宫保鸡丁100 → [辣度、忌口]
编辑后：宫保鸡丁100 → [辣度、分量]
```

当前后端没有逐项比较两份列表，而是：

1. 菜品记录100继续保留，基本信息按提交值更新。
2. 清除100下面的旧口味配置（辣度、忌口）。
3. 保存100下面的新口味配置（辣度、分量）。

因此“辣度”也重新保存，不是把保留的一组留在旧记录中只追加“分量”。这属于整组替换实现，不代表编辑必须这样设计。

可以理解为更换一张完整的“口味选项单”：旧单写“辣度、忌口”，新单写“辣度、分量”，最后采用新单。菜品本身没有删除，它仍然叫宫保鸡丁，编号仍是100。

只提交“分量”，当前保存结果就只会有分量，不会自动保留辣度。要保留辣度，就需要在新列表中也带上辣度。若什么都不想变，仍提交原来完整的两组；空列表表示最终不留口味组。

这个示例只解释预期数据变化，事务保障及当前代码中未见更新事务声明的情况仍见13.5。学习反馈：用户对“保留菜品100，换成新的完整口味列表”回复“可以”，下一步对照代码巩固。

### 13.3 为什么先删旧配置，再插入新配置

编辑可能同时涉及修改、增加、删除口味组。若逐条更新，需要比较哪些保留、哪些修改、哪些新增、哪些删除。

对数量较少、作为菜品附属配置的口味，当前项目选择整组替换，让最终保存的数据直接对应编辑后的完整列表，避免遗漏已删除口味组。不是说编辑所有一对多数据都应该这样做；如果子记录ID被其他业务长期引用，或者记录量大，需另行设计。

如果只追加新列表、不删除旧配置，会残留用户已删除的“忌口”，还可能重复插入保留的“辣度”。删除 SQL 的限制条件是 `WHERE dish_id = 当前菜品ID`，不会有意清空其他菜品的口味。

口味记录自己的ID可能重新生成，所属菜品ID不变。两类编号不要混淆。

### 13.4 空列表和缺省值的含义

当前代码先删旧口味，再判断新列表不为 null 且非空才插入。所以 flavors=[] 会清空该菜品的口味配置，适用于明确想去掉全部口味的操作。当前传入 null 也会走到先删后跳过插入的路径，不能把它当成“保留旧口味”。

前端需要提交完整新配置；如果只发送新增的一组、漏掉应保留的组，当前后端会只保留新增的那组。这是整组替换接口的语义，不能与“只修改传入字段”的局部更新混为一谈。

### 13.5 这三步也需要事务

期望菜品修改、删除旧口味、插入新口味作为同一事务提交。否则如果旧口味已删、新口味插入失败，数据可能只修改一半。

实际阅读的 DishServiceImpl 中，新增和删除方法有 @Transactional，但 updateWithFlavor 方法及该类上未见同样注解；接口声明也未见相应事务注解。因此不能因为其他方法有注解，就默认这次更新自动拥有相同的事务保障。是否还有其他事务配置，需要进一步核对运行配置；本节仅记录所见，不修改代码、不声称已经复现问题。

如果后续补齐该方法事务，应覆盖完整更新流程，而非只覆盖最后一次 INSERT，并验证正常保存、清空口味、保存失败时旧配置保留等场景。

小练习：用户只把价格改成30元，辣度和忌口都应保留。提交 flavors 时应该带完整两组，还是空数组？答案：当前实现应带完整两组；空数组会清除配置。菜品ID仍是原ID，不需要新增一条菜品。

### 13.6 对照代码：更新菜品 → 清理旧口味 → 保存新口味

教学输入是编辑后提交的完整 DishDTO：id=100，菜名为宫保鸡丁，价格30元，flavors包含“辣度、分量”两组。后端不需要重新生成菜品ID，因为这次是编辑已有菜品。

**先准备给 Mapper 的菜品对象。**

```java
Dish dish = new Dish();
BeanUtils.copyProperties(dishDTO, dish);
dishMapper.update(dish);
```

new Dish() 创建内存对象，不会新增数据库记录。属性复制后，这个对象具有 DTO 中的 id=100、price=30 等对应字段。update 根据这个ID修改原来的数据库记录；调用 insert 才是另一种数据库操作。不要把 Java 创建对象与数据库 INSERT 混为一谈。

**再删除原菜品的旧口味。**

```java
dishFlavorMapper.deleteByDishId(dishDTO.getId());
```

先执行 getId() 取出100，再把100交给 Mapper。对应删除条件为 `dish_flavor WHERE dish_id = 100`，只清理该菜品的旧配置，不删除 dish 表的菜品100。这里的 getId() 是读取已经提交的ID，不是新增时的主键回填。

**拿到这次提交的新列表，设置所属菜品ID，再保存。**

```java
List<DishFlavor> flavors = dishDTO.getFlavors();
if (flavors != null && flavors.size() > 0) {
    flavors.forEach(dishFlavor -> {
        dishFlavor.setDishId(dishDTO.getId());
    });
    dishFlavorMapper.insertBatch(flavors);
}
```

getFlavors() 从请求 DTO 取列表，并不查询数据库。forEach 依次为“辣度”和“分量”对象设置 dishId=100；setter 是内存赋值，不是 SQL。循环结束后的 insertBatch 才执行新口味的数据库插入。

| 代码 | 对数据库的作用 |
| --- | --- |
| new Dish / copyProperties | 只准备内存数据 |
| dishMapper.update | 修改已有菜品记录100 |
| deleteByDishId | 删除该菜品旧的口味配置 |
| getFlavors / setDishId | 读取请求列表并补充对象属性 |
| insertBatch | 插入完整的新口味列表 |

新菜品需要先插入并回填ID；编辑已有菜品直接使用提交的原ID。两者都会给口味设置所属菜品编号，但编号来源不同。

本小节以实际方法为准，不在教学代码中虚构已有 @Transactional；当前更新方法的事务声明情况仍见13.5。此处展示调用职责与成功路径，没有实际执行数据库修改。

小练习：为什么 updateWithFlavor 中出现 new Dish() 却没有新增一份宫保鸡丁？答案：new创建的是Java对象；数据库操作是update，按已有ID修改原记录。

## 14. Day 4 套餐管理：套餐与菜品怎样关联

### 14.1 套餐是把已有菜品组合成一件商品

教学示例：宫保鸡丁28元、米饭2元、柠檬茶8元。商家可以创建“宫保鸡丁套餐”，包含三样商品各1份，套餐价另设为32元。套餐售价不要求等于单品原价之和，当前方案在套餐表中单独保存 price。

这里是在配置商家可售卖的套餐商品，不是在记录用户已经下了一笔订单。

```text
宫保鸡丁套餐：32元
├─ 宫保鸡丁 × 1
├─ 米饭 × 1
└─ 柠檬茶 × 1
```

### 14.2 三张表分别记录什么

菜品表 dish，以下编号和金额仅为本节示例：

| id | name | price |
| --- | --- | --- |
| 100 | 宫保鸡丁 | 28 |
| 200 | 米饭 | 2 |
| 300 | 柠檬茶 | 8 |

套餐表 setmeal 保存套餐自己的基本资料：

| id | name | price |
| --- | --- | --- |
| 20 | 宫保鸡丁套餐 | 32 |

关联表 setmeal_dish 保存套餐包含哪些菜、各几份。这里只展示主要关联列，实际实体还定义关联记录自己的 id、菜品名称 name 与菜品原价 price 等字段。

| setmeal_id | dish_id | copies |
| --- | --- | --- |
| 20 | 100 | 1 |
| 20 | 200 | 1 |
| 20 | 300 | 1 |

第一行读成：套餐20包含菜品100，共1份。setmeal_id 是套餐编号，dish_id 是菜品编号，copies 是份数。若米饭改成2份，关联中的 copies 改为2，不需要为米饭再创建一条新的菜品记录。

### 14.3 为什么有一张单独的关联表

一个套餐可以包含多道菜，同一道菜也能出现在多个套餐中。例如米饭既属于套餐20，也属于套餐21；关联表可以再加一行 `setmeal_id=21, dish_id=200, copies=1`。

两边都可以对应多个对象，叫多对多关系。现阶段先理解成“用一张明细表列出每个套餐里有哪些菜”即可。

创建套餐主要是在保存新的组合商品及其包含关系，不是在 dish 表里重新创建一份同名的宫保鸡丁、米饭或柠檬茶。不要把套餐关联表和菜品口味表混为一张表：前者记录包含哪些菜，后者记录一道菜的可选口味。

### 14.4 Setmeal 与 SetmealDish 不是同一个类型

| Java 类型 | 对应含义 |
| --- | --- |
| Setmeal | 套餐自己的资料，例如套餐名、售价、分类、图片 |
| SetmealDish | 套餐中的一条菜品配置，例如套餐20里的米饭200，共1份 |
| SetmealDTO | 接收套餐表单，包括套餐资料与 setmealDishes 列表 |

DTO 中的 `List<SetmealDish> setmealDishes` 可以类比此前 DishDTO 中的 flavors：整份表单既有主资料，也有多条关联明细。但本节只建立表结构概念，保存调用顺序留到下一节。

前面“被套餐使用的菜品不能删除”正是在查询 setmeal_dish 是否有包含该菜品的记录。

小练习：关联表一行 `setmeal_id=20, dish_id=200, copies=2` 表示什么？答案：套餐20包含菜品200，共2份，不是套餐编号200，也不是创建两道新菜。

### 14.5 新增套餐：代码执行顺序

仍用套餐价32元的“宫保鸡丁套餐”，包含已有菜品100、200、300各1份。前端把套餐基本资料和完整的套餐明细一起提交给后端，Controller 调用 `setmealService.saveWithDish(setmealDTO)`。

收到的 DTO 可理解为：

```text
setmealDTO
├─ name：宫保鸡丁套餐
├─ price：32
├─ categoryId、image等基本字段
└─ setmealDishes
   ├─ dishId=100，copies=1（以及名称、菜品原价等字段）
   ├─ dishId=200，copies=1
   └─ dishId=300，copies=1
```

新增时菜品已经存在，所以前端选菜后明细里能提供 dishId。但新的套餐尚未保存，最终的套餐ID还需要插入后取得。以上是教学成功路径，不代表实际方法已完成对前端价格、菜品有效性、份数和非空列表的全部校验。

**第一步：复制套餐基本字段并插入。**

```java
Setmeal setmeal = new Setmeal();
BeanUtils.copyProperties(setmealDTO, setmeal);
setmealMapper.insert(setmeal);
```

这一步操作 setmeal 表，不会把 setmealDishes 列表自动保存到关联表，也不会在 dish 表重新插入已有菜品。

**第二步：读取回填的套餐ID。**

```java
Long setmealId = setmeal.getId();
```

Mapper XML 的 insert 配置了 `useGeneratedKeys="true" keyProperty="id"`，配合数据库生成主键，把值填回 setmeal.id。假设生成的套餐ID是20，这里读到的就是20。不要误认为这里读取的是菜品编号。

**第三步：取得前端提交的菜品明细。**

```java
List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
```

List 中每个对象表示套餐包含的一项菜品。getter 取的是请求对象里的列表，不是重新查询菜品表。

**第四步：为每项明细补上新套餐编号。**

```java
setmealDishes.forEach(setmealDish -> {
    setmealDish.setSetmealId(setmealId);
});
```

已有 dishId 仍为100、200、300，各自保留。每个对象的 setmealId 都设置成20：

| setmealId | dishId | copies |
| --- | --- | --- |
| 20 | 100 | 1 |
| 20 | 200 | 1 |
| 20 | 300 | 1 |

setSetmealId 是赋值套餐ID，不是修改 dishId。setter 只修改Java对象，尚未把明细写入数据库。

**第五步：批量保存关联明细。**

```java
setmealDishMapper.insertBatch(setmealDishes);
```

实际 XML 用 foreach 生成多组 VALUES，写入 setmeal_dish 的 setmeal_id、dish_id、name、price、copies。这里存的是“套餐包含关系”，没有调用 dishMapper.insert，因此不会新建三道菜。

实际 saveWithDish 上已有 `@Transactional`，在配置与调用正常生效、异常符合回滚规则时，新增套餐资料和新增关联明细作为同一事务处理。若明细保存失败，不应仅留下没有配置成功的套餐。没有实际执行回滚测试。

对照已学过的菜品新增：先保存主资料并取得ID，再把这个ID补到多条附属明细，最后保存明细。但口味列表是 DishFlavor，套餐明细是 SetmealDish，含义与字段各不相同。

小练习：明细原来 dishId=200，新套餐ID为20，调用 setSetmealId(20) 后 dishId 变成多少？答案：仍然是200，新增/补充的是 setmealId=20。

### 14.6 套餐起售、停售：先检查包含的菜品

套餐保存下来与允许顾客购买是不同状态。项目用 setmeal.status 表示套餐起售/停售，dish.status 表示每道菜自己的起售/停售：1为起售，0为停售。

例：套餐20包含宫保鸡丁100、米饭200、柠檬茶300。如果柠檬茶已经停售，当前业务不允许把该套餐起售。套餐是组合商品，不能仅改变套餐状态就忽略其中菜品的状态。

后端接口：

```text
POST /admin/setmeal/status/1?id=20  起售套餐20
POST /admin/setmeal/status/0?id=20  停售套餐20
```

status 是本次想改成的套餐目标状态，id 是套餐编号。它不是套餐内某道菜的编号，也不是套餐原来状态的自动切换指令。

Service 的逻辑：

```text
目标状态是1（起售）？
  是 → 查询套餐包含的菜品
       → 逐个检查当前状态
       → 发现任意停售菜品：抛出异常，停止，不执行套餐更新
       → 检查通过：继续更新套餐状态
  否（正常请求中为0，停售）→ 跳过起售检查，继续更新套餐状态
```

实际代码在 `if (status == StatusConstant.ENABLE)` 中调用：

```java
List<Dish> dishList = dishMapper.getBySetmealId(id);
```

此处传入的是套餐ID20，查询返回套餐关联的菜品对象列表，不是返回套餐对象，也不是返回全店所有菜品。

对应注解中的 SQL 整理为：

```sql
SELECT a.*
FROM dish a
LEFT JOIN setmeal_dish b ON a.id = b.dish_id
WHERE b.setmeal_id = #{setmealId};
```

关联表 b 限定套餐20，使用其中 dish_id 与菜品 a.id 匹配，再取出菜品记录，从而能检查实时的 dish.status。查询没有直接从关联表推断菜品状态。

项目在列表非空时逐个检查：

```java
dishList.forEach(dish -> {
    if (StatusConstant.DISABLE == dish.getStatus()) {
        throw new SetmealEnableFailedException(
            MessageConstant.SETMEAL_ENABLE_FAILED
        );
    }
});
```

只要其中一次抛出异常，方法后面的套餐更新就不会执行。不需要先把套餐起售再尝试撤销。

检查后，或正常停售时，构造仅携带ID和目标状态的套餐对象：

```java
Setmeal setmeal = Setmeal.builder()
        .id(id)
        .status(status)
        .build();
setmealMapper.update(setmeal);
```

起售成功的主要更新效果是 `UPDATE setmeal SET status=1 WHERE id=20`；停售对应0。这里只改变套餐状态，没有把所有关联菜品状态一起改变，也不删除套餐、菜品或关联记录。

注意当前方法的实际边界：仅当查询列表非空时检查其中菜品；不能据此断言已经校验空套餐、缺失菜品、非法status等全部情况。这个检查发生在起售请求处理时，也不等于系统已实现“之后任何菜品停售都会自动同步套餐”的持续约束。本节说明现有代码，没有修改实现。

小练习：请求 status=0，套餐中含有停售菜品，本方法会因该菜品停售而拒绝吗？答案：正常情况下不会，这个菜品状态检查仅在请求起售时执行。套餐停售也不会把单卖的米饭一起停售。

### 14.7 编辑套餐：换的是包含关系，不是删除原菜品

教学场景：套餐20原价32元，包含宫保鸡丁100、米饭200、柠檬茶300各1份。编辑后价格35元，保留宫保鸡丁、把米饭改成2份、将饮品换为已经存在的酸梅汤400。

```text
原套餐20：32元，100×1、200×1、300×1
新套餐20：35元，100×1、200×2、400×1
```

#### 先回显套餐基本信息和明细

getByIdWithDish 执行：

```java
Setmeal setmeal = setmealMapper.getById(id);
List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

SetmealVO setmealVO = new SetmealVO();
BeanUtils.copyProperties(setmeal, setmealVO);
setmealVO.setSetmealDishes(setmealDishes);
return setmealVO;
```

第一次查询 setmeal 的套餐资料；第二次查询 `setmeal_dish WHERE setmeal_id=20` 的包含明细；Java 组装 VO 后一次返回。前端据此展示原价格、已选菜品、份数等字段。这与菜品回显时组合基本信息和口味列表的思路相同，但列表内容不同。

#### 保存三步

Service 的 update 方法带有 @Transactional。主要顺序为：

```java
// 1. 准备并更新套餐基本信息
Setmeal setmeal = new Setmeal();
BeanUtils.copyProperties(setmealDTO, setmeal);
setmealMapper.update(setmeal);

// 2. 删除这个套餐原来的包含关系
Long setmealId = setmealDTO.getId();
setmealDishMapper.deleteBySetmealId(setmealId);

// 3. 为完整新明细设置同一个套餐ID，再插入
List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
setmealDishes.forEach(setmealDish -> {
    setmealDish.setSetmealId(setmealId);
});
setmealDishMapper.insertBatch(setmealDishes);
```

本次 setmealId 直接读取原来的20，不是创建新套餐，也不是再次生成主键。

第二步 SQL 是：

```sql
DELETE FROM setmeal_dish WHERE setmeal_id = 20;
```

它删除的是关联表中的旧组合记录，不是删除 setmeal 套餐表，也不是从 dish 菜品表删除宫保鸡丁、米饭、柠檬茶。其他套餐与这些菜品的关联也不会因为限定 setmeal_id=20 的清理而自动消失。

第三步保存的新组合：

| setmeal_id | dish_id | copies |
| --- | --- | --- |
| 20 | 100 | 1 |
| 20 | 200 | 2 |
| 20 | 400 | 1 |

结果是套餐20不再包含柠檬茶，但柠檬茶300仍是已有菜品，可以单独卖，也可以继续被其他套餐使用。copies=2 描述这份套餐含2份米饭，不是把单卖米饭的某个属性统一改成2。

#### 完整列表与事务

当前采用全量替换关系，前端应提交完整最终列表。只发酸梅汤一条，当前保存逻辑不会自动保留宫保鸡丁和米饭的关联；需要保留的明细也应随表单提交。

与前面菜品编辑不同，当前套餐更新代码直接对列表 forEach 并调用 insertBatch，没有同样的非空分支。不能照搬“空列表就成功清空”的结论，缺省、null、空套餐的允许规则与实现需单独核对。本节没有修改业务校验。

在事务配置和异常规则正常生效时，更新套餐、删除旧关系、插入新关系共同提交；中途需要回滚的失败应撤销本次数据库修改。仅有页面显示失败，不能当作回滚已经验证。本节没有实际执行编辑或故障测试。

小练习：将套餐20里的柠檬茶换成酸梅汤，是否需要删除 dish 表中的柠檬茶300？答案：不需要，移除的是套餐20与菜品300的包含关系。

### 14.8 删除套餐：删套餐和关联记录，保留已有菜品

例：套餐20包含菜品100、200、300。商家不再销售这个组合，希望删除套餐20。项目规则：起售中的套餐先拒绝删除，只有检查通过才执行删除。

Service 中分成两个循环：

```java
@Transactional
public void deleteBatch(List<Long> ids) {
    ids.forEach(id -> {
        Setmeal setmeal = setmealMapper.getById(id);
        if (StatusConstant.ENABLE == setmeal.getStatus()) {
            throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
        }
    });

    ids.forEach(setmealId -> {
        setmealMapper.deleteById(setmealId);
        setmealDishMapper.deleteBySetmealId(setmealId);
    });
}
```

第一个循环只检查，不删除。如果选中20、21两个套餐，而21仍然起售，会在检查阶段拒绝整次操作，尚未进入第二个删除循环。

第二个循环处理每个套餐的两项删除。套餐20对应：

```sql
DELETE FROM setmeal WHERE id = 20;
DELETE FROM setmeal_dish WHERE setmeal_id = 20;
```

第一条删套餐自己的资料，第二条删它原来的包含明细。没有执行 `DELETE FROM dish`，因此宫保鸡丁100、米饭200、柠檬茶300仍在菜品表里，其他套餐可继续使用。

| 数据 | 删除套餐20后的结果 |
| --- | --- |
| setmeal 中套餐20那一行 | 删除 |
| setmeal_dish 中 setmeal_id=20 的明细 | 删除 |
| dish 中已有菜品100、200、300 | 不因本次套餐删除而删除 |
| 其他套餐及其关联记录 | 不因限定套餐20的删除而删除 |

编辑套餐也清理旧关联，但随后写入新关联并保留套餐本身；删除套餐则连同套餐资料移除，也不再插入新明细。

当前 deleteBatch 上已有 @Transactional，在事务与异常规则正常生效时，多个套餐及其明细的删除属于同一个事务，避免只完成部分删除。按当前代码顺序先删套餐主记录再删明细；其他项目若采用限制性外键，需匹配其约束配置，不能不看表结构照搬顺序。

当前后端接口 `DELETE /admin/setmeal` 使用 `@RequestBody List<Long> ids`，请求正文示例是 `[20, 21]`。这与菜品删除接口用查询参数接收 ids 的写法不同，前端必须按实际接口约定提交。

“批量删除”描述一次可选择多个套餐，不保证只发一条 SQL。当前实现对每个编号循环查询和删除；前面的菜品批量删除使用 IN 列表，两者实现方式不同。

本节只解释当前业务主线，没有实际执行删除或异常实验；参数空列表、不存在的套餐等边界仍需要具体校验，不能仅凭代码有事务就认定业务验证完整。Controller 还有缓存清理，后续 Redis 阶段再讲。

小练习：套餐20与套餐21都含米饭200，删除套餐20后，是否应该把米饭200删掉？答案：不应该；移除套餐20及其明细，不影响已有菜品200和套餐21的包含关系。

### 14.9 套餐管理阶段复习

| 功能 | 核心数据库处理与规则 |
| --- | --- |
| 新增 | 插入套餐，回填套餐ID，再插入菜品关联明细 |
| 查询回显 | 查套餐与关联明细，组装VO |
| 编辑 | 更新套餐资料，清理旧关联，保存完整新关联；已有菜品仍保留 |
| 起售 | 查询包含的菜品，发现停售菜品则拒绝，否则更新套餐状态 |
| 停售 | 更新套餐状态，不把所有单品一起停售 |
| 删除 | 检查套餐状态，删除套餐与其关联，保留原来的菜品 |

让 AI 实现或修改这些功能时，应明确“变的是主资料、关联关系还是商品状态”，并检查实际 SQL 操作了哪张表、WHERE 用哪个编号、事务是否覆盖完整业务。页面上一个按钮不能代替这些数据库和业务决策。

## 15. Day 5：店铺营业状态与 Redis 入门

### 15.1 Redis 先理解为另一个数据存储服务

MySQL 存员工、分类、菜品、套餐等表。Redis 是另一个独立运行的数据存储服务，主要在内存中处理数据，常用于频繁读取的简单状态或缓存。它不是 Java 的普通变量，也不是一份 XML 配置文件。

先不展开内存、持久化、各种数据类型的底层细节。本项目的营业状态可以用一个键值对表示，不需要先设计一张营业状态表。

### 15.2 key 与 value

可以把本例理解成按名称存取的一条数据：

| key：这项数据的名字 | value：这项数据的值 |
| --- | --- |
| SHOP_STATUS | 1 |

key 用于定位数据，value 是对应的内容。当前项目约定1为营业、0为打烊；Redis 本身不认识这两个数字的业务含义，是应用代码规定的。

```text
SHOP_STATUS → 1：营业中
SHOP_STATUS → 0：打烊
```

以上是逻辑示意，不是让用户直接用 redis-cli 写字符串数字来替换项目序列化的 Integer。当前 RedisTemplate 配置显式使用字符串序列化 key，value 的实际编码还受其序列化器影响，不要求本节掌握。

### 15.3 管理员设置状态

后端接口：`PUT /admin/shop/1` 设置营业，`PUT /admin/shop/0` 设置打烊。Controller 中实际写入：

```java
redisTemplate.opsForValue().set(CacheConstant.SHOP_STATUS, status);
```

分开看：RedisTemplate 是 Java 访问 Redis 的工具；opsForValue() 取得操作 Redis 字符串类型值的入口；set(key, value) 写入键对应的值。本项目通过模板序列化保存 Java 的 Integer 对象，不要把这个对象类型与 Redis 底层类型混为一谈。

CacheConstant.SHOP_STATUS 是项目常量，其字符串内容为 SHOP_STATUS。调用 set 设置同一个 key 的新值，会覆盖该键原来的值，不是为每次开关店新建一条历史记录。

### 15.4 两端读取同一份状态

管理端查询接口 `GET /admin/shop/status`，用户端查询接口 `GET /user/shop/status`。两边使用相同的 Redis key，而不是各在浏览器里单独保存状态。

```java
Integer status = (Integer) redisTemplate.opsForValue()
        .get(CacheConstant.SHOP_STATUS);
```

get(key) 按名字读取对应值；当前代码将模板返回值转换成 Integer。读取不代表检查 MySQL 店铺表，本例的两个 Controller 都直接访问 Redis。

```text
管理员点击营业
    → 后端把 SHOP_STATUS 写成1
    → 顾客页面请求查询状态
    → 后端从同一个 key 读取1
    → 返回前端，页面显示营业中
```

这不代表商家修改后所有已打开页面都会自动收到通知，顾客页面需要再次查询或有相应刷新机制；此处没有用 WebSocket 实时推送。

### 15.5 key 不存在时怎么办

当前两个 Controller 都处理 null：

```java
if (status == null) {
    status = StatusConstant.DISABLE;
    redisTemplate.opsForValue().set(CacheConstant.SHOP_STATUS, status);
}
return Result.success(status);
```

即缺少这项值时，按打烊0处理，并写回 Redis。这是本项目的默认业务选择，不是 Redis 自动生成了0。null 表示没有读到这项数据，与明确保存的数字0不同。

Redis key 不存在与 Redis 服务连接失败也不同：前者可以正常读出 null，后者可能抛出连接异常，不能把这段 null 判断解释为已经处理全部服务故障。

### 15.6 为什么这里使用 Redis

营业状态很小，管理端修改、用户端可能频繁读取，用键值存取很直接。也可以使用 MySQL 保存，这不是业务只能使用 Redis 的强制要求。

本例是直接用 Redis 存状态，没有“Redis没有就从MySQL查店铺状态”的流程。后续菜品缓存会讲另一种流程：MySQL 保存业务记录，Redis 保存可复用的查询结果。不能把 Redis 所有使用场景都等同于缓存副本。

Redis 可以配置持久化，不要简单记成重启必然清空；也不要因能持久化就认为它对任何数据都有与关系数据库完全相同的保障。当前先学会具体业务的数据存放位置与读写路径。

当前项目只用一个固定SHOP_STATUS键代表全店状态；多店铺场景需按店铺区分键，是后续设计问题。方法没有在本节展示完整输入合法值校验，不能把任意整数自动视为合法状态。

小练习：管理员写入 SHOP_STATUS=0，顾客查询同一个key得到什么？答案：0，页面按项目约定显示打烊。只修改前端文字、没有发请求写入 Redis，会改变服务端状态吗？不会。

### 15.7 Redis、RedisTemplate 与 key 分别是什么

| 名称 | 是什么 | 本例的职责 |
| --- | --- | --- |
| Redis | 独立运行的数据存储服务 | 实际保存SHOP_STATUS对应的值 |
| RedisTemplate | Java应用中的访问工具对象 | 把代码中的读写操作交给Redis |
| SHOP_STATUS | 一个字符串形式的数据名称，即key | 指定要读写哪项数据 |

```text
Controller中的Java代码
    → 使用RedisTemplate
    → 通过连接访问Redis服务
    → 读写SHOP_STATUS对应的值
```

创建RedisTemplate对象不会自动安装或启动Redis，模板不是Redis数据库本身。Redis没启动或连接配置有误时，不能靠new一个模板让数据自动保存成功。

#### 把链式调用拆开看

```java
redisTemplate.opsForValue().set(CacheConstant.SHOP_STATUS, status);
```

opsForValue() 先提供字符串类型值的操作接口，set再执行具体写入。用分两行的方式理解，节选需相应导入ValueOperations：

```java
ValueOperations operations = redisTemplate.opsForValue();
operations.set(CacheConstant.SHOP_STATUS, status);
```

这是对现有原始类型RedisTemplate用法的解释，不是本节修改实现或推荐所有新代码都省略泛型。单独调用opsForValue()不会读取或写入SHOP_STATUS；具体操作由后面的get或set触发。

opsForValue是入口方法名，不是数据库key；SHOP_STATUS才是传给get/set定位数据的key。

#### RedisTemplate从哪里来

当前 RedisConfiguration 通过@Bean声明创建模板：

```java
@Bean
public RedisTemplate redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate template = new RedisTemplate();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    return template;
}
```

这里为便于阅读调整了局部变量名。连接工厂提供访问Redis的连接能力；key序列化器规定如何把Java中的key编码成Redis使用的数据。new模板只是创建访问工具，不是创建一台Redis服务。

Spring根据配置创建并管理模板对象。Controller通过：

```java
@Autowired
private RedisTemplate redisTemplate;
```

取得Spring管理的模板，因此每次处理请求时不必自己创建一套工具。@Autowired不是从Redis里读取数据；真正读取仍需get调用。

### 15.8 读取结果与最基本的排错

当前代码使用原始类型RedisTemplate，读取结果按Object处理，所以写了显式转换：

```java
Integer status = (Integer) redisTemplate.opsForValue()
        .get(CacheConstant.SHOP_STATUS);
```

可以读成：按key读取值，将返回对象当作Integer使用，再赋给status。这不是把任意字符串“1”解析成整数；存取的数据类型和序列化方式需要配套。不存在时可以得到null，随后走业务默认打烊逻辑。

该配置只显式设置了key序列化，没有把value配置成字符串序列化。不要据此假定Java写入的Integer与手工redis-cli SET写入字符串完全相同；具体序列化细节下一阶段按需展开。

| 现象 | 先检查什么 |
| --- | --- |
| 读取正常但值是null | key是否写入、读写key是否一致、是否连接同一实例和逻辑库 |
| Redis连接异常 | 服务是否启动、地址端口和认证配置、网络连接 |
| 值类型转换或反序列化失败 | 写入方类型与序列化器是否和读取方配套 |

连接失败不是“key不存在”。Controller的null判断不能自动修复连接错误。本节只解释代码，没有启动Redis或执行测试。

小练习：只写@Autowired RedisTemplate而不调用get，是否已经读取营业状态？答案：没有，取得的是操作工具。只调用opsForValue()是否已写入值？也没有，具体写入要执行set。

## 16. 用 AI 开发时，自己负责什么

### 16.1 描述一个可验收的需求

说明功能、已有接口、修改范围、业务规则和验证场景。例如：

```text
完善当前员工分页查询，沿用现有技术栈。
按姓名模糊搜索，搜索条件变化时回到第1页。
后端限制分页参数，明确缺省值和最大条数。
响应只包含页面需要的员工字段。
前端处理加载、空结果和失败，旧搜索结果不能覆盖新搜索。
保留登录校验，先检查已有实现，再补齐缺失部分。
完成后说明改动，并验证正常、空数据、失败和连续搜索场景。
```

### 16.2 验收比“代码能运行”多一步

| 功能 | 核心验证 |
| --- | --- |
| 登录 | 正确密码成功，错误密码失败，无有效凭证不能访问受保护接口 |
| 分页 | 第一页、最后一页、空结果、搜索后翻页，total 与 records 含义正确 |
| 编辑 | 正确回显、保留ID、取消不保存、保存后重新查询确认 |
| 状态修改 | 目标员工与目标状态正确，失败不能假装成功 |
| 删除分类 | 有关联拒绝，无关联允许，失败原因能显示 |
| 自动填充 | 操作人正确，更新时间变化，创建信息保留 |

前端藏按钮不代替后端权限检查；页面显示新值不证明数据库保存成功；AI 声称完成不代替实际验证。Git 查看改动和回退、日志排错、测试与部署将随后续业务继续学习。

## 17. 自测与答案

先自行回答，再展开答案。

1. 符合条件的员工共有47名，每页10名，查第4页：offset、total、本页条数、总页数、最后一页条数分别是多少？
2. `PageHelper.startPage(2, 10)` 是否已经查询了数据库？`new PageResult(...)` 会不会再查询？
3. MyBatis 是不是 XML 文件？通过什么把 Mapper 方法和 XML SQL 对应起来？
4. `name != null` 是否能判断用户改过姓名？能否拒绝空字符串？
5. 菜品编号101、category_id=1，两个数字分别表示什么？
6. 删除分类时先统计菜品和套餐数量，这是否使用了 JOIN？
7. 管理员1修改员工8，updateUser 应该是1还是8？
8. 为什么 Service 没写 setUpdateTime，数据库仍可能保存了修改时间？

<details>
<summary>展开答案</summary>

1. offset=30，`LIMIT 30, 10`；total=47；本页10条；共5页；最后一页7条。
2. startPage 设置要求，Mapper 调用触发查询；PageResult 只是结果包装。
3. MyBatis 是框架。XML 的 namespace 对应接口完整名称，select/update 等语句的 id 对应方法名。
4. 都不能。原值非 null 也会更新；空字符串也不是 null，需要另行校验。
5. 101 是菜品自己的编号，1 是所属分类编号。
6. 没有，是多次单表操作；业务涉及多张表不等于使用 JOIN。
7. 1，表示操作人；WHERE 中用于定位被修改员工的 id 是8。
8. AOP 在 Mapper 执行前给同一实体补充字段，Mapper SQL 再把字段写入数据库。

</details>

## 课程顺序与资料

按黑马官方大纲组织；以下是后续顺序，不代表已完成。

| 顺序 | 主题 |
| --- | --- |
| Day 1 | 项目介绍、环境搭建 |
| Day 2 | 员工、分类管理 |
| Day 3 | 菜品管理，包括公共字段自动填充等 |
| Day 4 | 套餐管理实战 |
| Day 5 | 店铺营业状态 |
| Day 6 | 微信登录、商品浏览 |
| Day 7 | 商品缓存、购物车 |
| Day 8 | 下单、支付 |
| Day 9 | 历史订单、订单管理实战 |
| Day 10 | 定时处理订单、来单提醒、催单 |
| Day 11 | 图形报表 |
| Day 12 | Excel 报表 |

[黑马官方课程大纲](https://xa.itheima.com/news/20230706/181545.html)。本项目增加的 AI 客服、知识库和慢 SQL 监控放在业务基础之后；当前客服主要是本地规则和关键词识别，不应直接等同于已经完成大模型工具调用。

### 对照项目代码

- [员工接口 Controller](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/controller/admin/EmployeeController.java)
- [员工业务 Service](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/service/impl/EmployeeServiceImpl.java)
- [员工 Mapper 接口](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/mapper/EmployeeMapper.java)
- [员工 Mapper XML](D:/sky-take-out/sky-take-out/sky-server/src/main/resources/mapper/EmployeeMapper.xml)
- [分页参数 DTO](D:/sky-take-out/sky-take-out/sky-pojo/src/main/java/com/sky/dto/EmployeePageQueryDTO.java)
- [分页返回对象](D:/sky-take-out/sky-take-out/sky-common/src/main/java/com/sky/result/PageResult.java)
- [已有前端接口练习](D:/sky-take-out/sky-take-out/frontend-learning/05-components/src/components/LoginPanel.vue)
- [分类业务 Service](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/service/impl/CategoryServiceImpl.java)
- [自动填充注解](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/annotation/AutoFill.java)
- [自动填充切面](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/aspect/AutoFillAspect.java)
- [BaseContext](D:/sky-take-out/sky-take-out/sky-common/src/main/java/com/sky/context/BaseContext.java)
- [管理端登录校验](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/interceptor/JwtTokenAdminInterceptor.java)
- [全局异常处理](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/handler/GlobalExceptionHandler.java)
- [文件上传接口](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/controller/admin/CommonController.java)
- [OSS 上传工具](D:/sky-take-out/sky-take-out/sky-common/src/main/java/com/sky/utils/AliOssUtil.java)
- [菜品提交参数](D:/sky-take-out/sky-take-out/sky-pojo/src/main/java/com/sky/dto/DishDTO.java)
- [菜品保存业务](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/service/impl/DishServiceImpl.java)
- [菜品插入与主键回填](D:/sky-take-out/sky-take-out/sky-server/src/main/resources/mapper/DishMapper.xml)
- [口味实体](D:/sky-take-out/sky-take-out/sky-pojo/src/main/java/com/sky/entity/DishFlavor.java)
- [口味批量插入](D:/sky-take-out/sky-take-out/sky-server/src/main/resources/mapper/DishFlavorMapper.xml)
- [菜品展示对象 DishVO](D:/sky-take-out/sky-take-out/sky-pojo/src/main/java/com/sky/vo/DishVO.java)
- [菜品分页参数](D:/sky-take-out/sky-take-out/sky-pojo/src/main/java/com/sky/dto/DishPageQueryDTO.java)
- [套餐与菜品的关联查询](D:/sky-take-out/sky-take-out/sky-server/src/main/resources/mapper/SetmealDishMapper.xml)
- [套餐实体](D:/sky-take-out/sky-take-out/sky-pojo/src/main/java/com/sky/entity/Setmeal.java)
- [套餐菜品关联实体](D:/sky-take-out/sky-take-out/sky-pojo/src/main/java/com/sky/entity/SetmealDish.java)
- [套餐提交参数](D:/sky-take-out/sky-take-out/sky-pojo/src/main/java/com/sky/dto/SetmealDTO.java)
- [管理端店铺状态](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/controller/admin/ShopController.java)
- [用户端店铺状态](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/controller/user/ShopController.java)
- [Redis 模板配置](D:/sky-take-out/sky-take-out/sky-server/src/main/java/com/sky/config/RedisConfiguration.java)
- [缓存键常量](D:/sky-take-out/sky-take-out/sky-common/src/main/java/com/sky/constant/CacheConstant.java)

### 已有学习资料

- [员工分页接口文档](D:/sky-take-out/sky-take-out/docs/learning/01-employee-page-api.md)
- [前端学习路线](D:/sky-take-out/sky-take-out/docs/learning/02-frontend-learning-path.md)
