import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { commentApi } from '@/api'

export function useComments(currentDiary: ReturnType<typeof ref<any>>) {
  const showCommentDialog = ref(false)
  const comments = ref<any[]>([])
  const commentsLoading = ref(false)
  const hasMoreComments = ref(false)
  const commentsPage = ref(1)
  const commentsPageSize = ref(20)

  const loadComments = async () => {
    if (!currentDiary.value?.id) return
    try {
      commentsLoading.value = true
      const res = await commentApi.getList(currentDiary.value.id, {
        pageNum: commentsPage.value, pageSize: commentsPageSize.value
      })
      if (res.code === 200 && res.data) {
        comments.value = res.data.map((comment: any) => {
          let interactions: string[] = []
          try { interactions = comment.interactions ? JSON.parse(comment.interactions) : [] } catch { interactions = [] }
          return { ...comment, liked: interactions.includes('like'), interactionCount: interactions.length }
        })
        hasMoreComments.value = false
      }
    } catch (error) {
      console.error('Failed to load comments:', error)
      ElMessage.error('加载留言失败')
    } finally {
      commentsLoading.value = false
    }
  }

  const handleLikeComment = async (comment: any) => {
    try {
      const interactions = comment.liked ? [] : ['like']
      const res = await commentApi.like(comment.id, interactions)
      if (res.code === 200) {
        comment.liked = !comment.liked
        ElMessage.success(comment.liked ? '点赞成功！' : '已取消点赞')
      }
    } catch (error) {
      ElMessage.error('操作失败')
    }
  }

  const handleReplyComment = () => {
    ElMessage.info('回复功能将在下一阶段开发')
  }

  const handleDeleteComment = async (comment: any) => {
    try {
      const res = await commentApi.delete(comment.id)
      if (res.code === 200) { ElMessage.success('留言已删除'); loadComments() }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }

  const loadMoreComments = () => { commentsPage.value++; loadComments() }
  const handleCommentSuccess = () => { loadComments() }

  return {
    showCommentDialog, comments, commentsLoading, hasMoreComments,
    loadComments, handleLikeComment, handleReplyComment, handleDeleteComment,
    loadMoreComments, handleCommentSuccess
  }
}
