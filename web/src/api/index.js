import { request } from '@/utils/request'

/** 后端接口集合（路径与网关路由/context-path 对齐） */
export const api = {
  // ===== 会员 =====
  sendCode: (mobile) => request('POST', '/member/member/send-code', { form: { mobile } }),
  login: (mobile, code) => request('POST', '/member/member/login', { form: { mobile, code } }),

  // ===== 乘车人 =====
  passengerList: (memberId) => request('GET', `/member/passenger/list?memberId=${memberId}`),
  passengerSave: (p) => request('POST', '/member/passenger/save', { json: p }),

  // ===== 查询（公开接口） =====
  stationList: (keyword = '') => request('GET', `/business/admin/station/query?keyword=${encodeURIComponent(keyword)}`),
  ticketQuery: (fromStationId, toStationId, runDate) =>
    request('GET', `/business/ticket/query?fromStationId=${fromStationId}&toStationId=${toStationId}&runDate=${runDate}`),

  // ===== 订单 =====
  orderSave: (order) => request('POST', '/business/order/save', { json: order }),
  // 异步下单（MQ 削峰）：毫秒级返回排队订单号，出票由后端异步完成，前端轮询订单状态
  orderAsyncSave: (order) => request('POST', '/business/order/async', { json: order }),
  orderList: () => request('GET', '/business/order/list'),
  orderDetail: (orderNo) => request('GET', `/business/order/detail?orderNo=${encodeURIComponent(orderNo)}`),
  orderPay: (orderNo) => request('POST', `/business/order/pay?orderNo=${encodeURIComponent(orderNo)}`),
  orderCancel: (orderNo) => request('POST', `/business/order/cancel?orderNo=${encodeURIComponent(orderNo)}`),
  orderRefund: (orderNo) => request('POST', `/business/order/refund?orderNo=${encodeURIComponent(orderNo)}`)
}
