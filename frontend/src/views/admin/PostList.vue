<template>
  <div class="post-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文章管理</span>
          <el-button type="primary" @click="goToNew">
            <el-icon><Plus /></el-icon>
            新建文章
          </el-button>
        </div>
      </template>

      <el-table :data="posts" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'published'" type="success">已发布</el-tag>
            <el-tag v-else type="info">草稿</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewPost(row.id)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button size="small" type="primary" @click="editPost(row.id)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              size="small"
              type="success"
              @click="publishPost(row.id)"
            >
              <el-icon><Check /></el-icon>
              发布
            </el-button>
            <el-button size="small" type="danger" @click="deletePost(row.id)">
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
import { useRouter } from 'vue-router'
import { blogApi, type BlogPost } from '@/api/blog'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const posts = ref<BlogPost[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)

const loadPosts = async () => {
  loading.value = true
  try {
    const { data } = await blogApi.getAllPosts(currentPage.value - 1, pageSize.value)
    posts.value = data.posts
    totalItems.value = data.totalItems
  } catch (error) {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadPosts()
}

const goToNew = () => {
  router.push('/admin/new')
}

const viewPost = (id: number) => {
  window.open(`/post/${id}`, '_blank')
}

const editPost = (id: number) => {
  router.push(`/admin/edit/${id}`)
}

const publishPost = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要发布这篇文章吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await blogApi.publishPost(id)
    ElMessage.success('发布成功')
    loadPosts()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('发布失败')
    }
  }
}

const deletePost = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇文章吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    
    await blogApi.deletePost(id)
    ElMessage.success('删除成功')
    loadPosts()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.post-list {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
