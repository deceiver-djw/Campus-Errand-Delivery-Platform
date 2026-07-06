<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi, orderApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const tasks = ref<any[]>([])
const activeTab = ref('all')
const loading = ref(false)

const filteredTasks = computed(() => {
  if (activeTab.value === 'all') return tasks.value
  return tasks.value.filter((t: any) => t.status === Number(activeTab.value))
})

const taskStatusMap: Record<number, string> = { 0: '待接单', 1: '已接单', 2: '已取消' }
const orderStatusMap: Record<number, string> = { 0: '已接单', 1: '已取件', 2: '配送中', 3: '已送达', 4: '待评价', 5: '已完成' }

async function loadTasks() {
  loading.value = true
  try {
    const res: any = await taskApi.list({ userId: userStore.userId, page: 1, size: 100 })
    tasks.value = res?.data?.records || res?.records || []
    // Load associated orders for accepted tasks
    for (const task of tasks.value) {
      if (task.status === 1) {
        try {
          const orderRes: any = await orderApi.list({ taskId: task.id, page: 1, size: 1 })
          const orders = orderRes?.data?.records || orderRes?.records || []
          task._order = orders[0] || null
        } catch { task._order = null }
      }
    }
  } finally { loading.value = false }
}

async function cancelTask(id: number) {
  try {
    await ElMessageBox.confirm('确定取消该任务吗？', '提示', { type: 'warning' })
  } catch { return }
  await taskApi.cancel(id)
  ElMessage.success('已取消')
  loadTasks()
}

async function confirmReceipt(orderId: number) {
  try {
    await ElMessageBox.confirm('确认收到快递了吗？', '确认收货', { type: 'info' })
  } catch { return }
  try {
    await orderApi.confirmArrive(orderId, userStore.userId!)
    ElMessage.success('已确认收货')
    loadTasks()
  } catch { ElMessage.error('操作失败') }
}

function getOrderStatusTag(status: number) {
  const styles: Record<number, string> = { 0: 'info', 1: 'warning', 2: '', 3: 'primary', 4: 'warning', 5: 'success' }
  return styles[status] || 'info'
}

onMounted(loadTasks)
</script>

<template>
  <div class="page-card">
    <h3>我的任务</h3>
    <el-tabs v-model="activeTab" @tab-change="loadTasks">
      <el-tab-pane label="全部" value="all" />
      <el-tab-pane label="待接单" value="0" />
      <el-tab-pane label="已接单" value="1" />
      <el-tab-pane label="已取消" value="2" />
    </el-tabs>
    <el-table :data="filteredTasks" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="expressPoint" label="快递点" />
      <el-table-column prop="pickupCode" label="取件码" />
      <el-table-column prop="deliveryAddr" label="送达地址" />
      <el-table-column prop="fee" label="跑腿费" width="80"><template #default="{ row }">¥{{ row.fee }}</template></el-table-column>
      <el-table-column label="任务状态" width="90">
        <template #default="{ row }"><el-tag :type="Number(row.status) === 0 ? 'warning' : Number(row.status) === 1 ? 'primary' : 'info'">{{ taskStatusMap[Number(row.status)] || '未知' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="配送状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row._order" :type="getOrderStatusTag(row._order.status)" size="small">
            {{ orderStatusMap[row._order.status] || '未知' }}
          </el-tag>
          <span v-else style="color:var(--text-secondary)">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="Number(row.status) === 0" type="danger" size="small" link @click="cancelTask(row.id)">取消</el-button>
          <el-button v-if="row._order && row._order.status === 3" type="success" size="small" @click="confirmReceipt(row._order.id)">确认收货</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!filteredTasks.length && !loading" description="暂无任务" />
  </div>
</template>
