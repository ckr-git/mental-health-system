export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  phone?: string
  email?: string
  role: 'PATIENT' | 'DOCTOR' | 'ADMIN'
  gender?: number
  age?: number
}

export interface LoginForm {
  username: string
  password: string
}

export interface RegisterForm {
  username: string
  password: string
  confirmPassword: string
  nickname: string
  phone?: string
  email?: string
  userType?: 'PATIENT' | 'DOCTOR'
  specialization?: string
}

export interface SymptomRecord {
  id?: number
  userId?: number
  symptomType: string
  description: string
  severity: number
  moodScore: number
  energyLevel: number
  sleepQuality: number
  stressLevel: number
  tags?: string
  createTime?: string
}

export interface MentalResource {
  id: number
  title: string
  type: string
  category: string
  content: string
  fileUrl?: string
  coverImage?: string
  author?: string
  duration?: number
  tags?: string
  viewCount: number
  likeCount: number
  downloadCount: number
  createTime?: string
}

export interface AssessmentReport {
  id: number
  userId: number
  doctorId?: number
  reportType: string
  title: string
  content: string
  diagnosis?: string
  suggestions?: string
  isAiGenerated: number
  status: number
  createTime?: string
}

export interface AIConversation {
  id: number
  userId: number
  question: string
  answer: string
  context?: string
  feedback?: number
  createTime: string
}

export interface ChatMessage {
  id?: number
  senderId: number
  senderName: string
  receiverId: number
  content: string
  type: 'TEXT' | 'IMAGE' | 'FILE'
  timestamp: string
  isRead: number
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
