<template>
  <div class="access-logs">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>访问日志</span>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索URL或IP"
              style="width: 300px; margin-right: 10px"
              clearable
              @clear="loadLogs"
            >
              <template #append>
                <el-button @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
            <el-button type="danger" @click="clearAllLogs">
              <el-icon><Delete /></el-icon>
              清空日志
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="logs" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
        <el-table-column prop="requestUrl" label="请求URL" min-width="200" />
        <el-table-column prop="requestMethod" label="方法" width="80" />
        <el-table-column prop="responseStatus" label="状态码" width="100">
          <template #default="{ row }">
            <el-tag
              :type="getStatusType(row.responseStatus)"
              size="small"
            >
              {{ row.responseStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseTime" label="响应时间(ms)" width="120" />
        <el-table-column prop="accessTime" label="访问时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.accessTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="userAgent" label="User Agent" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="deleteLog(row.id)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalItems"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

interface AccessLog {
  id: number
  ipAddress: string
  requestUrl: string
  requestMethod: string
  userAgent: string
  referer: string
  accessTime: string
  responseStatus: number
  responseTime: number
}

const logs = ref<AccessLog[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const totalItems = ref(0)
const searchKeyword = ref('')

const loadLogs = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/admin/access-logs', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })
    logs.value = data.logs
    totalItems.value = data.totalItems
  } catch (error) {
    ElMessage.error('加载访问日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    loadLogs()
    return
  }

  loading.value = true
  try {
    // Check if it's an IP address
    const isIp = /^(\d{1,3}\.){3}\d{1,3}$/.test(searchKeyword.value)
    const endpoint = isIp ? '/api/admin/access-logs/by-ip' : '/api/admin/access-logs/by-url'
    const paramKey = isIp ? 'ipAddress' : 'url'

    const { data } = await axios.get(endpoint, {
      params: {
        [paramKey]: searchKeyword.value,
        page: 0,
        size: pageSize.value
      }
    })
    logs.value = data.logs
    totalItems.value = data.totalItems
    currentPage.value = 1
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  if (searchKeyword.value.trim()) {
    handleSearch()
  } else {
    loadLogs()
  }
}

const deleteLog = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条日志吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await axios.delete(`/api/admin/access-logs/${id}`)
    ElMessage.success('删除成功')
    loadLogs()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const clearAllLogs = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有访问日志吗？此操作不可恢复！',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    await axios.delete('/api/admin/access-logs/clear')
    ElMessage.success('清空成功')
    loadLogs()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

const getStatusType = (status: number) => {
  if (status >= 200 && status < 300) return 'success'
  if (status >= 300 && status < 400) return 'info'
  if (status >= 400 && status < 500) return 'warning'
  return 'danger'
}

const formatDate = (dateString: string) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.access-logs {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
