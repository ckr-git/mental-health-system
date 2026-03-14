import { defineStore } from 'pinia'
import { ref } from 'vue'
import { assessmentApi } from '@/api/modules/patient/assessment'
import type {
  ScaleListItem,
  ScaleDetailDTO,
  AnswerOption,
  AnswerItem,
  SessionResultDTO,
  AssessmentHistoryItem,
  PageResult
} from '@/types'

export const useAssessmentStore = defineStore('assessment', () => {
  const scales = ref<ScaleListItem[]>([])
  const scalesLoading = ref(false)

  const currentScale = ref<ScaleDetailDTO | null>(null)
  const activeSessionId = ref<number | null>(null)
  const answers = ref<Map<number, AnswerItem>>(new Map())
  const currentQuestionIndex = ref(0)
  const autosaving = ref(false)

  const result = ref<SessionResultDTO | null>(null)

  const history = ref<PageResult<AssessmentHistoryItem> | null>(null)
  const historyLoading = ref(false)

  const loadScales = async () => {
    scalesLoading.value = true
    try {
      const res = await assessmentApi.getScales()
      if (res.code === 200) {
        scales.value = res.data
      }
    } finally {
      scalesLoading.value = false
    }
  }

  const loadScaleDetail = async (scaleCode: string) => {
    const res = await assessmentApi.getScaleDetail(scaleCode)
    if (res.code === 200) {
      currentScale.value = res.data
    }
  }

  const startSession = async (scaleCode: string) => {
    await loadScaleDetail(scaleCode)
    const res = await assessmentApi.startSession(scaleCode)
    if (res.code === 200) {
      activeSessionId.value = res.data
      answers.value = new Map()
      currentQuestionIndex.value = 0
      result.value = null
    }
    return res
  }

  const saveAnswer = async (itemId: number, answerValue: number, answerLabel: string) => {
    const answerItem: AnswerItem = { itemId, answerValue, answerLabel }
    answers.value.set(itemId, answerItem)

    if (activeSessionId.value) {
      autosaving.value = true
      try {
        await assessmentApi.saveAnswers(activeSessionId.value, [answerItem])
      } finally {
        autosaving.value = false
      }
    }
  }

  const submitSession = async () => {
    if (!activeSessionId.value) return null
    const res = await assessmentApi.submitSession(activeSessionId.value)
    if (res.code === 200) {
      result.value = res.data
    }
    return res
  }

  const loadHistory = async (pageNum = 1, pageSize = 10) => {
    historyLoading.value = true
    try {
      const res = await assessmentApi.getHistory(pageNum, pageSize)
      if (res.code === 200) {
        history.value = res.data
      }
    } finally {
      historyLoading.value = false
    }
  }

  const parseAnswerOptions = (optionsJson: string): AnswerOption[] => {
    try {
      return JSON.parse(optionsJson)
    } catch {
      return []
    }
  }

  const resetSession = () => {
    currentScale.value = null
    activeSessionId.value = null
    answers.value = new Map()
    currentQuestionIndex.value = 0
    result.value = null
  }

  return {
    scales,
    scalesLoading,
    currentScale,
    activeSessionId,
    answers,
    currentQuestionIndex,
    autosaving,
    result,
    history,
    historyLoading,
    loadScales,
    loadScaleDetail,
    startSession,
    saveAnswer,
    submitSession,
    loadHistory,
    parseAnswerOptions,
    resetSession,
  }
})
