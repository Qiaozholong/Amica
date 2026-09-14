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
// 返回 AssistantVo{ assistantId, userName, modelName, name, prompt }
export const apiCreateAssistant = (data) => post('/assistant/create', data)
// 助手列表 -> AssistantEntity{ id, userId, modelId, name, prompt, ... }（注意是 id 不是 assistantId）
export const apiGetAssistants = () => get('/assistant/getAllAssistant')

// ---------- 会话 / 消息 ----------
// 返回 ConversationVo{ id, title, status }
export const apiCreateConversation = (data) => post('/conversation/create', data)
// 会话列表 -> ConversationEntity{ id, userId, assistantId, title, systemPrompt, ... }
export const apiGetConversations = () => get('/conversation/getAllConversation')
// 发送消息，返回 ChatResponse{ content, model, inputTokens, outputtokens }
export const apiSendMessage = (conversationId, data) =>
  post(`/chat/${conversationId}/send`, data)
// 会话消息 -> MessagesEntity[]{ id, conversationId, role, content, seq, ... }
export const apiGetMessages = (conversationId) => get(`/chat/${conversationId}/get`)
