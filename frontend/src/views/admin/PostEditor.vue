<template>
  <div class="post-editor">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑文章' : '新建文章' }}</span>
          <div class="header-actions">
            <el-button @click="goBack">取消</el-button>
            <el-button type="primary" @click="savePost" :loading="saving">
              <el-icon><DocumentChecked /></el-icon>
              保存草稿
            </el-button>
            <el-button type="success" @click="saveAndPublish" :loading="saving">
              <el-icon><Upload /></el-icon>
              保存并发布
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入文章标题" />
        </el-form-item>

        <el-form-item label="作者">
          <el-input v-model="form.author" placeholder="请输入作者名称" />
        </el-form-item>

        <el-form-item label="摘要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入文章摘要"
          />
        </el-form-item>

        <el-form-item label="封面图">
          <div class="cover-upload">
            <el-input v-model="form.coverImage" placeholder="封面图片URL" />
            <el-upload
              :show-file-list="false"
              :before-upload="handleCoverUpload"
              :http-request="uploadCoverImage"
              accept="image/*"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>
                上传封面
              </el-button>
            </el-upload>
          </div>
          <div v-if="form.coverImage" class="cover-preview">
            <img :src="form.coverImage" alt="封面预览" />
          </div>
        </el-form-item>

        <el-form-item label="内容" required>
          <div class="editor-container">
            <div class="editor-toolbar">
              <el-button-group>
                <el-button size="small" @click="insertMarkdown('**', '**')">
                  <strong>B</strong>
                </el-button>
                <el-button size="small" @click="insertMarkdown('*', '*')">
                  <em>I</em>
                </el-button>
                <el-button size="small" @click="insertMarkdown('# ', '')">H1</el-button>
                <el-button size="small" @click="insertMarkdown('## ', '')">H2</el-button>
                <el-button size="small" @click="insertMarkdown('### ', '')">H3</el-button>
                <el-button size="small" @click="insertMarkdown('- ', '')">列表</el-button>
                <el-button size="small" @click="insertMarkdown('```\n', '\n```')">代码</el-button>
                <el-button size="small" @click="insertMarkdown('> ', '')">引用</el-button>
                <el-button size="small" @click="insertMarkdown('[', '](url)')">链接</el-button>
              </el-button-group>
              <el-upload
                :show-file-list="false"
                :before-upload="handleImageUpload"
                :http-request="uploadImage"
                accept="image/*"
              >
                <el-button size="small" type="primary">
                  <el-icon><Picture /></el-icon>
                  插入图片
                </el-button>
              </el-upload>
            </div>

            <div class="editor-content">
              <div class="editor-pane">
                <el-input
                  ref="editorRef"
                  v-model="form.content"
                  type="textarea"
                  placeholder="请输入Markdown内容..."
                  :rows="20"
                  @input="handleContentChange"
                />
              </div>
              <div class="preview-pane">
                <div class="preview-header">预览</div>
                <div class="markdown-body" v-html="renderedContent"></div>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { blogApi, type BlogPost } from '@/api/blog'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const router = useRouter()
const route = useRoute()
const editorRef = ref()
const saving = ref(false)

const form = ref<BlogPost>({
  title: '',
  content: '',
  summary: '',
  coverImage: '',
  author: '',
  status: 'draft'
})

const isEdit = computed(() => !!route.params.id)

// Configure marked
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

const renderedContent = computed(() => {
  if (!form.value.content) return '<p class="empty-hint">在左侧输入Markdown内容，这里将实时预览...</p>'
  return marked(form.value.content)
})

const loadPost = async () => {
  const id = Number(route.params.id)
  if (isNaN(id)) return

  try {
    const { data } = await blogApi.getPostById(id)
    form.value = data
  } catch (error) {
    ElMessage.error('加载文章失败')
  }
}

