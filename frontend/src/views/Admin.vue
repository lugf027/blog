<template>
  <div class="admin">
    <el-container>
      <el-aside width="200px" class="sidebar">
        <div class="logo">
          <h2>📝 博客管理</h2>
        </div>
        <el-menu
          :default-active="activeMenu"
          router
          class="menu"
        >
          <el-menu-item index="/admin">
            <el-icon><Document /></el-icon>
            <span>文章列表</span>
          </el-menu-item>
          <el-menu-item index="/admin/new">
            <el-icon><EditPen /></el-icon>
            <span>新建文章</span>
          </el-menu-item>
          <el-menu-item index="/admin/access-logs">
            <el-icon><List /></el-icon>
            <span>访问日志</span>
          </el-menu-item>
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            <span>返回首页</span>
          </el-menu-item>
          <el-menu-item @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="header">
          <h3>后台管理系统</h3>
          <div class="user-info">
            <span>欢迎，{{ username }}</span>
          </div>
        </el-header>
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)
const username = ref('')

onMounted(() => {
  username.value = localStorage.getItem('username') || '管理员'
})

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  ElMessage.success('退出登录成功')
  router.push('/login')
}
</script>

<style scoped>
.admin {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background-color: #304156;
  color: white;
  height: 100vh;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  margin: 0;
  font-size: 20px;
  color: white;
}

.menu {
  border: none;
  background-color: transparent;
}

.header {
  background-color: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header h3 {
  margin: 0;
  color: #303133;
}

.user-info {
  color: #606266;
  font-size: 14px;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  height: calc(100vh - 60px);
  overflow-y: auto;
}
</style>