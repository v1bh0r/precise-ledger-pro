import './assets/main.css'
import uk from 'uikit'
import Icons from 'uikit/dist/js/uikit-icons'
uk.use(Icons)


import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store' // Import the store


const app = createApp(App)

app.use(router)
  .use(store)
  .mount('#app')
