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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { blogApi, type BlogPost } from '@/api/blog'
import { ElMessage, ElNotification } from 'element-plus'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const router = useRouter()
const route = useRoute()
const editorRef = ref()
const saving = ref(false)
const uploadingImages = ref(false)  // 是否正在上传图片

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

// ============== 图片链接检测和替换功能 ==============

/**
 * 匹配 Markdown 中的图片链接
 * 支持格式: ![alt](url) 和 <img src="url">
 */
const extractImageUrls = (text: string): string[] => {
  const urls: string[] = []
  
  // 匹配 Markdown 图片语法: ![alt](url)
  const markdownImageRegex = /!\[([^\]]*)\]\(([^)]+)\)/g
  let match
  while ((match = markdownImageRegex.exec(text)) !== null) {
    const url = match[2].trim()
    if (isExternalImageUrl(url)) {
      urls.push(url)
    }
  }
  
  // 匹配 HTML img 标签: <img src="url">
  const htmlImageRegex = /<img[^>]+src=["']([^"']+)["'][^>]*>/gi
  while ((match = htmlImageRegex.exec(text)) !== null) {
    const url = match[1].trim()
    if (isExternalImageUrl(url)) {
      urls.push(url)
    }
  }
  
  // 匹配纯图片 URL (常见图片扩展名)
  const pureUrlRegex = /https?:\/\/[^\s<>"']+\.(?:jpg|jpeg|png|gif|webp)(?:\?[^\s<>"']*)?/gi
  while ((match = pureUrlRegex.exec(text)) !== null) {
    const url = match[0]
    // 避免重复添加已经在 Markdown 或 HTML 标签中的 URL
    if (isExternalImageUrl(url) && !urls.includes(url)) {
      urls.push(url)
    }
  }
  
  // 去重
  return [...new Set(urls)]
}

/**
 * 判断是否为外部图片链接 (非本站图片)
 */
const isExternalImageUrl = (url: string): boolean => {
  if (!url) return false
  
  // 排除本站的图片链接（支持相对路径和绝对路径）
  if (url.startsWith('/api/images/')) return false
  if (url.includes('/api/images/')) return false  // 本站完整 URL
  if (url.startsWith('data:')) return false  // 排除 base64
  
  // 检查是否为 http/https URL
  return url.startsWith('http://') || url.startsWith('https://')
}

/**
 * 替换文本中的图片 URL
 */
const replaceImageUrls = (text: string, mappings: Record<string, string>): string => {
  let result = text
  
  for (const [oldUrl, newUrl] of Object.entries(mappings)) {
    if (oldUrl !== newUrl) {  // 只替换成功上传的图片
      // 全局替换，包括在 Markdown 和 HTML 标签中
      result = result.split(oldUrl).join(newUrl)
    }
  }
  
  return result
}

// ============== 图片文件验证工具函数 ==============

/**
 * 图片文件头魔数定义
 */
const IMAGE_MAGIC_NUMBERS = {
  jpeg: [0xFF, 0xD8, 0xFF],
  png: [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A],
  gif: [0x47, 0x49, 0x46, 0x38],  // GIF8
  webpRiff: [0x52, 0x49, 0x46, 0x46],  // RIFF
  webpWebp: [0x57, 0x45, 0x42, 0x50]   // WEBP
}

/**
 * 通过文件头魔数检测图片类型
 * @param buffer ArrayBuffer 图片数据
 * @returns 图片MIME类型或null
 */
