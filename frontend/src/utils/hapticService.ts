/**
 * 触觉反馈工具（移动端）
 * 使用Vibration API提供触觉反馈
 */

// 检查是否支持触觉反馈
export const isVibrationSupported = (): boolean => {
  return 'vibrate' in navigator
}

// 触觉反馈模式
export enum HapticPattern {
  // 轻微震动 - 用于心情变化
  LIGHT = 'light',
  // 短促震动 - 用于完成打卡
  SHORT = 'short',
  // 中等震动 - 用于拉灯绳
  MEDIUM = 'medium',
  // 长震动 - 用于解锁成就
  LONG = 'long',
  // 双击震动 - 用于成功操作
  DOUBLE = 'double',
  // 三连震动 - 用于特殊效果
  TRIPLE = 'triple'
}

// 触觉反馈持续时间和模式映射
const hapticPatterns: Record<HapticPattern, number | number[]> = {
  [HapticPattern.LIGHT]: 10, // 10ms轻微震动
  [HapticPattern.SHORT]: 50, // 50ms短促震动
  [HapticPattern.MEDIUM]: 100, // 100ms中等震动
  [HapticPattern.LONG]: 200, // 200ms长震动
  [HapticPattern.DOUBLE]: [50, 50, 50], // 双击：震50ms,停50ms,震50ms
  [HapticPattern.TRIPLE]: [30, 30, 30, 30, 30] // 三连：震30ms,停30ms,震30ms,停30ms,震30ms
}

/**
 * 触发触觉反馈
 * @param pattern 反馈模式
 */
export const triggerHaptic = (pattern: HapticPattern = HapticPattern.SHORT): void => {
  if (!isVibrationSupported()) {
    console.log('Vibration API not supported')
    return
  }

  try {
    const vibrationPattern = hapticPatterns[pattern]
    navigator.vibrate(vibrationPattern)
  } catch (error) {
    console.error('Failed to trigger haptic feedback:', error)
  }
}

/**
 * 停止所有震动
 */
export const stopHaptic = (): void => {
  if (!isVibrationSupported()) return

  try {
    navigator.vibrate(0)
  } catch (error) {
    console.error('Failed to stop haptic feedback:', error)
  }
}

/**
 * 自定义震动模式
 * @param pattern 震动模式数组 [震动时间, 暂停时间, 震动时间, ...]
 */
export const triggerCustomHaptic = (pattern: number[]): void => {
  if (!isVibrationSupported()) return

  try {
    navigator.vibrate(pattern)
  } catch (error) {
    console.error('Failed to trigger custom haptic feedback:', error)
  }
}

// 预设场景的触觉反馈
export const hapticFeedback = {
  // 心情变化
  moodChange: () => triggerHaptic(HapticPattern.LIGHT),

  // 完成打卡
  checkIn: () => triggerHaptic(HapticPattern.SHORT),

  // 拉灯绳
  lightSwitch: () => triggerHaptic(HapticPattern.MEDIUM),

  // 解锁成就
  achievement: () => triggerHaptic(HapticPattern.LONG),

  // 成功操作
  success: () => triggerHaptic(HapticPattern.DOUBLE),

  // 交互装饰
  interact: () => triggerHaptic(HapticPattern.SHORT),

  // 放置装饰
  place: () => triggerHaptic(HapticPattern.MEDIUM),

  // 移除装饰
  remove: () => triggerHaptic(HapticPattern.SHORT),

  // 触发特效
  specialEffect: () => triggerHaptic(HapticPattern.TRIPLE),

  // 错误操作
  error: () => triggerCustomHaptic([100, 50, 100]),

  // 通知提醒
  notification: () => triggerCustomHaptic([50, 100, 50, 100, 50])
}
