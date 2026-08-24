<script setup>
// ============ 模型与提供商页签 ============
// 学习重点：① 复杂表单 + ② 一次性注册两个表（model + provider） + ③ 行内操作（给每个 provider 配 Key）
// 概念对应：表单字段 = DTO 字段；reactive({...}) = DTO 实例；函数 = Service 方法

import { reactive, ref } from 'vue'
import { apiRegisterModel, apiSetApiKey } from '../api'
import { state, upsert } from '../store'

// 注册模型表单的数据模型（对应后端 ModelDto：protocol/baseUrl/name/modelId）
const form = reactive({ protocol: 'openai', baseUrl: '', name: '', modelId: '' })
const msg = ref('')
const err = ref('')
const busy = ref(false)

// 每个 provider 的 API Key 输入值，key 是 providerId：
// 用空对象起步，运行时按需动态添加 key（reactive 支持给对象加新属性）
// 相当于 Map<Long, String>：keyInputs[providerId] = 输入框内容
const keyInputs = reactive({}) // providerId -> 输入中的 apiKey

async function copy(text) {
  // navigator.clipboard：调用浏览器剪贴板 API（无需用户选中文字）
  await navigator.clipboard.writeText(text)
}

async function doRegister() {
  err.value = ''
  msg.value = ''
  busy.value = true
  try {
    // 后端一次性完成 provider + model 两个表的注册，返回 AModelVo
    const vo = await apiRegisterModel({ ...form })
    // upsert：往本地缓存里插一条/更新一条（等价于“先查库看是否存在，存在返回已有”的本地版）
    upsert(
      state.providers,
      { providerId: vo.providerId, protocol: vo.protocol, baseUrl: vo.baseUrl, name: vo.name },
      (p) => p.providerId === vo.providerId
    )
    upsert(
      state.models,
      { id: null, providerId: vo.providerId, name: vo.name, modelId: vo.modelId },
      (m) => m.modelId === vo.modelId
    )
    msg.value = `注册成功。providerId=${vo.providerId}（注意：后端未返回 model 实体 id，请查库补填后再创建助手）`
  } catch (e) {
    err.value = e.message
  } finally {
    busy.value = false
  }
}

async function doSetKey(provider) {
  err.value = ''
  msg.value = ''
  // 从 keyInputs 里按 providerId 取出这个 provider 的输入值（等价于 map.get(providerId)）
  const apiKey = keyInputs[provider.providerId]
  if (!apiKey) {
    err.value = '请先输入 API Key'
    return
  }
  try {
    const vo = await apiSetApiKey({ providerId: provider.providerId, apiKey })
    // 把后端返回的脱敏 Key 记到本地，界面从此只显示脱敏值（明文不回显）
    provider.apiKeyMasked = vo.apiKey
    msg.value = `API Key 已加密存储（后端返回脱敏值：${vo.apiKey}，界面不再展示明文）`
  } catch (e) {
    err.value = e.message
  }
}
</script>

<template>
  <div class="card">
    <h2>模型与提供商</h2>
    <div class="hint">
      注意三点：① <kbd>baseUrl</kbd> 需要是<b>完整端点</b>（含 <kbd>/chat/completions</kbd>），不是域名；
      ② 同一个 (protocol, baseUrl) 的提供商会被复用，重复注册会返回已有记录；
      ③ <kbd>model_id</kbd> 全表唯一，换一个提供商不能注册同名模型。
    </div>

    <!-- 表单一：模型注册。field 包输入项，:style 控制布局（flex 占比） -->
    <form class="row" @submit.prevent="doRegister">
      <div class="field">
        <label>protocol（请求体样式）</label>
        <!-- select 下拉：v-model 绑定选择值，option 是选项 -->
        <select v-model="form.protocol">
          <option value="openai">openai（OpenAI 兼容协议）</option>
          <option value="anthropicai">anthropicai（暂未实现）</option>
          <option value="other">other</option>
        </select>
      </div>
      <div class="field" style="flex: 2">
        <label>baseUrl（完整端点）</label>
        <input v-model="form.baseUrl" placeholder="https://api.deepseek.com/chat/completions" style="width: 100%" required />
      </div>
      <div class="field">
        <label>模型名称</label>
        <input v-model="form.name" placeholder="DeepSeek-V4" required />
      </div>
      <div class="field">
        <label>模型ID（API用）</label>
        <input v-model="form.modelId" placeholder="deepseek-chat" required />
      </div>
      <button class="primary" type="submit" :disabled="busy">注册模型</button>
    </form>

    <div v-if="msg" class="msg ok">{{ msg }}</div>
    <div v-if="err" class="msg err">{{ err }}</div>

    <h3>提供商列表（含 API Key 配置）</h3>
    <table class="list">
      <thead>
        <tr><th>providerId</th><th>protocol</th><th>baseUrl</th><th>API Key</th><th></th></tr>
      </thead>
      <tbody>
        <!-- 行内操作：每一行一个输入框 + 按钮，点击调 doSetKey(p)，p 是当前行的 provider 对象 -->
        <tr v-for="p in state.providers" :key="p.providerId">
          <td class="mono">
            {{ p.providerId }}
            <button class="ghost" @click="copy(p.providerId)">复制</button>
          </td>
          <td>{{ p.protocol }}</td>
          <td class="mono" style="word-break: break-all">{{ p.baseUrl }}</td>
          <td>
            <!-- 已配置过就显示脱敏值，否则显示输入框（v-if/v-else 二选一渲染） -->
            <template v-if="p.apiKeyMasked">{{ p.apiKeyMasked }}</template>
            <input v-model="keyInputs[p.providerId]" type="password" placeholder="输入明文 Key" style="max-width: 180px" />
          </td>
          <td>
            <button class="ghost" @click="doSetKey(p)">配置密钥</button>
          </td>
        </tr>
        <tr v-if="!state.providers.length">
          <td colspan="5" class="hint gray" style="border: none">暂无提供商</td>
        </tr>
      </tbody>
    </table>

    <h3>模型列表</h3>
    <div class="hint gray">
      ⚠ <kbd>POST /model/register</kbd> 的返回体<b>不包含 model 实体 id</b>（即创建助手时需要的 <kbd>modelId</kbd>），
      目前只能查数据库补填。这是后端接口缺口，建议在 AModelVo 中补上 <kbd>id</kbd> 字段。
    </div>
    <table class="list">
      <thead>
        <tr><th>model 实体ID（手填）</th><th>模型名称</th><th>模型ID(API)</th><th>providerId</th></tr>
      </thead>
      <tbody>
        <!-- 可编辑单元格：v-model="m.id" 双向绑定到 model 对象上，改完 localStorage 自动持久化（store.js 的 watch 干的） -->
        <tr v-for="m in state.models" :key="m.modelId">
          <td>
            <input v-model="m.id" type="number" placeholder="查库补填" style="max-width: 130px" />
            <button class="ghost" @click="m.id && copy(m.id)">复制</button>
          </td>
          <td>{{ m.name }}</td>
          <td class="mono">{{ m.modelId }}</td>
          <td class="mono">{{ m.providerId }}</td>
        </tr>
        <tr v-if="!state.models.length">
          <td colspan="4" class="hint gray" style="border: none">暂无模型</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
