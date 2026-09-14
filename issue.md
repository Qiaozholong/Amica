# Amica 待改问题清单（致下次的我）

> 来源：2026 年代码走查 + 前端测试控制台（`frontend/`）联调发现的问题。
> 改的时候对照本清单逐条勾选；**修完一条就移到 `finish.md`**，本文件只留未完成的。
> 优先级：P1 会让前端/用户行为明显异常，优先修；P2 健壮性与语义；P3 后续增强。
> 相关文件：`finish.md`（已修正归档）、`todo.md`（路线图待办）。

---

## P1 - 逻辑/行为异常

### 2. 会话「已覆盖」判定前后不一致 ❓
- **位置**：`ConversationServiceImpl.java` L52（创建时 `systemPrompt == null` → "已覆盖"）对比 `ChatServiceImpl.java` L63（聊天链路 `isBlank()` 才回退）
- **现象**：前端传空字符串 `""` 时，创建返回"已覆盖"，聊天却实际回退到了助手 prompt。前端易踩（表单空值常是 `""` 而非 `null`）。
- **建议**：统一用 `isBlank()`（多余空格也视为未覆盖），或统一 `null` 判。前端同步注意。

### 3. `maxtokens` 的哨兵值用法和命名 ❓
- **位置**：`MessagesDto.java` L10（`int maxtokens`）、`ChatServiceImpl.java` L46（`!= 0 ? x : 1024`）
- **现象**：① 无法显式请求 0 token；② 字段名 `maxtokens` 非驼峰，前端拼成 `maxTokens` 会被 Jackson 静默忽略（默认配置不报错），查错半天。
- **建议**：改用 `Integer maxTokens` + 判空；命名改 `maxTokens`（改后要同步前端「会话与对话」页和 `frontend/README.md` 里的说明）。

### 4. Chat 链路无判空，全链 NPE ❓
- **位置**：`ChatServiceImpl.java` L48-52（`conv / assistant / model / providerEntity` 逐层 getById 后直接取属性）
- **现象**：会话/助手/模型/提供商任一环不存在（如引用了已删除的 ID），直接 NPE → 全局兜底 500「土豆炸啦！？」，前端不知道到底哪一环断了。
- **建议**：每层查到 null 时抛带明确语义的 `BusinessException`（"会话不存在"「助手不存在」「模型不存在」「提供商不存在」）。

### 5. 用户消息先落库，provider 失败不回滚 ❓
- **位置**：`ChatServiceImpl.java` L89-94（先 `save(userMsg)`）→ L96-97（provider.chat 可能抛异常）→ L99-104（assistant 消息才落库）
- **现象**：API Key 配错/限流/超时等失败时，用户消息已入库；下次发消息会把这条"没被回答的"消息带进上下文，用户感到"我明明失败了，它却记住了"。
- **建议**：三选一——①整个方法加 `@Transactional`（provider 调用失败即回滚用户消息，代价：长事务）；②把落库挪到 provider 成功之后；③失败时给消息打标记/落一条错误记录（保留现场，推荐）。同时把 `save` 改为批量落库（用户+助手消息一起存）。

### 6. 未配 API Key 时报错不明确 ❓
- **位置**：`OpenAiProvider.java` L29（构造时 `decrypt(provider.getApiKey())`，null → 抛异常），`ProviderServiceImpl.GetApiKey()` 同理
- **现象**：注册了模型但没配 Key 就发消息 → 笼统 500「土豆炸啦！？」。
- **建议**：发送前（或 ProviderFactory）校验，按情况抛「提供商未配置 API Key」。

### 21. `maxTokens` 默认 1024 过小 → 思考模型回复正文为空 ❓已定位，未修
- **位置**：`ChatServiceImpl.java` L60（`dto.getMaxtokens() != 0 ? dto.getMaxtokens() : 1024`）
- **现象**：AI 回复的正文为空。思考模型会**先把输出预算花在 reasoning 上**，1024 被思考链烧完后，轮到正文就没有余量了（`finish_reason` 大概率是 `"length"`）。
- **验证**：前端把 `maxtokens` 手动填 **4096/8192** 再发一条，正文即正常 → **已确认是此因**。
- **建议**：默认值提到 4096+；或改 `Integer maxTokens`、判空后**不传**该字段（交给服务端默认）。与问题 3 的命名/哨兵值一起改最省事。

