# Amica

个人 AI 助手后端,基于 Spring Boot 构建。通过可扩展的模型提供商体系接入多种大模型 API,提供用户认证、模型注册、多轮对话等能力。项目处于早期开发阶段。

## 技术栈

- Java 21
- Spring Boot 4.1
- MyBatis-Plus 3.5.15 + MySQL
- Spring Security Crypto(BCrypt 密码加密、AES 密钥加密)
- JJWT 0.12.6(JWT 签发/验签,模块已就位)
- Lombok

## 功能状态

| 功能 | 状态 | 说明 |
| --- | --- | --- |
| 用户注册 / 登录 | 已实现 | BCrypt 密码加密；**两个入口都签发 JWT** |
| JWT 鉴权 | 已实现 | `JwtUtil` + `JwtAuthenticationFilter`；`/auth/**` 放行，其余路径需 `Bearer` token |
| 数据归属（`user_id`） | 部分实现 | user / assistant / model / provider 已按用户隔离；**conversation / chat 待做**（见 `issue.md` 23 / 28） |
| 模型提供商注册 | 已实现 | 支持 OpenAI 兼容协议(openai),可扩展；**归属到用户**，唯一键 `(user_id, protocol, base_url)` |
| API Key 管理 | 已实现 | AES 对称加密存储；改 Key 时校验归属 |
| 多轮对话(Chat) | 已实现 | 会话上下文拼装 + 消息落库 + 多提供商适配 |
| 助手模板管理 | 已实现 | POST /assistant/create；引用 model 时校验归属 |
| 会话管理 | 已实现 | POST /conversation/create, 默认标题"话题N" |
| 列表 / 查询接口 | 已实现 | 用户/提供商/模型/助手/会话/消息查询, 统一 Result 返回 |

## 项目结构

```
Amica/
├── sql/
│   └── init.sql                  # 初始化建表脚本(6 张表)
├── src/main/java/com/example/Amica/
│   ├── Amica.java                # 启动类
│   ├── Common/                   # Result 统一返回 / 全局异常处理
│   ├── Config/                   # Security(BCrypt)、AES 加密、JWT(JwtUtil / 过滤器)
│   ├── Controller/               # auth / chat / model 接口层
│   ├── Dto/                      # 请求体
│   ├── Vo/                       # 响应体
│   ├── Entity/                   # 数据表实体
│   ├── Mapper/                   # MyBatis-Plus Mapper
│   ├── Service/                  # 业务接口 + 实现
│   └── Provider/                 # AI 提供商抽象(openai/anthropic)与模型对象
└── pom.xml
```

## 数据模型

```
user
 ├── provider (user_id) ── model (user_id) ── assistant (user_id)
 │      提供商/密钥          模型                助手模板
 └── conversation (user_id) ── messages
          会话                    消息

  外键引用链：provider ← model ← assistant ← conversation ← messages
```

- `provider`:模型提供商(**归属用户** / 协议 / 端点 / AES 加密后的 API Key),唯一键 `(user_id, protocol, base_url)`
- `model`:模型(**归属用户**),唯一键 `(user_id, model_id)`,外键指向 `provider`
- `assistant`:助手模板(**归属用户** / 名称 / 系统提示词),外键指向 `user` 与 `model`
- `conversation`:会话(所属用户 / 标题 / 固化的 system_prompt / 扩展 JSON)
- `messages`:消息(user / assistant / system / tool,带 seq 排序)
  —— **不单独存 `user_id`**,靠 `conversation_id` 间接归属,避免冗余

> 前四张表各自带 `user_id`，形成"每用户独立"的归属模型；查询一律带上归属条件，
> 避免"改个 id 就能读别人数据"（BOLA / `issue.md` 23 / 28）。

## Chat 链路

`POST /chat/{conversationId}/send`

1. 按 `conversationId` 查询会话
2. 链路查询:conversation -> assistant -> model -> provider
3. 拉取该会话全部历史消息(按 `seq` 升序)
4. `system_prompt` 为空时回退到 assistant 的 prompt
5. 组装请求:`system_prompt` + 历史消息 + 新用户消息
6. 保存用户消息到 `messages` 表
7. `ProviderFactory` 按协议选择 `AiProvider` 实现
8. 保存助手回复到 `messages` 表(上下文持续累积)
9. 返回 `ChatResponse`

## 快速开始

1. 创建数据库(默认 `aiclientdemo`)
2. 执行 `sql/init.sql` 建表
3. 修改 `src/main/resources/application.yaml` 中的数据源配置
4. 启动:

```bash
./mvnw spring-boot:run
```

或直接通过 IDE 运行 `Amica.java`。

## 配置

| 配置项 | 说明 |
| --- | --- |
| `APP_ENCRYPT_KEY` | 环境变量:API Key 的 AES 加密密钥(对称加密),缺省值仅供开发 |
| `SALT` | 环境变量:密钥加盐 |
| `jwt.secret-key` | `application.yaml`:JWT 签名密钥(HMAC-SHA,需 ≥ 256 bit) |
| `jwt.expiration` | `application.yaml`:JWT 过期时长(毫秒),当前约 70 天 |

## API 一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/register` | 注册 |
| POST | `/auth/login` | 登录 |
| GET | `/auth/get` | 用户列表 |
| GET | `/auth/get/{id}` | 单用户查询 |
| POST | `/model/register` | 模型注册 |
| POST | `/model/apikey` | API Key 管理 |
| GET | `/model/getallprovider` | 提供商列表（API Key 已脱敏） |
| GET | `/model/getAllModel/{providerId}` | 某提供商下的模型列表 |
| POST | `/assistant/create` | 创建助手 |
| GET | `/assistant/getAllAssistant` | 助手列表 |
| POST | `/conversation/create` | 创建会话 |
| GET | `/conversation/{assistantId}/getAllConversation` | 某助手下的会话列表 |
| POST | `/chat/{conversationId}/send` | 发送消息 |
| GET | `/chat/{conversationId}/get` | 会话消息列表 |

> **鉴权与归属**：除 `/auth/**` 放行外，其余接口都要求 `Authorization: Bearer <token>`（由 `JwtAuthenticationFilter` 校验，注册/登录都签发）。
> 已接入归属校验的模块：**user / assistant / model / provider**（查询带 `user_id`、创建用 token 里的 `userId`、引用其它模块时校验归属是否相同）；
> **conversation / chat 待做**（详见 `issue.md` 问题 23 / 28）。
>
> ⚠️ **前端联调注意**：主键是 19 位雪花 ID，**超出 JS 的安全整数范围**（`Number.MAX_SAFE_INTEGER` 只有 16 位），
> 直接用 `JSON.parse` 会丢精度。前端已用 `quoteBigInts` 兜底（见 `issue.md` 问题 29）。

## 路线图

- 完成 JWT 接入的剩余部分:**conversation / chat 的归属校验**、过滤器异常处理(`issue.md` 20 / 23 / 28)
- 后端把 `Long` 序列化成字符串(雪花 ID 的治本方案,前端目前靠 `quoteBigInts` 绕开,`issue.md` 29)
- 本地模型兼容(Ollama 等走 OpenAI 兼容端点,API Key 可空化)
- 多模态消息(content 从字符串改为数组,支持图片)
- `messages` 表 `(conversation_id, seq)` 唯一索引,防并发重复
- 前端形象(虚拟形象 / 桌宠)
- 多种输出途径,如 TTS 语音播报
- 类 Agent 能力
- 长期记忆存储
- 个性化

## 声明

本项目仅供学习研究使用,接入的第三方服务与 API 请遵守对应平台规则。
