<script setup>
// ============ 登录 / 注册页签 ============
// 后端 /auth/register 与 /auth/login 现在都返回 AuthVo{ id, account, nickname, token }：
// 注册即登录，两个入口都会签发 JWT。前端拿到后存进 store，http.js 会自动加到请求头。

import { onMounted, reactive, ref } from 'vue'
import { apiRegister, apiLogin, apiGetUsers } from '../api'
import { state, setAuth, clearAuth, isLoggedIn } from '../store'

// 两个表单的数据模型（对应后端 RegisterDto / LoginDto）
const regForm = reactive({ account: '', password: '', nickname: '' })
const logForm = reactive({ account: '', password: '' })
const msg = ref('')
const err = ref('')
const busy = ref(false)
// 用户列表（调试用；/auth/get 属于放行路径，未登录也能查）
const users = ref([])

async function loadUsers() {
  try {
    users.value = await apiGetUsers()
  } catch (e) {
    err.value = e.message
  }
}

async function doRegister() {
  err.value = ''
  msg.value = ''
  busy.value = true
  try {
    const vo = await apiRegister({ ...regForm })
    setAuth(vo) // 存 id / account / nickname / token
    msg.value = `注册成功，已自动登录。userId=${vo.id}`
    await loadUsers()
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
    const vo = await apiLogin({ ...logForm })
    setAuth(vo)
    msg.value = `登录成功。userId=${vo.id}`
    await loadUsers()
  } catch (e) {
    err.value = e.message
  } finally {
    busy.value = false
  }
}

function doLogout() {
  clearAuth()
  msg.value = '已退出登录'
  err.value = ''
}

// 进页面先拉一次用户列表，顺便确认后端是活的
onMounted(loadUsers)
</script>

<template>
  <div class="card">
    <h2>登录 / 注册</h2>

    <div class="hint gray">
      登录 / 注册成功后，后端返回 <kbd>AuthVo</kbd>（含 <kbd>id</kbd> 与 <kbd>token</kbd>）。
      <kbd>token</kbd> 由 <kbd>http.js</kbd> 自动放进 <kbd>Authorization: Bearer</kbd> 头 ——
      后端 JWT 过滤器对除 <kbd>/auth/**</kbd> 之外的所有路径都要求它，否则一律 401。
    </div>

    <!-- 当前登录态 -->
    <div v-if="isLoggedIn()" class="hint" style="border-color: var(--accent); color: var(--accent)">
      当前登录：<b>{{ state.account }}</b>（id={{ state.id }}，昵称 {{ state.nickname }}）
      <button class="ghost" style="margin-left: 10px" @click="doLogout">退出登录</button>
    </div>
    <div v-else class="hint">
      未登录 —— 除本页之外的功能都会返回 401，请先注册或登录。
    </div>

    <div style="display: flex; gap: 16px; flex-wrap: wrap">
      <!-- 注册表单 -->
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

      <!-- 登录表单 -->
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

    <h3>
      用户列表（GET /auth/get）
      <button class="ghost" style="margin-left: 8px" @click="loadUsers">刷新</button>
    </h3>
    <table class="list">
      <thead>
        <tr><th>id</th><th>账号</th><th>昵称</th><th>状态</th></tr>
      </thead>
      <tbody>
        <tr v-for="u in users" :key="u.id">
          <td class="mono">{{ u.id }}</td>
          <td>{{ u.account }}</td>
          <td>{{ u.nickname }}</td>
          <td>{{ u.status }}</td>
        </tr>
        <tr v-if="!users.length">
          <td colspan="4" class="hint gray" style="border: none">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
