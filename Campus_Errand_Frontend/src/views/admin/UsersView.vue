<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '@/api'

const users = ref<any[]>([])
const loading = ref(false)
const searchKey = ref('')
const searchType = ref('all')
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const roleMap: Record<number, string> = { 0: '普通用户', 1: '跑腿员', 2: '管理员' }
const roleTagMap: Record<number, string> = { 0: 'info', 1: 'primary', 2: 'danger' }

const showDialog = ref(false)
const isCreate = ref(false)
const editForm = reactive({
  id: 0,
  studentNo: '',
  nickname: '',
  phone: '',
  dormitory: '',
  role: 0,
  balance: 0,
})

function resetEditForm() {
  editForm.id = 0
  editForm.studentNo = ''
  editForm.nickname = ''
  editForm.phone = ''
  editForm.dormitory = ''
  editForm.role = 0
  editForm.balance = 0
}

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize.value }
    if (searchKey.value) {
      if (searchType.value === 'studentNo') params.studentNo = searchKey.value
      else if (searchType.value === 'nickname') params.nickname = searchKey.value
      else if (searchType.value === 'phone') params.phone = searchKey.value
      else {
        params.studentNo = searchKey.value
        params.nickname = searchKey.value
      }
    }
    const res: any = await userApi.list(params)
    users.value = res?.data?.records || res?.records || []
    total.value = res?.data?.total || res?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  load()
}

function handleClear() {
  searchKey.value = ''
  searchType.value = 'all'
  page.value = 1
  load()
}

function handlePageChange(p: number) {
  page.value = p
  load()
}

function handleSizeChange(s: number) {
  pageSize.value = s
  page.value = 1
  load()
}

async function toggleStatus(row: any) {
  const action = row.status ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}用户「${row.nickname || row.studentNo}」？`, `${action}确认`, {
      type: 'warning',
    })
  } catch {
    return
  }
  const newStatus = row.status ? 0 : 1
  await userApi.updateStatus(row.id, newStatus)
  ElMessage.success(`已${action}`)
  load()
}

function openCreate() {
  resetEditForm()
  isCreate.value = true
  showDialog.value = true
}

function editUser(user: any) {
  editForm.id = user.id
  editForm.studentNo = user.studentNo || ''
  editForm.nickname = user.nickname || ''
  editForm.phone = user.phone || ''
  editForm.dormitory = user.dormitory || ''
  editForm.role = user.role ?? 0
  editForm.balance = user.balance || 0
  isCreate.value = false
  showDialog.value = true
}

async function saveUser() {
  if (!editForm.nickname) {
    ElMessage.warning('请填写昵称')
    return
  }
  if (isCreate.value) {
    await userApi.save({
      studentNo: editForm.studentNo,
      nickname: editForm.nickname,
      phone: editForm.phone,
      dormitory: editForm.dormitory,
      role: editForm.role,
    })
    ElMessage.success('创建成功')
  } else {
    const oldUser = users.value.find((u: any) => u.id === editForm.id)
    if (oldUser && oldUser.role !== editForm.role) {
      try {
        await ElMessageBox.confirm(
          `确认将用户「${oldUser.nickname}」的角色从「${roleMap[oldUser.role]}」改为「${roleMap[editForm.role]}」？`,
          '角色变更确认',
          { type: 'warning' },
        )
      } catch {
        return
      }
    }
    await userApi.update(editForm.id, {
      nickname: editForm.nickname,
      phone: editForm.phone,
      dormitory: editForm.dormitory,
      role: editForm.role,
    })
    ElMessage.success('保存成功')
  }
  showDialog.value = false
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 8px">
      <h3 style="margin:0">用户管理</h3>
      <div style="display:flex;align-items:center;gap:8px">
        <el-select v-model="searchType" style="width:100px">
          <el-option label="全部" value="all" />
          <el-option label="学号" value="studentNo" />
          <el-option label="昵称" value="nickname" />
          <el-option label="手机号" value="phone" />
        </el-select>
        <el-input
          v-model="searchKey"
          placeholder="输入搜索内容"
          style="width: 200px"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleClear">重置</el-button>
        <el-button type="success" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <el-table :data="users" border stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="studentNo" label="学号" width="130" />
      <el-table-column prop="nickname" label="昵称" min-width="100" show-overflow-tooltip />
      <el-table-column prop="realName" label="真实姓名" width="100">
        <template #default="{ row }">{{ row.realName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="dormitory" label="宿舍楼栋" min-width="120" show-overflow-tooltip />
      <el-table-column label="角色" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="roleTagMap[row.role] as any" size="small">
            {{ roleMap[row.role] || '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="余额" width="100" align="right">
        <template #default="{ row }">¥{{ (row.balance || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="(row.status ? 'success' : 'danger') as any" size="small">
            {{ row.status ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="160">
        <template #default="{ row }">{{ row.createTime?.split(' ')[0] || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="editUser(row)">编辑</el-button>
          <el-button
            :type="(row.status ? 'danger' : 'success') as any"
            size="small"
            link
            @click="toggleStatus(row)"
          >
            {{ row.status ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !users.length" description="暂无用户数据" />

    <div v-if="total > pageSize" style="margin-top: 16px; display: flex; justify-content: flex-end">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="showDialog" :title="isCreate ? '新增用户' : '编辑用户'" width="460px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="90px">
        <el-form-item v-if="isCreate" label="学号" required>
          <el-input v-model="editForm.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item v-else label="学号">
          <el-input :model-value="editForm.studentNo" disabled />
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="宿舍楼栋">
          <el-input v-model="editForm.dormitory" placeholder="请输入宿舍楼栋" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width:100%">
            <el-option :value="0" label="普通用户" />
            <el-option :value="1" label="跑腿员" />
            <el-option :value="2" label="管理员" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isCreate" label="余额">
          <el-input-number v-model="editForm.balance" :min="0" disabled style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser">{{ isCreate ? '创建' : '保存' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
