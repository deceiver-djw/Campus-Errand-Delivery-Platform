<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { taskApi, expressPointApi, userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const points = ref<any[]>([])
const loading = ref(false)
const balance = ref(0)

const form = reactive({
  userId: userStore.userId,
  expressPoint: '',
  pickupCode: '',
  deliveryAddr: userStore.userInfo?.dormitory || '',
  fee: 5,
  isUrgent: 0,
  remark: '',
})

async function refreshBalance() {
  if (!userStore.userId) return
  const user: any = await userApi.getById(userStore.userId)
  balance.value = user?.balance ?? 0
}

function resetForm() {
  form.pickupCode = ''
  form.remark = ''
  form.isUrgent = 0
  form.fee = 5
}

onMounted(async () => {
  const [res] = await Promise.all([expressPointApi.list(), refreshBalance()])
  const resData = res as any
  points.value =
    resData?.data?.records || resData?.records || (Array.isArray(resData) ? resData : [])
})

async function submit() {
  if (!form.expressPoint) {
    ElMessage.warning('请选择快递点')
    return
  }
  if (!form.pickupCode) {
    ElMessage.warning('请输入取件码')
    return
  }
  if (!form.deliveryAddr) {
    ElMessage.warning('请输入送达地址')
    return
  }
  if (form.fee < 2) {
    ElMessage.warning('跑腿费最低2元')
    return
  }
  if (balance.value < form.fee) {
    ElMessage.warning('余额不足，请先充值')
    return
  }

  loading.value = true
  try {
    const ok: any = await taskApi.publish(form)
    if (ok === false) {
      ElMessage.error('发布失败，请检查余额是否充足')
      return
    }
    ElMessage.success('任务发布成功，已自动扣除 ¥' + form.fee)
    resetForm()
    refreshBalance()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-card">
    <div
      style="
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;
      "
    >
      <h3 style="margin: 0">发布任务</h3>
      <div style="display: flex; align-items: center; gap: 8px">
        <span style="color: var(--text-secondary); font-size: 14px">账户余额：</span>
        <span style="font-size: 18px; font-weight: 700; color: var(--primary-color)"
          >¥{{ balance }}</span
        >
        <el-button size="small" link type="primary" @click="refreshBalance">刷新</el-button>
      </div>
    </div>
    <el-form :model="form" label-width="90px" style="max-width: 560px">
      <el-form-item label="快递点" required>
        <el-select v-model="form.expressPoint" style="width: 100%" placeholder="请选择快递点">
          <el-option v-for="p in points" :key="p.id" :label="p.name" :value="p.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="取件码" required>
        <el-input v-model="form.pickupCode" placeholder="请输入取件码" />
      </el-form-item>
      <el-form-item label="送达地址" required>
        <el-input v-model="form.deliveryAddr" placeholder="请输入送达地址（如：3号楼302）" />
      </el-form-item>
      <el-form-item label="跑腿费">
        <el-input-number v-model="form.fee" :min="2" :max="50" :step="1" style="width: 120px" />
        <span style="margin-left: 8px; color: var(--text-secondary)">元（发布后自动扣除）</span>
      </el-form-item>
      <el-form-item label="加急">
        <el-switch v-model="form.isUrgent" :active-value="1" :inactive-value="0" />
        <span style="margin-left: 8px; color: var(--text-secondary); font-size: 12px"
          >加急任务会优先展示</span
        >
      </el-form-item>
      <el-form-item label="备注">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          placeholder="其他特殊要求（选填）"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">发布任务</el-button>
        <el-button style="margin-left: 8px" @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
