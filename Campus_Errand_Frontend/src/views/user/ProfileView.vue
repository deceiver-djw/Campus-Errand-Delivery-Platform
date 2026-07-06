<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Promotion } from '@element-plus/icons-vue'
import { userApi, runnerApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const user = ref<any>({})
const recharging = ref(false)
const uploadingAvatar = ref(false)
const avatarPreview = ref('')
const avatarFile = ref<File | null>(null)
const avatarInput = ref<HTMLInputElement>()

// 跑腿员申请相关
const runnerInfo = ref<any>(null)
const showRunnerDialog = ref(false)
const applyingRunner = ref(false)
const runnerForm = reactive({ realName: '', phone: '' })
const pwForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const avatarSrc = computed(() => {
  if (avatarPreview.value) return avatarPreview.value
  if (user.value.avatar) return 'http://192.168.106.1:8080' + user.value.avatar
  return ''
})

async function loadUser() {
  if (!userStore.userId) return
  const res: any = await userApi.getById(userStore.userId)
  user.value = res || {}
}

async function updateProfile() {
  if (!userStore.userId) return
  await userApi.update(userStore.userId, user.value)
  await userStore.refreshUserInfo()
  ElMessage.success('保存成功')
}

function triggerAvatarInput() {
  avatarInput.value?.click()
}

function handleAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG、GIF、WebP 格式的图片')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }
  avatarFile.value = file
  avatarPreview.value = URL.createObjectURL(file)
}

function cancelAvatarPreview() {
  avatarPreview.value = ''
  avatarFile.value = null
  if (avatarInput.value) avatarInput.value.value = ''
}

async function uploadAvatar() {
  if (!avatarFile.value || !userStore.userId) return
  uploadingAvatar.value = true
  try {
    await userApi.uploadAvatar(userStore.userId, avatarFile.value)
    ElMessage.success('头像上传成功')
    avatarPreview.value = ''
    avatarFile.value = null
    if (avatarInput.value) avatarInput.value.value = ''
    await userStore.refreshUserInfo()
    await loadUser()
  } catch {
    ElMessage.error('头像上传失败')
  } finally {
    uploadingAvatar.value = false
  }
}

async function recharge() {
  const amount = prompt('请输入充值金额（元）:')
  if (!amount) return
  recharging.value = true
  try {
    if (!userStore.userId) return
    const res: any = await userApi.recharge(userStore.userId, { amount: Number(amount) })
    if (res.code === 200) { ElMessage.success('充值成功'); loadUser() }
    else ElMessage.error(res.message)
  } finally { recharging.value = false }
}

