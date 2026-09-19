# Amica 待改问题清单（致下次的我）

> 来源：2026 年代码走查 + 前端测试控制台（`frontend/`）联调发现的问题。
> 改的时候对照本清单逐条勾选；**修完一条就移到 `finish.md`**，本文件只留未完成的。
> 优先级：P1 会让前端/用户行为明显异常，优先修；P2 健壮性与语义；P3 后续增强。
> 相关文件：`finish.md`（已修正归档）、`todo.md`（路线图待办）。
>
> **行号口径**：下面所有路径与行号都按 **`1cc171e`** 时的源码逐条核对过（不是凭记忆写的）。
> 包结构：`Provider/Impl/` 放实现、`Provider/ProviderFactory.java` 放工厂、异常处理在 `Common/`。

---

## P1 - 逻辑/行为异常

### 3. `maxtokens` 的哨兵值用法和命名 ❓
- **位置**：`Dto/Messages/MessagesDto.java` L13（`private int maxtokens;`）、`ChatServiceImpl.java` L61（`!= 0 ? x : 4096`）
- **现象**：① 无法显式请求 0 token；② 字段名 `maxtokens` 非驼峰，前端拼成 `maxTokens` 会被 Jackson 静默忽略（默认配置不报错），查错半天。
- **建议**：改用 `Integer maxTokens` + 判空；命名改 `maxTokens`（改后要同步前端「会话与对话」页和 `frontend/README.md` 里的说明）。
- **备注**：**默认值已从 1024 提到 4096**（问题 21 已修，见 `finish.md`）—— 本条**只剩命名与哨兵值用法**。
- **两处过期注释（顺手清）**：
  - `MessagesDto.java` L12「方法体还没写好处理这个参数的功能」—— 功能其实已经做了（`ChatServiceImpl` L61 就在用）；
  - `ChatServiceImpl.java` L60「默认为 1024」—— 代码已是 4096。

### 5. 用户消息先落库，provider 失败不回滚 ❓
- **位置**：`ChatServiceImpl.java` L130（先 `messagesService.save(userMsg)`）→ L133（`provider.chat(req)` 可能抛异常）→ L140（助手消息才落库）
- **现象**：API Key 配错/限流/超时等失败时，用户消息已入库；下次发消息会把这条"没被回答的"消息带进上下文，用户感到"我明明失败了，它却记住了"。
- **建议**：三选一——①整个方法加 `@Transactional`（provider 调用失败即回滚用户消息，代价：长事务）；②把落库挪到 provider 成功之后；③失败时给消息打标记/落一条错误记录（保留现场，推荐）。同时把两次 `save` 合并为批量落库（用户+助手消息一起存）。

### 6. 未配 API Key 时报错不明确 ❓
- **位置**：`Provider/Impl/OpenAiProvider.java` L27-31（构造时 L29 `apiKeyEncryptor.decrypt(provider.getApiKey())`，`api_key` 为 null 时直接炸）、`ProviderServiceImpl.GetApiKey()` L92-98（同理，且 L93 只按 id 查）
- **现象**：注册了模型但没配 Key 就发消息 → 笼统 500「土豆炸啦！？」，前端看不出是"Key 没配"。
- **建议**：构造/发送前校验 `apiKey` 是否为空，抛「提供商未配置 API Key」。

### 22. 正文解析未兜底 `reasoning_content`、未读 `finish_reason` ❓未修
- **位置**：`Provider/Impl/OpenAiProvider.java` L80-87（`parse()` 只取 `choices[0].message.content`，见 L82）
- **现象**：思考模型可能只在 `reasoning_content` 里给内容、`content` 为空 → `asText()` 对缺失字段返回 `""` → 空串被落库（`messages.content` 是 `TEXT NOT NULL`，**空串能过、null 过不了**）→ 下一轮把 `{"role":"assistant","content":""}` 当历史发回去 → 被 API 拒绝。
- **建议**：① `content` 为空时回退 `reasoning_content`；② 读出 `finish_reason`（`"length"` 是"被 max_tokens 截断"的铁证，目前完全没看，丢了最有用的诊断字段）；③ 拼历史时跳过 `content` 为空的消息。
- **备注**：与 `finish.md` 问题 21 是同一现象的两面——21 是"预算不够"（默认值已提到 4096），本条是"解析层没兜住"。

