import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

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
    return api.post<BlogPost>('/posts', post)
  },

  // Update post
  updatePost(id: number, post: BlogPost) {
    return api.put<BlogPost>(`/posts/${id}`, post)
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
