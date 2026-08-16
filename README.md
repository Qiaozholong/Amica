# Amica

个人 AI 助手后端,基于 Spring Boot 构建。通过可扩展的模型提供商体系接入多种大模型 API,提供用户认证、模型注册、多轮对话等能力。项目处于早期开发阶段。

## 技术栈

- Java 21
- Spring Boot 4.1
- MyBatis-Plus 3.5.15 + MySQL
- Spring Security Crypto(BCrypt 密码加密、AES 密钥加密)
- JJWT 0.12.6(JWT 鉴权,规划中)
- Lombok

## 功能状态

| 功能 | 状态 | 说明 |
| --- | --- | --- |
| 用户注册 / 登录 | 已实现 | BCrypt 密码加密 |
| JWT 鉴权 | 规划中 | 等主要接口稳定后再接入,避免影响联调 |
| 模型提供商注册 | 已实现 | 支持 OpenAI 兼容协议(openai),可扩展 |
| API Key 管理 | 已实现 | AES 对称加密存储 |
| 多轮对话(Chat) | 已实现 | 会话上下文拼装 + 多提供商适配 |
| 助手模板管理 | 规划中 | AssistantController 待实现 |
| 会话管理 | 规划中 | ConversationController 待实现 |

## 项目结构

```
Amica/
├── sql/
│   └── init.sql                  # 初始化建表脚本(6 张表)
├── src/main/java/com/example/Amica/
│   ├── Amica.java                # 启动类
│   ├── Common/                   # Result 统一返回 / 全局异常处理
│   ├── Config/                   # Security(BCrypt)、AES 加密配置
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
 ├── assistant   (user_id, 创建者) ── model ── provider
 │                                    model_id   provider_id
 └── conversation (user_id, 所属用户)
      └── messages (conversation_id, 消息)
          conversation 可选关联 assistant(来源模板, system_prompt 创建时固化)
```

- `provider`:模型提供商(协议 / 端点 / AES 加密后的 API Key)
- `model`:模型,属于某个提供商
- `assistant`:助手模板(名称 + 系统提示词)
- `conversation`:会话(标题 + 固化的 system_prompt + 扩展 JSON)
- `messages`:消息(user / assistant / system / tool,带 seq 排序)

## Chat 链路

`POST /chat/{conversationId}/send`

1. 按 `conversationId` 查询会话
2. 链路查询:conversation -> assistant -> model -> provider
3. 拉取该会话全部历史消息(按 `seq` 升序)
4. 组装请求:`system_prompt` + 历史消息 + 新用户消息
5. `ProviderFactory` 按协议选择 `AiProvider` 实现
6. 返回 `ChatResponse`

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

| 环境变量 | 说明 |
| --- | --- |
| `APP_ENCRYPT_KEY` | API Key 的 AES 加密密钥(对称加密) |
| `SALT` | 密钥加盐 |

## API 一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/register` | 注册 |
| POST | `/auth/login` | 登录 |
| GET | `/auth/show` | 用户列表(测试用) |
| POST | `/model/register` | 模型注册 |
| POST | `/model/apikey` | API Key 管理 |
| POST | `/chat/{conversationId}/send` | 发送消息 |

## 路线图

- 接入 JWT 鉴权
- 助手模板管理、会话管理接口
- 前端形象(虚拟形象 / 桌宠)
- 多种输出途径,如 TTS 语音播报
- 类 Agent 能力
- 长期记忆存储
- 个性化

## 声明

本项目仅供学习研究使用,接入的第三方服务与 API 请遵守对应平台规则。