---

## P2 - 接口语义 / 健壮性 / 安全

### 8. `ProviderFactory` 默认分支抛 401 ❓
- **位置**：`Provider/ProviderFactory.java` L21
- **现象**：不支持的协议抛 `BusinessException(401, provider.getProtocol())`，401 语义是"未授权"，且 message 只给协议名，输出成 `401 openai2` 这种样子。
- **建议**：`400` 或 `500` + `"不支持的协议: xxx"`。

### 9. `provider.name` 被固定为 protocol（唯一键那半已完成）❓半完成
- **已完成的一半**：唯一键 `UNIQUE KEY uk_model_id (model_id)` → **`uk_model_user (user_id, model_id)`**
  （随问题 26 一起做的，见 `finish.md`）→ **不同用户之间已经可以注册同名模型** ✅
- **剩余的一半**：`ProviderServiceImpl.java` L49 `provider.setName(dto.getProtocol())` —— 提供商显示名永远等于协议名，`name` 字段没有实际意义（`sql/init.sql` L23 的注释写的是"如:DeepSeek"，与实际不符）
- **建议**：提供商名由 `ModelDto` / `ProviderDto` 透传一个 name 字段，或单独接口维护。**属产品决策，不急。**

### 13. `stream=true` 被静默丢弃（一旦有人"补全"就是解析崩溃）❓
- **位置**：`Provider/Impl/OpenAiProvider.java` L75（`req.options().stream() != null` 才写 `stream`）、L80-87（`parse` 按普通 JSON 解析）
- **实况（已核对代码）**：`ChatServiceImpl.toChatOptions()` L49-55 **只写了 temperature / topP / reasoningEffort，没有调 `withStream(...)`**
  → `ChatOptions.stream` 恒为 `null`（`ChatOptions.none()` 就是全 null）→ L75 那个分支**从不成立**
  → **前端勾 `stream` 目前完全没有效果**（请求体里压根没有 `stream` 字段）。
  ⚠️ 这与问题 1 的老毛病同款：**参数静默失效**，界面给了开关却什么也不做。
- **真正的风险**：哪天顺手把 `.withStream(o.getStream())` 补上（"完善 options"最自然的一步），响应立刻变 SSE 流，`mapper.readTree` 直接解析失败。
- **建议**：二选一 —— ①**先别接**：`OptionsDto.stream` 保留但标注"暂不支持"，前端把勾选框**禁用或去掉**；
  ②**真要做**：`withStream` + SSE 逐行解析（`BodyHandlers.ofInputStream`，按 `data:` 前缀读、遇 `[DONE]` 结束）。
- **备注**：`ChatServiceImpl` L54 已注释"stream 先挂起"，方向是对的。
  ✅ **前端已于本轮禁用该勾选框并标注「暂不支持」**（`ChatPanel.vue`，含"为什么禁用"的注释）—— 不再给用户无效开关。
  放开的那天要同时做三件事：`toChatOptions` 里补 `withStream` + `OpenAiProvider` 写 SSE 解析 + 前端解除禁用。

### 14. 并发下 `seq` 可能重复 ❓
- **位置**：`ChatServiceImpl.java` L123（`history.last.seq + 1` 算 nextSeq）、`sql/init.sql` L84（`INDEX idx_conv_seq (conversation_id, seq)` 非唯一）
- **现象**：同一会话并发两条请求会算出相同 seq，排序乱序/覆盖。
- **建议**：`UNIQUE KEY uk_conv_seq (conversation_id, seq)`（README 路线图已列）+ 冲突重试；或改为对 conversation 加行锁。

### 15. 全局异常处理可读性 ❓
- **位置**：`Common/GlobalExceptionHandler.java` L18（业务异常日志只有"业务处理错误"，没带 `e.getMessage()`）、L53（兜底文案「土豆炸啦！？」）
- **现象**：排查时日志信息不足（同一批日志里 `handleMethodArgumentNotValidException` L29 都传了 `e`，这里却没传）；兜底文案让前端误以为"后端炸了"，实际可能是参数问题。
- **建议**：日志带上异常内容；兜底返回带真实错误摘要（内部细节可另传 header/仅日志）。

