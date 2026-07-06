<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { notificationApi, messageApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const notifications = ref<any[]>([])
const messages = ref<any[]>([])
const activeTab = ref('all')
const loading = ref(false)
const notifTypeFilter = ref('')

const typeColorMap: Record<string, string> = {
  '订单通知': 'primary',
  '快递状态': 'success',
  '系统通知': 'info',
  '审核通知': 'warning',
  '结算通知': 'danger',
  '任务通知': '',
}
const notificationTypes = computed(() => {
  const types = new Set<string>()
  notifications.value.forEach(n => { if (n.type) types.add(n.type) })
  return Array.from(types)
})
const filteredNotifications = computed(() => {
  if (!notifTypeFilter.value) return notifications.value
  return notifications.value.filter(n => n.type === notifTypeFilter.value)
})
const unreadNotifCount = computed(() => notifications.value.filter(n => !n.isRead).length)
const unreadMsgCount = computed(() => messages.value.length)
const totalUnread = computed(() => unreadNotifCount.value + unreadMsgCount.value)

// 全部消息：合并系统通知 + 站内消息
type UnifiedMsg = {
  id: string; src: string; title: string; content: string
  category: string; isRead: boolean; createTime: string; rawId: number
}
const allMessages = computed<UnifiedMsg[]>(() => {
  const list: UnifiedMsg[] = []
  notifications.value.forEach(n => {
    list.push({
      id: 'notif_' + n.id, src: 'notification',
      title: n.title || '无标题', content: n.content || '',
      category: n.type || '系统通知', isRead: !!n.isRead,
      createTime: n.createTime || '', rawId: n.id,
    })
  })
  messages.value.forEach(m => {
    list.push({
      id: 'msg_' + m.id, src: 'message',
      title: '站内消息', content: m.content || '',
      category: '站内消息', isRead: !!m.isRead,
      createTime: m.createTime || '', rawId: m.id,
    })
  })
  list.sort((a, b) => b.createTime.localeCompare(a.createTime))
  return list
})

function srcTag(src: string) { return src === 'notification' ? 'success' : 'warning' as const }
function srcLabel(src: string) { return src === 'notification' ? '系统通知' : '站内消息' }
function catTag(type: string) { return typeColorMap[type] || 'info' }

async function loadNotifications() {
  try {
    const res: any = await notificationApi.list(userStore.userId!, { page: 1, size: 50 })
    notifications.value = res?.data?.records || res?.records || []
  } catch { /* ignore */ }
}
async function loadMessages() {
  try {
    const res: any = await messageApi.userMessages(userStore.userId!)
    messages.value = res?.data?.records || res?.records || (Array.isArray(res) ? res : [])
  } catch { /* ignore */ }
}
async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadNotifications(), loadMessages()])
  } finally {
    loading.value = false
  }
}

async function readNotif(id: number) { loading.value = true; await notificationApi.readOne(id); await loadNotifications(); loading.value = false }
async function readAllNotif() { loading.value = true; await notificationApi.readAll(userStore.userId!); ElMessage.success('全部已读'); await loadNotifications(); loading.value = false }
async function readMsg(id: number) { loading.value = true; await messageApi.readOne(id); await loadMessages(); loading.value = false }
async function readAllMsg() { loading.value = true; await messageApi.readAll(userStore.userId!); ElMessage.success('全部已读'); await loadMessages(); loading.value = false }

function readOne(item: UnifiedMsg) {
  if (item.src === 'notification') readNotif(item.rawId)
  else readMsg(item.rawId)
}

function handleTabChange(tab: string) {
  loading.value = true
  if (tab === 'all') loadAll().finally(() => loading.value = false)
  else if (tab === 'notification') loadNotifications().finally(() => loading.value = false)
  else loadMessages().finally(() => loading.value = false)
}

onMounted(loadAll)
</script>

<template>
  <div class="page-card">
    <h3>消息中心</h3>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane value="all">
        <template #label>
          <span>全部消息 <el-badge :value="totalUnread" :hidden="!totalUnread" /></span>
        </template>
        <div class="tab-card">
          <el-table :data="allMessages" border stripe v-loading="loading">
            <el-table-column label="来源" width="100">
              <template #default="{ row }">
                <el-tag :type="srcTag(row.src)" size="small">{{ srcLabel(row.src) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="120" show-overflow-tooltip />
            <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="catTag(row.category)" size="small">{{ row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.isRead ? 'info' : 'danger'" size="small">{{ row.isRead ? '已读' : '未读' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="160" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button v-if="!row.isRead" type="primary" size="small" link @click="readOne(row)">标为已读</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!allMessages.length && !loading" description="暂无消息" />
        </div>
      </el-tab-pane>

      <el-tab-pane value="notification">
        <template #label>
          <span>系统通知 <el-badge :value="unreadNotifCount" :hidden="!unreadNotifCount" /></span>
        </template>
        <div class="tab-card">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
            <el-select v-model="notifTypeFilter" placeholder="筛选通知类型" clearable size="small" style="width:160px">
              <el-option v-for="t in notificationTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <el-button type="primary" size="small" @click="readAllNotif">全部已读</el-button>
          </div>
          <el-table :data="filteredNotifications" border stripe v-loading="loading">
            <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
            <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="catTag(row.type)" size="small">{{ row.type || '系统通知' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.isRead ? 'info' : 'danger'" size="small">{{ row.isRead ? '已读' : '未读' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="160" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button v-if="!row.isRead" type="primary" size="small" link @click="readNotif(row.id)">标为已读</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!filteredNotifications.length && !loading" description="暂无通知" />
        </div>
      </el-tab-pane>

      <el-tab-pane value="message">
        <template #label>
          <span>站内消息 <el-badge :value="unreadMsgCount" :hidden="!unreadMsgCount" /></span>
        </template>
        <div class="tab-card">
          <div style="display:flex;justify-content:flex-end;margin-bottom:12px">
            <el-button type="primary" size="small" @click="readAllMsg">全部已读</el-button>
          </div>
          <el-table :data="messages" border stripe v-loading="loading">
            <el-table-column prop="fromUserId" label="发送者" width="100" />
            <el-table-column prop="content" label="内容" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.isRead ? 'info' : 'danger'" size="small">{{ row.isRead ? '已读' : '未读' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="160" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button v-if="!row.isRead" type="primary" size="small" link @click="readMsg(row.id)">标为已读</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!messages.length && !loading" description="暂无站内消息" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
:deep(.el-tabs__active-bar) { background-color: #409EFF !important; }
:deep(.el-tabs__item.is-active) { color: #409EFF !important; }
:deep(.el-tabs__item.is-active span) { color: #409EFF !important; }
.tab-card {
  background: #fff; border-radius: 8px; padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
</style>
