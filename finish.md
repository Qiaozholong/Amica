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

- **残留**：① 所有列表**未按 userId 过滤**（全量返回）→ **issue.md 问题 23**；② 前端仍用 localStorage → 见 issue.md 的「前端待办」。

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
- **修法**：给 `ModelVo` 与 `AModelVo` 各加 `private Long id;`。`BeanUtils.copyProperties` 按同名自动带出，**逻辑无需改动**（`registerModel` 与 `getAllModels` 同时受益）。

### 19. `reasoningEffort` 三目分支写反 ✅已修正
- **原现象**：`? null : o.getReasoningEffort()` 分支写反 → ① `o == null` 时执行 `o.getReasoningEffort()` → NPE；② 用户传了值反而被丢弃，等于恒不传。
- **修法**：改为 `o != null && o.getReasoningEffort() != null ? o.getReasoningEffort() : null`（传了就收，没传保持 null 不传）。
