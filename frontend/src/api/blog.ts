import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ============== 日志工具函数 ==============
const logStyles = {
  request: 'background: #4CAF50; color: white; padding: 2px 6px; border-radius: 3px;',
  response: 'background: #2196F3; color: white; padding: 2px 6px; border-radius: 3px;',
  error: 'background: #f44336; color: white; padding: 2px 6px; border-radius: 3px;',
  info: 'color: #666;'
}

const formatTime = () => new Date().toLocaleTimeString('zh-CN', { hour12: false })

const logRequest = (config: InternalAxiosRequestConfig) => {
  const { method, url, params, data } = config
  console.groupCollapsed(`%c📤 请求 %c${method?.toUpperCase()} ${url} %c${formatTime()}`, 
    logStyles.request, 'color: #4CAF50; font-weight: bold;', logStyles.info)
  console.log('🔗 完整URL:', `${config.baseURL}${url}`)
  if (params) console.log('📋 Query参数:', params)
  if (data) {
    if (data instanceof FormData) {
      console.log('📦 请求体: [FormData]')
    } else {
      console.log('📦 请求体:', data)
    }
  }
  console.log('🔑 Token:', config.headers.Authorization ? '已携带' : '未携带')
  console.groupEnd()
}

const logResponse = (response: any) => {
  const { config, status, data } = response
  const duration = response.config._requestTime 
    ? `${Date.now() - response.config._requestTime}ms` 
    : 'N/A'
  
  console.groupCollapsed(`%c📥 响应 %c${config.method?.toUpperCase()} ${config.url} %c${status} ${duration}`,
    logStyles.response, 'color: #2196F3; font-weight: bold;', logStyles.info)
  console.log('📊 状态码:', status)
  console.log('📦 响应数据:', data)
  console.groupEnd()
}

const logError = (error: AxiosError) => {
  const { config, response } = error
  const status = response?.status || 'Network Error'
  const url = config?.url || 'Unknown'
  const method = config?.method?.toUpperCase() || 'Unknown'
  
  console.groupCollapsed(`%c❌ 错误 %c${method} ${url} %c${status}`,
    logStyles.error, 'color: #f44336; font-weight: bold;', logStyles.info)
  console.log('📊 状态码:', status)
  console.log('❗ 错误信息:', error.message)
  if (response?.data) {
    console.log('📦 错误详情:', response.data)
  }
  if (config?.data) {
    try {
      const requestData = typeof config.data === 'string' ? JSON.parse(config.data) : config.data
      console.log('📤 请求体:', requestData)
    } catch {
      console.log('📤 请求体:', config.data)
    }
  }
  console.groupEnd()
}

// ============== 请求拦截器 ==============
api.interceptors.request.use(
  (config) => {
    // 记录请求开始时间
    (config as any)._requestTime = Date.now()
    
    // 添加 JWT Token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // 打印请求日志
    logRequest(config)
    
    return config
  },
  (error) => {
    console.error('%c❌ 请求配置错误', logStyles.error, error)
    return Promise.reject(error)
  }
)

// ============== 响应拦截器 ==============
api.interceptors.response.use(
  (response) => {
    // 打印成功响应日志
    logResponse(response)
    return response
  },
  (error: AxiosError) => {
    // 打印错误日志
    logError(error)
    
    if (error.response?.status === 401) {
      // Token 过期或无效，清除登录状态并跳转登录页
      console.warn('%c🔒 认证失败，跳转登录页', logStyles.error)
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export interface BlogPost {
  id?: number
  title: string
  content: string
  summary?: string
  coverImage?: string
  author?: string
  status?: string
  viewCount?: number
  createdAt?: string
  updatedAt?: string
  publishedAt?: string
}

export interface PageResponse<T> {
  posts: T[]
  currentPage: number
  totalItems: number
  totalPages: number
}

export const blogApi = {
  // Get all posts (admin)
  getAllPosts(page = 0, size = 10) {
    return api.get<PageResponse<BlogPost>>('/posts', { params: { page, size } })
  },

  // Get published posts (public)
  getPublishedPosts(page = 0, size = 10) {
    return api.get<PageResponse<BlogPost>>('/posts/published', { params: { page, size } })
  },

  // Get post by ID
  getPostById(id: number) {
    return api.get<BlogPost>(`/posts/${id}`)
  },

  // View post (increment view count)
  viewPost(id: number) {
    return api.get<BlogPost>(`/posts/${id}/view`)
  },

  // Create post
  createPost(post: BlogPost) {
    // 清理空字符串，转为 undefined（JSON 序列化时会被忽略）
    const cleanedPost = {
      ...post,
      summary: post.summary || undefined,
      coverImage: post.coverImage || undefined,
      author: post.author || undefined,
    }
    return api.post<BlogPost>('/posts', cleanedPost)
  },

  // Update post
  updatePost(id: number, post: BlogPost) {
    // 清理空字符串，转为 undefined
    const cleanedPost = {
      ...post,
      summary: post.summary || undefined,
      coverImage: post.coverImage || undefined,
      author: post.author || undefined,
    }
    return api.put<BlogPost>(`/posts/${id}`, cleanedPost)
  },

  // Publish post
  publishPost(id: number) {
    return api.put<BlogPost>(`/posts/${id}/publish`)
  },

  // Delete post
  deletePost(id: number) {
    return api.delete(`/posts/${id}`)
  },

  // Search posts
  searchPosts(keyword: string, page = 0, size = 10) {
    return api.get<PageResponse<BlogPost>>('/posts/search', { params: { keyword, page, size } })
  },

  // Upload image
  uploadImage(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post<{ success: boolean; url: string; id: number }>('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}
