<script setup>
import { ref } from 'vue'
import { debugLogs } from './api/http'
import { resetLocalData } from './store'
import AuthPanel from './components/AuthPanel.vue'
import ModelPanel from './components/ModelPanel.vue'
import AssistantPanel from './components/AssistantPanel.vue'
import ChatPanel from './components/ChatPanel.vue'
import DebugPanel from './components/DebugPanel.vue'

const tab = ref('auth')
const tabs = [
  { id: 'auth', label: '登录 / 注册' },
  { id: 'model', label: '模型提供商' },
  { id: 'assistant', label: '助手' },
  { id: 'chat', label: '会话与对话' },
  { id: 'debug', label: '调试日志' },
]
</script>

<template>
  <div class="app">
    <header class="topbar">
      <h1>Amica 测试控制台 <span class="sub">后端 :9000（经 Vite 代理转发）</span></h1>
      <nav>
        <button
          v-for="t in tabs"
          :key="t.id"
          class="tab"
          :class="{ active: tab === t.id }"
          @click="tab = t.id"
        >
          {{ t.label }}
          <span v-if="t.id === 'debug' && debugLogs.length" class="badge">{{ debugLogs.length }}</span>
        </button>
      </nav>
      <button class="link danger" title="清空 localStorage 中的本地资源缓存" @click="resetLocalData">
        清空本地数据
      </button>
    </header>
    <main>
      <AuthPanel v-show="tab === 'auth'" />
      <ModelPanel v-show="tab === 'model'" />
      <AssistantPanel v-show="tab === 'assistant'" />
      <ChatPanel v-show="tab === 'chat'" />
      <DebugPanel v-show="tab === 'debug'" />
    </main>
  </div>
</template>
