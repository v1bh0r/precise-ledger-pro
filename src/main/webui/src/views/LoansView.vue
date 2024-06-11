<template>
  <div>
    <h1>Loans</h1>
    <RouterLink to="/new-loan" class="uk-button uk-button-primary uk-margin-bottom"
      >New Loan</RouterLink
    >
    <table class="uk-table uk-table-striped uk-table-hover">
      <thead>
        <tr>
          <th class="uk-text-left">ID</th>
          <th class="uk-text-left">Days in Year</th>
          <th class="uk-text-left">Currency Code</th>
          <th class="uk-text-left">External ID</th>
          <th class="uk-text-left">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="loan in loans" :key="loan.id">
          <td class="uk-text-left">{{ loan.id }}</td>
          <td class="uk-text-left">{{ loan.daysInYear }}</td>
          <td class="uk-text-left">{{ loan.currencyCode }}</td>
          <td class="uk-text-left">{{ loan.externalId }}</td>
          <td class="uk-text-left">
            <RouterLink :to="`/loans/${loan.id}`" class="uk-button uk-button-default"
              >View</RouterLink
            >
            <button @click="deleteLoan(loan.id)" class="uk-button uk-button-danger">Delete</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      loans: [],
      message: '',
      errorMessage: ''
    }
  },
  async created() {
    try {
      const response = await axios.get('/api/v1/loans')
      this.loans = response.data
    } catch (error) {
      console.error(error)
    }
  },
  methods: {
    async deleteLoan(id) {
      try {
        await axios.delete(`/api/v1/loans/${id}`)
        this.$store.commit('setMessage', 'Loan deleted successfully')
        this.$store.commit('setErrorMessage', '')
        this.loans = this.loans.filter((loan) => loan.id !== id) // Remove the deleted loan from the list
      } catch (error) {
        this.$store.commit('setErrorMessage', 'Failed to delete loan')
        this.$store.commit('setMessage', '')
      }
    }
  }
}
</script>
