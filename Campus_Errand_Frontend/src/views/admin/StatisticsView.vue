<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { userApi, runnerApi, taskApi, orderApi, settlementApi } from '@/api'

const stats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  totalRunners: 0,
  approvedRunners: 0,
  pendingAuditRunners: 0,
  totalTasks: 0,
  pendingTasks: 0,
  cancelledTasks: 0,
  totalOrders: 0,
  completedOrders: 0,
  inProgressOrders: 0,
  totalSettlement: 0,
  pendingSettlement: 0,
  settledAmount: 0,
})

const statCards = computed(() => [
  { label: '总用户数', value: stats.totalUsers, sub: `${stats.activeUsers} 启用`, icon: '👤', color: '#42a5f5' },
  { label: '跑腿员', value: stats.totalRunners, sub: `${stats.approvedRunners} 已通过 / ${stats.pendingAuditRunners} 待审`, icon: '🏃', color: '#66bb6a' },
  { label: '总任务', value: stats.totalTasks, sub: `${stats.pendingTasks} 待接单 / ${stats.cancelledTasks} 已取消`, icon: '📦', color: '#ffa726' },
  { label: '总订单', value: stats.totalOrders, sub: `${stats.completedOrders} 已完成 / ${stats.inProgressOrders} 进行中`, icon: '📋', color: '#ab47bc' },
  { label: '结算总额', value: `¥${stats.settledAmount.toFixed(2)}`, sub: `${stats.totalSettlement} 笔结算 / ${stats.pendingSettlement} 笔待结算`, icon: '💰', color: '#ef5350' },
])

const overviewData = computed(() => [
  { name: '待审核跑腿员', value: stats.pendingAuditRunners, type: 'warning' },
  { name: '待接单任务', value: stats.pendingTasks, type: 'warning' },
  { name: '进行中订单', value: stats.inProgressOrders, type: 'primary' },
  { name: '待结算订单', value: stats.pendingSettlement, type: 'warning' },
  { name: '已完成订单', value: stats.completedOrders, type: 'success' },
  { name: '已取消任务', value: stats.cancelledTasks, type: 'info' },
])

const ranking = ref<any[]>([])
const loading = ref(false)
const loadingRank = ref(false)

async function loadStats() {
  loading.value = true
  try {
    // 每个请求独立 try-catch，避免一个失败导致全部归零
    // 兜底：如果 total 为 0 但 records 有数据，使用 records.length（兼容分页插件未配置的场景）
    const safeTotal = (res: any) => {
      const total = res?.data?.total || res?.total || 0
      if (total > 0) return total
      const records = res?.data?.records || res?.records
      return Array.isArray(records) ? records.length : 0
    }
    const safeRecords = (res: any) => res?.data?.records || res?.records || []

    const [
      u, uActive,
      r, rApproved, rPending,
      t, tPending, tCancelled,
      o, oCompleted,
      s, sPending, sSettled,
    ] = await Promise.all([
      // 用户统计
      wrap(userApi.list({ page: 1, size: 1 })),
      wrap(userApi.list({ page: 1, size: 1, status: 1 })),
      // 跑腿员统计
      wrap(runnerApi.list({ page: 1, size: 1 })),
      wrap(runnerApi.list({ page: 1, size: 1, auditStatus: 1 })),
      wrap(runnerApi.list({ page: 1, size: 1, auditStatus: 0 })),
      // 任务统计
      wrap(taskApi.list({ page: 1, size: 1 })),
      wrap(taskApi.list({ page: 1, size: 1, status: 0 })),
      wrap(taskApi.list({ page: 1, size: 1, status: 2 })),
      // 订单统计
      wrap(orderApi.list({ page: 1, size: 1 })),
      wrap(orderApi.list({ page: 1, size: 1, status: 5 })),
      // 结算统计
      wrap(settlementApi.list({ page: 1, size: 1 })),
      wrap(settlementApi.list({ page: 1, size: 1, status: 0 })),
      wrap(settlementApi.list({ page: 1, size: 100, status: 1 })),
    ])

    stats.totalUsers = safeTotal(u)
    stats.activeUsers = safeTotal(uActive)
    stats.totalRunners = safeTotal(r)
    stats.approvedRunners = safeTotal(rApproved)
    stats.pendingAuditRunners = safeTotal(rPending)
    stats.totalTasks = safeTotal(t)
    stats.pendingTasks = safeTotal(tPending)
    stats.cancelledTasks = safeTotal(tCancelled)
    stats.totalOrders = safeTotal(o)
    stats.completedOrders = safeTotal(oCompleted)
    // 进行中 = 总数 - 已完成(5) - 已取消(任务取消则订单不会到已接单状态，这里用排除法)
    // 订单状态: 0=已接单, 1=已取件, 2=配送中, 3=已送达, 4=待评价, 5=已完成
    // 进行中 = 总数 - 已完成
    stats.inProgressOrders = Math.max(0, safeTotal(o) - safeTotal(oCompleted))
    stats.totalSettlement = safeTotal(s)
    stats.pendingSettlement = safeTotal(sPending)
    const settledList = safeRecords(sSettled)
    stats.settledAmount = settledList.reduce((sum: number, item: any) => sum + (Number(item.amount) || 0), 0)
  } catch {
    // 即使外层出错也不影响 loading 状态重置
  } finally {
    loading.value = false
  }
}

