<template>
  <div>
    <a-table :data-source="orders" :pagination="false" row-key="orderNo" :loading="loading">
      <a-table-column title="订单号" data-index="orderNo" :width="170"/>
      <a-table-column title="乘车日期" :width="110">
        <template #default="{ record }">{{ (record.runDate || '').slice(0, 10) }}</template>
      </a-table-column>
      <a-table-column title="乘车人/座位">
        <template #default="{ record }">
          <a-tag v-for="it in record.items || []" :key="it.id">
            {{ it.passengerName }} · {{ seatTypeName(it.seatType) }} · ¥{{ it.price }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="金额" :width="90">
        <template #default="{ record }">¥{{ record.totalAmount }}</template>
      </a-table-column>
      <a-table-column title="状态" :width="90">
        <template #default="{ record }">
          <a-badge :status="statusMeta(record.status).color" :text="statusMeta(record.status).text"/>
        </template>
      </a-table-column>
      <a-table-column title="下单时间" :width="160">
        <template #default="{ record }">{{ formatTime(record.createTime) }}</template>
      </a-table-column>
      <a-table-column title="操作" :width="160">
        <template #default="{ record }">
          <a-space>
            <a-button v-if="record.status === '0'" type="primary" size="small" @click="act(record, 'pay')">支付</a-button>
            <a-button v-if="record.status === '0'" size="small" danger @click="act(record, 'cancel')">取消</a-button>
            <a-button v-if="record.status === '1'" size="small" danger @click="act(record, 'refund')">退票</a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
    <a-empty v-if="!loading && orders.length === 0" description="暂无订单，去查票页面下一单吧"/>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { api } from '@/api'

const SEAT_NAMES = { 1: '商务座', 2: '一等座', 3: '二等座', 4: '硬卧', 5: '软卧' }
const STATUS = {
  0: { text: '待支付', color: 'warning' },
  1: { text: '已支付', color: 'processing' },
  2: { text: '已取消', color: 'default' },
  3: { text: '已退票', color: 'error' },
  4: { text: '出票中', color: 'processing' },
  5: { text: '出票失败', color: 'error' }
}

const orders = ref([])
const loading = ref(false)

// 出票中订单轮询：异步下单（MQ 削峰）后出票需几百毫秒，存在出票中订单时每 3s 刷新，2 分钟上限
const POLL_INTERVAL = 3000
const POLL_MAX = 40
let pollTimer = null
let pollCount = 0

function seatTypeName(code) {
  return SEAT_NAMES[code] || '座位' + code
}

function statusMeta(status) {
  return STATUS[status] || { text: status, color: 'default' }
}

function formatTime(ts) {
  if (!ts) return '-'
  const d = new Date(ts)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function load() {
  loading.value = true
  try {
    orders.value = await api.orderList() || []
  } finally {
    loading.value = false
  }
  schedulePollIfQueuing()
}

function schedulePollIfQueuing() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
  const queuing = orders.value.some(o => o.status === '4')
  if (!queuing || pollCount >= POLL_MAX) return
  pollCount++
  pollTimer = setTimeout(load, POLL_INTERVAL)
}

const ACT_TEXT = { pay: '支付', cancel: '取消', refund: '退票' }

function act(order, action) {
  const run = async () => {
    await api[`order${action[0].toUpperCase()}${action.slice(1)}`](order.orderNo)
    message.success(ACT_TEXT[action] + '成功')
    load()
  }
  if (action === 'pay') {
    run()
  } else {
    Modal.confirm({
      title: `确认${ACT_TEXT[action]}？`,
      content: `订单号 ${order.orderNo}`,
      onOk: run
    })
  }
}

onMounted(() => {
  pollCount = 0
  load()
})

onUnmounted(() => {
  if (pollTimer) clearTimeout(pollTimer)
})
</script>
