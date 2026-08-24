# Amica 待改问题清单（致下次的我）

> 来源：2026 年代码走查 + 前端测试控制台（`frontend/`）联调发现的问题。
> 后端本轮不改代码，改的时候对照本清单逐条勾选。
> 优先级：P1 会让前端/用户行为明显异常，优先修；P2 健壮性与语义；P3 后续增强。

---

## P1 - 逻辑/行为异常

### 1. `options` 参数被接受但不生效 ❓处理完成，但耦合的问题13还未完成
- **位置**：`src/main/java/com/example/Amica/Service/Impl/ChatServiceImpl.java` L75
- **现象**：`MessagesDto.options`（temperature/topP/reasoningEffort/stream）传了也没用，代码恒传 `ChatOptions.none()`。
- **建议**：把 `dto.getOptions()` 映射进 `ChatOptions`；顺带检查 `stream=true` 时 `OpenAiProvider` 仍是同步解析 JSON 响应，流式会解析失败（见问题 13）。

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

### 7. 缺少列表/查询接口（前端联调最大痛点）❓
- **位置**：`Controller/` 下只有 create/注册类 POST + `GET /auth/show`；无任何 list/get 接口
- **现象**：前端无法恢复状态——刷新页面后不知道有哪些会话/助手/模型，只能靠创建接口的返回值收集 ID；`model/register` 甚至不返回 model 实体 id（见问题 10）。
- **建议**（按需补）：
  - `GET /conversation/list?userId=` 及其 `GET /conversation/{id}/messages`
  - `GET /assistant/list?userId=`
  - `GET /model/list`、`GET /provider/list`（API Key 脱敏后返回）
- **前端现状**：`frontend/` 目前把资源缓存在 localStorage 顶替 list 接口（见 `frontend/README.md`），接口补齐后应切换为服务端拉取。

### 19. `reasoningEffort` 三目分支写反（NPE + 用户传值被丢弃）❓
- **位置**：`ChatServiceImpl.java` L50（`toChatOptions` 内）
- **现象**：`.withReasoningEffort(o != null && o.getReasoningEffort() != null ? null : o.getReasoningEffort())` 三目分支写反——
  ① 整体 options 未传（o=null）时进入 else 分支执行 `o.getReasoningEffort()` → NPE；
  ② 用户传了 reasoningEffort（如 "high"）时条件为 true 却返回 null → 用户值被丢弃，等于恒不传。
- **建议**：改为 `? o.getReasoningEffort() : null`（传了就收，没传保持 null 不传）。

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

### 10. 注册/响应泄露敏感信息 ❓
- **位置**：`UserServiceImpl.java` L36（register 用 `BeanUtils.copyProperties(user, result)` 把 BCrypt 哈希拷进返回体）、L52-54（`/auth/show` 直接返回整个 `UserEntity`）
- **现象**：注册响应和用户列表都带着 `$2a$...` 密码哈希；`/auth/show` 还是无鉴权公开接口。
- **建议**：VO 化（响应体不带 password）；`/auth/show` 仅测试用途就加注释说明并计划下线（JWT 上线后删除）。

### 11. `login` 不返回 userId ❓
- **位置**：`UserServiceImpl.java` L46-48（只 set account）
- **现象**：登录后前端拿不到 id，只能再调 `/auth/show` 按账号匹配（`frontend/` 已用这个临时方案，见 `AuthPanel.vue`）。
- **建议**：`LoginDto`/返回体补 `id`（接入 JWT 后此问题自然消失）。

### 12. `model/register` 不返回 model 实体 id ❓
- **位置**：`ModelServiceImpl.java` L55-59（ModelVo/AModelVo 都没有 id）
- **现象**：建助手需要 model 表实体 id，但接口拿不到，只能查库（`frontend/` 目前是手动输入框，见 `ModelPanel.vue`）。
- **建议**：`AModelVo` 补 `id` 字段。

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

---

## 前端侧备忘（frontend/ 已实现的应对）

| 后端问题 | 前端现状 |
| --- | --- |
| 无列表接口 | 资源缓存在 localStorage；接口补齐后改服务端拉取 |
| login 无 userId | AuthPanel 通过 `/auth/show` 按账号匹配 |
| model/register 无实体 id | ModelPanel 提供手填框（查库） |
| `maxtokens` 命名 | ChatPanel 严格发 `maxtokens`，界面有提示 |
| options 未生效 | ChatPanel 照发 + 提示"后端当前未生效" |
| 空串覆盖判定 | 创建会话统一传 `null` |

> 后端修完任何一条，记得同步 `frontend/` 对应提示，避免界面误导。
