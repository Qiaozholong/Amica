<script setup>
// ============ 调试日志页签（最“无脑”的一个组件，但能让你把 Postman 的习惯平移过来）============
// 学习重点：① v-for 渲染对象数组 ② 展开/收起（布尔值切换） ③ 计算属性的同理函数

import { ref } from 'vue'
// 这个数组在 http.js 里定义，所有组件共享（import 同一个模块就共享同一份内存）
import { debugLogs } from '../api/http'

const filter = ref('')
const showFailed = ref(true)

// filtered() 这里是普通函数而不是 computed：点击“清空日志”后父级会变更，函数重新执行也够用
// 等价于“先按条件过滤再遍历渲染”：过滤 + 模糊匹配路径/错误信息
const filtered = () =>
  debugLogs.filter(
    (l) =>
      (showFailed.value || l.status !== null) &&
      (!filter.value || l.path.includes(filter.value) || (l.respBody?.message || '').includes(filter.value))
  )

// 把对象转成缩进 JSON 字符串（缩进 2 格），方便阅读原始报文
// v === undefined 时返回 null 显示，避免 JSON.stringify(undefined) 变 undefined 的坑
function pretty(v) {
  if (v === undefined) return ''
  return JSON.stringify(v, null, 2)
}
</script>

<template>
  <div class="card">
    <h2>调试日志</h2>
    <div class="hint gray">
      每次请求的原文都会记录在这里（含请求体、响应体、耗时），相当于 Postman 的 console。
      后端异常时 HTTP 仍是 200，以 body.code 判断；<kbd>code=500</kbd> 且 message 为「土豆炸啦！？」，说明后端抛了未捕获异常。
    </div>
    <!-- 工具栏：过滤输入框 + 显隐开关 + 清空按钮（v-model 绑定响应式变量） -->
    <div class="row" style="gap: 8px; align-items: center; margin-bottom: 8px">
      <input v-model="filter" placeholder="按路径或错误信息过滤" style="background: #0d0f13; border: 1px solid var(--border); color: var(--text); border-radius: 6px; padding: 6px 9px" />
      <label style="font-size: 12px; color: var(--muted); display: flex; gap: 4px; align-items: center">
        <input v-model="showFailed" type="checkbox" /> 显示成功请求
      </label>
      <button class="ghost" @click="debugLogs.length = 0">清空日志</button>
    </div>

    <div v-if="!filtered().length" class="hint gray">暂无日志</div>
    <!-- 每条日志一个卡片：点击“展开”切换 l.expanded（布尔值），决定请求/响应体显示与否 -->
    <div v-for="l in filtered()" :key="l.id" class="card" style="padding: 10px 12px">
      <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
        <!-- :style 动态内联样式：GET 绿色、POST 蓝色（和方法名绑定） -->
        <span class="badge" :style="{ background: l.method === 'GET' ? 'var(--green)' : 'var(--accent)' }">{{ l.method }}</span>
        <span class="mono" style="color: var(--muted)">{{ l.time }}</span>
        <code>{{ l.path }}</code>
        <!-- 三目运算符：成功绿 / 失败红 -->
        <span :style="{ color: l.ok ? 'var(--green)' : 'var(--red)' }">
          {{ l.ok ? `code=${l.respBody?.code ?? '?'}` : `code=${l.respBody?.code ?? l.status}` }}
        </span>
        <span style="color: var(--muted)">{{ l.ms }}ms</span>
        <button class="ghost" @click="l.expanded = !l.expanded">{{ l.expanded ? '收起' : '展开' }}</button>
      </div>
      <!-- v-if="l.expanded"：展开后才渲染，节省 DOM -->
      <template v-if="l.expanded">
        <h3>请求体</h3>
        <pre class="code">{{ pretty(l.body) || '（无）' }}</pre>
        <h3>响应体</h3>
        <pre class="code">{{ pretty(l.respBody) || '（无）' }}</pre>
      </template>
    </div>
  </div>
</template>
