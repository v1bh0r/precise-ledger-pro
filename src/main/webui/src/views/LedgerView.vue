<template>
  <LoanLifecycle />

  <ul uk-accordion>
    <li class="uk-open">
      <a class="uk-accordion-title" href>Ledger</a>
      <div class="uk-accordion-content">
        <table
          class="uk-table uk-table-striped uk-table-small uk-table-responsive uk-table-divider"
        >
          <thead>
            <tr>
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
              <th>Source Activity Type</th>
              <th>Source Activity ID</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="entry in ledgerEntries" :key="entry.entryId">
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
              <td>{{ entry.sourceLedgerActivityType }}</td>
              <td>{{ entry.sourceLedgerActivityId }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </li>
    <li>
      <a class="uk-accordion-title" href>Activities</a>
      <div class="uk-accordion-content">
        <table
          class="uk-table uk-table-striped uk-table-small uk-table-responsive uk-table-divider"
        >
          <thead>
            <tr>
              <th>Activity Type</th>
              <th>Activity ID</th>
              <th>Common Name</th>
              <th>Amount</th>
              <th>Transaction Time</th>
              <th>Transaction Strategy</th>
              <th>Direction</th>
              <th>Spread</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="activity in ledgerActivities" :key="activity.id">
              <td>{{ activity.activityType }}</td>
              <td>{{ activity.activityId }}</td>
              <td>{{ activity.commonName }}</td>
              <td>{{ activity.amount.number }}</td>
              <td>{{ activity.transactionTime }}</td>
              <td>{{ activity.transactionStrategy }}</td>
              <td>{{ activity.direction }}</td>
              <td>{{ activity.spread }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </li>
  </ul>
</template>

<script>
import LoanLifecycle from '../components/LoanLifeCycleSimulation.vue'
import axios from 'axios'

export default {
  components: {
    LoanLifecycle
  },
  data() {
    return {
      ledgerEntries: [],
      ledgerActivities: [],
      refreshInterval: null
    }
  },
  async created() {
    this.refreshLedger()
    this.refreshInterval = setInterval(this.refreshLedger, 5000)
  },
  beforeUnmount() {
    clearInterval(this.refreshInterval)
  },
  methods: {
    async refreshLedger() {
      const loanId = this.$route.params.id
      const response = await axios.get(`/api/v1/loans/${loanId}/ledger`)
      this.ledgerEntries = response.data

      const response2 = await axios.get(`/api/v1/loans/${loanId}/ledger-activities`)
      this.ledgerActivities = response2.data
    }
  }
}
</script>
