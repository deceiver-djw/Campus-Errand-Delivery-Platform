import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/',
      component: MainLayout,
      redirect: '/user/task-publish',
      children: [
        // ======== 账户 ========
        {
          path: 'user/profile',
          name: 'Profile',
          component: () => import('@/views/user/ProfileView.vue'),
          meta: { title: '个人中心', roles: [0, 1, 2] },
        },
        // ======== 用户端 ========
        {
          path: 'user/task-publish',
          name: 'TaskPublish',
          component: () => import('@/views/user/TaskPublishView.vue'),
          meta: { title: '发布任务', roles: [0, 1, 2] },
        },
        {
          path: 'user/my-tasks',
          name: 'MyTasks',
          component: () => import('@/views/user/MyTasksView.vue'),
          meta: { title: '我的任务', roles: [0, 1, 2] },
        },
        {
          path: 'user/messages',
          name: 'UserMessages',
          component: () => import('@/views/user/MessagesView.vue'),
          meta: { title: '消息通知', roles: [0, 1, 2] },
        },
        {
          path: 'user/evaluations',
          name: 'UserEvaluations',
          component: () => import('@/views/user/EvaluationsView.vue'),
          meta: { title: '评价管理', roles: [0, 1, 2] },
        },

        // ======== 跑腿员端 ========
        {
          path: 'runner/grab-hall',
          name: 'GrabHall',
          component: () => import('@/views/runner/GrabHallView.vue'),
          meta: { title: '抢单大厅', roles: [1, 2] },
        },
        {
          path: 'runner/my-orders',
          name: 'MyOrders',
          component: () => import('@/views/runner/MyOrdersView.vue'),
          meta: { title: '我的接单', roles: [1, 2] },
        },
        {
          path: 'runner/income',
          name: 'RunnerIncome',
          component: () => import('@/views/runner/IncomeView.vue'),
          meta: { title: '收益结算', roles: [1, 2] },
        },
        {
          path: 'runner/evaluations',
          name: 'RunnerEvaluations',
          component: () => import('@/views/runner/EvaluationsView.vue'),
          meta: { title: '评价管理', roles: [1, 2] },
        },

        // ======== 管理端 ========
        {
          path: 'admin/users',
          name: 'AdminUsers',
          component: () => import('@/views/admin/UsersView.vue'),
          meta: { title: '用户管理', roles: [2] },
        },
        {
          path: 'admin/runners',
          name: 'AdminRunners',
          component: () => import('@/views/admin/RunnersView.vue'),
          meta: { title: '跑腿员管理', roles: [2] },
        },
        {
          path: 'admin/tasks',
          name: 'AdminTasks',
          component: () => import('@/views/admin/TasksView.vue'),
          meta: { title: '任务管理', roles: [2] },
        },
        {
          path: 'admin/orders',
          name: 'AdminOrders',
          component: () => import('@/views/admin/OrdersView.vue'),
          meta: { title: '订单管理', roles: [2] },
        },
        {
          path: 'admin/settlements',
          name: 'AdminSettlements',
          component: () => import('@/views/admin/SettlementsView.vue'),
          meta: { title: '结算管理', roles: [2] },
        },
        {
          path: 'admin/statistics',
          name: 'AdminStatistics',
          component: () => import('@/views/admin/StatisticsView.vue'),
          meta: { title: '数据统计', roles: [2] },
        },
      ],
    },
  ],
})

// 路由守卫：未登录跳转登录页，角色权限校验
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    return '/login'
  }
  if (token && to.path !== '/login') {
    const raw = localStorage.getItem('userInfo')
    const userInfo = raw && raw !== 'undefined' ? JSON.parse(raw) : null
    // userInfo 损坏时清空状态并跳转登录页
    if (!userInfo) {
      localStorage.clear()
      return '/login'
    }
    const userRole: number = userInfo?.role ?? 0
    const allowedRoles: number[] | undefined = to.meta?.roles as number[] | undefined
    if (allowedRoles && !allowedRoles.includes(userRole)) {
      // 根据角色跳转到对应首页
      const homeMap: Record<number, string> = {
        0: '/user/task-publish',
        1: '/runner/grab-hall',
        2: '/admin/statistics',
      }
      return homeMap[userRole] || '/user/task-publish'
    }
  }
})

export default router