async function changePassword() {
  if (!pwForm.oldPassword || !pwForm.newPassword) { ElMessage.warning('请填写完整'); return }
  if (pwForm.newPassword !== pwForm.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  if (!userStore.userId) return
  const res: any = await userApi.changePassword(userStore.userId, {
    oldPassword: pwForm.oldPassword,
    newPassword: pwForm.newPassword,
  })
  if (res.code === 200) ElMessage.success('密码修改成功')
  else ElMessage.error(res.message)
  pwForm.oldPassword = ''; pwForm.newPassword = ''; pwForm.confirmPassword = ''
}

async function loadRunnerInfo() {
  if (!userStore.userId || userStore.currentRole !== 0) return
  try {
    runnerInfo.value = await runnerApi.getByUserId(userStore.userId)
  } catch {
    runnerInfo.value = null
  }
}

function openRunnerDialog() {
  runnerForm.realName = user.value.realName || ''
  runnerForm.phone = user.value.phone || ''
  showRunnerDialog.value = true
}

async function submitRunnerApply() {
  if (!runnerForm.realName || !runnerForm.phone) {
    ElMessage.warning('请填写真实姓名和手机号')
    return
  }
  if (!userStore.userId) return
  applyingRunner.value = true
  try {
    const res: any = await runnerApi.apply({
      userId: userStore.userId,
      realName: runnerForm.realName,
      phone: runnerForm.phone,
    })
    if (res.code === 200) {
      ElMessage.success(res.message || '申请已提交，请等待管理员审核')
      showRunnerDialog.value = false
      await loadRunnerInfo()
    } else {
      ElMessage.warning(res.message || '申请提交失败')
    }
  } catch {
    // 网络错误已由 request 拦截器统一提示
  } finally {
    applyingRunner.value = false
  }
}

onMounted(() => {
  loadUser()
  loadRunnerInfo()
})
</script>

<template>
  <div class="page-card" style="max-width:600px;margin:0 auto">
    <h3>个人信息</h3>
    <el-form :model="user" label-width="90px" style="margin-top:16px">
      <el-form-item label="头像">
        <div style="display:flex;align-items:center;gap:16px">
          <el-tooltip content="点击更换头像" placement="top">
            <div class="avatar-wrapper" @click="triggerAvatarInput">
              <el-avatar :size="72" :src="avatarSrc" v-if="avatarSrc" />
              <el-avatar :size="72" v-else class="avatar-placeholder">
                <el-icon :size="36"><User /></el-icon>
              </el-avatar>
              <div class="avatar-overlay">
                <span>更换头像</span>
              </div>
            </div>
          </el-tooltip>
          <input
            ref="avatarInput"
            type="file"
            accept="image/*"
            style="display:none"
            @change="handleAvatarChange"
          />
          <div v-if="avatarFile" style="display:flex;align-items:center;gap:8px">
            <el-button type="primary" size="small" :loading="uploadingAvatar" @click="uploadAvatar">
              确认上传
            </el-button>
            <el-button size="small" @click="cancelAvatarPreview">取消</el-button>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="学号"><el-input v-model="user.studentNo" disabled /></el-form-item>
      <el-form-item label="昵称"><el-input v-model="user.nickname" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="user.phone" /></el-form-item>
      <el-form-item label="宿舍楼栋"><el-input v-model="user.dormitory" placeholder="默认送达地址" /></el-form-item>
      <el-form-item label="余额">
        <span style="font-size:20px;font-weight:700;color:var(--primary-color)">¥{{ user.balance || 0 }}</span>
        <el-button type="primary" size="small" style="margin-left:12px" :loading="recharging" @click="recharge">充值</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="updateProfile">保存修改</el-button>
      </el-form-item>
    </el-form>

    <el-divider />
    <h3>修改密码</h3>
    <el-form :model="pwForm" label-width="90px" style="max-width:360px;margin-top:16px">
      <el-form-item label="原密码"><el-input v-model="pwForm.oldPassword" type="password" show-password /></el-form-item>
      <el-form-item label="新密码"><el-input v-model="pwForm.newPassword" type="password" show-password /></el-form-item>
      <el-form-item label="确认密码"><el-input v-model="pwForm.confirmPassword" type="password" show-password /></el-form-item>
      <el-form-item><el-button type="warning" @click="changePassword">修改密码</el-button></el-form-item>
    </el-form>

    <!-- 跑腿员申请（仅普通用户可见） -->
    <template v-if="userStore.currentRole === 0">
      <el-divider />
      <h3>跑腿员认证</h3>
      <div style="margin-top:16px">
        <!-- 未申请 -->
        <el-card v-if="!runnerInfo" class="runner-apply-card" shadow="hover">
          <div class="runner-apply-content">
            <div>
              <el-icon :size="28" color="#42a5f5"><Promotion /></el-icon>
            </div>
            <div class="runner-apply-text">
              <h4 style="margin:0 0 4px;color:var(--text-primary)">申请成为跑腿员</h4>
              <p style="margin:0;color:var(--text-secondary);font-size:13px">
                成为跑腿员后，您可以接单代取快递赚取收益。需要管理员审核通过后方可接单。
              </p>
            </div>
            <el-button type="primary" @click="openRunnerDialog">立即申请</el-button>
          </div>
        </el-card>
        <!-- 待审核 -->
        <el-card v-else-if="runnerInfo.auditStatus === 0" class="runner-apply-card" shadow="hover">
          <div class="runner-apply-content">
            <div>
              <el-icon :size="28" color="#e6a23c"><Promotion /></el-icon>
            </div>
            <div class="runner-apply-text">
              <h4 style="margin:0 0 4px;color:var(--text-primary)">申请审核中</h4>
              <p style="margin:0;color:var(--text-secondary);font-size:13px">
                您的跑腿员申请正在审核中，请耐心等待管理员审核。审核通过后即可开始接单。
              </p>
            </div>
            <el-tag type="warning" size="large">审核中</el-tag>
          </div>
        </el-card>
        <!-- 已通过 -->
        <el-card v-else-if="runnerInfo.auditStatus === 1" class="runner-apply-card" shadow="hover">
          <div class="runner-apply-content">
            <div>
              <el-icon :size="28" color="#67c23a"><Promotion /></el-icon>
            </div>
            <div class="runner-apply-text">
              <h4 style="margin:0 0 4px;color:var(--text-primary)">跑腿员认证已通过</h4>
              <p style="margin:0;color:var(--text-secondary);font-size:13px">
                恭喜！您已成为认证跑腿员，可以前往抢单大厅接单赚取收益。
              </p>
            </div>
            <el-tag type="success" size="large">已通过</el-tag>
          </div>
        </el-card>
        <!-- 已拒绝 -->
        <el-card v-else-if="runnerInfo.auditStatus === 2" class="runner-apply-card" shadow="hover">
          <div class="runner-apply-content">
            <div>
              <el-icon :size="28" color="#f56c6c"><Promotion /></el-icon>
            </div>
            <div class="runner-apply-text">
              <h4 style="margin:0 0 4px;color:var(--text-primary)">申请已被拒绝</h4>
              <p style="margin:0;color:var(--text-secondary);font-size:13px">
                拒绝原因：{{ runnerInfo.auditRemark || '未提供原因' }}。您可以修改信息后重新申请。
              </p>
            </div>
            <el-button type="primary" @click="openRunnerDialog">重新申请</el-button>
          </div>
        </el-card>
      </div>
    </template>

    <!-- 申请跑腿员弹窗 -->
    <el-dialog v-model="showRunnerDialog" title="申请成为跑腿员" width="420px" :close-on-click-modal="false">
      <p style="color:var(--text-secondary);font-size:13px;margin:0 0 16px">
        成为跑腿员后，您可以接单代取快递赚取收益。请填写真实信息，提交后将由管理员审核。
      </p>
      <el-form :model="runnerForm" label-width="80px">
        <el-form-item label="真实姓名" required>
          <el-input v-model="runnerForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="runnerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRunnerDialog = false">取消</el-button>
        <el-button type="primary" :loading="applyingRunner" @click="submitRunnerApply">
          提交申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  line-height: 0;
  border: 2px dashed var(--border-color);
  padding: 2px;
  transition: border-color 0.3s;
}
.avatar-wrapper:hover {
  border-color: var(--primary-color);
}
.avatar-overlay {
  position: absolute;
  inset: 2px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: #fff;
  font-size: 13px;
}
.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}
.avatar-placeholder {
  background: linear-gradient(135deg, var(--blue-400), var(--pink-300));
}

.runner-apply-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  transition: border-color 0.3s;
}
.runner-apply-card:hover {
  border-color: var(--primary-color);
}
.runner-apply-content {
  display: flex;
  align-items: center;
  gap: 16px;
}
.runner-apply-text {
  flex: 1;
}
</style>