### 17. HTTP 恒 200 + body.code 约定 ❓（约定已被 401 打破，且 chat 两个端点写法不一致）
- **现状**：业务响应仍是 HTTP 200 + `body.code`；但 **JWT 过滤器对未认证请求直接返回 HTTP 401**（`JwtAuthenticationFilter.send401`，已实测）→ **"恒 200"已经不再成立**。
- **前端已适配**：`frontend/src/api/http.js` 同时判断 `body.code` 与 HTTP 状态码，遇 401 会清登录态并提示重新登录。
- **建议把它写成明确规则**（写下来就不算"混用"，而是有意的分层）：
  - **过滤器 / 拦截器层**（请求进不到 Controller）：用 HTTP 状态码（401 / 403）
  - **业务层**（Controller / Service）：HTTP 200 + `body.code`
- **顺带的不一致（Service 层返回类型）**：`ChatServiceImpl.sendMessage` 返回**裸 `ChatResponse`**、由 `ChatController.send` 包 `Result`；
  而 `getMessage` 自己就返回 `Result<List<MessagesEntity>>`、Controller 直接透传。**同一个类里两种风格**，择一统一。

### 18. `messages` 表 role 类型与实际使用不一致 ❓
- **位置**：`sql/init.sql` L77（注释 `user/assistant/system/tool`）、`ChatServiceImpl.java` L146-152（`toRole` 只认 user/assistant，其余抛 `RuntimeException("未知角色: ...")`）
- **说明**：将来存 system/tool 消息会崩（且抛的是裸 RuntimeException → 兜底 500）；扩 role 时同步改 `toRole`，或统一抛 `BusinessException`（AI 侧也可以不把 system 落库、只拼 prompt）。

### 20. JWT 过滤器：`extractUserId` 在 try/catch 之外，该报 401 的会被漏成 500 ❓未修（倾向方案 B）
- **位置**：`Config/JwtAuthenticationFilter.java` L44-49 是 try（只罩住 `validateToken`），**L50 `jwtUtil.extractUserId(token)` 在 try 之外**
- **现象**：`validateToken` 与 `extractUserId` 各自解析一次 token，而 try/catch 只罩住第一次；第二次抛出的运行时异常（`RequiredTypeException` / `NullPointerException`）没人接，直接冒出 `doFilter`。
  - **后果**：Filter 跑在 `DispatcherServlet` **之前**，`GlobalExceptionHandler`（`@RestControllerAdvice`）**接不到 Filter 里的异常** → 客户端拿到的是容器默认 500（不是 `Result` JSON），**该报 401 的场景被漏成 500**。
  - **触发条件**（签名与过期都正常时）：payload 缺 `userId` → `claims.get(...)` 返回 `null` → 拆箱 NPE；或 `userId` 非数字 → `RequiredTypeException`。（另：`claims.get("userId", long.class)` 传基本类型 Class 必抛，已改 `Long.class`。）
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
- **备注**：属 `todo.md` 第 1 项（JWT）的一部分，**正式启用 JWT 前必修**。与问题 28 剩下的那处（白名单过宽）在同一个文件里，建议一起改。
- **顺带**：L30 注释「标记特殊路径，以便放行，但目前先全部放行」与实际不符（早就不全放行了），建议改成"`/auth/**` 放行"。

### 28. 越权访问（对象级授权 / BOLA）：随 JWT 按模块逐步接入 ❓进行中（只剩白名单一处）
- **性质**：JWT 只解决了**认证**（你是谁），没解决**对象级授权**（这条数据是不是你的）。后者缺失就是业内所说的 IDOR / BOLA（OWASP API 安全第一位），也就是"改个 id 就能读别人数据"那类事故的根因。
- **进度**：✅ user、assistant（读写两侧）、model（读写两侧）、provider（`getProvider` / `apiKey` 归属校验）、
  **conversation**（`create` 校验 assistant 归属；`findAllConversation` 按 `userId + assistantId` 过滤 —— 实测 B 用 A 的 assistantId 查返回 **0 条**）、
  **chat**（`sendMessage` / `getMessage` 都按 `id + user_id + assistant_id` **三条件锁定会话**，取不到即 403「无权访问该会话」；
  链路下游一律用 `conv.getAssistantId()` **派生**，不再信入参 —— 已提交 `1cc171e`）。