const insertMarkdown = (before: string, after: string) => {
  const textarea = editorRef.value?.textarea
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = form.value.content.substring(start, end)
  const newText = before + selectedText + after

  form.value.content =
    form.value.content.substring(0, start) +
    newText +
    form.value.content.substring(end)

  // Set cursor position
  setTimeout(() => {
    textarea.focus()
    const newCursorPos = start + before.length + selectedText.length
    textarea.setSelectionRange(newCursorPos, newCursorPos)
  }, 0)
}

const handleImageUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB！')
    return false
  }
  return true
}

const uploadImage = async ({ file }: any) => {
  try {
    const { data } = await blogApi.uploadImage(file)
    if (data.success) {
      const imageMarkdown = `![图片](${data.url})`
      const textarea = editorRef.value?.textarea
      if (textarea) {
        const start = textarea.selectionStart
        form.value.content =
          form.value.content.substring(0, start) +
          imageMarkdown +
          form.value.content.substring(start)
        
        setTimeout(() => {
          textarea.focus()
          const newPos = start + imageMarkdown.length
          textarea.setSelectionRange(newPos, newPos)
        }, 0)
      }
      ElMessage.success('图片上传成功')
    }
  } catch (error) {
    ElMessage.error('图片上传失败')
  }
}

const handleCoverUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB！')
    return false
  }
  return true
}

const uploadCoverImage = async ({ file }: any) => {
  try {
    const { data } = await blogApi.uploadImage(file)
    if (data.success) {
      form.value.coverImage = data.url
      ElMessage.success('封面上传成功')
    }
  } catch (error) {
    ElMessage.error('封面上传失败')
  }
}

const handleContentChange = () => {
  // Auto-generate summary if empty
  if (!form.value.summary && form.value.content) {
    const plainText = form.value.content.replace(/[#*`>\-\[\]()]/g, '').trim()
    form.value.summary = plainText.substring(0, 200)
  }
}

const savePost = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }

  saving.value = true
  try {
    form.value.status = 'draft'
    if (isEdit.value) {
      await blogApi.updatePost(Number(route.params.id), form.value)
      ElMessage.success('保存成功')
    } else {
      const { data } = await blogApi.createPost(form.value)
      ElMessage.success('保存成功')
      router.replace(`/admin/edit/${data.id}`)
    }
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const saveAndPublish = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }

  saving.value = true
  try {
    let postId: number
    
    if (isEdit.value) {
      await blogApi.updatePost(Number(route.params.id), form.value)
      postId = Number(route.params.id)
    } else {
      const { data } = await blogApi.createPost(form.value)
      postId = data.id!
    }
    
    await blogApi.publishPost(postId)
    ElMessage.success('发布成功')
    router.push('/admin')
  } catch (error) {
    ElMessage.error('发布失败')
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  if (isEdit.value) {
    loadPost()
  }
})
</script>

<style scoped>
.post-editor {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.cover-upload {
  display: flex;
  gap: 12px;
  width: 100%;
}

.cover-preview {
  margin-top: 12px;
  width: 200px;
  height: 120px;
  border-radius: 4px;
  overflow: hidden;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.editor-container {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
}

.editor-content {
  display: flex;
  height: 600px;
}

.editor-pane,
.preview-pane {
  flex: 1;
  overflow-y: auto;
}

.editor-pane {
  border-right: 1px solid #dcdfe6;
}

.editor-pane :deep(.el-textarea__inner) {
  border: none;
  border-radius: 0;
  box-shadow: none;
  height: 100%;
  resize: none;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.6;
}

.preview-pane {
  background-color: #fff;
}

.preview-header {
  padding: 12px 16px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
  font-weight: 600;
  color: #606266;
}

.markdown-body {
  padding: 20px;
  min-height: calc(100% - 45px);
}

.empty-hint {
  color: #909399;
  text-align: center;
  padding: 40px 20px;
}
</style>

<style>
.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 16px;
  line-height: 1.6;
  color: #303133;
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
  background-color: #f6f8fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.9em;
}

.markdown-body pre {
  background-color: #f6f8fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin-bottom: 16px;
}

.markdown-body pre code {
  background-color: transparent;
  padding: 0;
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
