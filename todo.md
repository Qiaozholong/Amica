# Amica 待办清单（与 issue.md 并列，路线图项）

> 这里放不属于「代码走查/测试发现的问题」的路线图待办，与 `issue.md` 分开维护。
> 优先级沿用 P1/P2/P3；P1 优先做。

---

## 1. 实现 JWT，业务接口从 token 取身份
- **依赖**：`pom.xml` 已引入 JJWT 0.12.6 全套（jjwt-api / impl / jackson），仅代码未用。
- **为什么**：现在 `userId` 全靠前端手传，任何客户端可伪造去读/写他人数据；JWT 让服务端签发、可验签，`userId` 从 token 解出。
- **内容**：
  - `JwtUtil`（签发/验签，subject=userId）
  - 登录成功返回 `{id, account, token}`
  - 拦截器解析 `Authorization: Bearer`，把 `userId` 存入请求属性
  - 业务接口改从 token 取 `userId`（不再信任手传）
- **联动**：直接影响 issue.md Issue 7 列表接口写法（`?userId=` 应改从 token 取）；一并解决 Issue 11（登录不返回 userId）。开发期可长过期 + 弱守卫，但身份层保留。
- **状态**：✅ **已完成**（归属校验的落地记录见 `finish.md` 问题 23 / 26 / 27 / 28；除 `/auth/**` 外所有接口都要求 `Bearer`）。落地要点：
  - `Config/JwtUtil`（签发 / 验签 / 取 userId）+ `Config/JwtAuthenticationFilter`（`@Component`，自动注册）；
  - **注册与登录都签发 token**，返回 `AuthVo{id, account, nickname, token}`；
  - Filter 验签后 `req.setAttribute("userId", ...)`，各 Controller 用 `@RequestAttribute("userId")` 取；DTO 里的 `userId` 字段**已全部删除**。
- **遗留（都在同一个文件里，可一次改完）**：
  - ① `extractUserId` 放在 try 之外 → 该报 401 的场景漏成容器默认 500（`issue.md` 20）；
  - ② 白名单 `path.contains("/auth/")` 过宽 → `GET /auth/get` 无 token 即可枚举用户（`issue.md` 28）。