- 🔴 **仍剩一处：`/auth/**` 整段放行**。`JwtAuthenticationFilter` L32 的白名单是 `path.contains("/auth/")`，于是
  `AuthController` 的 `GET /auth/get`（L49，用户列表）与 `GET /auth/get/{id}`（L38）**不带 token 就能访问**
  （早前后端在跑时实测 `code=200`）—— 等于把用户枚举接口开放了。
  - **建议**：白名单由"前缀匹配"改为**精确匹配** `/auth/login` + `/auth/register`（默认拒绝），其余一律要求 token；
    顺手把问题 20 的白名单过宽一起收掉。
- **每个模块只检查两件事**：
  1. **读**：列表查询与按 id 的查询，是否带了**归属条件**（当前多处是"全量返回"）
  2. **写**：创建是否用 token 的 `userId` 落地归属；**引用其它模块时**（如 assistant → model）是否校验**归属相同**，而不只是"该记录存在"
- **写法原则**：归属条件**并进查询**（`WHERE id = ? AND user_id = ?`），不要"先查出来再判断"；对外统一回"不存在"，避免被当成 id 探测器来枚举。
- **备注**：`/auth/**` 是放行路径，那条链路上**没有** `userId` 属性。机制细节见问题 23 / 26 / 27。
- **待补验证**：chat 三条件锁定的**跨用户实测**还没做（A 拿 B 的 conversationId → 期望 403）。测的时候要起后端 + 两个账号的 token。

### 29. 雪花 ID（19 位）超出 JS 安全整数 → 前端传回的 id 失真 ❓前端已绕开，后端治本待做
- **现状**：**前端已绕开** —— 新增 `frontend/src/api/bigint.js` 的 `quoteBigInts()`，在 `JSON.parse` **之前**把值位置上的 ≥16 位整数转成字符串（逐字符扫描，不误伤字符串内容）；`http.js` 统一使用；`AssistantPanel` / `ChatPanel` 去掉了 id 上的 `Number()`；`store.js` 会作废旧缓存里的数字型 id 强制重新登录。
  - 实测闭环：拉 provider 列表拿到的 id 是字符串且**精确**，原样发回 `POST /model/apikey` → `code=200`；
  - 对比：朴素 `JSON.parse` 得到 `2099362928699224000`（**错**），`quoteBigInts` 之后是 `2099362928699224066`（**对**）。
  - ⚠️ **在那之前，任何新写的前端代码取 id 都必须走 `quoteBigInts` 的解析路径**，否则会重新踩坑。
- **后端治本方案（仍未做）**：把 `Long` 序列化成字符串
  - 全局：给 Jackson 注册 `Long/long → String` 序列化器；或逐字段 `@JsonSerialize(using = ToStringSerializer.class)`
  - 备选：主键策略由雪花改自增（`IdType.AUTO`，DB 的 AUTO_INCREMENT 已就绪）—— 但会动到已有数据和 JWT 里的 `userId`，风险更大
- **位置**：实体主键用 MyBatis-Plus 雪花 ID（约 19 位，如 `2099362928699224066`），前端经 `JSON.parse` 接收后**数值被四舍五入**
- **现象**：前端「配置 API Key」报 **「提供商不存在或无权操作」**。实测对比：
  - 真实 id `2099362928699224066` → `code=200 success`
  - 前端实际回传的 id → `code=500 提供商不存在或无权操作`
  - 原因：`Number.MAX_SAFE_INTEGER` = `9007199254740991`（**16 位**），19 位 ID 超出 → IEEE 754 double 只能表示到 `...224064`，**差值 -2**
