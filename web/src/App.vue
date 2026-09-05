<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header style="display: flex; align-items: center; background: #fff; border-bottom: 1px solid #f0f0f0; padding: 0 24px">
      <span style="font-size: 18px; font-weight: 600; margin-right: 40px">🚄 TrainTicketing</span>
      <a-menu v-model:selectedKeys="activeMenu" mode="horizontal" style="flex: 1; border: none"
              @click="({ key }) => $router.push(key)">
        <a-menu-item key="/">车票查询</a-menu-item>
        <a-menu-item key="/orders">我的订单</a-menu-item>
      </a-menu>
      <template v-if="isLogin">
        <span style="margin-right: 16px">{{ mobile }}</span>
        <a-button size="small" @click="logout">退出</a-button>
      </template>
      <a-button v-else type="primary" size="small" @click="$router.push('/login')">登录</a-button>
    </a-layout-header>
    <a-layout-content style="padding: 24px; max-width: 1100px; margin: 0 auto; width: 100%">
      <router-view/>
    </a-layout-content>
  </a-layout>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'

const store = useStore()
const route = useRoute()
const router = useRouter()
const isLogin = computed(() => store.getters.isLogin)
const mobile = computed(() => store.state.mobile)
const activeMenu = ref([route.path])

watch(() => route.path, (p) => { activeMenu.value = [p] })

function logout() {
  store.commit('logout')
  router.push('/login')
}
</script>
