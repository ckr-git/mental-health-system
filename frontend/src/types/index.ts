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
  specialization?: string
  status?: number
}

// Alias for backward compatibility
export type User = UserInfo

export interface ProfileAggregate {
  userId: number
  username: string
  nickname: string
  avatar?: string
  phone?: string
  email?: string
  gender?: number
  age?: number
  role: string
  specialization?: string
  profileId?: number
  realName?: string
  birthDate?: string
  maritalStatus?: string
  occupation?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  emergencyContactRelation?: string
  introduction?: string
  medicalHistory?: string
  allergyHistory?: string
  familyHistory?: string
  consentFlags?: string
  intakeTags?: string
  profileVersion?: number
  createTime?: string
  updateTime?: string
}

export interface UpdateProfileCommand {
  nickname?: string
  email?: string
  phone?: string
  gender?: number
  age?: number
  realName?: string
  birthDate?: string
  maritalStatus?: string
  occupation?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  emergencyContactRelation?: string
  introduction?: string
  medicalHistory?: string
  allergyHistory?: string
  familyHistory?: string
  consentFlags?: string
  intakeTags?: string
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
  overallScore?: number
  anxietyScore?: number
  depressionScore?: number
  stressScore?: number
  sleepScore?: number
  emotionScore?: number
  socialScore?: number
  summary?: string
  assessmentSessionId?: number
  scaleCode?: string
  severityLevel?: string
  rawScoreJson?: string
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

// ======== Assessment Engine Types ========

export interface ScaleListItem {
  id: number
  scaleCode: string
  scaleName: string
  scaleType: string
  introText: string
  estimatedMinutes: number
  itemCount: number
}

export interface AnswerOption {
  label: string
  value: number
}

export interface ScaleItem {
  id: number
  scaleId: number
  itemNo: number
  itemCode: string
  questionText: string
  answerOptions: string // JSON string of AnswerOption[]
  reverseScored: number
  dimensionCode: string
  requiredFlag: number
}

export interface ScaleDetailDTO {
  id: number
  scaleCode: string
  scaleName: string
  introText: string
  items: ScaleItem[]
}

export interface AnswerItem {
  itemId: number
  answerValue: number
  answerLabel: string
}

export interface SessionResultDTO {
  sessionId: number
  scaleCode: string
  scaleName: string
  totalScore: number
  severityLevel: string
  interpretation: string
  scoreBreakdown: string
  submittedAt: string
}

export interface AssessmentHistoryItem {
  sessionId: number
  scaleCode: string
  scaleName: string
  totalScore: number
  severityLevel: string
  submittedAt: string
}
