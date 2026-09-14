<script setup>
// ============ 会话与对话页签 ============
// 会话列表与消息都从服务端拉取：
//   GET /conversation/getAllConversation   -> 会话列表
//   GET /chat/{conversationId}/get         -> 该会话的消息
// 发消息后面板会**重新拉一次消息**，保证界面和数据库一致（不再靠本地 push 模拟）。

import { onMounted, reactive, ref, watch } from 'vue'
import {
  apiCreateConversation,
  apiSendMessage,
  apiGetConversations,
  apiGetMessages,
  apiGetAssistants,
} from '../api'
import { state, isLoggedIn } from '../store'

const form = reactive({ assistantId: '', title: '', systemPrompt: '' })
const msg = ref('')
const err = ref('')
const busy = ref(false)

const conversations = ref([])
const assistants = ref([])

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

// 会话归属状态：列表接口返回的是实体，没有 status 字段，这里按 systemPrompt 派生
function statusOf(c) {
  return c.systemPrompt && c.systemPrompt.trim() ? '已覆盖' : '未覆盖'
}

async function copy(text) {
  await navigator.clipboard.writeText(text)
}

async function loadConversations() {
  if (!isLoggedIn()) return
  try {
    conversations.value = await apiGetConversations()
  } catch (e) {
    err.value = e.message
  }
}

async function loadAssistants() {
  if (!isLoggedIn()) return
  try {
    assistants.value = await apiGetAssistants()
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
  await loadConversations()
  await loadAssistants()
}

async function doCreate() {
  err.value = ''
  msg.value = ''
  busy.value = true
  try {
    // 注意：后端会话模块还没接 JWT（ConversationDto 仍要求 userId），所以这里仍要传
    const vo = await apiCreateConversation({
      userId: state.id,
      assistantId: Number(form.assistantId),
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
    // 失败时后端已把用户消息落库（问题 5 未修），刷新一下让现场可见
    await loadMessages()
  } finally {
    chatBusy.value = false
  }
}

// 切换会话时清掉上一轮的错误提示
watch(currentId, () => {
  chatErr.value = ''
})

onMounted(loadAll)
</script>

<template>
  <div class="card">
    <h2>会话与对话</h2>

    <form class="row" @submit.prevent="doCreate">
      <div class="field" style="flex: 2">
        <label>助手模板</label>
        <select v-model="form.assistantId">
          <option value="" disabled>选择助手</option>
          <option v-for="a in assistants" :key="a.id" :value="a.id">
            {{ a.name }} (id={{ a.id }})
          </option>
        </select>
      </div>
      <div class="field">
        <label>标题（留空自动生成"话题N"）</label>
        <input v-model="form.title" placeholder="可留空" />
      </div>
      <div class="field" style="flex: 2">
        <label>对话级 systemPrompt（覆盖助手 prompt）</label>
        <textarea v-model="form.systemPrompt" placeholder="可留空" />
      </div>
      <button class="primary" type="submit" :disabled="busy || !isLoggedIn() || !form.assistantId">
        创建会话
      </button>
      <button class="ghost" type="button" @click="loadAll">刷新</button>
    </form>
    <div class="hint gray">
      会话模块的后端接口还没接 JWT（<kbd>ConversationDto</kbd> 仍要求 <kbd>userId</kbd>），
      所以这里仍然要把它传上去 —— 等会话模块也改成从 token 取，这个字段就能删掉。
    </div>

    <div v-if="msg" class="msg ok">{{ msg }}</div>
    <div v-if="err" class="msg err">{{ err }}</div>

    <h3>
      会话列表（GET /conversation/getAllConversation）
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
          <td colspan="4" class="hint gray" style="border: none">
            {{ isLoggedIn() ? '暂无会话' : '未登录 —— 请先到「登录 / 注册」页登录' }}
          </td>
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
        ⚠ <kbd>stream</kbd> 勾上会返回 SSE 流，后端目前按普通 JSON 解析（issue 13，未修），建议保持不勾。
      </div>
      <div v-if="chatErr" class="msg err">{{ chatErr }}</div>
    </template>
  </div>
</template>
