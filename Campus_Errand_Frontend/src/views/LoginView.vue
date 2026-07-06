<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElForm, ElFormItem, ElInput, ElButton } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const isRegister = ref(false)
const loading = ref(false)

const form = reactive({
  studentNo: '',
  password: '',
  nickname: '',
  phone: '',
  confirmPassword: '',
})

async function handleSubmit() {
  if (!form.studentNo || !form.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (isRegister.value && form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      await userApi.register({
        studentNo: form.studentNo,
        password: form.password,
        nickname: form.nickname,
        phone: form.phone,
      })
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
      form.nickname = ''
      form.phone = ''
      form.confirmPassword = ''
    } else {
      const res: any = await userApi.login({
        studentNo: form.studentNo,
        password: form.password,
      })
      // 检查业务状态码，处理登录失败（用户不存在/密码错误等）
      if (res.code !== 200) {
        ElMessage.error(res.message || '用户名或密码错误')
        return
      }
      const token = res.token || res.data?.token
      const user = res.user || res.data?.user
      userStore.setLogin(token, user)
      ElMessage.success('登录成功')
      const homeMap: Record<number, string> = {
        0: '/user/task-publish',
        1: '/runner/grab-hall',
        2: '/admin/statistics',
      }
      router.push(homeMap[user?.role] || '/user/task-publish')
    }
  } catch {
    // 网络错误已由 request 拦截器统一提示，此处仅阻止异常传播
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="40" color="#42a5f5"><User /></el-icon>
        <h2>{{ isRegister ? '用户注册' : '校园跑腿' }}</h2>
        <p>{{ isRegister ? '创建账号开始使用' : '登录您的账号' }}</p>
      </div>

      <el-form @submit.prevent="handleSubmit" label-position="top">
        <el-form-item label="学号">
          <el-input v-model="form.studentNo" placeholder="请输入学号" size="large" />
        </el-form-item>
        <el-form-item v-if="isRegister" label="昵称">
          <el-input v-model="form.nickname" placeholder="给自己起个名字" size="large" />
        </el-form-item>
        <el-form-item v-if="isRegister" label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item v-if="isRegister" label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" size="large" show-password />
        </el-form-item>

        <el-button type="primary" size="large" :loading="loading" @click="handleSubmit" style="width:100%">
          {{ isRegister ? '注 册' : '登 录' }}
        </el-button>
      </el-form>

      <div class="login-footer">
        <span>{{ isRegister ? '已有账号？' : '没有账号？' }}</span>
        <el-button type="primary" link @click="isRegister = !isRegister; form.confirmPassword = ''">
          {{ isRegister ? '去登录' : '去注册' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--blue-50) 0%, var(--pink-50) 50%, var(--blue-100) 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(66, 165, 245, 0.12);
}
.login-header {
  text-align: center;
  margin-bottom: 30px;
}
.login-header h2 {
  margin: 12px 0 4px;
  color: var(--text-primary);
  font-size: 22px;
}
.login-header p {
  color: var(--text-secondary);
  font-size: 13px;
}
.login-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