### 22. 正文解析未兜底 `reasoning_content`、未读 `finish_reason` ❓未修
- **位置**：`OpenAiProvider.java` L80-87（`parse()` 只取 `choices[0].message.content`）
- **现象**：思考模型可能只在 `reasoning_content` 里给内容、`content` 为空 → `asText()` 对缺失字段返回 `""` → 空串被落库（`messages.content` 是 `TEXT NOT NULL`，**空串能过、null 过不了**）→ 下一轮把 `{"role":"assistant","content":""}` 当历史发回去 → 被 API 拒绝。
- **建议**：① `content` 为空时回退 `reasoning_content`；② 读出 `finish_reason`（`"length"` 是"被 max_tokens 截断"的铁证，目前完全没看，丢了最有用的诊断字段）；③ 拼历史时跳过 `content` 为空的消息。
- **备注**：与问题 21 是同一现象的两面——21 是"预算不够"，22 是"解析层没兜住"。

---

## P2 - 接口语义 / 健壮性 / 安全

### 8. `ProviderFactory` 默认分支抛 401 ❓
- **位置**：`ProviderFactory.java` L21
- **现象**：不支持的协议抛 `BusinessException(401, "xxx")`，401 语义是"未授权"，且 message 只给协议名，像"401 openai2"这种输出。
- **建议**：`400` 或 `500` + `"不支持的协议: xxx"`。

### 9. `model_id` 全表唯一 + `provider.name` 被固定为 protocol ❓
- **位置**：`sql/init.sql` L39（`UNIQUE KEY uk_model_id`）、`ProviderServiceImpl.java` L44（`provider.setName(dto.getProtocol())`）
- **现象**：① 不同提供商想注册同名模型（如两个端点都有 `deepseek-chat`）会被"模型已存在"拦下；② 提供商显示名永远等于协议名，`name` 字段没有实际意义。
- **建议**：唯一键改为 `(provider_id, model_id)`；提供商名由 `ModelDto` 透传或单独接口维护。

### 13. `stream=true` 会解析失败（做了 options 后必踩）❓
- **位置**：`OpenAiProvider.java` L79-86（parse 按普通 JSON 解析），L75 却会把 `stream` 写进请求体
- **现象**：一旦 options 生效且前端勾选 stream，响应是 SSE 流，`mapper.readTree` 直接解析失败。
- **建议**：要么 options 里先别放开 stream，要么实现 SSE 流式解析（plan 分批；HTTPClient 用 BodyHandlers.ofInputStream 边读边吐）。

### 14. 并发下 `seq` 可能重复 ❓
- **位置**：`ChatServiceImpl.java` L87（`history.last.seq + 1` 计算 nextSeq）、`sql/init.sql` L80（索引非唯一）
- **现象**：同一会话并发两条请求会算出相同 seq，排序乱序/覆盖。
- **建议**：`UNIQUE KEY uk_conv_seq (conversation_id, seq)`（README 路线图已列）+ 冲突重试；或改为对 conversation 加行锁。

### 15. 全局异常处理可读性 ❓
- **位置**：`GlobalExceptionHandler.java` L18（业务异常日志只有"业务处理错误"，无异常内容）、L53（兜底文案"土豆炸啦！？"）
- **现象**：排查时日志信息不足；兜底文案让前端误以为"后端炸了"，实际可能是参数问题。
- **建议**：日志带上 `e.getMessage()`（或异常栈）；兜底返回带真实错误摘要（内部细节可另传 header/仅日志）。

### 20. JWT 过滤器：`extractUserId` 在 try/catch 之外，该报 401 的会被漏成 500 ❓未修（倾向方案 B）
- **位置**：`Config/JwtAuthenticationFilter.java` L44-50（`validateToken` 在 try 内，`extractUserId` 在 try 外）
- **现象**：`validateToken` 与 `extractUserId` 各自解析一次 token，而 try/catch 只罩住第一次；第二次抛出的运行时异常（`RequiredTypeException` / `NullPointerException`）没人接，直接冒出 `doFilter`。
  - **后果**：Filter 跑在 `DispatcherServlet` **之前**，`GlobalExceptionHandler`（`@RestControllerAdvice`）**接不到 Filter 里的异常** → 客户端拿到的是容器默认 500（不是 `Result` JSON），**该报 401 的场景被漏成 500**。
  - **触发条件**（签名与过期都正常时）：payload 缺 `userId` → `claims.get(...)` 返回 `null` → 赋给 `long` 拆箱 NPE；或 `userId` 非数字 → `RequiredTypeException`。（另：`claims.get("userId", long.class)` 传基本类型 Class 必抛，已改 `Long.class`。）
