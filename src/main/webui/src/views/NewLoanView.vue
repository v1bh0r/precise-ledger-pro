<template>
  <form @submit.prevent="submitForm" class="uk-form-stacked">
    <div class="uk-margin">
      <label class="uk-form-label" for="interestRate">Interest Rate:</label>
      <div class="uk-form-controls">
        <input
          id="interestRate"
          v-model="interestRate"
          type="number"
          step="0.01"
          class="uk-input"
          required
        />
      </div>
    </div>

    <div class="uk-margin">
      <label class="uk-form-label" for="daysInYear">Days in Year:</label>
      <div class="uk-form-controls">
        <input id="daysInYear" v-model="form.daysInYear" type="number" class="uk-input" required />
      </div>
    </div>
    <div class="uk-margin">
      <label class="uk-form-label" for="currencyCode">Currency Code:</label>
      <div class="uk-form-controls">
        <input
          id="currencyCode"
          v-model="form.currencyCode"
          type="text"
          class="uk-input"
          required
        />
      </div>
    </div>
    <div class="uk-margin">
      <label class="uk-form-label" for="externalId">External ID:</label>
      <div class="uk-form-controls">
        <input id="externalId" v-model="form.externalId" type="text" class="uk-input" />
      </div>
    </div>
    <div class="uk-margin">
      <label class="uk-form-label" for="lastLedgerFreezePrincipalBalance"
        >Last Ledger Freeze Principal Balance:</label
      >
      <div class="uk-form-controls">
        <input
          id="lastLedgerFreezePrincipalBalance"
          v-model="form.lastLedgerFreezePrincipalBalance"
          type="number"
          step="0.01"
          class="uk-input"
        />
      </div>
    </div>
    <div class="uk-margin">
      <label class="uk-form-label" for="lastLedgerFreezeInterestBalance"
        >Last Ledger Freeze Interest Balance:</label
      >
      <div class="uk-form-controls">
        <input
          id="lastLedgerFreezeInterestBalance"
          v-model="form.lastLedgerFreezeInterestBalance"
          type="number"
          step="0.01"
          class="uk-input"
        />
      </div>
    </div>
    <div class="uk-margin">
      <label class="uk-form-label" for="lastLedgerFreezeFeeBalance"
        >Last Ledger Freeze Fee Balance:</label
      >
      <div class="uk-form-controls">
        <input
          id="lastLedgerFreezeFeeBalance"
          v-model="form.lastLedgerFreezeFeeBalance"
          type="number"
          step="0.01"
          class="uk-input"
        />
      </div>
    </div>
    <div class="uk-margin">
      <label class="uk-form-label" for="lastLedgerFreezeExcessBalance"
        >Last Ledger Freeze Excess Balance:</label
      >
      <div class="uk-form-controls">
        <input
          id="lastLedgerFreezeExcessBalance"
          v-model="form.lastLedgerFreezeExcessBalance"
          type="number"
          step="0.01"
          class="uk-input"
        />
      </div>
    </div>
    <div class="uk-margin">
      <label class="uk-form-label" for="lastLedgerFrozenOn">Last Ledger Frozen On:</label>
      <div class="uk-form-controls">
        <input
          id="lastLedgerFrozenOn"
          v-model="form.lastLedgerFrozenOn"
          type="datetime-local"
          class="uk-input"
        />
      </div>
    </div>
    <div class="uk-margin">
      <button type="submit" class="uk-button uk-button-primary">Create Loan</button>
    </div>
  </form>
</template>

<script>
import axios from 'axios'
import router from '../router'
export default {
  data() {
    return {
      form: {
        daysInYear: '',
        currencyCode: '',
        externalId: '',
        lastLedgerFreezePrincipalBalance: '',
        lastLedgerFreezeInterestBalance: '',
        lastLedgerFreezeFeeBalance: '',
        lastLedgerFreezeExcessBalance: '',
        lastLedgerFrozenOn: ''
      },
      interestRate: ''
    }
  },
  methods: {
    async submitForm() {
      try {
        const response = await axios.post('/api/v1/loans', this.form)
        console.log(response.data)
        this.$store.commit('setMessage', 'Loan ' + response.data.id + ' created successfully')
        this.$store.commit('setErrorMessage', '')
        // After loan creation, create interest rate
        const interestRateResponse = await axios.post('/api/v1/interest-rates', {
          loanId: response.data.id,
          rate: this.interestRate,
          effectiveAt: new Date().toISOString() // assuming the interest rate is effective immediately
        })
        console.log(interestRateResponse.data)
        router.push('/')
      } catch (error) {
        this.$store.commit('setMessage', '')
        this.$store.commit('setErrorMessage', error)
        console.error(error)
      }
    }
  }
}
</script>
