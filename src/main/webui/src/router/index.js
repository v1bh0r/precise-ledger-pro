import { createRouter, createWebHistory } from 'vue-router'
import LoansView from '../views/LoansView.vue'
import NewLoanView from '../views/NewLoanView.vue'
import LedgerView from '../views/LedgerView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: LoansView
    },
    {
      path: '/loans',
      name: 'Loans',
      component: LoansView
    },
    {
      path: '/new-loan',
      name: 'Loans',
      component: NewLoanView
    },
    {
      path: '/loans/:id',
      name: 'Ledger',
      component: LedgerView
    }
  ]
})

export default router
