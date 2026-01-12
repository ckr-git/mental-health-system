import { Howl } from 'howler'

/**
 * 音效管理服务
 * 负责环境音和交互音效的播放控制
 */
class SoundService {
  private ambientSound: Howl | null = null
  private currentAmbientType: string = ''
  private volume: number = 0.5
  private ambientEnabled: boolean = true
  private interactionEnabled: boolean = true

  // 环境音文件映射
  private ambientSounds: Record<string, string> = {
    rainy: '/sounds/rain.mp3',
    stormy: '/sounds/thunder.mp3',
    sunny: '/sounds/birds.mp3',
    cloudy: '/sounds/wind.mp3',
    night: '/sounds/night.mp3',
    snowy: '/sounds/wind.mp3'
  }

  // 交互音效文件映射
  private interactionSounds: Record<string, string> = {
    'card-flip': '/sounds/card-flip.mp3',
    'write': '/sounds/write.mp3',
    'switch': '/sounds/switch.mp3',
    'send-letter': '/sounds/send.mp3',
    'open-letter': '/sounds/open.mp3',
    'click': '/sounds/click.mp3',
    'unlock': '/sounds/unlock.mp3',
    'place': '/sounds/place.mp3',
    'interact': '/sounds/interact.mp3'
  }

  /**
   * 播放环境音
   */
  playAmbient(weatherType: string) {
    if (!this.ambientEnabled) return

    // 如果当前已经在播放相同类型的环境音，不重复播放
    if (this.currentAmbientType === weatherType && this.ambientSound?.playing()) {
      return
    }

    // 停止当前环境音
    this.stopAmbient()

    const soundPath = this.ambientSounds[weatherType]
    if (!soundPath) {
      console.warn(`No ambient sound found for weather type: ${weatherType}`)
      return
    }

    try {
      this.ambientSound = new Howl({
        src: [soundPath],
        loop: true,
        volume: this.volume,
        html5: true, // 使用HTML5 Audio以减少延迟
        onloaderror: (id, error) => {
          console.warn(`Failed to load ambient sound: ${soundPath}`, error)
        },
        onplayerror: (id, error) => {
          console.warn(`Failed to play ambient sound: ${soundPath}`, error)
        }
      })

      this.ambientSound.play()
      this.currentAmbientType = weatherType
    } catch (error) {
      console.error('Error playing ambient sound:', error)
    }
  }

  /**
   * 停止环境音
   */
  stopAmbient() {
    if (this.ambientSound) {
      this.ambientSound.fade(this.volume, 0, 500) // 0.5秒淡出
      setTimeout(() => {
        this.ambientSound?.stop()
        this.ambientSound?.unload()
        this.ambientSound = null
      }, 500)
    }
    this.currentAmbientType = ''
  }

  /**
   * 播放交互音效
   */
  playInteraction(soundType: string) {
    if (!this.interactionEnabled) return

    const soundPath = this.interactionSounds[soundType]
    if (!soundPath) {
      console.warn(`No interaction sound found for type: ${soundType}`)
      return
    }

    try {
      const sound = new Howl({
        src: [soundPath],
        volume: this.volume * 0.7, // 交互音效音量稍低
        onloaderror: (id, error) => {
          console.warn(`Failed to load interaction sound: ${soundPath}`, error)
        },
        onplayerror: (id, error) => {
          console.warn(`Failed to play interaction sound: ${soundPath}`, error)
        }
      })

      sound.play()
    } catch (error) {
      console.error('Error playing interaction sound:', error)
    }
  }

  /**
   * 设置音量
   * @param volume 音量值 0-1
   */
  setVolume(volume: number) {
    this.volume = Math.max(0, Math.min(1, volume)) // 限制在0-1之间
    if (this.ambientSound) {
      this.ambientSound.volume(this.volume)
    }
  }

  /**
   * 获取当前音量
   */
  getVolume(): number {
    return this.volume
  }

  /**
   * 设置环境音开关
   */
  setAmbientEnabled(enabled: boolean) {
    this.ambientEnabled = enabled
    if (!enabled) {
      this.stopAmbient()
    }
  }

  /**
   * 设置交互音效开关
   */
  setInteractionEnabled(enabled: boolean) {
    this.interactionEnabled = enabled
  }

  /**
   * 获取环境音开关状态
   */
  isAmbientEnabled(): boolean {
    return this.ambientEnabled
  }

  /**
   * 获取交互音效开关状态
   */
  isInteractionEnabled(): boolean {
    return this.interactionEnabled
  }

  /**
   * 从localStorage加载设置
   */
  loadSettings() {
    try {
      const settings = localStorage.getItem('soundSettings')
      if (settings) {
        const parsed = JSON.parse(settings)
        this.volume = parsed.volume ?? 0.5
        this.ambientEnabled = parsed.ambientEnabled ?? true
        this.interactionEnabled = parsed.interactionEnabled ?? true
      }
    } catch (error) {
      console.error('Failed to load sound settings:', error)
    }
  }

  /**
   * 保存设置到localStorage
   */
  saveSettings() {
    try {
      const settings = {
        volume: this.volume,
        ambientEnabled: this.ambientEnabled,
        interactionEnabled: this.interactionEnabled
      }
      localStorage.setItem('soundSettings', JSON.stringify(settings))
    } catch (error) {
      console.error('Failed to save sound settings:', error)
    }
  }
}

// 导出单例
export const soundService = new SoundService()

// 初始化时加载设置
soundService.loadSettings()

// 便捷导出函数
export const playAmbient = (weatherType: string) => soundService.playAmbient(weatherType)
export const stopAmbient = () => soundService.stopAmbient()
export const playSound = (soundType: string) => soundService.playInteraction(soundType)
export const setVolume = (volume: number) => {
  soundService.setVolume(volume)
  soundService.saveSettings()
}
export const setSoundEnabled = (ambient: boolean, interaction: boolean) => {
  soundService.setAmbientEnabled(ambient)
  soundService.setInteractionEnabled(interaction)
  soundService.saveSettings()
}
