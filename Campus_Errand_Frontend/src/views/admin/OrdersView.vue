<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { orderApi } from '@/api'

const orders = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref<number | undefined>(undefined)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '已接单', type: 'warning' },
  1: { label: '已取件', type: 'primary' },
  2: { label: '配送中', type: 'primary' },
  3: { label: '已送达', type: 'success' },
  4: { label: '待评价', type: 'warning' },
  5: { label: '已完成', type: 'success' },
}

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (statusFilter.value !== undefined) params.status = statusFilter.value
    const res: any = await orderApi.list(params)
    orders.value = res?.data?.records || res?.records || []
    total.value = res?.data?.total || res?.total || 0
  } finally {
    loading.value = false
  }
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
      <h3 style="margin:0">订单管理</h3>
      <div style="display:flex;align-items:center;gap:8px">
        <el-select v-model="statusFilter" placeholder="筛选状态" clearable @change="handleFilterChange" style="width:140px">
          <el-option v-for="(v, k) in statusMap" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </div>

    <el-table :data="orders" border stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="taskId" label="任务ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="70" />
      <el-table-column prop="runnerId" label="跑腿员ID" width="80" />
      <el-table-column label="订单状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="(statusMap[Number(row.status)]?.type || 'info') as any" size="small">
            {{ statusMap[Number(row.status)]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="acceptTime" label="接单时间" width="155">
        <template #default="{ row }">{{ row.acceptTime || '-' }}</template>
      </el-table-column>
      <el-table-column prop="pickupTime" label="取件时间" width="155">
        <template #default="{ row }">{{ row.pickupTime || '-' }}</template>
      </el-table-column>
      <el-table-column prop="deliveryTime" label="配送时间" width="155">
        <template #default="{ row }">{{ row.deliveryTime || '-' }}</template>
      </el-table-column>
      <el-table-column prop="arriveTime" label="送达时间" width="155">
        <template #default="{ row }">{{ row.arriveTime || '-' }}</template>
      </el-table-column>
      <el-table-column prop="completeTime" label="完成时间" width="155">
        <template #default="{ row }">{{ row.completeTime || '-' }}</template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !orders.length" description="暂无订单数据" />

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
