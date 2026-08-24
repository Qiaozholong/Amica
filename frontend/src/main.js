import { createApp } from 'vue'
import App from './App.vue'
import './styles.css'

// 前端程序的“启动类”，对应后端的 SpringApplication.run(...)
// 三件事：引入根组件 App.vue、引入全局样式、把应用挂载到 index.html 的 <div id="app"> 上
// 模板里的 import 和 Java 的 import 作用一样：把别的文件里的东西引入当前作用域
createApp(App).mount('#app')
