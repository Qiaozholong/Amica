<script setup>
import { reactive, ref, watch } from 'vue'
import { apiCreateConversation, apiSendMessage } from '../api'
import { state, upsert } from '../store'

const form = reactive({ userId: '', assistantId: '', title: '', systemPrompt: '' })
const msg = ref('')
const err = ref('')
const busy = ref(false)

// 当前正在对话的会话
const currentId = ref(null)
const chatText = ref('')
const chatBusy = ref(false)
const chatErr = ref('')
const chatForm = reactive({ maxtokens: 0, temperature: '', topP: '', reasoningEffort: '', stream: false })

const current = () => state.conversations.find((c) => c.id === currentId.value)

async function copy(text) {
  await navigator.clipboard.writeText(text)
}

async function doCreate() {
  err.value = ''
  msg.value = ''
  busy.value = true
  try {
    const vo = await apiCreateConversation({
      userId: Number(form.userId),
      assistantId: Number(form.assistantId),
      title: form.title || null,
      systemPrompt: form.systemPrompt || null,
    })
    upsert(
      state.conversations,
      { id: vo.id, userId: Number(form.userId), assistantId: Number(form.assistantId), title: vo.title, status: vo.status, systemPrompt: form.systemPrompt || null, messages: [] },
      (c) => c.id === vo.id
    )
    currentId.value = vo.id
    msg.value = `会话创建成功。id=${vo.id}，标题「${vo.title}」，状态=${vo.status}`
  } catch (e) {
    err.value = e.message
  } finally {
    busy.value = false
  }
}

async function doSend() {
  const conv = current()
  if (!conv || !chatText.value.trim() || chatBusy.value) return
  chatErr.value = ''
  const content = chatText.value.trim()
  chatText.value = ''
  chatBusy.value = true

  // 组装请求体：字段名与后端完全一致（maxtokens，非 maxTokens！）
  const options = {
    temperature: chatForm.temperature === '' ? null : Number(chatForm.temperature),
    topP: chatForm.topP === '' ? null : Number(chatForm.topP),
    reasoningEffort: chatForm.reasoningEffort || null,
    stream: chatForm.stream,
  }
  const body = {
    content,
    maxtokens: Number(chatForm.maxtokens) || 0,
    options,
  }

  conv.messages.push({ role: 'user', content, seq: (conv.messages.at(-1)?.seq ?? -1) + 1 })
  try {
    const resp = await apiSendMessage(conv.id, body)
    conv.messages.push({ role: 'assistant', content: resp.content, model: resp.model, tokens: `${resp.inputTokens} in / ${resp.outputtokens} out`, seq: conv.messages.at(-1).seq + 1 })
  } catch (e) {
    // 后端先把用户消息落库、provider 失败时不回滚 —— 下次发消息仍会把这句带进上下文
    conv.messages.push({ role: 'meta', content: `⚠ 请求失败：${e.message}（后端已把这条用户消息落库，重发时仍会带进上下文）` })
  } finally {
    chatBusy.value = false
  }
}

watch(currentId, () => {
  chatErr.value = ''
})
</script>

<template>
  <div class="card">
    <h2>会话与对话</h2>

    <form class="row" @submit.prevent="doCreate">
      <div class="field">
        <label>用户</label>
        <select v-model="form.userId">
          <option value="" disabled>选择用户</option>
          <option v-for="u in state.users" :key="u.id" :value="u.id">{{ u.account }} (id={{ u.id }})</option>
        </select>
      </div>
      <div class="field" style="flex: 1">
        <label>助手模板</label>
        <select v-model="form.assistantId">
          <option value="" disabled>选择助手</option>
          <option v-for="a in state.assistants" :key="a.assistantId" :value="a.assistantId">
            {{ a.name }} (id={{ a.assistantId }})
          </option>
        </select>
      </div>
      <div class="field">
        <label>标题（留空自动生成"话题N"）</label>
        <input v-model="form.title" placeholder="可留空" />
      </div>
      <div class="field" style="flex: 1">
        <label>对话级 systemPrompt（覆盖助手 prompt）</label>
        <textarea v-model="form.systemPrompt" placeholder="可留空" />
      </div>
      <button class="primary" type="submit" :disabled="busy || !form.userId || !form.assistantId">
        创建会话
      </button>
    </form>
    <div class="hint">
      判定隐藏坑：后端创建时用 <kbd>systemPrompt == null</kbd> 判断「已覆盖」，但聊天链路用 <kbd>isBlank()</kbd> 判断是否回退。
      这里直接传 <kbd>null</kbd>，避免传空字符串时两边结论不一致。
    </div>
    <div v-if="msg" class="msg ok">{{ msg }}</div>
    <div v-if="err" class="msg err">{{ err }}</div>

    <h3>会话列表（本地缓存）</h3>
    <table class="list">
      <thead>
        <tr><th>id</th><th>标题</th><th>覆盖状态</th><th>消息数</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="c in state.conversations" :key="c.id" :class="{ 'active-row': c.id === currentId }">
          <td class="mono">{{ c.id }}</td>
          <td>{{ c.title }}</td>
          <td>{{ c.status }}</td>
          <td>{{ c.messages.length }}</td>
          <td>
            <button class="ghost" @click="currentId = c.id">打开</button>
            <button class="ghost" @click="copy(c.id)">复制id</button>
          </td>
        </tr>
        <tr v-if="!state.conversations.length">
          <td colspan="5" class="hint gray" style="border: none">暂无会话</td>
        </tr>
      </tbody>
    </table>

    <template v-if="current()">
      <h3>当前会话：{{ current().title }}（id={{ current().id }}）</h3>
      <div class="hint gray">
        发送体字段为后端原样：<kbd>maxtokens</kbd>（不是 maxTokens，前端拼错会被静默忽略）；
        <kbd>options</kbd> 后端<b>当前未生效</b>（ChatServiceImpl 恒传 <kbd>ChatOptions.none()</kbd>），此处发送仅为观察请求体。
      </div>

      <div class="chat-box">
        <div v-for="(m, i) in current().messages" :key="i" class="bubble" :class="m.role">
          <template v-if="m.role === 'meta'">{{ m.content }}</template>
          <template v-else>
            {{ m.content }}
            <div v-if="m.tokens" style="font-size: 11px; opacity: 0.7; margin-top: 4px">{{ m.tokens }}</div>
          </template>
        </div>
        <div v-if="!current().messages.length" class="meta" style="align-self: center">还没有消息</div>
        <div v-if="chatBusy" class="meta">思考中…</div>
      </div>

      <form class="row" style="margin-top: 8px" @submit.prevent="doSend">
        <div class="field" style="flex: 3">
          <label>消息内容</label>
          <textarea v-model="chatText" style="min-width: 100%" @keydown.enter.exact.prevent="doSend" />
        </div>
        <div class="field">
          <label>maxtokens（0=后端默认1024）</label>
          <input v-model.number="chatForm.maxtokens" type="number" min="0" style="width: 120px" />
        </div>
        <div class="field">
          <label>temperature</label>
          <input v-model="chatForm.temperature" type="number" step="0.1" min="0" max="2" style="width: 90px" placeholder="不传" />
        </div>
        <div class="field">
          <label>topP</label>
          <input v-model="chatForm.topP" type="number" step="0.1" min="0" max="1" style="width: 90px" placeholder="不传" />
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
      <div v-if="chatErr" class="msg err">{{ chatErr }}</div>
    </template>
  </div>
</template>
