# Amica 已修正问题归档（finish）

> 从 `issue.md` 移入的、**已修复**的问题。保留原描述 + 记下当时的修法，作为改动记录。
> 规则：修完一条就从 `issue.md` 移到本文件，`issue.md` 里不留已完成条目（否则清单会越读越长，失去"待办"的作用）。

---

### 1. `options` 参数被接受但不生效 ✅已修正
- **原位置**：`ChatServiceImpl.java`（`toChatOptions` 内）
- **原现象**：`MessagesDto.options`（temperature / topP / reasoningEffort / stream）传了也没用，代码恒传 `ChatOptions.none()`。
- **修法**：`toChatOptions(OptionsDto)` 把 `dto.getOptions()` 映射进 `ChatOptions` —— temperature / topP 给默认值（0.8 / 0.9），`reasoningEffort` 传了才带。
- **残留**：`stream` 仍挂起，由 **issue.md 问题 13** 单独跟踪。

### 7. 缺少列表/查询接口 ✅后端已修正
- **原现象**：`Controller/` 下只有 create/注册类 POST，前端刷新后无法恢复状态（不知道有哪些会话/助手/模型）。
- **修法**：补齐 6 个查询接口，全部 `Result` 包裹：

  | 资源 | 接口 |
  | --- | --- |
  | 用户 | `GET /auth/get`、`GET /auth/get/{id}` |
  | 提供商 | `GET /model/getallprovider` |
  | 模型 | `GET /model/getAllModel/{providerId}` |
  | 助手 | `GET /assistant/getAllAssistant` |
  | 会话 | `GET /conversation/getAllConversation` |
  | 消息 | `GET /chat/{conversationId}/{assistantId}/get` |

- **残留（后续已收敛）**：① 归属过滤 —— user / assistant / model / provider / conversation / chat **均已按 `userId` 过滤**（issue.md 问题 23 / 28 已完成，只剩 `/auth/**` 白名单一处）；② 前端已整体改为服务端拉取（见 issue.md 的「前端状态」）。

### 10. 注册/响应泄露敏感信息 ✅已修正
- **原现象**：register 响应与用户列表都带着 `$2a$...` BCrypt 密码哈希；`/auth/show` 还是无鉴权公开接口。
- **修法**：返回体全部 VO 化 —— 查询用 `UserInfoVo`，注册/登录用 `AuthVo`，**两者都不含 `password` 字段**；`/auth/show` 与 `showUsers()` 已移除。
- **核对**：全项目 grep 确认已无任何 `Result<UserEntity>` 形式的返回；`password` 仅出现在 `UserEntity` 字段、请求 DTO、以及 `UserServiceImpl` 内部（BCrypt 编解码）。

### 11. `login` 不返回 userId ✅已修正
- **原现象**：登录只 `setAccount(...)`，前端拿不到 id，只能再调 `/auth/get` 按账号匹配。
- **修法**：`login()` 改为 `BeanUtils.copyProperties(exist, result)` 并签发 token，返回 `AuthVo(id, account, nickname, token)`。
- **踩坑记录**：中途拷贝源误写成 `dto`（`LoginDto`），导致响应里 `id` / `nickname` 恒为 `null` —— 已改回 `exist`。

### 12. `model/register` 不返回 model 实体 id ✅已修正
- **原现象**：`ModelVo` / `AModelVo` 都没有 `id`，但建助手需要 model 表的实体 id。
- **修法**：给 VO 加 `id`，`BeanUtils.copyProperties` 按同名自动带出，**逻辑无需改动**（`registerModel` 与 `getAllModels` 同时受益）。
- **后续收敛**：`AModelVo` 与 `ModelVo` **合并为一个 `ModelVo{id, userId, providerId, name, modelId}`**（字段与 `ModelEntity` 对齐），register / list 共用，`AModelVo` 已删除。

### 19. `reasoningEffort` 三目分支写反 ✅已修正
- **原现象**：`? null : o.getReasoningEffort()` 分支写反 → ① `o == null` 时执行 `o.getReasoningEffort()` → NPE；② 用户传了值反而被丢弃，等于恒不传。
- **修法**：改为 `o != null && o.getReasoningEffort() != null ? o.getReasoningEffort() : null`（传了就收，没传保持 null 不传）。

### 26. provider / model 归属到用户（方案 B）✅已修正
- **原计划**：`assistant` / `conversation` 已有 `user_id`，缺 `provider` / `model`；决定走**方案 B**（各带直接的 owner 列），并**否掉**了"靠 `provider ← model ← assistant → user` 间接推归属"的做法（方向反了 / 配 Key 时还没 assistant / 一个 model 被多人引用导致归属歧义）。
- **修法**：
  1. **DDL**：`provider` / `model` 各加 `user_id BIGINT NOT NULL`；唯一键由 `(protocol, base_url)` / `(model_id)` 改为 **`(user_id, protocol, base_url)`** / **`(user_id, model_id)`**；各加 `user_id → user(id)` 外键。`sql/init.sql` 同步。
  2. `ProviderEntity` / `ModelEntity` 各加 `userId` 字段。
  3. `registerProvider` / `registerModel` 的**查重与写入**都带 `userId`（含两处 `DuplicateKeyException` 兜底分支）。
  4. `getProvider` / `getAllModels` 按 `userId` 过滤。
  5. **跨模块归属校验**：`AssistantServiceImpl` 引用 model 时改为 `.eq(id).eq(userId)`（不再只判"存在"）。