const detectImageTypeFromBuffer = (buffer: ArrayBuffer): string | null => {
  const bytes = new Uint8Array(buffer)
  
  if (bytes.length < 12) return null
  
  // 检查 JPEG: FF D8 FF
  if (bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
    return 'image/jpeg'
  }
  
  // 检查 PNG: 89 50 4E 47 0D 0A 1A 0A
  if (bytes.length >= 8) {
    const pngMagic = IMAGE_MAGIC_NUMBERS.png
    let isPng = true
    for (let i = 0; i < pngMagic.length; i++) {
      if (bytes[i] !== pngMagic[i]) {
        isPng = false
        break
      }
    }
    if (isPng) return 'image/png'
  }
  
  // 检查 GIF: 47 49 46 38
  if (bytes.length >= 4) {
    const gifMagic = IMAGE_MAGIC_NUMBERS.gif
    let isGif = true
    for (let i = 0; i < gifMagic.length; i++) {
      if (bytes[i] !== gifMagic[i]) {
        isGif = false
        break
      }
    }
    if (isGif) return 'image/gif'
  }
  
  // 检查 WebP: RIFF....WEBP
  if (bytes.length >= 12) {
    const riffMagic = IMAGE_MAGIC_NUMBERS.webpRiff
    const webpMagic = IMAGE_MAGIC_NUMBERS.webpWebp
    let isRiff = true
    for (let i = 0; i < riffMagic.length; i++) {
      if (bytes[i] !== riffMagic[i]) {
        isRiff = false
        break
      }
    }
    if (isRiff) {
      let isWebp = true
      for (let i = 0; i < webpMagic.length; i++) {
        if (bytes[8 + i] !== webpMagic[i]) {
          isWebp = false
          break
        }
      }
      if (isWebp) return 'image/webp'
    }
  }
  
  return null
}

/**
 * 验证图片完整性
 * @param buffer ArrayBuffer 图片数据
 * @param imageType 图片MIME类型
 * @returns 是否完整
 */
const validateImageIntegrity = (buffer: ArrayBuffer, imageType: string): boolean => {
  const bytes = new Uint8Array(buffer)
  
  // 基本大小检查
  if (bytes.length < 100) return false
  
  switch (imageType) {
    case 'image/jpeg': {
      // JPEG 文件应该以 FF D9 结尾（EOI marker）
      // 有些 JPEG 可能在 EOI 后有额外数据，向前查找
      for (let i = bytes.length - 2; i >= Math.max(0, bytes.length - 100); i--) {
        if (bytes[i] === 0xFF && bytes[i + 1] === 0xD9) {
          return true
        }
      }
      return false
    }
    case 'image/png': {
      // PNG 文件应该以 IEND chunk 结尾
      if (bytes.length >= 8) {
        const iendSignature = [0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82]
        let isPngEnd = true
        for (let i = 0; i < 8; i++) {
          if (bytes[bytes.length - 8 + i] !== iendSignature[i]) {
            isPngEnd = false
            break
          }
        }
        return isPngEnd
      }
      return false
    }
    case 'image/gif': {
      // GIF 文件应该以 0x3B (;) 结尾
      return bytes[bytes.length - 1] === 0x3B
    }
    case 'image/webp': {
      // WebP 完整性较难验证，只检查魔数
      return bytes.length >= 12
    }
    default:
      return false
  }
}

/**
 * 前端下载图片并上传到服务器
 * 用于后端下载失败时的降级方案
 */
const downloadAndUploadImage = async (url: string): Promise<string | null> => {
  try {
    // 使用 fetch 在前端下载图片
    const response = await fetch(url, {
      mode: 'cors',
      credentials: 'include'  // 携带 cookie，支持需要登录的网站
    })
    
    if (!response.ok) {
      console.warn(`前端下载图片失败: ${url}, status: ${response.status}`)
      return null
    }
    
    const arrayBuffer = await response.arrayBuffer()
    
    // 通过文件头魔数检测真实的图片类型（而不是依赖 Content-Type）
    const detectedType = detectImageTypeFromBuffer(arrayBuffer)
    if (!detectedType) {
      console.warn(`下载的内容不是有效的图片文件: ${url}`)
      return null
    }
    
    // 验证图片完整性
    if (!validateImageIntegrity(arrayBuffer, detectedType)) {
      console.warn(`下载的图片文件不完整或已损坏: ${url}`)
      return null
    }
    
    // 检查大小限制
    if (arrayBuffer.byteLength > 10 * 1024 * 1024) {
      console.warn(`图片大小超过限制: ${url}, size: ${arrayBuffer.byteLength}`)
      return null
    }
    
    // 根据检测到的类型确定文件扩展名
    const extensionMap: Record<string, string> = {
      'image/jpeg': '.jpg',
      'image/png': '.png',
      'image/gif': '.gif',
      'image/webp': '.webp'
    }
    const extension = extensionMap[detectedType] || '.jpg'
    
    // 创建 Blob 和 File 对象
    const blob = new Blob([arrayBuffer], { type: detectedType })
    const filename = `image_${Date.now()}${extension}`
    const file = new File([blob], filename, { type: detectedType })
    
    // 上传到服务器
    const { data } = await blogApi.uploadImage(file)
    
    if (data.success) {
      return data.url
    }
    
    return null
  } catch (error) {
    console.warn(`前端下载并上传图片失败: ${url}`, error)
    return null
  }
}

