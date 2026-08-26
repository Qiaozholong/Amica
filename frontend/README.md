# Amica 前端测试控制台

Vue 3 + Vite 单页应用，用于代替 Postman 联调 Amica 后端。
所有接口调用原始请求/响应都会记录在「调试日志」页，可逐步回看。

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

## 测试流程（页面顺序即业务顺序）

1. **登录 / 注册** — 注册后自动登录，并通过 `GET /auth/get` 匹配 userId
2. **模型提供商** — `POST /model/register` 注册模型，然后在提供商行内配置 API Key（`POST /model/apikey`）
3. **助手** — `POST /assistant/create`，需要用户在第一步的 userId + 模型的实体 id
4. **会话与对话** — `POST /conversation/create` 建会话，然后在聊天框里 `POST /chat/{id}/send` 多轮对话
5. **调试日志** — 每步请求的原文、响应、耗时

## 与后端接口的对应关系

| 页面操作 | 请求 |
| --- | --- |
| 注册 | `POST /auth/register` |
| 登录 | `POST /auth/login` |
| 拉取用户列表 | `GET /auth/get` |
| 注册模型 | `POST /model/register` |
| 配置 API Key | `POST /model/apikey` |
| 创建助手 | `POST /assistant/create` |
| 创建会话 | `POST /conversation/create` |
| 发送消息 | `POST /chat/{conversationId}/send` |

## 本控制台暴露的后端问题（均会在界面里给出提示）

- `POST /auth/login` 不返回 userId，只能靠 `GET /auth/get` 按账号匹配（建议登录返回体补 id）
- `POST /model/register` 不返回 model 实体 id，创建助手需要的 `modelId` 目前只能查数据库
- 发送消息时 `options`（temperature 等）后端未生效，恒定传 `ChatOptions.none()`
- 字段名是 `maxtokens`（非 `maxTokens`），拼错会被静默忽略
- 会话创建时 `systemPrompt` 空串判为「已覆盖」，聊天链路却按 `isBlank()` 回退，两边不一致（本页统一传 `null` 规避）
- 后端无列表接口，所有资源缓存在 localStorage，点击「清空本地数据」可重置

## 目录结构

> 所有源码文件都带**学习向注释**（模仿后端注释风格：字段注释用途、关键逻辑注释"为什么"，并标注对应后端概念）。
> 第一次看建议顺序：`api/http.js`（最像 Java）→ `store.js` → `App.vue` → 各面板组件 → `ChatPanel.vue`（最复杂）。

```
frontend/
├── index.html
├── vite.config.js          # 端口 + /api 代理
├── package.json
└── src/
    ├── main.js
    ├── App.vue             # 页签导航
    ├── styles.css
    ├── store.js            # 全局状态 + localStorage 持久化
    ├── api/
    │   ├── http.js         # 请求封装 + 调试日志
    │   └── index.js        # 与后端接口一一对应
    └── components/
        ├── AuthPanel.vue
        ├── ModelPanel.vue
        ├── AssistantPanel.vue
        ├── ChatPanel.vue
        └── DebugPanel.vue
```
