export const statusMap: Record<number, string> = {
  0: '待确认', 1: '已确认', 2: '已完成', 3: '已取消', 4: '已过期'
}
export const statusTagMap: Record<number, string> = {
  0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'info'
}
export const typeMap: Record<string, string> = {
  CONSULTATION: '咨询', THERAPY: '治疗', REVIEW: '复诊'
}
export const typeTagMap: Record<string, string> = {
  CONSULTATION: 'primary', THERAPY: 'success', REVIEW: 'warning'
}
export const getStatusName = (s: number) => statusMap[s] || '未知'
export const getStatusTag = (s: number) => statusTagMap[s] || ''
export const getAppointmentTypeName = (t: string) => typeMap[t] || t
export const getAppointmentTypeTag = (t: string) => typeTagMap[t] || ''
export const formatDateTime = (dt: string) => dt ? dt.replace('T', ' ') : '-'
