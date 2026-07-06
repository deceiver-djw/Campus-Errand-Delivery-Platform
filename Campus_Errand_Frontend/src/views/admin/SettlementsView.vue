<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { settlementApi } from '@/api'

const settlements = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref<number | undefined>(undefined)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待结算', type: 'warning' },
  1: { label: '已结算', type: 'success' },
  2: { label: '已退款', type: 'danger' },
}

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (statusFilter.value !== undefined) params.status = statusFilter.value
    const res: any = await settlementApi.list(params)
    settlements.value = res?.data?.records || res?.records || []
    total.value = res?.data?.total || res?.total || 0
  } finally {
    loading.value = false
  }
}

async function doSettle(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认对订单 #${row.orderId} 进行结算？金额：¥${(row.amount || 0).toFixed(2)}`,
      '结算确认',
      { type: 'warning', confirmButtonText: '确认结算' },
    )
  } catch {
    return
  }
  await settlementApi.settle(row.id)
  ElMessage.success('结算成功')
  load()
}

async function doRefund(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认对订单 #${row.orderId} 进行退款？金额：¥${(row.amount || 0).toFixed(2)}`,
      '退款确认',
      { type: 'warning', confirmButtonText: '确认退款' },
    )
  } catch {
    return
  }
  await settlementApi.refund(row.id)
  ElMessage.success('退款成功')
  load()
}

function handleFilterChange() {
  page.value = 1
  load()
}

function handlePageChange(p: number) {
  page.value = p
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;flex-wrap:wrap;gap:8px">
      <h3 style="margin:0">结算管理</h3>
      <div style="display:flex;align-items:center;gap:8px">
        <el-select v-model="statusFilter" placeholder="筛选状态" clearable @change="handleFilterChange" style="width:140px">
          <el-option v-for="(v, k) in statusMap" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </div>

    <el-table :data="settlements" border stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="orderId" label="订单ID" width="80" />
      <el-table-column prop="runnerId" label="跑腿员ID" width="90" />
      <el-table-column label="金额" width="110" align="right">
        <template #default="{ row }">
          <span style="color:var(--primary-color);font-weight:600">¥{{ (row.amount || 0).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="(statusMap[Number(row.status)]?.type || 'info') as any" size="small">
            {{ statusMap[Number(row.status)]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }">{{ row.createTime || '-' }}</template>
      </el-table-column>
      <el-table-column prop="settleTime" label="结算时间" width="160">
        <template #default="{ row }">{{ row.settleTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <template v-if="Number(row.status) === 0">
            <el-button type="success" size="small" link @click="doSettle(row)">结算</el-button>
            <el-button type="danger" size="small" link @click="doRefund(row)">退款</el-button>
          </template>
          <span v-else style="color:var(--text-secondary);font-size:12px">-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !settlements.length" description="暂无结算记录" />

    <div v-if="total > pageSize" style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination
        v-model:current-page="page"
        :total="total"
        :page-size="pageSize"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>