/** 包装单个 API 调用，失败时返回 null 而不影响其他调用 */
async function wrap<T>(promise: Promise<T>): Promise<T | null> {
  try {
    return await promise
  } catch {
    return null
  }
}

async function loadRanking() {
  loadingRank.value = true
  try {
    const res: any = await runnerApi.ranking()
    // 兼容不同响应格式: 直接数组 / { data: [...] } / { records: [...] }
    const list = res?.data?.records || res?.data || res?.records || res
    ranking.value = (Array.isArray(list) ? list : []).slice(0, 20)
  } catch {
    ranking.value = []
  } finally {
    loadingRank.value = false
  }
}

onMounted(() => {
  loadStats()
  loadRanking()
})
</script>

<template>
  <div v-loading="loading">
    <h3 style="margin:0 0 16px">数据统计</h3>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :xs="24" :sm="12" :md="8" :lg="4" v-for="(card, i) in statCards" :key="i" style="margin-bottom:12px">
        <el-card class="stat-card" shadow="hover">
          <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
            <span style="font-size:20px">{{ card.icon }}</span>
            <span class="stat-label">{{ card.label }}</span>
          </div>
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-sub">{{ card.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 跑腿员排行 -->
      <el-col :xs="24" :md="14" style="margin-bottom:12px">
        <div class="page-card">
          <h3 style="margin-top:0">跑腿员接单排行</h3>
          <el-table :data="ranking" border stripe v-loading="loadingRank" max-height="480">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="realName" label="姓名" min-width="100" />
            <el-table-column prop="phone" label="手机号" width="130">
              <template #default="{ row }">{{ row.phone || '-' }}</template>
            </el-table-column>
            <el-table-column prop="orderCount" label="接单数" width="90" sortable />
            <el-table-column label="评分" width="140">
              <template #default="{ row }">
                <el-rate :model-value="row.score || 0" :max="5" disabled show-score />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingRank && !ranking.length" description="暂无排行数据" />
        </div>
      </el-col>

      <!-- 数据概览 -->
      <el-col :xs="24" :md="10" style="margin-bottom:12px">
        <div class="page-card">
          <h3 style="margin-top:0">数据概览</h3>
          <el-table :data="overviewData" border style="width:100%">
            <el-table-column prop="name" label="指标" />
            <el-table-column label="数值">
              <template #default="{ row }">
                <el-tag :type="(row.type as any)" size="small">{{ row.value }}</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <h3 style="margin-top:24px">订单完成率</h3>
          <div style="margin-top:12px">
            <div style="margin-bottom:8px;display:flex;justify-content:space-between;font-size:13px">
              <span style="color:var(--text-secondary)">完成进度</span>
              <span style="font-weight:600">{{ stats.totalOrders ? ((stats.completedOrders / stats.totalOrders) * 100).toFixed(1) : 0 }}%</span>
            </div>
            <el-progress
              :percentage="stats.totalOrders ? Math.round((stats.completedOrders / stats.totalOrders) * 100) : 0"
              :stroke-width="18"
              :color="'#67c23a'"
              :striped="true"
              :striped-flow="true"
            >
              <span style="font-size:12px">{{ stats.completedOrders }} / {{ stats.totalOrders }}</span>
            </el-progress>
          </div>

          <h3 style="margin-top:24px">待处理事项</h3>
          <div style="margin-top:8px;display:flex;flex-direction:column;gap:8px">
            <div v-if="stats.pendingAuditRunners" style="display:flex;align-items:center;gap:8px">
              <el-tag type="warning" size="small">{{ stats.pendingAuditRunners }} 项</el-tag>
              <span style="font-size:13px">跑腿员申请待审核</span>
            </div>
            <div v-if="stats.pendingTasks" style="display:flex;align-items:center;gap:8px">
              <el-tag type="warning" size="small">{{ stats.pendingTasks }} 项</el-tag>
              <span style="font-size:13px">任务待接单</span>
            </div>
            <div v-if="stats.pendingSettlement" style="display:flex;align-items:center;gap:8px">
              <el-tag type="warning" size="small">{{ stats.pendingSettlement }} 项</el-tag>
              <span style="font-size:13px">订单待结算</span>
            </div>
            <span v-if="!stats.pendingAuditRunners && !stats.pendingTasks && !stats.pendingSettlement" style="color:var(--text-secondary);font-size:13px">
              暂无待处理事项
            </span>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-card {
  text-align: center;
  padding: 8px 0;
}
.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 4px;
}
.stat-sub {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
