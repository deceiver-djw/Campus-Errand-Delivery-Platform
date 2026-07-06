import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi, runnerApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const raw = localStorage.getItem('userInfo')
  const userInfo = ref<any>(raw && raw !== 'undefined' ? JSON.parse(raw) : null)

  const isLoggedIn = computed(() => !!token.value)
  const userId = computed(() => userInfo.value?.id)
  const currentRole = computed<number>(() => userInfo.value?.role ?? 0)
  const roleLabel = computed(() => {
    const labels: Record<number, string> = { 0: '普通用户', 1: '跑腿员', 2: '管理员' }
    return labels[currentRole.value] || '普通用户'
  })

  async function login(studentNo: string, password: string) {
    const res: any = await userApi.login({ studentNo, password })
    token.value = res.data?.token || ''
    userInfo.value = res.data?.user || null
    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return res
  }

  function setLogin(t: string, user: any) {
    token.value = t
    userInfo.value = user
    localStorage.setItem('token', t)
    if (user != null) {
      localStorage.setItem('userInfo', JSON.stringify(user))
    } else {
      localStorage.removeItem('userInfo')
    }
  }

  async function refreshUserInfo() {
    if (!userId.value) return
    const res: any = await userApi.getById(userId.value)
    userInfo.value = res?.data || res || userInfo.value
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  async function becomeRunner(form: { realName: string; phone: string }) {
    if (!userId.value) throw new Error('未登录')
    // 提交跑腿员申请
    await runnerApi.apply({
      userId: userId.value,
      realName: form.realName,
      phone: form.phone,
    })
    // 更新用户角色为跑腿员
    await userApi.update(userId.value, { role: 1 })
    await refreshUserInfo()
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.clear()
  }

  return { token, userInfo, currentRole, isLoggedIn, userId, roleLabel, login, setLogin, refreshUserInfo, becomeRunner, logout }
})
