<script setup>
// ============ 根组件：相当于后端的“主控制器/路由表” ============
// 作用：顶部页签导航 + 容器渲染。理解这一个文件的模板语法，组件就算入门了：
// import 组件 = 引入依赖；ref() = 响应式数据；v-for = for 循环渲染；v-show = 按条件显示/隐藏

import { ref } from 'vue'
// import 进来的都是“模块导出”，对应 Java 的 import 包名
import { debugLogs } from './api/http'
import { resetLocalData } from './store'
// 各功能页签对应的组件（对应 Controller 下的不同方法）
import AuthPanel from './components/AuthPanel.vue'
import ModelPanel from './components/ModelPanel.vue'
import AssistantPanel from './components/AssistantPanel.vue'
import ChatPanel from './components/ChatPanel.vue'
import DebugPanel from './components/DebugPanel.vue'

// ref()：把字符串包成响应式数据，页面引用 tab 的地方会自动跟随变化（无需手动 setState/刷新）
const tab = ref('auth')
// 页签配置数组：模板里 v-for 遍历它来生成导航按钮（相当于“配置驱动渲染”）
const tabs = [
  { id: 'auth', label: '登录 / 注册' },
  { id: 'model', label: '模型提供商' },
  { id: 'assistant', label: '助手' },
  { id: 'chat', label: '会话与对话' },
  { id: 'debug', label: '调试日志' },
]
</script>

<template>
  <!-- 模板 = 声明式视图：只描述“长什么样”，数据变了 Vue 自动更新 DOM -->
  <div class="app">
    <header class="topbar">
      <h1>
        Amica 测试控制台
        <span class="sub">后端 :9000（经 Vite 代理转发）</span>
      </h1>
      <!-- v-for 遍历 tabs 生成按钮；:class 动态绑定样式（当前页签高亮）；@click 绑定点击事件 -->
      <nav>
        <button
          v-for="t in tabs"
          :key="t.id"
          class="tab"
          :class="{ active: tab === t.id }"
          @click="tab = t.id"
        >
          {{ t.label }}
          <!-- 插值 {{ }} 把 JS 的值渲染到页面；v-if 条件渲染（不满足就不生成这个元素） -->
          <span v-if="t.id === 'debug' && debugLogs.length" class="badge">{{ debugLogs.length }}</span>
        </button>
      </nav>
      <button class="link danger" title="清空 localStorage 中的本地资源缓存" @click="resetLocalData">
        清空本地数据
      </button>
    </header>
    <main>
      <!-- v-show：只是 CSS 隐藏，组件始终存在；v-if 则会被销毁重建。
           页签场景用 v-show 省去切换时的重建开销 -->
      <AuthPanel v-show="tab === 'auth'" />
      <ModelPanel v-show="tab === 'model'" />
      <AssistantPanel v-show="tab === 'assistant'" />
      <ChatPanel v-show="tab === 'chat'" />
      <DebugPanel v-show="tab === 'debug'" />
    </main>
  </div>
</template>