- **建议**（倾向**方案 B**）：让 `JwtUtil` 暴露 `public Claims parse(String token)`，Filter 内一次解析，验签 + 取值放同一个 try：
  ```java
  try {
      Claims claims = jwtUtil.parse(token);
      req.setAttribute("userId", claims.get("userId", Long.class));
  } catch (Exception e) {
      send401(res, "token无效或已过期");
      return;
  }
  chain.doFilter(request, response);
  ```
  方案 A（最小改动）：把 `jwtUtil.extractUserId(token)` 移进已有的 try 块。
- **备注**：属 `todo.md` 第 1 项（JWT）的一部分，**正式启用 JWT 前必修**。

### 23. 业务接口仍从 DTO 信任 `userId`（缺所有权校验）❓未修
- **位置**：`AssistantServiceImpl.java` L32/L43（`dto.getUserId()`）、`ConversationServiceImpl.java` L31/L44（`dto.getUserId()`）
- **现象**：`AssistantDto.userId` / `ConversationDto.userId` 由前端手传且带 `@NotNull`。当前是**多用户模型**（有 register），所以 A 只要传 B 的 `userId`，就能读/写别人的会话与助手。
- **建议**：过滤器已把 `userId` 放进 request attribute，Controller 用 `@RequestAttribute("userId") Long userId` 取出后传给 service；同时删掉两个 DTO 里的 `userId` 字段与 `@NotNull`（那两个字段的注释本来也写着"应该是从 jwt 拿的"）。
- **备注**：这就是"所有权校验"。⚠️ `/auth/**` 是放行路径，过滤器**不会**给它设 `userId` 属性，所以在 auth 路径上用 `@RequestAttribute` 会因属性缺失直接报错。

### 24. `ModelController.apikey` 缺 `@Valid` ❓暂无害
- **位置**：`ModelController.java` L34（`@RequestBody ApiKeyDto dto`，对比同文件 L29 的 register 有 `@Valid`）
- **说明**：`ApiKeyDto` 目前没写任何校验注解，所以暂无实际影响；一旦给它加 `@NotNull` 等注解，这里必须同步补 `@Valid`，否则注解不生效（同问题 21 那次的 `MessagesDto` 踩法）。

### 26. provider / model 的归属：决定走**方案 B**（每用户独立，各自加 owner 列）❓决策已定，未实施
- **背景**：目标是"各模块都绑定用户"。`assistant` / `conversation` **已有 `user_id`**（→ 问题 23，零 DDL 即可完成绑定）；缺的是 `provider` / `model`。
- **已否掉的方案**：靠 `provider ← model ← assistant → user` 这条链**间接**推归属。**不行**，三个原因：
  1. **方向反了**：链能回答的只是"**谁在用**这个 provider"，而授权要的是"**谁拥有**"它；没人引用时链直接断。
  2. **无 assistant 时无从归属**：真实流程是「注册 provider → **配 API Key** → 建 assistant」，而**配 Key 那一步就要鉴权**，那时还没有任何 assistant。
  3. **归属歧义**：`model` 现在全局唯一（`uk_model_id`），A、B 的 assistant 引用的是**同一行**；A 删掉自己的 assistant，归属就凭空消失（多对多特征，间接绑定表达不了）。
- **决定**：走**方案 B** —— `provider` / `model` 归用户所有，各带**直接的 owner 列**。
- **实施清单**（一起做，否则会半途不一致）：
  1. `provider` 加 `user_id BIGINT NOT NULL`；唯一键 `uk_provider (protocol, base_url)` → **`(user_id, protocol, base_url)`**（不改的话，B 注册与 A 相同的端点会被"已存在"顶掉）
  2. `model` 加 `user_id BIGINT NOT NULL`；唯一键 `uk_model_id (model_id)` → **`(user_id, model_id)`**（**合并问题 9**）
  3. `ProviderServiceImpl.registerProvider` 的查重（现按 `baseUrl + protocol`）带上 `user_id`
  4. `ModelServiceImpl.registerModel` 的查重（现按 `modelId`）带上 `user_id`
  5. 所有 create/register 的 `userId` 改从 token 取（= 问题 23）
  6. ⚠️ **跨模块归属校验**：建 assistant 时校验「引用的 `model` **属于同一 userId**」；建 conversation 时校验「引用的 `assistant` 属于同一 userId」。现在只判**存在**不判**归属**（`AssistantServiceImpl:38` 就是 `existModel == null` 一判了事）
