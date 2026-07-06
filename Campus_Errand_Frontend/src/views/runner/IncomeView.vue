<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { settlementApi, runnerApi, userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const settlements = ref<any[]>([])
const runnerId = ref<number>(0)
const runnerInfo = ref<any>({})
const balance = ref(0)
const loading = ref(false)

const stats = reactive({
  total: 0,
  settled: 0,
  pending: 0,
  refunded: 0,
  orderCount: 0,
  avgScore: 0,
})

async function load() {
  loading.value = true
  try {
    if (!runnerId.value) {
      const runner: any = await runnerApi.getByUserId(userStore.userId!)
      runnerId.value = runner?.id || 0
      runnerInfo.value = runner || {}
      stats.orderCount = runner?.orderCount || 0
      stats.avgScore = runner?.score || 0
    }
    if (!runnerId.value) { settlements.value = []; return }

    const [res, userRes] = await Promise.all([
      settlementApi.getByRunner(runnerId.value),
      userApi.getById(userStore.userId!),
    ])
    const resData = res as any
    settlements.value = resData?.data?.records || resData?.records || (Array.isArray(resData) ? resData : [])
    balance.value = (userRes as any)?.balance ?? 0

    stats.settled = settlements.value.filter((s: any) => s.status === 1).reduce((a: number, s: any) => a + (s.amount || 0), 0)
    stats.pending = settlements.value.filter((s: any) => s.status === 0).reduce((a: number, s: any) => a + (s.amount || 0), 0)
    stats.refunded = settlements.value.filter((s: any) => s.status === 2).reduce((a: number, s: any) => a + (s.amount || 0), 0)
    stats.total = stats.settled + stats.pending
  } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">累计收益</div>
          <div class="stat-value" style="color:var(--primary-color)">¥{{ stats.total }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">已结算</div>
          <div class="stat-value" style="color:#67c23a">¥{{ stats.settled }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">待结算</div>
          <div class="stat-value" style="color:#e6a23c">¥{{ stats.pending }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">已退款</div>
          <div class="stat-value" style="color:#f56c6c">¥{{ stats.refunded }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 跑腿员信息 -->
    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-label">当前余额</div>
          <div class="stat-value" style="color:var(--primary-color);font-size:22px">¥{{ balance }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-label">累计接单</div>
          <div class="stat-value" style="color:#67c23a">{{ stats.orderCount }} 单</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-label">平均评分</div>
          <div class="stat-value" style="color:#e6a23c;display:flex;align-items:center;gap:8px">
            <el-rate :model-value="Number(stats.avgScore)" :max="5" disabled />
            <span>{{ stats.avgScore }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="page-card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:4px">
        <h3>收益明细</h3>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
      <el-table :data="settlements" border stripe v-loading="loading">
        <el-table-column prop="id" label="结算ID" width="80" />
        <el-table-column prop="orderId" label="订单ID" width="80" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <!-- 状态 0=待结算, 1=已结算(收入), 2=已退款(支出) -->
            <span v-if="Number(row.status) === 2" style="font-size:14px;font-weight:700;color:#f56c6c">-¥{{ row.amount }}</span>
            <span v-else-if="Number(row.status) === 1" style="font-size:14px;font-weight:700;color:#67c23a">+¥{{ row.amount }}</span>
            <span v-else style="font-size:14px;font-weight:700;color:#e6a23c">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 1 ? 'success' : Number(row.status) === 2 ? 'danger' : 'warning'" size="small">
              {{ ['待结算', '已结算', '已退款'][Number(row.status)] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column prop="settleTime" label="结算时间" width="160">
          <template #default="{ row }">{{ row.settleTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
      </el-table>
      <el-empty v-if="!settlements.length && !loading" description="暂无结算记录" />
    </div>
  </div>
</template>