/**
 * 批量在前端下载并上传图片
 */
const downloadAndUploadImages = async (urls: string[]): Promise<Record<string, string>> => {
  const mappings: Record<string, string> = {}
  
  // 并发下载，但限制同时进行的数量
  const concurrencyLimit = 3
  const chunks: string[][] = []
  for (let i = 0; i < urls.length; i += concurrencyLimit) {
    chunks.push(urls.slice(i, i + concurrencyLimit))
  }
  
  for (const chunk of chunks) {
    const results = await Promise.all(
      chunk.map(async (url) => {
        const newUrl = await downloadAndUploadImage(url)
        return { url, newUrl }
      })
    )
    
    for (const { url, newUrl } of results) {
      mappings[url] = newUrl || url  // 失败时保留原 URL
    }
  }
  
  return mappings
}

/**
 * 从剪切板获取图片文件
 */
const getImageFilesFromClipboard = (clipboardData: DataTransfer | null): File[] => {
  if (!clipboardData) return []
  
  const files: File[] = []
  const items = clipboardData.items
  
  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    // 检查是否为图片类型
    if (item.type.startsWith('image/')) {
      const file = item.getAsFile()
      if (file) {
        files.push(file)
      }
    }
  }
  
  return files
}

/**
 * 上传剪切板中的图片文件并插入到编辑器
 */
const uploadClipboardImages = async (files: File[]): Promise<void> => {
  if (files.length === 0) return
  
  uploadingImages.value = true
  
  ElNotification({
    title: '检测到剪切板图片',
    message: `正在上传 ${files.length} 张图片...`,
    type: 'info',
    duration: 3000
  })
  
  const textarea = editorRef.value?.textarea
  const cursorPosition = textarea?.selectionStart ?? form.value.content.length
  
  let successCount = 0
  let insertedMarkdown = ''
  
  // 并发上传，但限制同时进行的数量
  const concurrencyLimit = 3
  const chunks: File[][] = []
  for (let i = 0; i < files.length; i += concurrencyLimit) {
    chunks.push(files.slice(i, i + concurrencyLimit))
  }
  
  for (const chunk of chunks) {
    const results = await Promise.all(
      chunk.map(async (file) => {
        try {
          // 验证文件大小
          if (file.size > 10 * 1024 * 1024) {
            console.warn(`图片大小超过限制: ${file.name}, size: ${file.size}`)
            return null
          }
          
          const { data } = await blogApi.uploadImage(file)
          if (data.success) {
            return data.url
          }
          return null
        } catch (error) {
          console.warn(`上传图片失败: ${file.name}`, error)
          return null
        }
      })
    )
    
    for (const url of results) {
      if (url) {
        successCount++
        // 根据索引生成图片描述
        const imageDesc = files.length > 1 ? `图片${successCount}` : '图片'
        insertedMarkdown += `![${imageDesc}](${url})\n`
      }
    }
  }
  
  uploadingImages.value = false
  
  if (insertedMarkdown) {
    // 在光标位置插入图片 Markdown
    form.value.content = 
      form.value.content.substring(0, cursorPosition) +
      insertedMarkdown +
      form.value.content.substring(cursorPosition)
    
    // 移动光标到插入内容之后
    setTimeout(() => {
      if (textarea) {
        textarea.focus()
        const newPos = cursorPosition + insertedMarkdown.length
        textarea.setSelectionRange(newPos, newPos)
      }
    }, 0)
    
    ElNotification({
      title: '图片上传完成',
      message: `成功上传 ${successCount}/${files.length} 张图片`,
      type: successCount === files.length ? 'success' : 'warning',
      duration: 3000
    })
  } else {
    ElNotification({
      title: '图片上传失败',
      message: '所有图片上传均失败，请重试',
      type: 'error',
      duration: 3000
    })
  }
}

