<script setup>
import { computed, reactive, ref } from 'vue'
import { apiCreateAssistant } from '../api'
import { state, upsert } from '../store'

const form = reactive({ userId: '', modelId: '', name: '', prompt: '' })
const msg = ref('')
const err = ref('')
const busy = ref(false)

const usableModels = computed(() => state.models.filter((m) => m.id))

async function copy(text) {
  await navigator.clipboard.writeText(text)
}

async function doCreate() {
  err.value = ''
  msg.value = ''
  busy.value = true
  try {
    const vo = await apiCreateAssistant({
      userId: Number(form.userId),
      modelId: Number(form.modelId),
      name: form.name,
      prompt: form.prompt,
    })
    upsert(
      state.assistants,
      {
        assistantId: vo.assistantId,
        userId: Number(form.userId),
        modelId: Number(form.modelId),
        name: vo.name,
        prompt: vo.prompt,
      },
      (a) => a.assistantId === vo.assistantId
    )
    msg.value = `助手创建成功。assistantId=${vo.assistantId}（modelName=${vo.modelName}）`
  } catch (e) {
    err.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="card">
    <h2>助手模板</h2>
    <div class="hint gray">
      <kbd>userId</kbd> 来自「登录 / 注册」页；<kbd>modelId</kbd> 是 <b>model 表实体 id</b>（不是 API 用的 model_id），
      需先在「模型提供商」页把模型实体 id 查库补填，否则下拉里选不到。
    </div>

    <form class="row" @submit.prevent="doCreate">
      <div class="field">
        <label>用户</label>
        <select v-model="form.userId">
          <option value="" disabled>选择用户</option>
          <option v-for="u in state.users" :key="u.id" :value="u.id">
            {{ u.account }} (id={{ u.id }})
          </option>
        </select>
      </div>
      <div class="field" style="flex: 1">
        <label>模型（已补填实体id的）</label>
        <select v-model="form.modelId">
          <option value="" disabled>选择模型</option>
          <option v-for="m in usableModels" :key="m.modelId" :value="m.id">
            {{ m.name }} (实体id={{ m.id }})
          </option>
        </select>
      </div>
      <div class="field">
        <label>助手名称</label>
        <input v-model="form.name" placeholder="Java 导师" required />
      </div>
      <div class="field">
        <label>系统提示词（prompt）</label>
        <textarea v-model="form.prompt" placeholder="你是一位 Java 导师……" />
      </div>
      <button class="primary" type="submit" :disabled="busy || !form.userId || !form.modelId">
        创建助手
      </button>
    </form>

    <div v-if="msg" class="msg ok">{{ msg }}</div>
    <div v-if="err" class="msg err">{{ err }}</div>

    <h3>已创建助手</h3>
    <table class="list">
      <thead>
        <tr><th>assistantId</th><th>名称</th><th>userId</th><th>model 实体id</th><th>prompt</th></tr>
      </thead>
      <tbody>
        <tr v-for="a in state.assistants" :key="a.assistantId">
          <td class="mono">
            {{ a.assistantId }}
            <button class="ghost" @click="copy(a.assistantId)">复制</button>
          </td>
          <td>{{ a.name }}</td>
          <td class="mono">{{ a.userId }}</td>
          <td class="mono">{{ a.modelId }}</td>
          <td style="max-width: 280px; word-break: break-all">{{ a.prompt }}</td>
        </tr>
        <tr v-if="!state.assistants.length">
          <td colspan="5" class="hint gray" style="border: none">暂无助手</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
