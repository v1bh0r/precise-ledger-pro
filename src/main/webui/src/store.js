import { createStore } from 'vuex'

export default createStore({
  state: {
    message: '',
    errorMessage: ''
  },
  mutations: {
    setMessage(state, message) {
      state.message = message
    },
    setErrorMessage(state, errorMessage) {
      state.errorMessage = errorMessage
    }
  }
})