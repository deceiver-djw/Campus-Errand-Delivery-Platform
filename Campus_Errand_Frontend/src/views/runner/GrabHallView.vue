<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox, ElInput, ElInputNumber, ElButton, ElTable, ElTableColumn, ElTag, ElEmpty, ElRate } from 'element-plus'
import { taskApi, orderApi, expressPointApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const tasks = ref<any[]>([])
const points = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)

const filters = reactive({
  expressPoint: '',
  minFee: undefined as number | undefined,
  maxFee: undefined as number | undefined,
})

async function loadPoints() {
  const res: any = await expressPointApi.list()
  points.value = res || []
}

async function load() {
  loading.value = true
  try {
    const params: any = { page: 1, size: 50 }
    if (filters.expressPoint) params.expressPoint = filters.expressPoint
    if (filters.minFee !== undefined) params.minFee = filters.minFee
    if (filters.maxFee !== undefined) params.maxFee = filters.maxFee
    const res: any = await taskApi.available(params)
    tasks.value = res?.data?.records || res?.records || []
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function refresh() {
  refreshing.value = true
  await load()
}

async function grab(taskId: number) {
  try {
    await ElMessageBox.confirm('确认抢单？抢单后请及时完成配送任务。', '提示', { type: 'info' })
  } catch {
    return
  }
  loading.value = true
  try {
    const res: any = await orderApi.grab({ taskId, runnerId: userStore.userId })
    if (res.code === 200) {
      ElMessage.success('抢单成功，请在"我的接单"中查看')
      await load()
    } else {
      ElMessage.warning(res.message || '抢单失败')
    }
  } catch {
    ElMessage.error('抢单失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPoints()
  load()
})
</script>
<template>
  <div class="page-card">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <div style="display:flex;gap:12px">
        <el-select v-model="filters.expressPoint" placeholder="选择快递点" clearable style="width:200px">
          <el-option v-for="p in points" :key="p.id" :label="p.name" :value="p.name" />
        </el-select>
        <el-input-number v-model="filters.minFee" placeholder="最低费" :min="0" style="width:120px" />
        <el-input-number v-model="filters.maxFee" placeholder="最高费" :min="0" style="width:120px" />
        <el-button type="primary" @click="load">搜索</el-button>
      </div>
      <el-button @click="refresh" :loading="refreshing">刷新列表</el-button>
    </div>

    <el-table :data="tasks" border stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="任务ID" width="80" />
      <el-table-column prop="expressPoint" label="快递点" />
      <el-table-column prop="pickupCode" label="取件码" width="100" />
      <el-table-column prop="deliveryAddr" label="送达地址" show-overflow-tooltip />
      <el-table-column label="发布者" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.userId === userStore.userId" type="info" size="small">自己</el-tag>
          <el-tag v-else type="primary" size="small">他人</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跑腿费" width="100">
        <template #default="{ row }">
          <span style="font-size:16px;font-weight:700;color:var(--success-color)">¥{{ row.fee }}</span>
        </template>
      </el-table-column>
      <el-table-column label="加急" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isUrgent" type="danger" size="small">加急</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="160" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            v-if="row.userId === userStore.userId"
            type="info"
            size="small"
            disabled
          >
            自己的任务
          </el-button>
          <el-button v-else type="success" size="small" @click="grab(row.id)">抢单</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!tasks.length && !loading" description="暂无可抢任务" />
  </div>
</template>
