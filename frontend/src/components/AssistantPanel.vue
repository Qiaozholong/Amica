<script setup>
// ============ 助手页签 ============
// 关键变化：userId 不再由前端选 —— 后端 createAssistant 已经改成从 JWT 取 userId，
// AssistantDto 里的 userId 字段也删掉了。这里只传 modelId + name + prompt。
// 另外 model 不再手填 id：将每个提供商下的模型展平成一个下拉（自动带上 model 实体 id）。

import { onMounted, reactive, ref } from 'vue'
import { apiCreateAssistant, apiGetAssistants, apiGetProviders, apiGetModels } from '../api'
import { isLoggedIn } from '../store'

const form = reactive({ modelId: '', name: '', prompt: '' })
const msg = ref('')
const err = ref('')
const busy = ref(false)

// 展平后的可选模型：{ id, name, modelId, providerLabel }
const modelOptions = ref([])
// 助手列表（服务端 AssistantEntity：注意字段是 id，不是 assistantId）
const assistants = ref([])

async function loadModelOptions() {
  if (!isLoggedIn()) return
  try {
    const providers = await apiGetProviders()
    const out = []
    // 依次拉取每个提供商下的模型（后端接口是按 providerId 查的）
    for (const p of providers) {
      const models = await apiGetModels(p.id)
      for (const m of models) {
        out.push({
          id: m.id,
          name: m.name,
          modelId: m.modelId,
          providerLabel: `${p.protocol} @ ${p.baseUrl}`,
        })
      }
    }
    modelOptions.value = out
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

async function loadAll() {
  err.value = ''
  await loadModelOptions()
  await loadAssistants()
}

async function copy(text) {
  await navigator.clipboard.writeText(text)
}

async function doCreate() {
  err.value = ''
  msg.value = ''
  busy.value = true
  try {
    const vo = await apiCreateAssistant({
      modelId: Number(form.modelId),
      name: form.name,
      prompt: form.prompt || null,
    })
    msg.value = `助手创建成功。assistantId=${vo.assistantId}（model=${vo.modelName}）`
    form.name = ''
    form.prompt = ''
    await loadAssistants()
  } catch (e) {
    err.value = e.message
  } finally {
    busy.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="card">
    <h2>助手模板</h2>
    <div class="hint gray">
      <kbd>userId</kbd> 由后端从 JWT 取（前端不再传）；<kbd>modelId</kbd> 是 <b>model 表实体 id</b>，
      从下面的下拉里直接选 —— 选项来自「提供商 → 模型」的展开结果。
      后端在创建时会校验该 model <b>是否属于当前用户</b>，引用别人的模型会被拒。
    </div>

    <form class="row" @submit.prevent="doCreate">
      <div class="field" style="flex: 2">
        <label>模型（model 实体 id）</label>
        <select v-model="form.modelId">
          <option value="" disabled>选择模型</option>
          <option v-for="m in modelOptions" :key="m.id" :value="m.id">
            {{ m.name }} (id={{ m.id }}) — {{ m.providerLabel }}
          </option>
        </select>
      </div>
      <div class="field">
        <label>助手名称</label>
        <input v-model="form.name" placeholder="Java 导师" required />
      </div>
      <div class="field" style="flex: 2">
        <label>系统提示词（prompt，可空）</label>
        <textarea v-model="form.prompt" placeholder="你是一位 Java 导师……" />
      </div>
      <button class="primary" type="submit" :disabled="busy || !isLoggedIn() || !form.modelId">
        创建助手
      </button>
      <button class="ghost" type="button" @click="loadAll">刷新</button>
    </form>

    <div v-if="msg" class="msg ok">{{ msg }}</div>
    <div v-if="err" class="msg err">{{ err }}</div>

    <h3>
      助手列表（GET /assistant/getAllAssistant）
      <button class="ghost" style="margin-left: 8px" @click="loadAssistants">刷新</button>
    </h3>
    <table class="list">
      <thead>
        <tr><th>id</th><th>名称</th><th>model 实体 id</th><th>prompt</th></tr>
      </thead>
      <tbody>
        <tr v-for="a in assistants" :key="a.id">
          <td class="mono">
            {{ a.id }}
            <button class="ghost" @click="copy(a.id)">复制</button>
          </td>
          <td>{{ a.name }}</td>
          <td class="mono">{{ a.modelId }}</td>
          <td style="max-width: 320px; word-break: break-all">{{ a.prompt }}</td>
        </tr>
        <tr v-if="!assistants.length">
          <td colspan="4" class="hint gray" style="border: none">
            {{ isLoggedIn() ? '暂无助手' : '未登录 —— 请先到「登录 / 注册」页登录' }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