- **备注**：`messages` **不加** `user_id` —— 它靠 `conversation_id` 间接归属，冗余加列只会多维护一份一致性。

### 27. `ProviderServiceImpl.apiKey()` 只判存在不判归属 → 任意用户可覆盖他人 API Key ❓未修（安全）
- **位置**：`ProviderServiceImpl.java` L72-78（`getById(dto.getProviderId())` 后只判 `== null`，随即 `setApiKey` + `updateById`）
- **现象**：只要登录（拿到合法 token），传任意 `providerId` 就能**覆盖别人的 API Key**。
- **建议**：与问题 26 同源（没有归属列），但**即使暂不做方案 B 也该处理**：
  - 走方案 B 后 → `apiKey()` 改为 `lambdaQuery().eq(id).eq(user_id, 当前userId).one()`，查不到就抛「提供商不存在或无权操作」；
  - 若仍把 provider 当全局配置 → 至少在代码里注明"这是单用户前提下的设计"，别当漏洞放着不管。

### 28. 越权访问（对象级授权 / BOLA）：随 JWT 按模块逐步接入 ❓进行中
- **性质**：JWT 只解决了**认证**（你是谁），没解决**对象级授权**（这条数据是不是你的）。后者缺失就是业内所说的 IDOR / BOLA（OWASP API 安全第一位），也就是"改个 id 就能读别人数据"那类事故的根因。
- **计划**：随 JWT **逐模块接入**。已完成 user；进行中 assistant；conversation / model / provider / chat 待做。
- **每个模块只检查两件事**：
  1. **读**：列表查询与按 id 的查询，是否带了**归属条件**（当前多处是"全量返回"）
  2. **写**：创建是否用 token 的 `userId` 落地归属；**引用其它模块时**（如 assistant → model）是否校验**归属相同**，而不只是"该记录存在"
- **写法原则**：归属条件**并进查询**（`WHERE id = ? AND user_id = ?`），不要"先查出来再判断"；对外统一回"不存在"，避免被当成 id 探测器来枚举。
- **备注**：`/auth/**` 是放行路径，那条链路上**没有** `userId` 属性。机制细节见问题 23 / 26 / 27。

### 29. 雪花 ID（19 位）超出 JS 安全整数 → 前端传回的 id 失真 ❓未修（前端联调阻断）
- **位置**：实体主键用 MyBatis-Plus 雪花 ID（约 19 位，如 `2099362928699224066`），前端经 `JSON.parse` 接收后**数值被四舍五入**
- **现象**：前端「配置 API Key」报 **「提供商不存在或无权操作」**。实测对比：
  - 真实 id `2099362928699224066` → `code=200 success`
  - 前端实际回传的 id → `code=500 提供商不存在或无权操作`
  - 原因：`Number.MAX_SAFE_INTEGER` = `9007199254740991`（**16 位**），19 位 ID 超出 → IEEE 754 double 只能表示到 `...224064`，**差值 -2**
- **影响面**：**所有"从列表/响应里取 id 再回传"的调用**都中招（配置 Key、建助手填 modelId、建会话填 assistantId…），不止 apiKey 一处
- **建议**（治本，雪花 ID 的标准配套做法）：**后端把 Long 序列化成字符串**
  - 全局：给 Jackson 注册 `Long/long → String` 序列化器；或逐字段 `@JsonSerialize(using = ToStringSerializer.class)`
  - ⚠️ **前端必须同步**：去掉 id 上的 `Number(...)` 转换（`AssistantPanel` 的 `Number(form.modelId)`、`ChatPanel` 的 `Number(form.assistantId)`、以及直接用 `state.id` 的地方），让字符串**原样回传**
  - 备选：主键策略由雪花改自增（`IdType.AUTO`，DB 的 AUTO_INCREMENT 已就绪）—— 但会动到已有数据和 JWT 里的 `userId`，风险更大
- **备注**：Spring MVC 用的是 **Jackson 3**（`tools.jackson`），配置别写成 Jackson 2 的包名（`com.fasterxml.jackson`）。

