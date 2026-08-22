<script setup>
import { reactive, ref } from 'vue'
import { apiRegister, apiLogin, apiShowUsers } from '../api'
import { state } from '../store'

const regForm = reactive({ account: '', password: '', nickname: '' })
const logForm = reactive({ account: '', password: '' })
const msg = ref('')
const err = ref('')
const busy = ref(false)

// 后端 /auth/login 只返回 account，不返回 userId；
// 唯一能拿到 userId 的接口是 GET /auth/show，这里按账号匹配（临时联调方案）
async function resolveUserId(account) {
  const users = await apiShowUsers()
  state.users = users.map((u) => ({ id: u.id, account: u.account, nickname: u.nickname }))
  const me = state.users.find((u) => u.account === account)
  state.userId = me ? me.id : null
  if (!me) throw new Error(`后端未找到账号 ${account} 对应的用户，userId 无法确定`)
  return me?.id
}

async function doRegister() {
  err.value = ''
  msg.value = ''
  busy.value = true
  try {
    await apiRegister({ ...regForm })
    await resolveUserId(regForm.account)
    state.account = regForm.account
    msg.value = `注册成功，已自动登录。userId=${state.userId}`
  } catch (e) {
    err.value = e.message
  } finally {
    busy.value = false
  }
}

async function doLogin() {
  err.value = ''
  msg.value = ''
  busy.value = true
  try {
    await apiLogin({ ...logForm })
    await resolveUserId(logForm.account)
    state.account = logForm.account
    msg.value = `登录成功。userId=${state.userId}`
  } catch (e) {
    err.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="card">
    <h2>登录 / 注册</h2>
    <div class="hint gray">
      当前后端尚未接入 JWT，所有资源接口都需要手传 <kbd>userId</kbd>。
      登录接口<i>不返回</i> userId，本控制台通过 <kbd>GET /auth/show</kbd> 按账号匹配获得（临时方案，建议后端在 login 返回体中补上 id）。
    </div>
    <div v-if="state.userId" class="hint" style="border-color: var(--accent); color: var(--accent)">
      当前用户：<b>{{ state.account }}</b>（userId = {{ state.userId }}）
    </div>

    <div style="display: flex; gap: 16px; flex-wrap: wrap">
      <form class="row" style="flex: 1; min-width: 360px" @submit.prevent="doRegister">
        <div class="field">
          <label>账号</label>
          <input v-model="regForm.account" required />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="regForm.password" type="password" required />
        </div>
        <div class="field">
          <label>昵称</label>
          <input v-model="regForm.nickname" required />
        </div>
        <button class="primary" type="submit" :disabled="busy">注册</button>
      </form>

      <form class="row" style="flex: 1; min-width: 280px" @submit.prevent="doLogin">
        <div class="field">
          <label>账号</label>
          <input v-model="logForm.account" required />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="logForm.password" type="password" required />
        </div>
        <button class="primary" type="submit" :disabled="busy">登录</button>
      </form>
    </div>

    <div v-if="msg" class="msg ok">{{ msg }}</div>
    <div v-if="err" class="msg err">{{ err }}</div>

    <h3>用户列表（GET /auth/show）</h3>
    <table class="list">
      <thead>
        <tr><th>id</th><th>账号</th><th>昵称</th></tr>
      </thead>
      <tbody>
        <tr v-for="u in state.users" :key="u.id">
          <td class="mono">{{ u.id }}</td>
          <td>{{ u.account }}</td>
          <td>{{ u.nickname }}</td>
        </tr>
        <tr v-if="!state.users.length">
          <td colspan="3" class="hint gray" style="border: none">暂无数据，先注册一个账号试试（注册成功后会自动拉取列表）</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
