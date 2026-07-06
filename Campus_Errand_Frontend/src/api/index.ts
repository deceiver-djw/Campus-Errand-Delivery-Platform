import request from './request'

// ============ 用户 API ============
export const userApi = {
  login: (data: { studentNo: string; password: string }) =>
    request.post('/user/login', data),
  register: (data: any) => request.post('/user/register', data),
  list: (params: any) => request.get('/user/list', { params }),
  getById: (id: number) => request.get(`/user/${id}`),
  save: (data: any) => request.post('/user', data),
  update: (id: number, data: any) => request.put(`/user/${id}`, data),
  updateStatus: (id: number, status: number) =>
    request.put(`/user/${id}/status`, null, { params: { status } }),
  delete: (id: number) => request.delete(`/user/${id}`),
  search: (studentNo: string) =>
    request.get('/user/search', { params: { studentNo } }),
  changePassword: (id: number, data: any) => request.put(`/user/${id}/password`, data),
  recharge: (id: number, data: any) => request.put(`/user/${id}/balance`, data),
  uploadAvatar: (id: number, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request.post(`/user/${id}/avatar`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

// ============ 快递点 API ============
export const expressPointApi = {
  list: () => request.get('/express-point/list'),
  all: () => request.get('/express-point/all'),
  save: (data: any) => request.post('/express-point', data),
  update: (id: number, data: any) => request.put(`/express-point/${id}`, data),
  updateStatus: (id: number, status: number) =>
    request.put(`/express-point/${id}/status`, null, { params: { status } }),
  delete: (id: number) => request.delete(`/express-point/${id}`),
}

// ============ 跑腿员 API ============
export const runnerApi = {
  getById: (id: number) => request.get(`/runner/${id}`),
  getByUserId: (userId: number) => request.get(`/runner/user/${userId}`),
  list: (params: any) => request.get('/runner/list', { params }),
  apply: (data: any) => request.post('/runner/apply', data),
  audit: (id: number, auditStatus: number, remark?: string) =>
    request.put(`/runner/${id}/audit`, null, { params: { auditStatus, remark } }),
  updateStatus: (id: number, status: number) =>
    request.put(`/runner/${id}/status`, null, { params: { status } }),
  ranking: () => request.get('/runner/ranking'),
}

// ============ 任务 API ============
export const taskApi = {
  getById: (id: number) => request.get(`/task/${id}`),
  list: (params: any) => request.get('/task/list', { params }),
  available: (params: any) => request.get('/task/available', { params }),
  publish: (data: any) => request.post('/task', data),
  update: (id: number, data: any) => request.put(`/task/${id}`, data),
  cancel: (id: number) => request.put(`/task/${id}/cancel`),
}

// ============ 订单 API ============
export const orderApi = {
  getById: (id: number) => request.get(`/order/${id}`),
  list: (params: any) => request.get('/order/list', { params }),
  myAccept: (params: any) => request.get('/order/my-accept', { params }),
  grab: (data: { taskId: number; runnerId: number }) => request.post('/order/grab', data),
  updateStatus: (id: number, status: number, operatorId?: number) =>
    request.put(`/order/${id}/status`, null, { params: { status, operatorId } }),
  confirmArrive: (id: number, operatorId?: number) =>
    request.put(`/order/${id}/confirm`, null, { params: { operatorId } }),
  complete: (id: number, operatorId?: number) =>
    request.put(`/order/${id}/complete`, null, { params: { operatorId } }),
}

// ============ 订单日志 API ============
export const orderLogApi = {
  getByOrderId: (orderId: number) => request.get(`/order-log/order/${orderId}`),
}

// ============ 评价 API ============
export const evaluationApi = {
  save: (data: any) => request.post('/evaluation', data),
  getByOrderId: (orderId: number) => request.get(`/evaluation/order/${orderId}`),
  getByFromUser: (userId: number) => request.get(`/evaluation/from/${userId}`),
  getByToUser: (userId: number) => request.get(`/evaluation/to/${userId}`),
}

// ============ 结算 API ============
export const settlementApi = {
  list: (params: any) => request.get('/settlement/list', { params }),
  getByRunner: (runnerId: number) => request.get(`/settlement/runner/${runnerId}`),
  settle: (id: number) => request.put(`/settlement/${id}/settle`),
  refund: (id: number) => request.put(`/settlement/${id}/refund`),
}

// ============ 消息 API ============
export const messageApi = {
  send: (data: any) => request.post('/message', data),
  chat: (userId1: number, userId2: number) =>
    request.get('/message/chat', { params: { userId1, userId2 } }),
  unread: (userId: number) => request.get(`/message/unread/${userId}`),
  userMessages: (userId: number) => request.get(`/message/user/${userId}`),
  readOne: (id: number) => request.put(`/message/${id}/read`),
  readAll: (userId: number) => request.put(`/message/read-all/${userId}`),
}

// ============ 通知 API ============
export const notificationApi = {
  list: (userId: number, params: any) =>
    request.get(`/notification/user/${userId}`, { params }),
  unread: (userId: number) => request.get(`/notification/unread/${userId}`),
  unreadCount: (userId: number) => request.get(`/notification/unread-count/${userId}`),
  readOne: (id: number) => request.put(`/notification/${id}/read`),
  readAll: (userId: number) => request.put(`/notification/read-all/${userId}`),
}

// ============ 流水 API ============
export const transactionApi = {
  list: (params: any) => request.get('/transaction/list', { params }),
  userRecords: (userId: number, params: any) =>
    request.get(`/transaction/user/${userId}`, { params }),
}
