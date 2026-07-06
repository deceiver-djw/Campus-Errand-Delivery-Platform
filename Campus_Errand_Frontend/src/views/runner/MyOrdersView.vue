<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, runnerApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const orders = ref<any[]>([])
const runnerId = ref<number>(0)
const loading = ref(false)

const statusMap: Record<number, string> = {
  0: '已接单',
  1: '已取件',
  2: '配送中',
  3: '已送达',
  4: '待评价',
  5: '已完成',
}
const statusTagType: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'primary',
  3: 'success',
  4: 'warning',
  5: 'success',
}
const statusActions: Record<number, string> = { 0: '确认取件', 1: '开始配送', 2: '确认送达' }

async function load() {
  loading.value = true
  try {
    // 先获取跑腿员档案的真实ID（Runner.id，非User.id）
    if (!runnerId.value) {
      const runner: any = await runnerApi.getByUserId(userStore.userId!)
      runnerId.value = runner?.id || 0
    }
    if (!runnerId.value) {
      orders.value = []
      return
    }
    const res: any = await orderApi.myAccept({ page: 1, size: 50, runnerId: runnerId.value })
    orders.value = res?.data?.records || res?.records || []
  } finally {
    loading.value = false
  }
}

async function advanceOrder(order: any) {
  const curStatus = Number(order.status)
  const nextStatus = curStatus + 1
  if (nextStatus > 3) {
    ElMessage.warning('无法继续推进状态')
    return
  }

  const actionName = statusActions[curStatus] || '推进'
  try {
    await ElMessageBox.confirm(`确认执行"${actionName}"操作？`, '提示', { type: 'info' })
  } catch {
    return
  }

  try {
    await orderApi.updateStatus(order.id, nextStatus, userStore.userId!)
    ElMessage.success(`${actionName}成功`)
    load()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <h3>我的接单</h3>
    <el-table :data="orders" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="taskId" label="任务ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="70" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(statusTagType[Number(row.status)] || 'info') as any" size="small">
            {{ statusMap[Number(row.status)] || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="acceptTime" label="接单时间" width="160" />
      <el-table-column prop="pickupTime" label="取件时间" width="160">
        <template #default="{ row }">{{ row.pickupTime || '-' }}</template>
      </el-table-column>
      <el-table-column prop="deliveryTime" label="配送时间" width="160">
        <template #default="{ row }">{{ row.deliveryTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button
            v-if="Number(row.status) >= 0 && Number(row.status) < 3"
            type="primary"
            size="small"
            @click="advanceOrder(row)"
          >
            {{ statusActions[Number(row.status)] || '推进' }}
          </el-button>
          <el-tag v-else-if="Number(row.status) === 4" type="warning" size="small">等待评价</el-tag>
          <el-tag v-else-if="Number(row.status) === 5" type="success" size="small">已完成</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!orders.length && !loading" description="暂无订单" />
  </div>
</template>