/**
 * 处理粘贴事件 - 支持剪切板图片和外部图片链接
 */
const handlePaste = async (event: ClipboardEvent) => {
  const clipboardData = event.clipboardData
  
  // 优先处理剪切板中的图片文件（如截图）
  const imageFiles = getImageFilesFromClipboard(clipboardData)
  
  if (imageFiles.length > 0) {
    // 阻止默认粘贴行为，避免浏览器插入图片的 base64 或其他默认处理
    event.preventDefault()
    
    // 上传剪切板图片
    await uploadClipboardImages(imageFiles)
    return
  }
  
  // 如果没有图片文件，处理文本中的外部图片链接
  const pastedText = clipboardData?.getData('text/plain')
  if (!pastedText) return
  
  // 提取外部图片链接
  const imageUrls = extractImageUrls(pastedText)
  
  if (imageUrls.length === 0) return
  
  // 显示检测到的图片数量提示
  ElNotification({
    title: '检测到外部图片',
    message: `正在上传 ${imageUrls.length} 张图片到服务器...`,
    type: 'info',
    duration: 3000
  })
  
  uploadingImages.value = true
  
  try {
    // 第一步：尝试后端批量上传
    const { data } = await blogApi.uploadImagesFromUrls(imageUrls)
    
    let finalMappings: Record<string, string> = {}
    let failedUrls: string[] = []
    
    if (data.success && data.mappings) {
      finalMappings = { ...data.mappings }
      failedUrls = data.failed || []
    }
    
    // 第二步：如果有后端下载失败的，尝试前端下载
    if (failedUrls.length > 0) {
      ElNotification({
        title: '部分图片后端下载失败',
        message: `正在尝试前端下载 ${failedUrls.length} 张图片...`,
        type: 'warning',
        duration: 3000
      })
      
      const frontendMappings = await downloadAndUploadImages(failedUrls)
      
      // 合并前端上传的结果
      for (const [url, newUrl] of Object.entries(frontendMappings)) {
        if (newUrl !== url) {
          finalMappings[url] = newUrl
        }
      }
    }
    
    // 等待粘贴操作完成
    await new Promise(resolve => setTimeout(resolve, 50))
    
    // 获取粘贴后的内容
    const currentContent = form.value.content
    
    // 替换图片链接
    const updatedContent = replaceImageUrls(currentContent, finalMappings)
    
    if (updatedContent !== currentContent) {
      form.value.content = updatedContent
      
      // 统计成功上传的数量
      const uploadedCount = Object.entries(finalMappings).filter(([k, v]) => k !== v).length
      
      ElNotification({
        title: '图片上传完成',
        message: `成功上传 ${uploadedCount}/${imageUrls.length} 张图片`,
        type: uploadedCount === imageUrls.length ? 'success' : 'warning',
        duration: 3000
      })
    }
  } catch (error: any) {
    console.error('图片上传失败:', error)
    ElNotification({
      title: '图片上传失败',
      message: error.response?.data?.message || '部分图片可能未能成功上传',
      type: 'warning',
      duration: 5000
    })
  } finally {
    uploadingImages.value = false
  }
}

/**
 * 设置粘贴事件监听
 */
const setupPasteListener = () => {
  const textarea = editorRef.value?.textarea
  if (textarea) {
    textarea.addEventListener('paste', handlePaste)
  }
}

/**
 * 移除粘贴事件监听
 */
const removePasteListener = () => {
  const textarea = editorRef.value?.textarea
  if (textarea) {
    textarea.removeEventListener('paste', handlePaste)
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
  // 延迟设置粘贴监听器，确保 DOM 已渲染
  setTimeout(() => {
    setupPasteListener()
  }, 100)
})

onUnmounted(() => {
  removePasteListener()
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
