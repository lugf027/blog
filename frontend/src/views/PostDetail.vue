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
        <p>由 <a href="https://with.woa.com/" style="color: #8A2BE2;" target="_blank">With</a> 通过自然语言生成</p>
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
  background-color: #f5f7fa;
  text-align: center;
  padding: 20px;
  color: #909399;
  border-top: 1px solid #e4e7ed;
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