- **残留**：`ConversationServiceImpl` 建会话时校验 assistant 归属 —— 随会话模块的 JWT 接入一起做（见 issue.md 问题 23 / 28）。
- **备注**：`messages` **未加** `user_id`（靠 `conversation_id` 间接归属，避免冗余）。

### 27. `ProviderServiceImpl.apiKey()` 只判存在不判归属 ✅已修正
- **原现象**：`getById(dto.getProviderId())` 后只判 `== null` → 任何登录用户传任意 `providerId` 就能**覆盖别人的 API Key**。
- **修法**：改为 `lambdaQuery().eq(id).eq(userId).one()`，查不到就抛「提供商不存在或无权操作」。
- **实测**：B 用户拿 A 的 `providerId` 调 `/model/apikey` → 被拒（`提供商不存在或无权操作`）。

### 30. `registerProvider`「已存在」分支漏设 `providerId` ✅已修正
- **原现象**：在同一提供商下注册**第二个**模型 → `code=500 土豆炸啦`；日志为 `Field 'provider_id' doesn't have a default value`（INSERT 里根本没有 `provider_id`）。
- **原根因**：`ProviderEntity` 字段叫 `id`，`RegisteredProviderVo` 叫 `providerId` → `BeanUtils.copyProperties(exist, Vo)` 按同名匹配**拷不过去** → `providerId` 恒为 `null` → 撞 `NOT NULL`。且抛的是 `DataIntegrityViolationException`，`catch (DuplicateKeyException)` 接不住。
- **实际修法**（比"补一句 setter"更优）：**把 `RegisteredProviderVo.providerId` 改名为 `id`**，让字段名与实体对齐 → `copyProperties` 两个分支都自动带出 id，**无需任何手写 setter**。
- **实测**：同一 provider 下再注册 `deepseek-v4-pro` → `code=200`，返回的 `providerId` 非空；该 provider 下确认有 2 个模型。
- **模式总结**：与 11 / 12 以及 assistant 的 VO 重构是**同一个模式** —— **VO 字段名与实体对齐，`BeanUtils` 就全自动**。建议以后凡是用 `copyProperties` 的地方，先检查 VO 字段名有没有和实体对上。

### 23. 业务接口从 DTO 信任 `userId` ✅已修正
- **原现象**：`AssistantDto.userId` / `ConversationDto.userId` 由前端手传且带 `@NotNull` —— 多用户模型下，A 传 B 的 `userId` 就能读/写别人的数据。
- **修法**：`JwtAuthenticationFilter` 验签后把 `userId` 写进 request attribute → 各 Controller 用 `@RequestAttribute("userId")` 取出传给 service；同时**删掉 DTO 里的 `userId` 字段**。
- **核对**：全项目 `grep dto.getUserId()` **无匹配**；所有 DTO 里**已无 `private Long userId`**。
- **覆盖范围**：

  | 模块 | 位置 |
  | --- | --- |
  | user | `AuthController` 相关端点 |
  | assistant | `createAssistant` / `findByUserId` |
  | model / provider | `registerModel` / `getAllModels` / `apiKey` / `getProvider` |
  | conversation | `create` / `findAllConversation` |
  | chat | `sendMessage` / `getMessage` |

- **备注**：⚠️ `/auth/**` 是放行路径，过滤器**不会**给它设 `userId` 属性 —— 所以 auth 路径上不能用 `@RequestAttribute`（会因属性缺失直接报错）。

### 2. 会话「已覆盖」判定前后不一致 ✅已修正
- **原现象**：创建会话时用 `systemPrompt == null` 判「已覆盖」，而聊天链路用 `isBlank()` 决定是否回退到助手 prompt
  → 前端传空字符串 `""`（表单空值的常见形态）时，**创建说"已覆盖"、聊天却实际回退了**，两边结论不一致。
- **修法**：创建侧统一改成 `sp == null || sp.isBlank()`，与聊天链路口径一致。
- **踩坑记录**：改的时候取值写错了 —— `String sp = dto.getTitle();`（变量名叫 `sp`，取的却是 `title`），
  导致"明明填了 systemPrompt 也报未覆盖"。已改回 `dto.getSystemPrompt()`。
- **实测（三个用例）**：

  | 用例 | title | systemPrompt | status |
  | --- | --- | --- | --- |
  | 1 | 留空 | 留空 | 未覆盖 ✅ |
  | 2 | 留空 | **有值** | **已覆盖** ✅（修复前这里错报"未覆盖"） |
  | 3 | 有值 | `"   "` 空白串 | 未覆盖 ✅（`isBlank()` 生效） |

