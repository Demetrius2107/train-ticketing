<template>
  <div>
    <a-card style="margin-bottom: 16px">
      <a-form layout="inline" @submit.prevent>
        <a-form-item label="出发站">
          <a-select v-model:value="queryForm.fromStationId" show-search option-filter-prop="label"
                    style="width: 140px" placeholder="出发站"
                    :options="stationOptions"/>
        </a-form-item>
        <a-form-item label="到达站">
          <a-select v-model:value="queryForm.toStationId" show-search option-filter-prop="label"
                    style="width: 140px" placeholder="到达站"
                    :options="stationOptions"/>
        </a-form-item>
        <a-form-item label="日期">
          <a-date-picker v-model:value="queryForm.runDate" value-format="YYYY-MM-DD"/>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="querying" @click="query">查询</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-table :data-source="trains" :pagination="false" row-key="id" :loading="querying">
      <a-table-column title="车次" data-index="trainCode" :width="90"/>
      <a-table-column title="出发-到达" :width="200">
        <template #default="{ record }">
          {{ stationName(record.departStationId) }} → {{ stationName(record.arriveStationId) }}
        </template>
      </a-table-column>
      <a-table-column title="时间" :width="140">
        <template #default="{ record }">{{ record.startTime }} - {{ record.endTime }}</template>
      </a-table-column>
      <a-table-column title="余票">
        <template #default="{ record }">
          <a-tag v-for="r in record.remainingList || []" :key="r.seatType"
                 :color="(r.remainingCount || 0) > 0 ? 'green' : 'red'">
            {{ seatTypeName(r.seatType) }} {{ r.remainingCount }} 张
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="" :width="100">
        <template #default="{ record }">
          <a-button type="primary" size="small"
                    :disabled="!hasTicket(record)"
                    @click="openBooking(record)">预订</a-button>
        </template>
      </a-table-column>
    </a-table>
    <a-empty v-if="!querying && queried && trains.length === 0" description="暂无车次，可先运行 script/seed-demo.py 造演示数据"/>

    <!-- 预订弹窗 -->
    <a-modal v-model:open="booking.visible" title="确认预订" @ok="submitOrder" :confirm-loading="booking.submitting">
      <a-form layout="vertical">
        <a-form-item label="座位类型">
          <a-select v-model:value="booking.seatType" style="width: 200px"
                    :options="booking.seatOptions"/>
        </a-form-item>
        <a-form-item label="乘车人">
          <a-checkbox-group v-model:value="booking.passengerIds" style="display: block">
            <a-checkbox v-for="p in passengers" :key="p.id" :value="p.id">
              {{ p.name }}（{{ maskIdCard(p.idCard) }}）
            </a-checkbox>
          </a-checkbox-group>
          <div v-if="passengers.length === 0" style="margin-top: 8px">
            <a-typography-text type="secondary">名下暂无乘车人，可先新增：</a-typography-text>
            <a-input v-model:value="newPassenger.name" placeholder="姓名" size="small" style="width: 120px; margin: 8px 8px 0 0"/>
            <a-input v-model:value="newPassenger.idCard" placeholder="身份证号" size="small" style="width: 200px; margin-right: 8px"/>
            <a-button size="small" type="primary" @click="addPassenger">新增乘车人</a-button>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { api } from '@/api'
import { useStore } from 'vuex'

const SEAT_NAMES = { 1: '商务座', 2: '一等座', 3: '二等座', 4: '硬卧', 5: '软卧' }
const store = useStore()
const router = useRouter()

const stations = ref([])
const stationOptions = computed(() =>
  stations.value.map(s => ({ value: s.id, label: s.name })))
function stationName(id) {
  const s = stations.value.find(x => x.id === id)
  return s ? s.name : id
}
function seatTypeName(code) {
  return SEAT_NAMES[code] || '座位' + code
}

const queryForm = reactive({
  fromStationId: undefined,
  toStationId: undefined,
  runDate: defaultDate()
})
const querying = ref(false)
const queried = ref(false)
const trains = ref([])

function defaultDate() {
  const d = new Date(Date.now() + 7 * 24 * 3600 * 1000)
  return d.toISOString().slice(0, 10)
}

onMounted(async () => {
  stations.value = await api.stationList() || []
})

async function query() {
  if (!queryForm.fromStationId || !queryForm.toStationId || !queryForm.runDate) {
    message.warning('请选择出发站、到达站和日期')
    return
  }
  if (queryForm.fromStationId === queryForm.toStationId) {
    message.warning('出发站与到达站不能相同')
    return
  }
  querying.value = true
  try {
    trains.value = await api.ticketQuery(queryForm.fromStationId, queryForm.toStationId, queryForm.runDate) || []
    queried.value = true
  } finally {
    querying.value = false
  }
}

function hasTicket(record) {
  return (record.remainingList || []).some(r => (r.remainingCount || 0) > 0)
}

// ===== 预订 =====
const passengers = ref([])
const booking = reactive({
  visible: false,
  submitting: false,
  train: null,
  seatType: undefined,
  seatOptions: [],
  passengerIds: []
})
const newPassenger = reactive({ name: '', idCard: '' })

async function openBooking(record) {
  booking.train = record
  booking.seatOptions = (record.remainingList || [])
    .filter(r => (r.remainingCount || 0) > 0)
    .map(r => ({ value: r.seatType, label: `${seatTypeName(r.seatType)}（余 ${r.remainingCount} 张）` }))
  booking.seatType = booking.seatOptions[0]?.value
  booking.passengerIds = []
  passengers.value = await api.passengerList(store.state.memberId) || []
  booking.visible = true
}

async function addPassenger() {
  if (!newPassenger.name || !newPassenger.idCard) {
    message.warning('请填写乘车人姓名和身份证号')
    return
  }
  const id = await api.passengerSave({
    memberId: store.state.memberId,
    name: newPassenger.name,
    idCard: newPassenger.idCard,
    type: '1'
  })
  passengers.value.push({ id, name: newPassenger.name, idCard: newPassenger.idCard })
  message.success('乘车人已新增')
  newPassenger.name = ''
  newPassenger.idCard = ''
}

async function submitOrder() {
  if (!booking.seatType) {
    message.warning('请选择座位类型')
    return
  }
  if (booking.passengerIds.length === 0) {
    message.warning('请至少勾选一名乘车人')
    return
  }
  booking.submitting = true
  try {
    const chosen = passengers.value.filter(p => booking.passengerIds.includes(p.id))
    // 异步下单：立即拿到排队订单号，出票由后端消费者完成，订单页轮询终态
    const orderNo = await api.orderAsyncSave({
      idempotentKey: crypto.randomUUID(),
      dailyTrainId: booking.train.id,
      departStationId: booking.train.departStationId,
      arriveStationId: booking.train.arriveStationId,
      runDate: booking.train.runDate,
      seatType: booking.seatType,
      passengers: chosen.map(p => ({ passengerId: p.id, name: p.name, idCard: p.idCard }))
    })
    booking.visible = false
    message.success('已进入排队出票，订单号 ' + orderNo + '，可在订单页查看结果')
    router.push('/orders')
  } finally {
    booking.submitting = false
  }
}

function maskIdCard(idCard) {
  return idCard ? idCard.slice(0, 6) + '********' + idCard.slice(-4) : ''
}
</script>
