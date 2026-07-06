<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { runnerApi, notificationApi, messageApi } from '@/api'
import {
  User,
  List,
  ChatDotRound,
  Star,
  Management,
  DataAnalysis,
  Shop,
  TakeawayBox,
  Money,
  Setting,
  Promotion,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(true)
const applyingRunner = ref(false)
const showRunnerDialog = ref(false)

// 未读消息数量（分开统计，用于不同位置的展示）
const unreadMsgCount = ref(0)          // 头部角标：通知 + 消息总数
const unreadNotifCount = ref(0)        // 侧边栏角标：仅系统通知
let msgTimer: ReturnType<typeof setInterval> | null = null

async function refreshUnreadCount() {
  if (!userStore.userId) return
  try {
    const [notifRes, msgRes]: any[] = await Promise.all([
      notificationApi.unreadCount(userStore.userId),
      messageApi.unread(userStore.userId),
    ])
    const notifCount = typeof notifRes === 'number' ? notifRes : (notifRes?.count ?? 0)
    const msgCount = Array.isArray(msgRes) ? msgRes.length : 0
    unreadNotifCount.value = notifCount
    unreadMsgCount.value = notifCount + msgCount
  } catch {
    /* ignore */
  }
}

function goMessages() {
  router.push('/user/messages')
}

onMounted(() => {
  refreshUnreadCount()
  msgTimer = setInterval(refreshUnreadCount, 30000)
})

onUnmounted(() => {
  if (msgTimer) {
    clearInterval(msgTimer)
    msgTimer = null
  }
})

const runnerForm = reactive({
  realName: '',
  phone: '',
})

const avatarSrc = computed(() => {
  const avatar = userStore.userInfo?.avatar
  if (!avatar) return ''
  return 'http://192.168.106.1:8080' + avatar
})

interface MenuItem {
  index: string
  title: string
  icon: any
  children?: MenuItem[]
}

// 菜单配置：跑腿员(1)同时显示用户和跑腿菜单
const menuConfig: Record<number, MenuItem[]> = {
  0: [
    // 普通用户
    { index: '/user/profile', title: '个人中心', icon: Setting },
    { index: '/user/task-publish', title: '发布任务', icon: Shop },
    { index: '/user/my-tasks', title: '我的任务', icon: List },
    { index: '/user/messages', title: '消息通知', icon: ChatDotRound },
    { index: '/user/evaluations', title: '评价管理', icon: Star },
  ],
  1: [
    // 跑腿员 — 用户菜单 + 跑腿菜单
    { index: '/user/profile', title: '个人中心', icon: Setting },
    { index: '/user/task-publish', title: '发布任务', icon: Shop },
    { index: '/user/my-tasks', title: '我的任务', icon: List },
    { index: '/user/messages', title: '消息通知', icon: ChatDotRound },
    { index: '/user/evaluations', title: '评价管理', icon: Star },
    { index: '/runner/grab-hall', title: '抢单大厅', icon: TakeawayBox },
    { index: '/runner/my-orders', title: '我的接单', icon: List },
    { index: '/runner/income', title: '收益结算', icon: Money },
    { index: '/runner/evaluations', title: '我收到的评价', icon: Star },
  ],
  2: [
    // 管理员
    { index: '/admin/users', title: '用户管理', icon: User },
    { index: '/admin/runners', title: '跑腿员管理', icon: Management },
    { index: '/admin/tasks', title: '任务管理', icon: Shop },
    { index: '/admin/orders', title: '订单管理', icon: List },
    { index: '/admin/settlements', title: '结算管理', icon: Money },
    { index: '/admin/statistics', title: '数据统计', icon: DataAnalysis },
  ],
}

const currentMenu = computed(() => menuConfig[userStore.currentRole])
const activeMenu = computed(() => route.path)

function handleMenuSelect(index: string) {
  router.push(index)
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

function openRunnerDialog() {
  runnerForm.realName = userStore.userInfo?.realName || ''
  runnerForm.phone = userStore.userInfo?.phone || ''
  showRunnerDialog.value = true
}

async function submitRunnerApply() {
  if (!runnerForm.realName || !runnerForm.phone) {
    ElMessage.warning('请填写真实姓名和手机号')
    return
  }
  if (!userStore.userId) return
  applyingRunner.value = true
  try {
    const res: any = await runnerApi.apply({
      userId: userStore.userId,
      realName: runnerForm.realName,
      phone: runnerForm.phone,
    })
    if (res.code === 200) {
      ElMessage.success(res.message || '申请已提交，请等待管理员审核')
      showRunnerDialog.value = false
    } else {
      ElMessage.warning(res.message || '申请提交失败')
    }
  } catch {
    // 网络错误已由 request 拦截器统一提示
  } finally {
    applyingRunner.value = false
  }
}
</script>

<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo-area" :class="{ collapsed: isCollapse }">
        <el-tooltip content="校园跑腿代取快递系统" placement="right">
          <TakeawayBox class="logo-icon" />
        </el-tooltip>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        @select="handleMenuSelect"
      >
        <template v-for="item in currentMenu" :key="item.index">
          <el-menu-item :index="item.index">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>
              {{ item.title }}
              <el-badge
                v-if="item.index === '/user/messages' && unreadNotifCount > 0"
                :value="unreadNotifCount"
                class="menu-badge"
              />
            </template>
          </el-menu-item>
        </template>
      </el-menu>

      <!-- 底部：角色信息 / 申请跑腿员 -->
      <div class="role-switcher" v-show="!isCollapse">
        <span class="role-label">当前角色: {{ userStore.roleLabel }}</span>
        <el-button
          v-if="userStore.currentRole === 0"
          type="primary"
          size="small"
          :icon="Promotion"
          @click="openRunnerDialog"
          style="width: 100%"
        >
          申请成为跑腿员
        </el-button>
      </div>
    </el-aside>

    <!-- 主体区域 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-button link @click="isCollapse = !isCollapse">
            <el-icon :size="18"><List /></el-icon>
          </el-button>
          <span class="header-title">{{ route.meta?.title || '校园跑腿代取快递平台' }}</span>
        </div>
        <div class="header-right">
          <el-badge :value="unreadMsgCount" :hidden="unreadMsgCount === 0" class="msg-badge" @click="goMessages">
            <el-icon :size="20"><ChatDotRound /></el-icon>
          </el-badge>
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" :src="avatarSrc" class="avatar-icon" v-if="avatarSrc" />
              <el-avatar :size="32" class="avatar-icon" v-else>
                <el-icon :size="18"><User /></el-icon>
              </el-avatar>
              <span class="user-name">{{ userStore.roleLabel }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/user/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 申请跑腿员弹窗 -->
    <el-dialog v-model="showRunnerDialog" title="申请成为跑腿员" width="420px" :close-on-click-modal="false">
      <p style="color:var(--text-secondary);font-size:13px;margin:0 0 16px">
        成为跑腿员后，您可以接单代取快递赚取收益。请填写真实信息，提交后将由管理员审核。
      </p>
      <el-form :model="runnerForm" label-width="80px">
        <el-form-item label="真实姓名" required>
          <el-input v-model="runnerForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="runnerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRunnerDialog = false">取消</el-button>
        <el-button type="primary" :loading="applyingRunner" @click="submitRunnerApply">
          提交申请
        </el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background: linear-gradient(180deg, var(--blue-50) 0%, var(--pink-50) 100%);
  border-right: 1px solid var(--border-color);
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo-area {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px 12px;
  gap: 10px;
  border-bottom: 1px solid var(--border-light);
}
.logo-area.collapsed {
  padding: 18px 8px;
}
.logo-icon {
  font-size: 28px;
  color: var(--primary-color);
  flex-shrink: 0;
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--blue-600), var(--pink-400));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
}

.el-menu {
  border-right: none;
  background: transparent;
  flex: 1;
}
.el-menu-item:hover {
  background: linear-gradient(90deg, var(--blue-100), var(--pink-100));
}
.el-menu-item.is-active {
  background: linear-gradient(90deg, var(--blue-200), var(--pink-200));
  color: var(--blue-700);
  font-weight: 600;
}
.menu-badge {
  margin-left: 8px;
}
.menu-badge :deep(.el-badge__content) {
  font-size: 11px;
}

.role-switcher {
  padding: 12px;
  border-top: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}
.role-label {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 顶部栏 */
.layout-header {
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  box-shadow: 0 1px 4px rgba(66, 165, 245, 0.08);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.msg-badge {
  cursor: pointer;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.avatar-icon {
  background: linear-gradient(135deg, var(--blue-400), var(--pink-300));
}
.user-name {
  font-size: 13px;
  color: var(--text-primary);
}

/* 内容区 */
.layout-main {
  background: var(--bg-page);
  padding: 20px;
  overflow-y: auto;
}
</style>
