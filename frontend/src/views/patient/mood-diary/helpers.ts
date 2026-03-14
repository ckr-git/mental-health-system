export const moodEmojis: Record<number, string> = {
  1: '😭', 2: '😢', 3: '😔', 4: '😕', 5: '😐',
  6: '🙂', 7: '😊', 8: '😄', 9: '😁', 10: '🤩'
}

export const weatherIcons: Record<string, string> = {
  sunny: '☀️', cloudy: '⛅', rain: '🌧️', storm: '⛈️', clear: '🌤️'
}

export const weatherLabels: Record<string, string> = {
  sunny: '晴朗', cloudy: '多云', rain: '阴雨', storm: '风暴', clear: '放晴'
}

export const statusTypes: Record<string, string> = {
  pending: 'info', processing: 'warning', completed: 'success'
}

export const statusLabels: Record<string, string> = {
  pending: '待处理', processing: '处理中', completed: '已完成'
}

export const getWeatherIcon = (weather: string) => weatherIcons[weather] || '☀️'
export const getWeatherLabel = (weather: string) => weatherLabels[weather] || weather
export const getStatusType = (status: string) => statusTypes[status] || 'info'
export const getStatusLabel = (status: string) => statusLabels[status] || status

export const formatDateTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}
