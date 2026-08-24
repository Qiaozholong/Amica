import { get, post } from './http'

// ============ 后端接口定义（这一层就是“前端版的 ApiService 接口”）============
// 每个函数对应 Controller 的一个接口，函数名即方法名，注释里标了后端路径和字段来源
// 字段名保持后端原样（含 maxtokens 这种非标准命名），前端不擅自改名

// POST /auth/register  注册
export const apiRegister = (data) => post('/auth/register', data)

// POST /auth/login     登录（注意：后端只返回 account，不返回 userId）
export const apiLogin = (data) => post('/auth/login', data)

// GET /auth/show       用户列表（测试用；唯一能拿到 userId 的接口）
export const apiShowUsers = () => get('/auth/show')

// POST /model/register 模型注册（model + provider 一体注册）
export const apiRegisterModel = (data) => post('/model/register', data)

// POST /model/apikey   提供商 API Key 加密存储，返回脱敏结果
export const apiSetApiKey = (data) => post('/model/apikey', data)

// POST /assistant/create 创建助手
export const apiCreateAssistant = (data) => post('/assistant/create', data)

// POST /conversation/create 创建会话
export const apiCreateConversation = (data) => post('/conversation/create', data)

// POST /chat/{conversationId}/send 发送消息
// 路径参数走模板字符串拼接（等价于后端 @PathVariable），data 是 @RequestBody
export const apiSendMessage = (conversationId, data) =>
  post(`/chat/${conversationId}/send`, data)
