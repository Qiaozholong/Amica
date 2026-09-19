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

---

## 2. 多模态消息 + 模型能力声明（**能力驱动，格式由代码推导**）
- **为什么**：现在 `MessagesDto.content` / `ChatMessage.content` 都是纯文本，图片等输入无处安放。
  但**不能假设所有模型都吃多模态** —— 文本模型收到 `content` 数组，轻则 400，重则网关**静默丢图**、模型回一句"我没有看到任何图片"，
  而客户端拿到的是一条**正常回复**（与问题 1 的 `temperature`、问题 13 的 `stream` 同款"参数静默失效"，不该踩第三次）。
- **调研结论**（2026-06 查 RikkaHub / LobeChat / Cherry Studio / NextChat）：
  - **没有一家让用户"选消息格式"** —— 它们声明的都是**能力/模态**，格式是「能力 + 方言」推导出来的；
  - LobeChat / Cherry Studio **内置模型库**，能力随模型定义自带（勘误当 bug 提 issue / 走 PR 发版）；NextChat 靠 **model_id 名字猜**（`isVisionModel` + `VISION_MODELS` 环境变量）；
  - **但 Amica 是用户自带 `base_url` + 任意 `model_id`**（官方 / 中转 / 自建 / 以后的本地 Ollama），**既没有库、名字也猜不准 → 只能由用户在模型页声明**。
    这是本项选择"用户声明"而非"自动检测"的原因。
- **核心设计：两条正交轴**
  | 轴 | 位置 | 现状 | 决定什么 |
  | --- | --- | --- | --- |
  | 传输方言 | `provider.protocol` | ✅ 已有 | `buildBody` / `parse` 怎么写（openai / anthropic） |
  | 任务能力 | **`model.capability`** | 🆕 新增 | messages 怎么编码、响应怎么解码 |
  - 枚举值 `chat`（默认）/ `chat_vision` / `image_gen`；**默认 `chat` = 默认拒绝**，存量行自动落到最保守档，**零迁移**。
  - 生图**不是** vision 的超集（是兄弟节点）→ 用枚举而非布尔；将来真出现"能读又能写"的统一模型，再退化成 flags。
  - 格式**永远由能力推导，不暴露给用户**。
  - **逃生舱**：`provider` 加 `extra_body`（合并任意 JSON 进请求体）→ provider 怪癖不用改代码（抄 RikkaHub 的 `customBodies`）。
- **计划（每步可独立验收）**

  | 步 | 内容 | 负责 | 验收 |
  | --- | --- | --- | --- |
  | 1 | `model.capability` 字段（DDL `DEFAULT 'chat'`）+ Entity/DTO/VO + 注册时可选；**入口校验**：消息带 parts 但能力不支持 → `400「该模型不支持图片输入」`（**不许把上游 400 原样透传**，同问题 6 / 15 的毛病）；`AssistantVo` 带出 `capability` | 后端 | 旧模型 = `chat` 且行为与现在**完全一致**；前端能拿到能力 |
  | 2 | 前端：模型注册页加能力选择；聊天页**仅当所选助手的模型支持视觉时**才显示选图入口 | 前端 | 纯文本模型下选图入口**不出现** |
  | 3 | **parts 打通**（存储格式现在定，因为改它最贵）：`PartDto(type, text, url, mime)`；`ChatMessage` 加 `parts`（保留 2 参构造，老调用点不动）；parts 存 **`messages.metadata`**（JSON 列已存在 → **零 DDL**）；`parts == null` = 纯文本，**老数据天然兼容**；`buildBody` 里 parts → `content` 数组；图片来源**先只支持贴 https 链接**（零存储基础设施） | 后端 | 发一张**写着随机字符的图** → 模型答对内容；再发纯文字 → 老路径不坏 |
  | 4 | **历史窗口**（抄 RikkaHub 的 `contextMessageSize`）：每次只带最近 N 条消息的 parts，更早的降级为纯文本 —— 治掉现在"**全量重发**"的毛病 | 后端 | 连续多轮发图，请求体里旧图不再重复出现 |
  | 5 | **OCR 降级**（抄 RikkaHub）：设置里指定一个"OCR 模型"（任意支持视觉的模型）；当前模型不支持视觉时，图 → OCR 模型 → **文本替换原图**；结果**按图片哈希缓存**（同一张图不重复调用） | 后端 | 纯文本模型 + 附图 → 回答与图相关，且库里落的是提取文本 |
  | 6 | **生图**能力（`image_gen`）：独立编解码 + **响应侧解码**（生图返回的是图不是文字；`ChatServiceImpl` L136-140 的 `resp.content()` 与 `ChatResponse(content, model, input, output)` **都假定文本出** —— 输入解耦了输出没解耦等于没解耦） | 后端 | 到这一步**才**抽 `MessageCodec` 接口（第二个实现者出现才付抽象的账） |

- **前端清单（我负责）**：
  - 模型注册页：能力下拉（`chat` / `chat_vision` / `image_gen`）
  - 助手能力 → `ChatPanel` 据此决定是否显示选图入口（走 `GET /assistant/getAllAssistant`，不要去逐 provider 拉模型）
  - 第 3 步：待发图片的 URL 输入 / 缩略图；历史渲染读 `metadata.parts` 显示图片
  - ⚠️ 两个坑：① `metadata` 在后端是 **`String`**，JSON 响应里它是**字符串** → 前端要**再 `JSON.parse` 一次**；② 老规矩，id 一律走 `quoteBigInts`、不做 `Number()`
- **已定决策**：① 枚举三个值**够用**，不够再加；② "改模型能力"的端点**推迟**（先只支持注册时选，选错就重新注册，`uk_model_user` 会挡住重复注册）；③ **OCR 降级做**（性价比高于图片上传本身）。
- **待定的两个实现细节**：
  - OCR 模型放哪：建议 `user` 加一列 `ocr_model_id BIGINT NULL`，**不加外键** —— 避免和 `model.user_id` 形成双向依赖、把插入顺序搞纠缠；
  - OCR 缓存放哪：建议小表 `ocr_cache(hash CHAR(64) PRIMARY KEY, text MEDIUMTEXT, model_id VARCHAR(64), create_time)`，键用**图片字节的 SHA-256**，加个过期清理即可（RikkaHub 是 3 天）。
- **顺带的坑（施工时必踩）**：
  1. `MessagesDto.content` 现在是 **`@NotBlank String`** → **只发图不发字会被校验直接拒掉**，必须改成"`content` 空**且** parts 空才报错"；
  2. `messages.content` 是 **`TEXT`（65,535 字节）**，一张 200KB 的图 base64 后约 270KB **存不下** → 库里存路径，**发请求时按需转 data URL**（本地 `localhost` URL 上游取不到）；
  3. `metadata` 是 MySQL **JSON 列会校验**，必须用 Jackson 生成，手拼字符串会报 **3140**。
- **关联**：`issue.md` 13（`stream` 是空开关，与本项同属"能力/参数该不该暴露"）、22（未兜 `reasoning_content`）、5（无回滚，接流式后更明显）。
