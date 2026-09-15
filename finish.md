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
  | 消息 | `GET /chat/{conversationId}/get` |

- **残留（后续已收敛）**：① 归属过滤 —— user / assistant / model / provider 已按 `userId` 过滤；**conversation / chat 待做**（issue.md 问题 23 / 28）；② 前端已整体改为服务端拉取（见 issue.md 的「前端状态」）。

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
