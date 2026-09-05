<template>
  <a-row class="login" style="margin-top: 80px">
    <a-col :span="8" :offset="8" class="login-main">
      <h1 style="text-align: center"><rocket-two-tone />&nbsp;TrainTicketing 售票系统</h1>
      <a-form :model="loginForm" autocomplete="off" @finish="login">
        <a-form-item name="mobile" :rules="[{ required: true, message: '请输入手机号!' }]">
          <a-input v-model:value="loginForm.mobile" placeholder="手机号" size="large"/>
        </a-form-item>
        <a-form-item name="code" :rules="[{ required: true, message: '请输入验证码!' }]">
          <a-input v-model:value="loginForm.code" placeholder="验证码" size="large">
            <template #addonAfter>
              <a @click="sendCode">{{ countdown > 0 ? countdown + 's 后重发' : '获取验证码' }}</a>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" block size="large" html-type="submit" :loading="loading">登录</a-button>
        </a-form-item>
      </a-form>
      <a-typography-text type="secondary" style="display: block; text-align: center">
        开发环境（容器部署）：验证码会直接弹出显示，无需查看日志
      </a-typography-text>
    </a-col>
  </a-row>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { api } from '@/api'
import { useStore } from 'vuex'

const store = useStore()
const router = useRouter()
const route = useRoute()

const loginForm = reactive({ mobile: '', code: '' })
const loading = ref(false)
const countdown = ref(0)

async function sendCode() {
  if (!loginForm.mobile) {
    message.warning('请先输入手机号')
    return
  }
  if (countdown.value > 0) return
  // 容器环境 member.sms.mock-return-code=true 时，content 直接就是 6 位验证码
  const code = await api.sendCode(loginForm.mobile)
  if (code && /^\d{6}$/.test(code)) {
    loginForm.code = code
    message.success('开发模式：验证码已自动填入 ' + code)
  } else {
    message.success('验证码已发送')
  }
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function login() {
  if (!loginForm.mobile || !loginForm.code) {
    message.warning('请输入手机号和验证码')
    return
  }
  loading.value = true
  try {
    const content = await api.login(loginForm.mobile, loginForm.code)
    store.commit('setLogin', { token: content.token, memberId: content.id, mobile: content.mobile })
    message.success('登录成功')
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}
</script>
