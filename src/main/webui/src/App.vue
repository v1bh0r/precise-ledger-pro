<script setup>
import { ref, watchEffect, computed } from 'vue'
import { useStore } from 'vuex'
import { RouterLink, RouterView } from 'vue-router'

const store = useStore()
const message = computed(() => store.state.message)
const errorMessage = computed(() => store.state.errorMessage)
const messageKey = ref(0)
const errorMessageKey = ref(0)

watchEffect(() => {
  if (message.value) {
    messageKey.value++
    setTimeout(() => {
      store.commit('setMessage', '')
    }, 5000) // 5 seconds
  }
})

watchEffect(() => {
  if (errorMessage.value) {
    errorMessageKey.value++
    setTimeout(() => {
      store.commit('setErrorMessage', '')
    }, 5000) // 5 seconds
  }
})
</script>

<template>
  <header class="uk-background-primary uk-light uk-padding-small uk-position-z-index">
    <nav class="uk-navbar-container uk-navbar-transparent" uk-navbar>
      <div class="uk-navbar-left">
        <RouterLink class="uk-navbar-item uk-logo" to="/">
          <span uk-icon="icon: check; ratio: 2"></span>
          <span>Precise Ledger Pro</span>
        </RouterLink>
      </div>
      <div class="uk-navbar-right">
        <ul class="uk-navbar-nav">
          <li><a href="#">About</a></li>
          <li><a href="#">Contact</a></li>
        </ul>
      </div>
    </nav>
  </header>

  <div class="uk-grid-collapse uk-grid-match" uk-grid>
    <!-- Main Content Area -->
    <div class="uk-width-expand@s uk-padding-small">
      <div v-show="message" :key="messageKey" class="uk-alert-success" uk-alert>
        <p>{{ message }}</p>
      </div>
      <div v-show="errorMessage" :key="errorMessageKey" class="uk-alert-danger" uk-alert>
        <p>{{ errorMessage }}</p>
      </div>
      <RouterView />
    </div>
  </div>
</template>
