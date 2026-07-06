<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi } from '@/api'

const tasks = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref<number | undefined>(undefined)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待接单', type: 'warning' },
  1: { label: '已接单', type: 'primary' },
  2: { label: '已取消', type: 'info' },
}

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (statusFilter.value !== undefined) params.status = statusFilter.value
    const res: any = await taskApi.list(params)
    tasks.value = res?.data?.records || res?.records || []
    total.value = res?.data?.total || res?.total || 0
  } finally {
    loading.value = false
  }
}

async function forceCancel(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认强制取消任务 #${row.id}（快递点：${row.expressPoint || '-'}）？此操作不可撤销。`,
      '强制取消确认',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '返回' },
    )
  } catch {
    return
  }
  await taskApi.cancel(row.id)
  ElMessage.success('任务已取消')
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
      <h3 style="margin:0">任务管理</h3>
      <div style="display:flex;align-items:center;gap:8px">
        <el-select v-model="statusFilter" placeholder="筛选状态" clearable @change="handleFilterChange" style="width:140px">
          <el-option v-for="(v, k) in statusMap" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </div>

    <el-table :data="tasks" border stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="userId" label="发布用户ID" width="100" />
      <el-table-column prop="expressPoint" label="快递点" min-width="120" show-overflow-tooltip />
      <el-table-column prop="remark" label="备注说明" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column prop="deliveryAddr" label="送达地址" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ row.deliveryAddr || '-' }}</template>
      </el-table-column>
      <el-table-column label="跑腿费" width="90" align="right">
        <template #default="{ row }">
          <span style="color:var(--primary-color);font-weight:600">¥{{ (row.fee || 0).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="(statusMap[Number(row.status)]?.type || 'info') as any" size="small">
            {{ statusMap[Number(row.status)]?.label || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="160">
        <template #default="{ row }">{{ row.createTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="Number(row.status) !== 2"
            type="danger"
            size="small"
            link
            @click="forceCancel(row)"
          >
            强制取消
          </el-button>
          <span v-else style="color:var(--text-secondary);font-size:12px">-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !tasks.length" description="暂无任务数据" />

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
