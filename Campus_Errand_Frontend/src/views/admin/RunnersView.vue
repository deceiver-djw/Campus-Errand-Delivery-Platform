<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { runnerApi } from '@/api'

const runners = ref<any[]>([])
const activeTab = ref(-1)
const loading = ref(false)
const searchKey = ref('')
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filtered = computed(() => {
  let list = activeTab.value === -1 ? runners.value : runners.value.filter((r: any) => r.auditStatus === activeTab.value)
  if (searchKey.value) {
    const kw = searchKey.value.toLowerCase()
    list = list.filter((r: any) =>
      (r.realName || '').toLowerCase().includes(kw) ||
      (r.phone || '').toLowerCase().includes(kw),
    )
  }
  return list
})

async function load() {
  loading.value = true
  try {
    // 一次性加载全部跑腿员（管理端数据量通常可控），避免分页导致前端筛选不全
    const res: any = await runnerApi.list({ page: 1, size: 999 })
    // 兼容不同的响应格式：{ records } 或 { data: { records } }
    runners.value = res?.data?.records || res?.records || []
    total.value = res?.data?.total || res?.total || 0
  } finally {
    loading.value = false
  }
}

async function audit(id: number, status: number) {
  let remark = ''
  if (status === 2) {
    try {
      const r: any = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请')
      remark = r.value || ''
    } catch {
      return
    }
  } else {
    try {
      await ElMessageBox.confirm('确认通过此跑腿员申请？通过后该用户将获得跑腿员身份。', '通过确认', { type: 'warning' })
    } catch {
      return
    }
  }
  await runnerApi.audit(id, status, remark)
  ElMessage.success(status === 1 ? '审核已通过' : '已拒绝')
  load()
}

async function toggleRunnerStatus(row: any) {
  const newStatus = row.status ? 0 : 1
  const action = newStatus ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${action}跑腿员「${row.realName}」？`, `${action}确认`, { type: 'warning' })
  } catch {
    return
  }
  await runnerApi.updateStatus(row.id, newStatus)
  ElMessage.success(`已${action}`)
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;flex-wrap:wrap;gap:8px">
      <h3 style="margin:0">跑腿员管理</h3>
      <div style="display:flex;align-items:center;gap:8px">
        <el-input
          v-model="searchKey"
          placeholder="搜索姓名/手机号"
          style="width:200px"
          clearable
        />
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="() => {}">
      <el-tab-pane :name="-1">
        <template #label>全部 <el-badge :value="runners.length" class="tab-badge" /></template>
      </el-tab-pane>
      <el-tab-pane :name="0">
        <template #label>待审核 <el-badge :value="runners.filter((r:any)=>r.auditStatus===0).length" type="warning" class="tab-badge" /></template>
      </el-tab-pane>
      <el-tab-pane :name="1">
        <template #label>已通过 <el-badge :value="runners.filter((r:any)=>r.auditStatus===1).length" type="success" class="tab-badge" /></template>
      </el-tab-pane>
      <el-tab-pane :name="2">
        <template #label>已拒绝 <el-badge :value="runners.filter((r:any)=>r.auditStatus===2).length" type="danger" class="tab-badge" /></template>
      </el-tab-pane>
    </el-tabs>

    <el-table :data="filtered" border stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="userId" label="用户ID" width="70" />
      <el-table-column prop="realName" label="真实姓名" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="orderCount" label="接单数" width="80" align="center" />
      <el-table-column label="累计收益" width="110" align="right">
        <template #default="{ row }">
          <span style="color:var(--primary-color);font-weight:600">¥{{ (row.totalIncome || 0).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="评分" width="130" align="center">
        <template #default="{ row }">
          <el-rate :model-value="row.score || 0" :max="5" disabled show-score />
        </template>
      </el-table-column>
      <el-table-column label="审核状态" min-width="130">
        <template #default="{ row }">
          <el-tooltip
            v-if="row.auditStatus === 2 && row.auditRemark"
            :content="'拒绝原因：' + row.auditRemark"
            placement="top"
          >
            <el-tag type="danger" size="small">已拒绝</el-tag>
          </el-tooltip>
          <el-tag
            v-else
            :type="(row.auditStatus === 1 ? 'success' : 'warning') as any"
            size="small"
          >
            {{ row.auditStatus === 1 ? '已通过' : '待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="(row.status ? 'success' : 'danger') as any" size="small">
            {{ row.status ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="160">
        <template #default="{ row }">{{ row.createTime?.split(' ')[0] || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.auditStatus === 0">
            <el-button type="success" size="small" link @click="audit(row.id, 1)">通过</el-button>
            <el-button type="danger" size="small" link @click="audit(row.id, 2)">拒绝</el-button>
          </template>
          <template v-else>
            <el-button
              :type="(row.status ? 'danger' : 'success') as any"
              size="small"
              link
              @click="toggleRunnerStatus(row)"
            >
              {{ row.status ? '禁用' : '启用' }}
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !filtered.length" description="暂无跑腿员数据" />
  </div>
</template>
