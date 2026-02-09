<template>
  <div class="post-detail">
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <el-button @click="goBack" circle>
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <h1 class="logo">📝 我的博客</h1>
        </div>
      </el-header>

      <el-main class="main-content">
        <div class="container">
          <el-card v-if="post" class="post-card" shadow="never">
            <div class="post-header">
              <h1 class="post-title">{{ post.title }}</h1>
              <div class="post-meta">
                <span class="meta-item">
                  <el-icon><User /></el-icon>
                  {{ post.author || '匿名' }}
                </span>
                <span class="meta-item">
                  <el-icon><View /></el-icon>
                  {{ post.viewCount || 0 }}
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

            <div class="post-content markdown-body" v-html="renderedContent"></div>
          </el-card>

          <el-empty v-else-if="!loading" description="文章不存在" />
        </div>
      </el-main>

      <el-footer class="footer">
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
      </el-footer>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { blogApi, type BlogPost } from '@/api/blog'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

const router = useRouter()
const route = useRoute()
const post = ref<BlogPost | null>(null)
const loading = ref(false)

// Configure marked with highlight.js
marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  }
}))

marked.use({
  breaks: true,
  gfm: true
})

const renderedContent = computed(() => {
  if (!post.value?.content) return ''
  return marked(post.value.content)
})

const loadPost = async () => {
  const id = Number(route.params.id)
  if (isNaN(id)) {
    ElMessage.error('无效的文章ID')
    return
  }

  loading.value = true
  try {
    const { data } = await blogApi.viewPost(id)
    post.value = data
  } catch (error) {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '未知'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadPost()
})
</script>

<style scoped>
.post-detail {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0;
  height: 80px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 0 20px;
}

.logo {
  font-size: 28px;
  font-weight: bold;
  margin: 0;
}

.main-content {
  padding: 40px 20px;
}

.container {
  max-width: 900px;
  margin: 0 auto;
}

.post-card {
  padding: 40px;
}

.post-header {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.post-title {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin: 0 0 16px 0;
  line-height: 1.4;
}

.post-meta {
  display: flex;
  gap: 24px;
  font-size: 14px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.post-cover {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 30px;
}

.post-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-content {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
}

.footer {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  text-align: center;
  padding: 24px 20px;
  color: #6c757d;
  border-top: 1px solid #dee2e6;
}

.footer-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.footer-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #495057;
  text-decoration: none;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.3s ease;
  font-size: 14px;
}

.footer-link:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #212529;
  transform: translateY(-2px);
}

.footer-icon {
  width: 18px;
  height: 18px;
}

.footer-divider {
  color: #adb5bd;
  font-size: 12px;
}

.footer-icp {
  margin-top: 6px;
  font-size: 12px;
}

.footer-icp a {
  color: #909399;
  text-decoration: none;
  transition: color 0.3s ease;
}

.footer-icp a:hover {
  color: #409eff;
}

.footer a {
  text-decoration: none;
}
</style>

<style>
.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-body h1 {
  font-size: 2em;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 0.3em;
}

.markdown-body h2 {
  font-size: 1.5em;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 0.3em;
}

.markdown-body h3 {
  font-size: 1.25em;
}

.markdown-body p {
  margin-bottom: 16px;
}

.markdown-body code {
  background-color: rgba(175, 184, 193, 0.2);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.85em;
}

.markdown-body pre {
  background-color: #0d1117;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin-bottom: 16px;
  line-height: 1.45;
}

.markdown-body pre code {
  background-color: transparent;
  padding: 0;
  font-size: 0.85em;
  line-height: 1.45;
  color: #c9d1d9;
}

.markdown-body blockquote {
  border-left: 4px solid #dfe2e5;
  padding-left: 16px;
  color: #6a737d;
  margin: 16px 0;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 2em;
  margin-bottom: 16px;
}

.markdown-body li {
  margin-bottom: 8px;
}

.markdown-body img {
  max-width: 100%;
  border-radius: 4px;
  margin: 16px 0;
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 16px;
}

.markdown-body table th,
.markdown-body table td {
  border: 1px solid #dfe2e5;
  padding: 8px 12px;
}

.markdown-body table th {
  background-color: #f6f8fa;
  font-weight: 600;
}

.markdown-body a {
  color: #0366d6;
  text-decoration: none;
}

.markdown-body a:hover {
  text-decoration: underline;
}
</style>
