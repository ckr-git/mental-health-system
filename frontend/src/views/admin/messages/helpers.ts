export const notificationTypeMap: Record<string, string> = {
  SYSTEM: '系统通知', PERSONAL: '个人通知', BROADCAST: '广播通知'
}
export const notificationTypeTagMap: Record<string, string> = {
  SYSTEM: 'primary', PERSONAL: 'success', BROADCAST: 'warning'
}
export const priorityTagMap: Record<string, string> = {
  LOW: 'info', NORMAL: '', HIGH: 'warning', URGENT: 'danger'
}
export const feedbackTypeMap: Record<string, string> = {
  BUG: 'Bug报告', FEATURE: '功能建议', COMPLAINT: '投诉', OTHER: '其他'
}
export const feedbackTypeTagMap: Record<string, string> = {
  BUG: 'danger', FEATURE: 'success', COMPLAINT: 'warning', OTHER: 'info'
}
export const feedbackStatusMap: Record<number, string> = {
  0: '待处理', 1: '处理中', 2: '已完成', 3: '已关闭'
}
export const feedbackStatusTagMap: Record<number, string> = {
  0: 'warning', 1: 'primary', 2: 'success', 3: 'info'
}
export const alertLevelTagMap: Record<string, string> = {
  INFO: 'info', WARNING: 'warning', ERROR: 'danger', CRITICAL: 'danger'
}
export const alertStatusMap: Record<number, string> = {
  0: '未处理', 1: '处理中', 2: '已处理', 3: '已忽略'
}
export const alertStatusTagMap: Record<number, string> = {
  0: 'danger', 1: 'primary', 2: 'success', 3: 'info'
}
export const announcementTypeMap: Record<string, string> = {
  SYSTEM: '系统公告', MAINTENANCE: '维护公告', FEATURE: '新功能', ACTIVITY: '活动公告'
}
export const announcementStatusMap: Record<number, string> = {
  0: '草稿', 1: '已发布', 2: '已下线'
}
export const announcementStatusTagMap: Record<number, string> = {
  0: 'info', 1: 'success', 2: 'warning'
}
