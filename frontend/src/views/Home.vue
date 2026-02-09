<template>
  <div class="home">
    <!-- Hero Section - 满屏自我介绍 -->
    <section class="hero-section">
      <div class="hero-content">
        <div class="avatar-container">
          <div class="avatar">
            <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=developer" alt="Avatar" />
          </div>
        </div>
        <h1 class="hero-title">你好，我是 Android 开发工程师</h1>
        <p class="hero-subtitle">腾讯 · 安卓客户端开发</p>
        <div class="hero-description">
          <p>专注于 Android 应用开发，热爱技术分享</p>
          <p>擅长 Kotlin、Java、Android Framework</p>
          <p>致力于打造优质的移动端用户体验</p>
        </div>
        <div class="hero-tags">
          <el-tag type="primary" effect="dark">Android</el-tag>
          <el-tag type="success" effect="dark">Kotlin</el-tag>
          <el-tag type="warning" effect="dark">Java</el-tag>
          <el-tag type="danger" effect="dark">Flutter</el-tag>
          <el-tag type="info" effect="dark">性能优化</el-tag>
        </div>
        <div class="scroll-indicator" @click="scrollToPosts">
          <el-icon class="scroll-icon"><ArrowDown /></el-icon>
          <span>查看我的博客</span>
        </div>
      </div>
    </section>

    <!-- Blog Posts Section -->
    <section class="posts-section" ref="postsSection">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">📝 技术博客</h2>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索文章..."
              class="search-input"
              clearable
              @keyup.enter="handleSearch"
              @clear="loadPosts"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>

        <el-empty v-if="posts.length === 0 && !loading" description="暂无文章" />
        
        <div v-else class="posts-list" v-loading="loading">
          <div
            v-for="post in posts"
            :key="post.id"
            class="post-item"
            @click="goToPost(post.id!)"
          >
            <div class="post-date">
              <div class="date-day">{{ getDay(post.publishedAt) }}</div>
              <div class="date-month">{{ getMonth(post.publishedAt) }}</div>
            </div>
            <div class="post-main">
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-summary">{{ post.summary || '暂无摘要' }}</p>
              <div class="post-meta">
                <span class="meta-item">
                  <el-icon><User /></el-icon>
                  {{ post.author || '匿名' }}
                </span>
                <span class="meta-item">
                  <el-icon><View /></el-icon>
                  {{ post.viewCount || 0 }} 次阅读
                </span>
                <span class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(post.publishedAt) }}
                </span>
              </div>
            </div>
            <div v-if="post.coverImage" class="post-cover">
              <img :src="post.coverImage" :alt="post.title" />
            </div>
          </div>
        </div>

        <div v-if="totalPages > 1" class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="totalItems"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer-links">
        <a href="https://github.com/lugf027" target="_blank" rel="noopener noreferrer" class="footer-link">
          <svg class="footer-icon" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
          </svg>
          GitHub
        </a>
        <span class="footer-divider">•</span>
        <a href="mailto:lugf027@qq.com" class="footer-link">
          <svg class="footer-icon" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>
          </svg>
          lugf027@qq.com
        </a>
      </div>
      <div class="footer-icp">
        <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">豫ICP备2022011647号</a>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { blogApi, type BlogPost } from '@/api/blog'
import { ElMessage } from 'element-plus'

const router = useRouter()
const posts = ref<BlogPost[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)
const totalPages = ref(0)
const searchKeyword = ref('')
const postsSection = ref<HTMLElement>()

const loadPosts = async () => {
  loading.value = true
  try {
    const { data } = await blogApi.getPublishedPosts(currentPage.value - 1, pageSize.value)
    posts.value = data.posts
    totalItems.value = data.totalItems
    totalPages.value = data.totalPages
  } catch (error) {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadPosts()
  postsSection.value?.scrollIntoView({ behavior: 'smooth' })
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    loadPosts()
    return
  }
  
  loading.value = true
  try {
    const { data } = await blogApi.searchPosts(searchKeyword.value, 0, pageSize.value)
    posts.value = data.posts
    totalItems.value = data.totalItems
    totalPages.value = data.totalPages
    currentPage.value = 1
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const scrollToPosts = () => {
  postsSection.value?.scrollIntoView({ behavior: 'smooth' })
}

const goToPost = (id: number) => {
  router.push(`/post/${id}`)
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '未知'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

const getDay = (dateString?: string) => {
  if (!dateString) return '00'
  const date = new Date(dateString)
  return String(date.getDate()).padStart(2, '0')
}

const getMonth = (dateString?: string) => {
  if (!dateString) return 'Jan'
  const date = new Date(dateString)
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  return months[date.getMonth()]
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.home {
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* Hero Section */
.hero-section {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  position: relative;
}

.hero-content {
  text-align: center;
  padding: 40px;
  animation: fadeInUp 1s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.avatar-container {
  margin-bottom: 30px;
}

.avatar {
  width: 150px;
  height: 150px;
  margin: 0 auto;
  border-radius: 50%;
  overflow: hidden;
  border: 5px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 16px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.hero-subtitle {
  font-size: 24px;
  margin: 0 0 30px 0;
  opacity: 0.9;
}

.hero-description {
  font-size: 18px;
  line-height: 1.8;
  margin-bottom: 30px;
  opacity: 0.85;
}

.hero-description p {
  margin: 8px 0;
}

.hero-tags {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 50px;
}

.hero-tags .el-tag {
  font-size: 14px;
  padding: 8px 16px;
}

.scroll-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  opacity: 0.8;
  transition: opacity 0.3s;
  animation: bounce 2s infinite;
}

.scroll-indicator:hover {
  opacity: 1;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

.scroll-icon {
  font-size: 32px;
}

/* Posts Section */
.posts-section {
  padding: 80px 20px;
  min-height: 100vh;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  flex-wrap: wrap;
  gap: 20px;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.search-input {
  width: 300px;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.post-item {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  gap: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.post-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.post-date {
  flex-shrink: 0;
  width: 70px;
  text-align: center;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 8px;
}

.date-day {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}

.date-month {
  font-size: 14px;
  margin-top: 4px;
  opacity: 0.9;
}

.post-main {
  flex: 1;
  min-width: 0;
}

.post-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: #303133;
}

.post-summary {
  font-size: 15px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 16px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.post-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #909399;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.post-cover {
  flex-shrink: 0;
  width: 200px;
  height: 150px;
  border-radius: 8px;
  overflow: hidden;
}

.post-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.post-item:hover .post-cover img {
  transform: scale(1.1);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

/* Footer */
.footer {
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  text-align: center;
  padding: 30px 20px;
  color: #ecf0f1;
}

.footer-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.footer-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #ecf0f1;
  text-decoration: none;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.3s ease;
  font-size: 14px;
}

.footer-link:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  transform: translateY(-2px);
}

.footer-icon {
  width: 18px;
  height: 18px;
}

.footer-divider {
  color: rgba(255, 255, 255, 0.4);
  font-size: 12px;
}

.footer-icp {
  margin-top: 8px;
  font-size: 12px;
}

.footer-icp a {
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  transition: color 0.3s ease;
}

.footer-icp a:hover {
  color: #fff;
}

.footer a {
  text-decoration: none;
}

/* Responsive */
@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 18px;
  }

  .hero-description {
    font-size: 16px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-input {
    width: 100%;
  }

  .post-item {
    flex-direction: column;
  }

  .post-cover {
    width: 100%;
    height: 200px;
  }

  .post-date {
    width: 60px;
  }

  .date-day {
    font-size: 24px;
  }
}
</style>
