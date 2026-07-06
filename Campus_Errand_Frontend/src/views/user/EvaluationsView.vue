<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { evaluationApi, orderApi, runnerApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const sentList = ref<any[]>([])
const pendingOrders = ref<any[]>([])
const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('pending')

const evalDialogVisible = ref(false)
const evalForm = reactive({
  orderId: 0,
  targetUserId: 0,
  score: 5,
  content: '',
})

const pendingCount = computed(() => pendingOrders.value.length)

async function loadPending() {
  try {
    const orderRes: any = await orderApi.list({
      userId: userStore.userId,
      status: 4,
      page: 1,
      size: 50,
    })
    const completedOrders = orderRes?.data?.records || orderRes?.records || []
    const pending = []
    for (const order of completedOrders) {
      const evalRes: any = await evaluationApi.getByOrderId(order.id)
      const evalList =
        evalRes?.data?.records || evalRes?.records || (Array.isArray(evalRes) ? evalRes : [])
      if (!evalList || evalList.length === 0) {
        pending.push(order)
      }
    }
    pendingOrders.value = pending
  } catch {
    /* ignore */
  }
}

async function loadSent() {
  try {
    const res: any = await evaluationApi.getByFromUser(userStore.userId!)
    sentList.value = res?.data?.records || res?.records || (Array.isArray(res) ? res : [])
  } catch {
    /* ignore */
  }
}

async function openEvalDialog(order: any) {
  evalForm.orderId = order.id
  evalForm.score = 5
  evalForm.content = ''
  try {
    const runner: any = await runnerApi.getById(order.runnerId)
    evalForm.targetUserId = runner?.userId || 0
  } catch {
    evalForm.targetUserId = 0
  }
  evalDialogVisible.value = true
}

async function submitEvaluation() {
  if (!evalForm.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }
  submitting.value = true
  try {
    await evaluationApi.save({
      orderId: evalForm.orderId,
      fromUserId: userStore.userId!,
      toUserId: evalForm.targetUserId,
      score: evalForm.score,
      content: evalForm.content,
      type: 0,
    })
    ElMessage.success('评价成功')
    evalDialogVisible.value = false
    loadPending()
    loadSent()
  } finally {
    submitting.value = false
  }
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadPending(), loadSent()])
  } finally {
    loading.value = false
  }
}

function handleTabChange(tab: string) {
  loading.value = true
  if (tab === 'pending') loadPending().finally(() => (loading.value = false))
  else if (tab === 'sent') loadSent().finally(() => (loading.value = false))
}

onMounted(() => loadAll())
</script>

<template>
  <div class="page-card">
    <h3>评价管理</h3>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane name="pending">
        <template #label>
          <span>待评价 <el-badge :value="pendingCount" :hidden="!pendingCount" /></span>
        </template>
        <div class="tab-card">
          <el-table :data="pendingOrders" border stripe v-loading="loading">
            <el-table-column prop="id" label="订单ID" width="80" />
            <el-table-column prop="runnerId" label="跑腿员ID" width="100" />
            <el-table-column prop="acceptTime" label="接单时间" width="160" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="openEvalDialog(row)"
                  >去评价</el-button
                >
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!pendingOrders.length && !loading" description="暂无待评价订单" />
        </div>
      </el-tab-pane>

      <el-tab-pane name="sent">
        <template #label>
          <span>发出的评价</span>
        </template>
        <div class="tab-card">
          <el-table :data="sentList" border stripe v-loading="loading">
            <el-table-column label="评分" width="100">
              <template #default="{ row }">
                <el-rate :model-value="row.score" :max="5" disabled />
              </template>
            </el-table-column>
            <el-table-column prop="content" label="评价内容" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
          <el-empty v-if="!sentList.length && !loading" description="暂无发出的评价" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="evalDialogVisible"
      title="评价跑腿员服务"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="evalForm.score" :max="5" show-score />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="evalForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入评价内容，描述跑腿员的服务质量"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evalDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEvaluation"
          >提交评价</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
:deep(.el-tabs__active-bar) {
  background-color: #409eff !important;
}
:deep(.el-tabs__item.is-active) {
  color: #409eff !important;
}
:deep(.el-tabs__item.is-active span) {
  color: #409eff !important;
}
.tab-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
</style>
