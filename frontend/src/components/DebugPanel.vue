<script setup>
import { ref } from 'vue'
import { debugLogs } from '../api/http'

const filter = ref('')
const showFailed = ref(true)

const filtered = () =>
  debugLogs.filter(
    (l) =>
      (showFailed.value || l.status !== null) &&
      (!filter.value || l.path.includes(filter.value) || (l.respBody?.message || '').includes(filter.value))
  )

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
    <div class="row" style="gap: 8px; align-items: center; margin-bottom: 8px">
      <input v-model="filter" placeholder="按路径或错误信息过滤" style="background: #0d0f13; border: 1px solid var(--border); color: var(--text); border-radius: 6px; padding: 6px 9px" />
      <label style="font-size: 12px; color: var(--muted); display: flex; gap: 4px; align-items: center">
        <input v-model="showFailed" type="checkbox" /> 显示成功请求
      </label>
      <button class="ghost" @click="debugLogs.length = 0">清空日志</button>
    </div>

    <div v-if="!filtered().length" class="hint gray">暂无日志</div>
    <div v-for="l in filtered()" :key="l.id" class="card" style="padding: 10px 12px">
      <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
        <span class="badge" :style="{ background: l.method === 'GET' ? 'var(--green)' : 'var(--accent)' }">{{ l.method }}</span>
        <span class="mono" style="color: var(--muted)">{{ l.time }}</span>
        <code>{{ l.path }}</code>
        <span :style="{ color: l.ok ? 'var(--green)' : 'var(--red)' }">
          {{ l.ok ? `code=${l.respBody?.code ?? '?'}` : `code=${l.respBody?.code ?? l.status}` }}
        </span>
        <span style="color: var(--muted)">{{ l.ms }}ms</span>
        <button class="ghost" @click="l.expanded = !l.expanded">{{ l.expanded ? '收起' : '展开' }}</button>
      </div>
      <template v-if="l.expanded">
        <h3>请求体</h3>
        <pre class="code">{{ pretty(l.body) || '（无）' }}</pre>
        <h3>响应体</h3>
        <pre class="code">{{ pretty(l.respBody) || '（无）' }}</pre>
      </template>
    </div>
  </div>
</template>
