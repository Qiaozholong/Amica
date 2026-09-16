<script setup>
// ============ 会话与对话页签 ============
// 后端接口现状：
//   GET  /conversation/{assistantId}/getAllConversation  -> 某个助手下的会话列表（按助手筛）
//   POST /conversation/create                            -> 建会话（userId 从 token 取，前端不再传）
//   GET  /chat/{conversationId}/get                      -> 该会话的消息
//   POST /chat/{conversationId}/send                      -> 发消息
//
// 因为会话列表是**按助手**查的，本页的交互改成"先选助手，再看它的会话"：
// 顶部那个助手选择器既是列表的筛选条件，也是"创建会话"的目标。

import { onMounted, reactive, ref, watch } from 'vue'
import {
  apiCreateConversation,
  apiSendMessage,
  apiGetConversations,
  apiGetMessages,
  apiGetAssistants,
} from '../api'
import { isLoggedIn } from '../store'

// 当前选中的助手：决定会话列表范围 + 新会话建在谁名下
const currentAssistantId = ref('')
// 创建会话表单（ConversationDto：assistantId / title / systemPrompt；userId 已由后端从 token 取）
const form = reactive({ title: '', systemPrompt: '' })
const msg = ref('')
const err = ref('')
const busy = ref(false)

const assistants = ref([])
const conversations = ref([])

const currentId = ref(null)
const messages = ref([])
const chatText = ref('')
const chatBusy = ref(false)
const chatErr = ref('')
// maxtokens 默认 4096：思考模型会先消耗输出预算做推理，1024 很容易把正文挤掉变成空回复
const chatForm = reactive({
  maxtokens: 4096,
  temperature: '',
  topP: '',
  reasoningEffort: '',
  stream: false,
})

function current() {
  return conversations.value.find((c) => c.id === currentId.value)
}

// 列表接口返回的是 ConversationEntity（没有 status 字段），覆盖状态按 systemPrompt 派生
function statusOf(c) {
  return c.systemPrompt && c.systemPrompt.trim() ? '已覆盖' : '未覆盖'
}

async function copy(text) {
  await navigator.clipboard.writeText(text)
}

async function loadAssistants() {
  if (!isLoggedIn()) return
  try {
    assistants.value = await apiGetAssistants()
    // 还没选过助手时：只有一个就自动选中，多个就让用户选
    if (!currentAssistantId.value && assistants.value.length === 1) {
      currentAssistantId.value = assistants.value[0].id
    }
  } catch (e) {
    err.value = e.message
  }
}

async function loadConversations() {
  if (!isLoggedIn() || !currentAssistantId.value) {
    conversations.value = []
    return
  }
  try {
    conversations.value = await apiGetConversations(currentAssistantId.value)
  } catch (e) {
    err.value = e.message
  }
}

async function loadMessages() {
  if (!currentId.value) return
  try {
    messages.value = await apiGetMessages(currentId.value)
  } catch (e) {
    chatErr.value = e.message
  }
}

async function loadAll() {
  err.value = ''
  await loadAssistants()
  await loadConversations()
}

async function doCreate() {
  err.value = ''
  msg.value = ''
  busy.value = true
  try {
    // id 一律不要 Number() —— 雪花 id 超出 JS 安全整数，转数字会丢精度（见 issue 29）
    const vo = await apiCreateConversation({
      assistantId: currentAssistantId.value,
      title: form.title || null,
      systemPrompt: form.systemPrompt || null,
    })
    msg.value = `会话创建成功。id=${vo.id}，标题「${vo.title}」，状态=${vo.status}`
    form.title = ''
    form.systemPrompt = ''
    await loadConversations()
    await openConversation(vo.id)
  } catch (e) {
    err.value = e.message
  } finally {
    busy.value = false
  }
}

async function openConversation(id) {
  currentId.value = id
  chatErr.value = ''
  await loadMessages()
}

async function doSend() {
  const content = chatText.value.trim()
  if (!content || chatBusy.value || !currentId.value) return
  chatErr.value = ''
  chatText.value = ''
  chatBusy.value = true
  try {
    const options = {
      temperature: chatForm.temperature === '' ? null : Number(chatForm.temperature),
      topP: chatForm.topP === '' ? null : Number(chatForm.topP),
      reasoningEffort: chatForm.reasoningEffort || null,
      stream: chatForm.stream,
    }
    await apiSendMessage(currentId.value, {
      content,
      maxtokens: Number(chatForm.maxtokens) || 0,
      options,
    })
    // 以服务端为准重新拉取（用户消息 + 助手回复都在库里）
    await loadMessages()
  } catch (e) {
    chatErr.value = e.message
    // 失败时后端已把用户消息落库（issue.md 问题 5 未修），刷新一下让现场可见
    await loadMessages()
  } finally {
    chatBusy.value = false
  }
}

// 换助手 -> 清空当前会话与消息，重新拉该助手的会话列表
watch(currentAssistantId, async () => {
  currentId.value = null
  messages.value = []
  chatErr.value = ''
  msg.value = ''
  await loadConversations()
})

// 切换会话时清掉上一轮的错误提示
watch(currentId, () => {
  chatErr.value = ''
})

onMounted(loadAll)
</script>

