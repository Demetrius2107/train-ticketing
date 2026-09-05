import { createStore } from 'vuex'

const TOKEN_KEY = 'tt_token'
const USER_KEY = 'tt_user'

function loadUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY)) || {}
  } catch (e) {
    return {}
  }
}

export default createStore({
  state: {
    token: localStorage.getItem(TOKEN_KEY) || '',
    memberId: loadUser().memberId || '',
    mobile: loadUser().mobile || ''
  },
  getters: {
    isLogin: (state) => !!state.token
  },
  mutations: {
    setLogin(state, { token, memberId, mobile }) {
      state.token = token
      state.memberId = memberId
      state.mobile = mobile
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify({ memberId, mobile }))
    },
    logout(state) {
      state.token = ''
      state.memberId = ''
      state.mobile = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
