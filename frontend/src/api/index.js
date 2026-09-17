import { get, post } from './http'

// ============ 后端接口定义（前端版的 ApiService）============
// 字段名一律保持后端原样（含 maxtokens 这种非标准命名），前端不擅自改名。
// 除 /auth/** 之外的所有接口都要求 Authorization: Bearer <token>（http.js 已统一带上）。

// ---------- 认证 / 用户 ----------
// 返回 AuthVo{ id, account, nickname, token } —— 注册即登录，两个接口都发 token
export const apiRegister = (data) => post('/auth/register', data)
export const apiLogin = (data) => post('/auth/login', data)
// 用户列表（/auth/** 属于放行路径，不带 token 也能调，主要用于调试）
export const apiGetUsers = () => get('/auth/get')

// ---------- 提供商 / 模型 ----------
// 一次性注册 provider + model，返回 AModelVo{ id, name, modelId, protocol, baseUrl, providerId }
export const apiRegisterModel = (data) => post('/model/register', data)
// 配置 API Key，返回 ApiKeyVo{ apiKey }（脱敏后的值）
export const apiSetApiKey = (data) => post('/model/apikey', data)
// 当前用户的提供商列表 -> ProviderVo{ id, name, protocol, baseUrl, createTime, updateTime }
export const apiGetProviders = () => get('/model/getallprovider')
// 某个提供商下的模型列表 -> ModelVo{ id, name, modelId }
export const apiGetModels = (providerId) => get(`/model/getAllModel/${providerId}`)

// ---------- 助手 ----------
// 创建助手，返回 AssistantVo{ id, userId, modelId, name, prompt }
export const apiCreateAssistant = (data) => post('/assistant/create', data)
// 助手列表 -> List<AssistantVo>，字段与 create 完全一致（create / list 已统一）
export const apiGetAssistants = () => get('/assistant/getAllAssistant')

// ---------- 会话 / 消息 ----------
// 建会话，返回 ConversationVo{ id, userId, assistantId, systemPrompt, title, status }
// 注意：ConversationDto 里已经没有 userId 了（后端从 token 取）
export const apiCreateConversation = (data) => post('/conversation/create', data)
// 会话列表 -> ConversationEntity[]；路径**带 assistantId**（查的是"某个助手下的会话"）
export const apiGetConversations = (assistantId) =>
  get(`/conversation/${assistantId}/getAllConversation`)
// 发送消息，返回 ChatResponse{ content, model, inputTokens, outputtokens }
// 路径**带 assistantId**：后端按 id + userId + assistantId 三条件锁定会话，取不到即 403（防越权）
export const apiSendMessage = (conversationId, assistantId, data) =>
  post(`/chat/${conversationId}/${assistantId}/send`, data)
// 会话消息 -> MessagesEntity[]{ id, conversationId, role, content, seq, ... }
export const apiGetMessages = (conversationId, assistantId) =>
  get(`/chat/${conversationId}/${assistantId}/get`)
