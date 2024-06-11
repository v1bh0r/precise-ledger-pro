<template>
  <LoanLifecycle />
  <table>
    <thead>
      <tr>
        <th>Entry ID</th>
        <th>Entry Type</th>
        <th>Amount</th>
        <th>Principal</th>
        <th>Interest</th>
        <th>Fee</th>
        <th>Excess</th>
        <th>Principal Balance</th>
        <th>Interest Balance</th>
        <th>Fee Balance</th>
        <th>Excess Balance</th>
        <th>Effective At</th>
        <th>Created At</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="entry in ledgerEntries" :key="entry.entryId">
        <td>{{ entry.entryId }}</td>
        <td>{{ entry.entryType }}</td>
        <td>{{ entry.amount.number }}</td>
        <td>{{ entry.principal.number }}</td>
        <td>{{ entry.interest.number }}</td>
        <td>{{ entry.fee.number }}</td>
        <td>{{ entry.excess.number }}</td>
        <td>{{ entry.principalBalance.number }}</td>
        <td>{{ entry.interestBalance.number }}</td>
        <td>{{ entry.feeBalance.number }}</td>
        <td>{{ entry.excessBalance.number }}</td>
        <td>{{ entry.effectiveAt }}</td>
        <td>{{ entry.createdAt }}</td>
      </tr>
    </tbody>
  </table>
  <button @click="refreshLedger" class="uk-button uk-button-primary">Refresh Ledger</button>
</template>

<script>
import axios from 'axios'
import LoanLifecycle from '../components/LoanLifeCycleSimulation.vue'

export default {
  components: {
    LoanLifecycle
  },
  data() {
    return {
      ledgerEntries: []
    }
  },
  async created() {
    this.refreshLedger()
  },
  methods: {
    async refreshLedger() {
      const loanId = this.$route.params.id // get the loan ID from the route params
      const response = await axios.get(`/api/v1/loans/${loanId}/ledger`)
      this.ledgerEntries = response.data
    }
  }
}
</script>
