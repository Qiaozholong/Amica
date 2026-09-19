# Amica 前端测试控制台

Vue 3 + Vite 单页应用，用于代替 Postman 联调 Amica 后端。
所有接口调用的原始请求/响应都会记录在「调试日志」页，可逐步回看。

## 启动

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

默认通过 Vite 代理把 `/api/*` 转发到 `http://localhost:9000`（后端未配 CORS，故不在浏览器里直连）。
如需换后端地址，修改 `vite.config.js` 里的 `server.proxy` 即可。

构建产物：

```bash
npm run build      # 输出 dist/，可托管到任意静态服务器或 Nginx
```

## 登录态与鉴权（先看这段）

- 后端 JWT 过滤器对**除 `/auth/**` 之外的所有路径**都要求 `Authorization: Bearer <token>`，
  所以**必须先登录 / 注册**，否则一切接口都返回 401。
- 登录 / 注册都返回 `AuthVo{ id, account, nickname, token }`，四个字段一起存进 `store.js`
  （落盘到 localStorage 的 `amica-auth`），`http.js` 会自动把 token 加到请求头。
- 收到 401 时会**自动清掉登录态**并回到未登录状态。
- 资源（provider / model / assistant / conversation）**不再缓存在 localStorage**，每次都从服务端拉取。

## 雪花 ID 保护（写新代码前必看）

后端主键是 19 位雪花 ID（如 `2099362928699224066`），**超出 JS 的 `Number.MAX_SAFE_INTEGER`（16 位）**。
直接 `JSON.parse` 会把 id 四舍五入（实测 `...224066` 变成 `...224000`），前端再把 id 发回去就永远对不上
—— 表现是「提供商不存在或无权操作」「模型不存在」这类看起来很莫名的错误。

`api/bigint.js` 的 `quoteBigInts()` 在 `JSON.parse` **之前**把值位置上的 ≥16 位整数转成字符串
（逐字符扫描，不会误伤字符串内容），而后端 Jackson 会把数字字符串自动转回 `Long`。

**写新代码时务必注意：id 一律不要做 `Number()` 转换**，字符串原样传回即可。

## 测试流程（页面顺序即业务顺序）

1. **登录 / 注册** — 注册即登录，直接用响应里的 `id` 与 `token`
2. **模型提供商** — `POST /model/register` 注册模型（响应里的 `id` 就是模型实体 id），
   然后在提供商行内配置 API Key（`POST /model/apikey`）；可展开查看该提供商下的模型
3. **助手** — `POST /assistant/create`；`userId` 由后端从 token 取，前端只需选 model（`modelId`）
4. **会话与对话** — 先在页顶部**选一个助手**（会话列表是**按助手**查的），
   再 `POST /conversation/create` 在该助手下建会话，然后在聊天框里 `POST /chat/{conversationId}/{assistantId}/send` 多轮对话
5. **调试日志** — 每步请求的原文、响应、耗时

## 与后端接口的对应关系

| 页面操作 | 请求 |
| --- | --- |
| 注册 / 登录 | `POST /auth/register`、`POST /auth/login` |
| 用户列表（调试用） | `GET /auth/get` |
| 注册模型（含 provider） | `POST /model/register` |
| 配置 API Key | `POST /model/apikey` |
| 提供商列表 | `GET /model/getallprovider` |
| 某提供商下的模型 | `GET /model/getAllModel/{providerId}` |
| 创建助手 / 助手列表 | `POST /assistant/create`、`GET /assistant/getAllAssistant` |
| 创建会话 / 某助手的会话列表 | `POST /conversation/create`、`GET /conversation/{assistantId}/getAllConversation` |
| 发送消息 / 拉取消息 | `POST /chat/{conversationId}/{assistantId}/send`、`GET /chat/{conversationId}/{assistantId}/get` |

## 已知的后端限制（界面里会给出对应提示）

- 字段名是 `maxtokens`（非 `maxTokens`），拼错会被**静默忽略**（`issue.md` 3）
- `stream` 勾选框**已禁用**并标注「暂不支持」：后端 `toChatOptions` 没把 stream 往下传，勾了也不生效（`issue.md` 13）
- 会话创建时 `systemPrompt` 传空串会被判为「已覆盖」，但聊天链路按 `isBlank()` 回退，两边不一致（本页统一传 `null`）（`issue.md` 2）
- ✅ 已修的两条：`maxTokens` 默认值已提到 **4096**（`issue.md` 21）；chat 两个端点**已做会话归属校验**
  （按 `id + user_id + assistant_id` 锁定，不属于自己 → 403「无权访问该会话」），前端照常带 token 即可（`issue.md` 28）
- ⚠️ `GET /auth/get`（本页「用户列表（调试用）」）**后端仍不需要 token**（白名单过宽，`issue.md` 28）—— 仅供调试，别当正式功能依赖

## 目录结构

> 所有源码文件都带**学习向注释**（模仿后端注释风格：字段注释用途、关键逻辑注释"为什么"，并标注对应后端概念）。
> 第一次看建议顺序：`api/http.js`（最像 Java）→ `api/bigint.js` → `store.js` → `App.vue` → 各面板组件 → `ChatPanel.vue`（最复杂）。

```
frontend/
├── index.html
├── vite.config.js          # 端口 + /api 代理
├── package.json
└── src/
    ├── main.js
    ├── App.vue             # 页签导航 + 登录态
    ├── styles.css
    ├── store.js            # 登录态（id / account / nickname / token）+ localStorage 持久化
    ├── api/
    │   ├── bigint.js       # 雪花 ID 保护（quoteBigInts）
    │   ├── http.js         # 请求封装 + Authorization 头 + 调试日志
    │   └── index.js        # 与后端接口一一对应
    └── components/
        ├── AuthPanel.vue
        ├── ModelPanel.vue
        ├── AssistantPanel.vue
        ├── ChatPanel.vue
        └── DebugPanel.vue
```
