<template>
  <div class="loan-lifecycle uk-container uk-margin uk-width-1-1">
    <!-- General Ledger Activity Form -->
    <section class="general-ledger-activity uk-margin">
      <h2>General Ledger Activity</h2>
      <form @submit.prevent="postActivity" class="uk-form-stacked">
        <table class="uk-table uk-table-divider">
          <thead>
            <tr>
              <th>Activity ID</th>
              <th>Common Name</th>
              <th>Amount</th>
              <th>Direction</th>
              <th>Spread</th>
              <th>Effective At</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>
                <input v-model="newActivity.activityId" class="uk-input" type="text" readonly />
              </td>
              <td>
                <input v-model="newActivity.commonName" class="uk-input" type="text" required />
              </td>
              <td>
                <input v-model.number="newActivity.amount" class="uk-input" type="number" />
              </td>
              <td>
                <select v-model="newActivity.direction" class="uk-select">
                  <option value="CREDIT">CREDIT</option>
                  <option value="DEBIT">DEBIT</option>
                </select>
              </td>
              <td><input v-model="newActivity.spread" class="uk-input" type="text" /></td>
              <td>
                <input v-model="newActivity.effectiveAt" class="uk-input" type="datetime-local" />
              </td>
              <td>
                <button type="submit" class="uk-button uk-button-primary">Post Activity</button>
              </td>
            </tr>
          </tbody>
        </table>
      </form>

      <form @submit.prevent="submitReversal" class="uk-form-stacked">
        <table class="uk-table uk-table-divider">
          <thead>
            <tr>
              <th>Activity Type</th>
              <th>Activity ID</th>
              <th>Common Name</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>
                <input
                  id="activityType"
                  v-model="reversal.activityType"
                  class="uk-input"
                  type="text"
                  required
                />
              </td>
              <td>
                <input
                  id="activityId"
                  v-model="reversal.activityId"
                  class="uk-input"
                  type="text"
                  required
                />
              </td>
              <td>
                <input
                  id="commonName"
                  v-model="reversal.commonName"
                  class="uk-input"
                  type="text"
                  required
                />
              </td>
              <td>
                <button type="submit" class="uk-button uk-button-primary">Submit Reversal</button>
              </td>
            </tr>
          </tbody>
        </table>
      </form>
    </section>

    <!-- Time Control Section -->
    <section class="time-control uk-margin">
      <h2>Time Control</h2>
      <div>
        <label for="speed">Speed of Time:</label>
        <input
          v-model="timeSpeed"
          class="uk-range"
          type="range"
          min="1"
          max="10"
          step="1"
          id="speed"
        />
        <span>{{ timeSpeed }}x</span>
      </div>
      <div>
        <span>Current Date: {{ currentDate }}</span>
        <label for="currentDate">Current Date:</label>
        <input
          v-model="currentDateInput"
          @change="updateCurrentDate"
          class="uk-input"
          type="datetime-local"
          id="currentDate"
        />
      </div>
      <button @click="toggleSimulation" class="uk-button uk-button-default">
        {{ this.isSimulationRunning ? 'Pause' : 'Start' }}
      </button>
    </section>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      isSimulationRunning: false,
      reversal: {
        activityType: '',
        activityId: '',
        commonName: ''
      },
      newActivity: {
        activityId: this.generateId(),
        commonName: '',
        activityType: 'Transaction', // Hardcoded value
        transactionStrategy: 'ComputationalSpread', // Default value
        principal: 0,
        interest: 0,
        fee: 0,
        excess: 0,
        direction: 'DEBIT', // Default value
        spread: 'FIP', // Default text
        reversalActivityType: '',
        reversalActivityId: '',
        effectiveAt: new Date().toISOString().slice(0, 16),
        transactionTime: new Date().toISOString().slice(0, 16),
        amount: 0
      },
      activities: [],
      timeSpeed: 1,
      currentDate: new Date(),
      simulationInterval: null,
      currentDateInput: new Date().toISOString().slice(0, 16)
    }
  },
  watch: {
    'reversal.activityId': 'updateReversalCommonName',
    'reversal.activityType': 'updateReversalCommonName'
  },
  beforeUnmount() {
    clearInterval(this.simulationInterval)
  },
  methods: {
    async postActivity() {
      const loanId = this.$route.params.id
      try {
        this.newActivity.transactionTime = this.currentDate.toISOString().slice(0, 16)
        const response = await axios.post(
          `/api/v1/loans/${loanId}/ledger-activities`,
          this.newActivity
        )
        this.newActivity.id = response.data.id
        this.resetForm()
      } catch (error) {
        console.error('Error posting activity:', error)
      }
    },
    generateId() {
      return '_' + Math.random().toString(36).substr(2, 9)
    },
    resetForm() {
      this.newActivity = {
        activityId: this.generateId(),
        commonName: '',
        activityType: 'Transaction', // Hardcoded value
        transactionStrategy: 'ComputationalSpread', // Default value
        principal: 0,
        interest: 0,
        fee: 0,
        excess: 0,
        direction: 'DEBIT', // Default value
        spread: 'FIP', // Default text
        reversalActivityType: '',
        reversalActivityId: '',
        effectiveAt: this.currentDate.toISOString().slice(0, 16),
        transactionTime: this.currentDate.toISOString().slice(0, 16),
        amount: 0
      }
    },
    updateReversalCommonName() {
      this.reversal.commonName = `Reversal - ${this.reversal.activityType} - ${this.reversal.activityId}`
    },
    toggleSimulation() {
      if (this.isSimulationRunning) {
        this.pauseSimulation()
      } else {
        this.startSimulation()
      }
    },
    startSimulation() {
      this.isSimulationRunning = true
      let previousDate = new Date(this.currentDate)
      if (this.simulationInterval) return
      this.simulationInterval = setInterval(() => {
        this.currentDate = new Date(this.currentDate.getTime() + 3600000 * this.timeSpeed) // 1 hour per second at 1x speed
        this.newActivity.transactionTime = this.newActivity.effectiveAt = this.currentDate
          .toISOString()
          .slice(0, 16)

        // Check if it's the start of a new day
        if (previousDate.getDate() !== this.currentDate.getDate()) {
          this.postInterestAccrual()
          previousDate = this.currentDate
        }
      }, 1000)
    },
    pauseSimulation() {
      this.isSimulationRunning = false
      clearInterval(this.simulationInterval)
      this.simulationInterval = null
    },
    async created() {
      const loanId = this.$route.params.id
      try {
        const response = await axios.get(`/api/v1/loans/${loanId}/ledger-activities`)
        const ledgerEntries = response.data
        if (ledgerEntries.length > 0) {
          const lastEntry = ledgerEntries[ledgerEntries.length - 1]
          this.currentDate = new Date(new Date(lastEntry.createdAt).getTime() + 1000)
        }
      } catch (error) {
        console.error('Error fetching ledger entries:', error)
      }
    },
    async postInterestAccrual() {
      const loanId = this.$route.params.id
      try {
        const startOfDay = new Date(this.currentDate)
        startOfDay.setHours(0, 0, 0, 0)
        const interestAccrual = {
          activityId: 'SOD-' + startOfDay.toISOString().split('T')[0],
          commonName: 'Interest Accrual',
          activityType: 'StartOfDay',
          transactionStrategy: this.newActivity.transactionStrategy,
          principal: 0,
          interest: this.newActivity.interest, // Example interest accrual
          fee: 0,
          excess: 0,
          direction: 'CREDIT', // Interest accrual is typically a credit
          spread: this.newActivity.spread,
          reversalActivityType: '',
          reversalActivityId: '',
          effectiveAt: startOfDay.toISOString().slice(0, 16),
          transactionTime: this.currentDate.toISOString().slice(0, 16),
          amount: this.newActivity.interest
        }
        const response = await axios.post(
          `/api/v1/loans/${loanId}/ledger-activities`,
          interestAccrual
        )
        interestAccrual.id = response.data.id
      } catch (error) {
        console.error('Error posting interest accrual:', error)
      }
    },
    updateCurrentDate() {
      this.currentDate = new Date(this.currentDateInput)
    },
    async submitReversal() {
      try {
        const reversalData = {
          activityId: this.reversal.activityId,
          commonName: this.reversal.commonName,
          activityType: 'Reversal',
          reversalActivityId: this.reversal.activityId,
          reversalActivityType: this.reversal.activityType,
          effectiveAt: this.currentDate,
          transactionTime: this.currentDate
        }
        const loanId = this.$route.params.id // get the loan ID from the route params
        await axios.post(`/api/v1/loans/${loanId}/ledger-activities`, reversalData)
      } catch (error) {
        console.error(error)
      }
    }
  }
}
</script>

<style>
.loan-lifecycle {
  font-family: Arial, sans-serif;
}
.uk-table-divider > tbody > tr > td {
  vertical-align: middle;
}
.time-control input[type='range'] {
  width: 200px;
}
.activity-log ul {
  list-style-type: none;
  padding: 0;
}
.activity-log li {
  background: #f0f0f0;
  margin-bottom: 5px;
  padding: 10px;
  border-radius: 4px;
}
</style>
