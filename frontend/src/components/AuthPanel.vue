<script setup>
// ============ 登录/注册页签 ============
// 学习重点：表单与后端交互的完整链路
// ① reactive() 存表单数据（相当于把 DTO 字段挂到内存对象上）
// ② v-model 双向绑定：输入框和 JS 变量互相同步（你改输入框，react 对象跟着变；反之亦然）
// ③ @submit.prevent 拦截表单默认提交（默认会刷新页面），改为调用自己的方法
// ④ try/catch 处理后端返回的错误（和 Java 的 try/catch 一模一样，catch 里拿 e.message）

import { reactive, ref } from 'vue'
import { apiRegister, apiLogin, apiGetUsers } from '../api'
import { state } from '../store'

// 两个表单的数据模型（相当于后端两个 DTO：RegisterDto / LoginDto）
// reactive() 和 ref() 的区别：ref 包单个值（如字符串），reactive 包 对象/数组（这里是表单对象）
const regForm = reactive({ account: '', password: '', nickname: '' })
const logForm = reactive({ account: '', password: '' })
// ref() 包字符串：页面消息提示。msg = 成功信息，err = 错误信息，busy = 请求进行中（防止重复点击）
const msg = ref('')
const err = ref('')
const busy = ref(false)

// 后端 /auth/login 只返回 account，不返回 userId；
// 唯一能拿到 userId 的接口是 GET /auth/get，这里按账号匹配（临时联调方案）
async function resolveUserId(account) {
  // await：调用后端接口，等价于同步写法调用 Service 方法
  const users = await apiGetUsers()
  // map：把后端返回的 UserInfoVo 数组转换成前端需要的精简结构
  // （后端已不再返回 password 字段，这里只取 id/account/nickname）
  state.users = users.map((u) => ({ id: u.id, account: u.account, nickname: u.nickname }))
  // find：从数组里按条件找第一个匹配项（等价于 SQL where account = ? limit 1）
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
    // {...regForm}：展开运算符，把表单对象解开复制一份（避免把引用直接发给后端）
    await apiRegister({ ...regForm })
    await resolveUserId(regForm.account)
    state.account = regForm.account
    msg.value = `注册成功，已自动登录。userId=${state.userId}`
  } catch (e) {
    // 和 Java catch 一样：e.message 就是 http.js 里 throw 的 message（后端 Result 的 message）
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
      登录接口<i>不返回</i> userId，本控制台通过 <kbd>GET /auth/get</kbd> 按账号匹配获得（临时方案，建议后端在 login 返回体中补上 id）。
    </div>
    <div v-if="state.userId" class="hint" style="border-color: var(--accent); color: var(--accent)">
      当前用户：<b>{{ state.account }}</b>（userId = {{ state.userId }}）
    </div>

    <div style="display: flex; gap: 16px; flex-wrap: wrap">
      <!-- 两个表单并排：注册表单、登录表单 -->
      <!-- @submit.prevent="doRegister"：提交时拦截默认行为，执行 doRegister -->
      <form class="row" style="flex: 1; min-width: 360px" @submit.prevent="doRegister">
        <div class="field">
          <label>账号</label>
          <!-- v-model 双向绑定：输入什么，regForm.account 就是什么（后端见到的就是这个值） -->
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
        <!-- :disabled="busy"：请求期间禁用按钮，后面才允许再次点击（防重复提交） -->
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

    <!-- v-if + msg：有成功消息才渲染这行（条件渲染，相当于 if (msg != null) 输出 HTML） -->
    <div v-if="msg" class="msg ok">{{ msg }}</div>
    <div v-if="err" class="msg err">{{ err }}</div>

    <h3>用户列表（GET /auth/get）</h3>
    <!-- table 渲染列表：v-for 遍历 + :key 唯一标识（等价于 for 循环输出 <tr>） -->
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
        <!-- 空态提示：列表没有数据时显示这一行（v-if 和 v-for 一起用的常见写法） -->
        <tr v-if="!state.users.length">
          <td colspan="3" class="hint gray" style="border: none">暂无数据，先注册一个账号试试（注册成功后会自动拉取列表）</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
