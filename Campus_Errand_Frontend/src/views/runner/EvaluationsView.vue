<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { evaluationApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const list = ref<any[]>([])
const loading = ref(false)

const avgScore = computed(() => {
  if (list.value.length === 0) return 0
  const total = list.value.reduce((sum, item) => sum + (item.score || 0), 0)
  return (total / list.value.length).toFixed(1)
})

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await evaluationApi.getByToUser(userStore.userId!)
    list.value = res || []
  } finally {
    loading.value = false
  }
})
</script>
<template>
  <div class="page-card">
    <div style="display: flex; align-items: center; gap: 24px; margin-bottom: 20px">
      <h3>我收到的评价</h3>
      <div style="display: flex; align-items: center; gap: 8px">
        <span style="color: var(--text-secondary)">平均评分：</span>
        <el-rate :model-value="Number(avgScore)" :max="5" disabled />
        <span style="font-size: 16px; font-weight: 700; color: var(--primary-color)">{{
          avgScore
        }}</span>
        <span style="color: var(--text-secondary)">（{{ list.length }}条）</span>
      </div>
    </div>

    <div class="tab-card">
    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column label="评分" width="100">
        <template #default="{ row }">
          <el-rate :model-value="row.score" :max="5" disabled />
        </template>
      </el-table-column>
      <el-table-column prop="content" label="评价内容" show-overflow-tooltip />
      <el-table-column prop="createTime" label="时间" width="160" />
    </el-table>
    <el-empty v-if="!list.length && !loading" description="暂无评价" />
    </div>
  </div>
</template>

<style scoped>
.tab-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
</style>