### 4. Chat 链路无判空，全链 NPE ✅已修正
- **原现象**：`conversation / assistant / model / provider` 逐层 `getById` 后**直接取属性**，任一环为 null（如引用了已删除的 ID）就 NPE → 全局兜底 500「土豆炸啦！？」，前端分不清断在哪一环。
- **修法**：每层查到 null 都抛带语义的 `BusinessException` —— 会话取不到 → `403「无权访问该会话」`；
  下游三环 → **`500`**「助手不存在」「模型不存在」「提供商不存在」（`ChatServiceImpl` L77 / L81 / L85）。
- **口径**：会话那一步是**鉴权**（403 合理）；下游三环是"数据链路断了"，用 500 与全局兜底同口径 —— 可以接受，因为 message 已经明确，不用再靠「土豆炸啦！？」猜是哪一环。将来若想改 404，只动这三个常量即可。

### 21. `maxTokens` 默认 1024 过小 → 思考模型回复正文为空 ✅已修正
- **原现象**：思考模型会**先把输出预算花在 reasoning 上**，1024 被思考链烧完后正文没有余量 → AI 回复正文为空（`finish_reason` 大概率是 `"length"`）。
- **验证过程**：前端把 `maxtokens` 手动填 **4096/8192** 再发一条，正文即正常 → 确认是此因。
- **修法**：`ChatServiceImpl` 默认值 `1024` → **`4096`**
  （`int maxTokens = dto.getMaxtokens() != 0 ? dto.getMaxtokens() : 4096;`）；前端默认值也是 4096，两边一致。
- **残留**：① `ChatServiceImpl.java` L60 注释仍写"默认为1024"，与代码不一致（未改）；② 字段名 / 哨兵值用法见 issue.md 问题 3。
- **备注**：与 issue.md 问题 22（未兜 `reasoning_content` / 未读 `finish_reason`）是同一现象的两面。

### 24. `ModelController.apikey` 缺 `@Valid` ✅已修正
- **原现象**：`@RequestBody ApiKeyDto dto` 没加 `@Valid`（同文件 register 有），一旦 `ApiKeyDto` 写了校验注解也**不会生效**。
- **修法**：`ApiKeyDto` 补 `@NotBlank(message = "密钥不能为空,请填充api密钥")`（`apiKey`）+ `@NotNull(message = "提供商编号为空")`（`providerId`）；`ModelController.apikey` 补 `@Valid`。
- **踩坑记录**：`providerId` 一开始写成 `@NotBlank` —— **`@NotBlank` 只适用于 `CharSequence`**，用在 `Long` 上会在校验阶段抛 `UnexpectedTypeException`。数字类型必须用 `@NotNull`。
- **核对**：`git show a749d59:src/.../ApiKeyDto.java` 确认 `@NotBlank` / `@NotNull` 都已在提交里。
  ⚠️ 该提交的 message 写的是"待改 @NotNull"，是**提交信息写岔了**，代码本身当时就是对的（按 `git diff` 无改动即为证）。

### 28（chat 部分）. `sendMessage` / `getMessage` 无会话归属校验 ✅已修正 —— 提交 `1cc171e`
- **原现象**：`ChatServiceImpl` 只按 `conversationId` 取会话 → 任何登录用户改个 id 就能**读别人的历史、往别人会话里写消息、烧别人的 API Key 额度**（BOLA，整套里最严重的一条）。
- **修法**（三个动作）：
  1. **端点路径带上 `assistantId`**：`POST /chat/{conversationId}/{assistantId}/send`、`GET /chat/{conversationId}/{assistantId}/get`；Controller 用 `@RequestAttribute("userId")` 取身份，**Service 签名同步加 `userId` / `assistantId`**。
  2. **三条件锁定会话**：`lambdaQuery().eq(id).eq(userId).eq(assistantId).one()`，取不到即 `403「无权访问该会话」` —— 越权在**第一步**就被挡掉，**不再只是"该记录存在"**。
  3. **链路改为从会话派生 + 逐层判空**：`assistant = getById(conv.getAssistantId())`（不再用入参 assistantId 去查，避免与 conv 断链后用到别人的助手/模型/Key）；`history` / `userMsg` / `asstMsg` 一律用 `conv.getId()`。
- **写法要点（可复用）**：下游查询喂 `conv.getId()` **而不是路径变量** —— 这样"有人删掉那句归属校验"会**直接编译不过**，把安全约束钉进类型里。
- **实测**：`compile_exit=0`（编译通过）；两个端点报的都是同一句「无权访问该会话」。
  ⚠️ **跨用户实测还没做** —— 需要后端在跑 + 两个账号的 token，期望：A 拿 B 的 `conversationId` 调 `send` / `get` 都返回 `403 无权访问该会话`。
- **残留**：`/auth/**` 整段放行的白名单问题，见 issue.md 问题 28。
