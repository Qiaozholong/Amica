<script setup>
// ============ 模型与提供商页签 ============
// 两类数据都从服务端拉取：
//   GET /model/getallprovider          -> 当前用户的提供商
//   GET /model/getAllModel/{providerId} -> 某个提供商下的模型
// 注册走 POST /model/register（一次性建 provider + model），返回的 AModelVo 里**已经带 model 实体 id**，
// 所以不再需要手填 id。

import { onMounted, reactive, ref } from 'vue'
import { apiRegisterModel, apiSetApiKey, apiGetProviders, apiGetModels } from '../api'
import { isLoggedIn } from '../store'

const form = reactive({
  protocol: 'openai',
  baseUrl: 'https://api.deepseek.com/v1/chat/completions',
  name: '',
  modelId: '',
})
const msg = ref('')
const err = ref('')
const busy = ref(false)

// 提供商列表（服务端）
const providers = ref([])
// providerId -> ModelVo[]（展开某个提供商时才拉）
const modelsByProvider = reactive({})
// providerId -> 是否展开模型列表
const expanded = reactive({})
// providerId -> 输入中的明文 API Key
const keyInputs = reactive({})

async function loadProviders() {
  if (!isLoggedIn()) return
  try {
    providers.value = await apiGetProviders()
  } catch (e) {
    err.value = e.message
  }
}

async function loadModels(providerId) {
  try {
    modelsByProvider[providerId] = await apiGetModels(providerId)
  } catch (e) {
    err.value = e.message
  }
}

async function toggleModels(p) {
  const id = p.id
  expanded[id] = !expanded[id]
  if (expanded[id]) await loadModels(id)
}

async function copy(text) {
  await navigator.clipboard.writeText(text)
}

async function doRegister() {
  err.value = ''
  msg.value = ''
  busy.value = true
  try {
    const vo = await apiRegisterModel({ ...form })
    msg.value =
      `注册成功。providerId=${vo.providerId}，model 实体 id=${vo.id}` +
      `（这个 id 就是创建助手时要用的 modelId）`
    await loadProviders()
    // 顺手展开并拉取这个 provider 下的模型，方便立刻看到刚注册的
    if (vo.providerId) {
      expanded[vo.providerId] = true
      await loadModels(vo.providerId)
    }
  } catch (e) {
    err.value = e.message
  } finally {
    busy.value = false
  }
}

async function doSetKey(provider) {
  err.value = ''
  msg.value = ''
  const apiKey = keyInputs[provider.id]
  if (!apiKey) {
    err.value = '请先输入 API Key'
    return
  }
  try {
    const vo = await apiSetApiKey({ providerId: provider.id, apiKey })
    provider.apiKeyMasked = vo.apiKey
    keyInputs[provider.id] = ''
    msg.value = `API Key 已加密存储，后端返回脱敏值：${vo.apiKey}`
  } catch (e) {
    err.value = e.message
  }
}

onMounted(loadProviders)
</script>

<template>
  <div class="card">
    <h2>模型与提供商</h2>
    <div class="hint">
      ① <kbd>baseUrl</kbd> 必须是<b>完整端点</b>（含 <kbd>/chat/completions</kbd>），不是域名；
      ② 同一个用户下重复的 <kbd>(protocol, baseUrl)</kbd> 会复用已有提供商；
      ③ 唯一键分别是 <kbd>(user_id, protocol, base_url)</kbd> 与 <kbd>(user_id, model_id)</kbd> ——
      <b>不同用户之间互不影响</b>，所以两个账号可以注册同一个模型。
    </div>

    <!-- 注册表单 -->
    <form class="row" @submit.prevent="doRegister">
      <div class="field">
        <label>protocol</label>
        <select v-model="form.protocol">
          <option value="openai">openai（OpenAI 兼容协议）</option>
          <option value="anthropicai">anthropicai（暂未实现）</option>
          <option value="other">other</option>
        </select>
      </div>
      <div class="field" style="flex: 2">
        <label>baseUrl（完整端点）</label>
        <input v-model="form.baseUrl" placeholder="https://api.deepseek.com/v1/chat/completions" style="width: 100%" required />
      </div>
      <div class="field">
        <label>模型名称</label>
        <input v-model="form.name" placeholder="DeepSeek V4 Flash" required />
      </div>
      <div class="field">
        <label>模型ID（API 用）</label>
        <input v-model="form.modelId" placeholder="deepseek-v4-flash" required />
      </div>
      <button class="primary" type="submit" :disabled="busy || !isLoggedIn()">注册模型</button>
    </form>

    <div v-if="msg" class="msg ok">{{ msg }}</div>
    <div v-if="err" class="msg err">{{ err }}</div>

    <h3>
      提供商列表（GET /model/getallprovider）
      <button class="ghost" style="margin-left: 8px" @click="loadProviders">刷新</button>
    </h3>
    <table class="list">
      <thead>
        <tr><th>id</th><th>protocol</th><th>baseUrl</th><th>API Key</th><th></th></tr>
      </thead>
      <tbody>
        <template v-for="p in providers" :key="p.id">
          <tr>
            <td class="mono">
              {{ p.id }}
              <button class="ghost" @click="copy(p.id)">复制</button>
            </td>
            <td>{{ p.protocol }}</td>
            <td class="mono" style="word-break: break-all">{{ p.baseUrl }}</td>
            <td>
              <span v-if="p.apiKeyMasked" class="mono">{{ p.apiKeyMasked }}</span>
              <input
                v-model="keyInputs[p.id]"
                type="password"
                placeholder="输入明文 Key"
                style="max-width: 180px"
              />
            </td>
            <td style="white-space: nowrap">
              <button class="ghost" @click="doSetKey(p)">配置密钥</button>
              <button class="ghost" style="margin-left: 4px" @click="toggleModels(p)">
                {{ expanded[p.id] ? '收起模型' : '查看模型' }}
              </button>
            </td>
          </tr>
          <!-- 展开：该提供商下的模型（含 model 实体 id） -->
          <tr v-if="expanded[p.id]">
            <td colspan="5" style="background: #0d0f13">
              <div class="hint gray" style="margin: 4px 0">
                GET /model/getAllModel/{{ p.id }} —— 下面的 <kbd>model 实体 id</kbd> 就是创建助手时要填的 modelId
              </div>
              <table class="list">
                <thead>
                  <tr><th>model 实体 id</th><th>模型名称</th><th>模型ID(API)</th></tr>
                </thead>
                <tbody>
                  <tr v-for="m in modelsByProvider[p.id] || []" :key="m.id">
                    <td class="mono">
                      {{ m.id }}
                      <button class="ghost" @click="copy(m.id)">复制</button>
                    </td>
                    <td>{{ m.name }}</td>
                    <td class="mono">{{ m.modelId }}</td>
                  </tr>
                  <tr v-if="!(modelsByProvider[p.id] || []).length">
                    <td colspan="3" class="hint gray" style="border: none">该提供商下暂无模型</td>
                  </tr>
                </tbody>
              </table>
            </td>
          </tr>
        </template>
        <tr v-if="!providers.length">
          <td colspan="5" class="hint gray" style="border: none">
            {{ isLoggedIn() ? '暂无提供商，先在上方注册一个模型' : '未登录 —— 请先到「登录 / 注册」页登录' }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