<template>
  <div class="card">
    <h2>会话与对话</h2>

    <div class="hint gray">
      会话列表是<b>按助手</b>查的（<kbd>GET /conversation/{assistantId}/getAllConversation</kbd>），
      所以先选助手 —— 选中的助手同时也是「创建会话」的目标。
      <kbd>userId</kbd> 由后端从 token 取，前端不再传。
    </div>

    <!-- 助手选择器：既是列表筛选条件，也是新建会话的目标 -->
    <div class="row" style="margin-bottom: 4px">
      <div class="field" style="flex: 2">
        <label>助手（决定会话列表范围 + 创建目标）</label>
        <select v-model="currentAssistantId">
          <option value="" disabled>选择助手</option>
          <option v-for="a in assistants" :key="a.id" :value="a.id">
            {{ a.name }} (id={{ a.id }})
          </option>
        </select>
      </div>
      <button class="ghost" type="button" @click="loadAll">刷新</button>
    </div>

    <div v-if="isLoggedIn() && !assistants.length" class="hint">
      还没有助手 —— 请先到「助手」页创建一个。
    </div>
    <div v-if="!isLoggedIn()" class="hint">未登录 —— 请先到「登录 / 注册」页登录。</div>

    <template v-if="currentAssistantId">
      <!-- 创建会话 -->
      <form class="row" @submit.prevent="doCreate">
        <div class="field">
          <label>标题（留空自动生成"话题N"）</label>
          <input v-model="form.title" placeholder="可留空" />
        </div>
        <div class="field" style="flex: 3">
          <label>对话级 systemPrompt（留空则回退到助手 prompt）</label>
          <textarea v-model="form.systemPrompt" placeholder="可留空" />
        </div>
        <button class="primary" type="submit" :disabled="busy">创建会话</button>
      </form>

      <div v-if="msg" class="msg ok">{{ msg }}</div>
      <div v-if="err" class="msg err">{{ err }}</div>

      <h3>
        会话列表（该助手下）
        <button class="ghost" style="margin-left: 8px" @click="loadConversations">刷新</button>
      </h3>
      <table class="list">
        <thead>
          <tr><th>id</th><th>标题</th><th>systemPrompt 覆盖</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="c in conversations" :key="c.id" :class="{ 'active-row': c.id === currentId }">
            <td class="mono">{{ c.id }}</td>
            <td>{{ c.title }}</td>
            <td>{{ statusOf(c) }}</td>
            <td style="white-space: nowrap">
              <button class="ghost" @click="openConversation(c.id)">打开</button>
              <button class="ghost" style="margin-left: 4px" @click="copy(c.id)">复制id</button>
            </td>
          </tr>
          <tr v-if="!conversations.length">
            <td colspan="4" class="hint gray" style="border: none">该助手下暂无会话，先创建一个</td>
          </tr>
        </tbody>
      </table>

      <template v-if="current()">
        <h3>
          当前会话：{{ current().title }}（id={{ current().id }}）
          <button class="ghost" style="margin-left: 8px" @click="loadMessages">刷新消息</button>
        </h3>
        <div class="hint gray">
          发送体字段名是后端原样：<kbd>maxtokens</kbd>（不是 maxTokens）。
          <kbd>maxtokens</kbd> 默认给 <b>4096</b> —— 思考模型会先花预算推理，1024 容易让正文变成空回复。
          <kbd>options</kbd>（temperature / topP / reasoningEffort）后端已生效。
        </div>

        <div class="chat-box">
          <div v-for="m in messages" :key="m.id" class="bubble" :class="m.role">
            {{ m.content }}
            <div style="font-size: 11px; opacity: 0.6; margin-top: 4px">
              seq={{ m.seq }} · {{ m.role }}
            </div>
          </div>
          <div v-if="!messages.length" class="meta" style="align-self: center">还没有消息</div>
          <div v-if="chatBusy" class="meta">思考中…</div>
        </div>

        <form class="row" style="margin-top: 8px" @submit.prevent="doSend">
          <div class="field" style="flex: 3">
            <label>消息内容（回车发送 / Shift+回车换行）</label>
            <textarea v-model="chatText" style="min-width: 100%" @keydown.enter.exact.prevent="doSend" />
          </div>
          <div class="field">
            <label>maxtokens</label>
            <input v-model.number="chatForm.maxtokens" type="number" min="0" style="width: 110px" />
          </div>
          <div class="field">
            <label>temperature</label>
            <input v-model="chatForm.temperature" type="number" step="0.1" min="0" max="2" style="width: 90px" placeholder="默认0.8" />
          </div>
          <div class="field">
            <label>topP</label>
            <input v-model="chatForm.topP" type="number" step="0.1" min="0" max="1" style="width: 90px" placeholder="默认0.9" />
          </div>
          <div class="field">
            <label>reasoningEffort</label>
            <select v-model="chatForm.reasoningEffort" style="width: 110px">
              <option value="">不传</option>
              <option value="low">low</option>
              <option value="medium">medium</option>
              <option value="high">high</option>
            </select>
          </div>
          <div class="field">
            <label>stream</label>
            <input v-model="chatForm.stream" type="checkbox" style="width: 16px" />
          </div>
          <button class="primary" type="submit" :disabled="chatBusy || !chatText.trim()">发送</button>
        </form>
        <div class="hint gray" style="margin-top: 6px">
          ⚠ <kbd>stream</kbd> 勾上会返回 SSE 流，后端目前按普通 JSON 解析（issue.md 问题 13，未修），建议保持不勾。
        </div>
        <div v-if="chatErr" class="msg err">{{ chatErr }}</div>
      </template>
    </template>
  </div>
</template>