- **影响面**：**所有"从列表/响应里取 id 再回传"的调用**都中招（配置 Key、建助手填 modelId、建会话填 assistantId…），不止 apiKey 一处
- **备注**：Spring MVC 用的是 **Jackson 3**（`tools.jackson`），配置别写成 Jackson 2 的包名（`com.fasterxml.jackson`）。
  但 `Provider/Impl/OpenAiProvider.java` L10-11 自己 new 的 `ObjectMapper` 用的是 **Jackson 2**（`com.fasterxml.jackson`）—— 两套并存是有意的（JJWT/工具类用 2），改的时候别顺手"统一"掉。

---

## P3 - 后续/备忘

### 16. AnthropicProvider 是空壳 ❓
- **位置**：`Provider/Impl/AnthropicProvider.java`（**只有 4 行**：package + `public class AnthropicProvider {}`，`implements AiProvider` 都没有）、`Provider/ProviderFactory.java` L20（已注释）
- **说明**：目前只有 openai 协议可用；做 Anthropic 时注意其请求体格式不同（`x-api-key` 头、`system` 独立字段、`max_tokens` 必填）。

### 25. 授权（角色/权限）模块：确认**不做** ❓备忘（结论）
- **结论**：个人助手只有**一种主体**（所有者），没有角色/权限矩阵可分配 → **不引入** RBAC / `@PreAuthorize` / 角色表。
- **需要的是另外两层**：① 认证（已完成，JWT）；② **所有权校验**（见问题 23 / 28 —— user / assistant / model / provider / conversation / chat 均已完成，只剩白名单一处）。授权层留空。
- **何时再回来看**：真出现第二种角色（如管理员、只读访客）时再设计；现在做就是空壳。

---

## 前端状态（frontend/）

**本轮已完成**（前端整体重写，与服务端对齐）：
- 资源全部改为**服务端拉取**；localStorage 只保留登录态（key：`amica-auth`）
- `http.js` 统一带 `Authorization: Bearer`，遇 401 自动清登录态并提示重新登录
- 登录 / 注册直接用 `AuthVo{id, account, nickname, token}`；**删掉了** `/auth/get` 按账号匹配的临时方案
- 模型注册改用响应里的 model 实体 id，不再手填；助手 / 会话列表都走服务端
- **雪花 ID 保护**（见问题 29）：`api/bigint.js` 的 `quoteBigInts()`；**所有 id 一律不做 `Number()` 转换**
- 发消息后重新拉取消息列表，保证界面与库一致
- **chat 端点路径已同步**（`api/index.js` + `ChatPanel.vue`）：
  `POST /chat/{conversationId}/{assistantId}/send`、`GET /chat/{conversationId}/{assistantId}/get`；
  传的 `assistantId` 就是页顶选中的助手（`watch(currentAssistantId)` 会置空 `currentId`，所以两者必然一致）
- `maxtokens` 默认填 4096（后端默认值现已是 4096，两边一致；保留显式传值只是习惯）

**仍待前端同步的后端改动**：

| 后端问题 | 前端待改 |
| --- | --- |
| 问题 3 `maxtokens` 改名 `maxTokens` | 后端改名后，同步 `ChatPanel` 与 `frontend/README.md` |
| ~~问题 21 默认值 1024 偏小~~ | ✅ 已修（后端默认 4096），前端无需再改 |
| 问题 13 `stream=true` | ✅ **已禁用**勾选框并标注「暂不支持」（`ChatPanel.vue`），等后端做了 SSE 再放开 |
| 问题 28 剩下的白名单 | 若把 `/auth/get` 收回鉴权，前端「用户列表（调试用）」需带 token（已带）或直接下掉该页 |

> 📌 **接下来的前端工作不在本表里**：多模态 / 模型能力那块见 **`todo.md` 第 2 项**
> （模型页加能力选择、聊天页按能力决定是否显示选图入口、读 `metadata.parts` 渲染图片 —— 注意 `metadata` 是字符串，要再 `JSON.parse` 一次）。

> ✅ `frontend/README.md` 已同步重写（登录态与鉴权、雪花 ID 保护、新接口表、已知限制）。
> ✅ conversation / chat 均已接 JWT：前端不再传 `userId`；会话列表改为**按助手**查（`ChatPanel` 顶部先选助手）。
