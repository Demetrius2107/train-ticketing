/**
 * 极简请求封装（fetch，无第三方依赖）：
 * - 自动附带 Authorization: Bearer {token}
 * - 按后端统一返回 CommonResp 解包：success=true 返回 content，否则 message.error 并 reject
 */
import { message } from 'ant-design-vue'

const TOKEN_KEY = 'tt_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export async function request(method, path, { form, json, silent } = {}) {
  const headers = {}
  let body
  if (form) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded'
    body = new URLSearchParams(form).toString()
  }
  if (json) {
    headers['Content-Type'] = 'application/json;charset=UTF-8'
    body = JSON.stringify(json)
  }
  const token = getToken()
  if (token) {
    headers['Authorization'] = 'Bearer ' + token
  }
  let resp
  try {
    resp = await fetch(path, { method, headers, body })
  } catch (e) {
    message.error('网络异常，请确认后端服务已启动')
    throw e
  }
  if (!resp.ok) {
    const err = new Error('HTTP ' + resp.status)
    if (!silent) message.error(err.message)
    throw err
  }
  const data = await resp.json()
  if (!data.success) {
    const err = new Error(data.message || '请求失败')
    if (!silent) message.error(err.message)
    throw err
  }
  return data.content
}