### 30. `registerProvider`「已存在」分支漏设 `providerId` → 注册第二个模型 500 ❓未修
- **位置**：`ProviderServiceImpl.registerProvider` 的 `if (exist != null)` 分支
- **现象**：在**已有**提供商下注册第二个模型（同 baseUrl + protocol、不同 modelId）→ `code=500 土豆炸啦`。日志铁证：
  `Field 'provider_id' doesn't have a default value`，且 `INSERT INTO model ( id, user_id, name, model_id )` —— **provider_id 根本没进 SQL**（值为 null）
- **根因**：`ProviderEntity` 的字段叫 **`id`**，而 `RegisteredProviderVo` 叫 **`providerId`**，`BeanUtils.copyProperties(exist, Vo)` 按**同名字段**拷贝 → **拷不过去** → `providerId` 恒为 `null` → `model.setProviderId(null)` 撞 `NOT NULL`。且抛的是 `DataIntegrityViolationException`，外面的 `catch (DuplicateKeyException)` **接不住**，直接 500。
- **修法**：`if (exist != null)` 分支里补一行 `Vo.setProviderId(exist.getId());`
- **备注**：老 bug（改归属之前就存在），前端联调才把它暴露出来。

---

## P3 - 后续/备忘

### 16. AnthropicProvider 是空壳 ❓
- **位置**：`ProviderImpl/AnthropicProvider.java`（空类）、`ProviderFactory.java` L20（已注释）
- **说明**：目前只有 openai 协议可用；做 Anthropic 时注意其请求体格式不同（x-api-key 头、system 独立字段、max_tokens 必填）。

### 17. HTTP 恒 200 + body.code 约定 ❓
- **说明**：目前所有响应 HTTP 200，业务成败看 `body.code`。这是可以接受的约定，但前端必须统一判断 body.code（`frontend/src/api/http.js` 已按此实现）。确认团队沿用，不再各自混用状态码。

### 18. `messages` 表 role 类型与实际使用不一致 ❓
- **位置**：`sql/init.sql` L73（注释支持 user/assistant/system/tool）、`ChatServiceImpl.java` L110-115（`toRole` 只认 user/assistant，其余抛 RuntimeException）
- **说明**：将来存 system/tool 消息会崩；扩 role 时同步改 `toRole`（或 AI 侧不把 system 落库、只拼 prompt）。

### 25. 授权（角色/权限）模块：确认**不做** ❓备忘（结论）
- **结论**：个人助手只有**一种主体**（所有者），没有角色/权限矩阵可分配 → **不引入** RBAC / `@PreAuthorize` / 角色表。
- **需要的是另外两层**：① 认证（已完成，JWT）；② **所有权校验**（见问题 23，未做）。授权层留空。
- **何时再回来看**：真出现第二种角色（如管理员、只读访客）时再设计；现在做就是空壳。

---

## 前端待办（后端已就绪，前端待改）

| 项 | 前端现状 | 待改 |
| --- | --- | --- |
| 列表接口 | 资源缓存在 localStorage | 改为服务端拉取：`/model/getallprovider`、`/model/getAllModel/{id}`、`/assistant/getAllAssistant`、`/conversation/getAllConversation`；消息用 `/chat/{id}/get` |
| 过滤器强制 Bearer | 请求**未带** `Authorization` 头 → 非 `/auth/**` 接口一律 401 | 登录/注册后存 `AuthVo.token`，在 `http.js` 统一加 `Authorization: Bearer <token>` |
| `login` 已返回 `id`/`token` | AuthPanel 仍靠 `/auth/get` 按账号匹配 | 直接用登录响应里的 `id`，删掉匹配逻辑 |
| `model/register` 已返回实体 id | ModelPanel 仍是手填框 | 改为取响应/模型列表里的 `id` |
| options 已生效 | ChatPanel 提示"后端当前未生效" | 删掉该提示 |
| `maxtokens` 默认 1024 偏小 | 手动填 4096+ 规避 | 后端待改（见问题 21） |
| `maxtokens` 命名 | ChatPanel 严格发 `maxtokens` | 后端待改（见问题 3） |
| 空串覆盖判定 | 创建会话统一传 `null` | 后端待改（见问题 2） |

> 后端修完任何一条，记得同步 `frontend/` 对应提示，避免界面误导。
