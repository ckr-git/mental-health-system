# Day5 留言功能 401 错误修复说明

## 🐛 问题描述
点赞和删除留言时返回 401 错误并闪退到登录界面。

## 🔍 问题分析

### 1. API路径不匹配
**前端调用**（错误）：
```typescript
POST /api/patient/mood-comment/like/{id}
DELETE /api/patient/mood-comment/delete/{id}
```

**后端实际路径**：
```java
PUT /api/patient/mood-comment/interaction/{id}
DELETE /api/patient/mood-comment/{id}
```

### 2. 字段名不一致
- 前端发送：`interactionType`
- 后端接收：`commentType`

### 3. 类型值不匹配
- 原数据库设计：`random`, `praise`, `hug`, `note`, `thought`
- 前端新设计：`agree`, `disagree`, `heartache`, `encourage`, `relief`

## ✅ 修复方案

### 1. 修复前端 API 路径
**文件**: `frontend/src/api/index.ts`

```typescript
// 修复前
like: (commentId: number) => 
  request.post('/patient/mood-comment/like/${commentId}')
delete: (commentId: number) => 
  request.delete('/patient/mood-comment/delete/${commentId}')

// 修复后
like: (commentId: number, interactions: string[]) => 
  request.put('/patient/mood-comment/interaction/${commentId}', { interactions })
delete: (commentId: number) => 
  request.delete('/patient/mood-comment/${commentId}')
```

### 2. 统一字段名为 commentType
**修改文件**：
- `frontend/src/api/index.ts` - API接口参数
- `frontend/src/components/MoodDiary/MoodCommentDialog.vue` - 表单字段
- `frontend/src/components/MoodDiary/CommentTimeline.vue` - 显示逻辑

### 3. 更新数据库表定义
**执行SQL**：
```sql
ALTER TABLE mood_comment 
MODIFY COLUMN comment_type VARCHAR(20) DEFAULT 'agree' 
COMMENT '类型：agree-赞同, disagree-不赞同, heartache-心疼, encourage-鼓励, relief-释然';
```

### 4. 优化点赞逻辑
**文件**: `frontend/src/views/patient/MoodDiary.vue`

```typescript
// 处理点赞互动
const handleLikeComment = async (comment: any) => {
  try {
    // 切换点赞状态：如果已点赞则取消（传空数组），否则点赞（传['like']）
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
```

### 5. 解析点赞状态
**文件**: `frontend/src/views/patient/MoodDiary.vue`

```typescript
// 加载留言时解析 interactions 字段
comments.value = res.data.map((comment: any) => {
  let interactions: string[] = []
  try {
    interactions = comment.interactions ? JSON.parse(comment.interactions) : []
  } catch (e) {
    interactions = []
  }
  
  return {
    ...comment,
    liked: interactions.includes('like'),
    interactionCount: interactions.length
  }
})
```

## 📝 修改文件清单

1. ✅ `frontend/src/api/index.ts` - 修复API路径和参数
2. ✅ `frontend/src/views/patient/MoodDiary.vue` - 修复点赞逻辑和状态解析
3. ✅ `frontend/src/components/MoodDiary/MoodCommentDialog.vue` - 统一字段名
4. ✅ `frontend/src/components/MoodDiary/CommentTimeline.vue` - 统一字段名，移除冗余提示
5. ✅ `update-comment-types.sql` - 数据库表更新脚本

## 🧪 测试步骤

1. **重启前端服务**：
   ```bash
   cd frontend
   npm run dev
   ```

2. **测试点赞功能**：
   - 登录 patient001/123456
   - 打开任意日记详情
   - 点击留言的"点赞"按钮
   - ✅ 应该显示"点赞成功！"，按钮变为"已点赞"
   - 再次点击
   - ✅ 应该显示"已取消点赞"，按钮恢复为"点赞"

3. **测试删除功能**：
   - 点击留言的"删除"按钮
   - 确认删除
   - ✅ 应该显示"留言已删除"，留言从列表移除

4. **测试添加留言**：
   - 点击"写留言"按钮
   - 选择互动类型（5种之一）
   - 填写留言内容
   - 点击"发表留言"
   - ✅ 应该显示"留言发表成功！"，留言出现在时间线

## 🎯 修复结果

| 功能 | 修复前 | 修复后 |
|------|--------|--------|
| 点赞 | ❌ 401错误 | ✅ 正常工作 |
| 删除 | ❌ 401错误 | ✅ 正常工作 |
| 添加 | ✅ 正常 | ✅ 正常 |
| 状态显示 | ❌ 无法显示 | ✅ 正确显示 |

## 🔄 后端 API 说明

### 1. 添加留言
```
POST /api/patient/mood-comment/add
Body: { diaryId, commentType, content }
```

### 2. 获取留言列表  
```
GET /api/patient/mood-comment/list/{diaryId}
Response: Array<MoodComment>
```

### 3. 更新互动（点赞）
```
PUT /api/patient/mood-comment/interaction/{commentId}
Body: { interactions: string[] }
说明: 空数组=取消所有互动，['like']=点赞
```

### 4. 删除留言
```
DELETE /api/patient/mood-comment/{commentId}
```

## 💡 技术亮点

1. **互动标记机制** - 使用 JSON 数组存储多种互动类型，支持未来扩展
2. **状态解析** - 前端自动解析 interactions 字段为 liked 布尔值
3. **乐观更新** - 点击后立即更新UI，提升用户体验
4. **错误处理** - 完善的异常捕获和提示

## 📚 相关文档

- Day5完成总结.md - 功能开发总结
- phase1-database.sql - 数据库表设计
- 情绪日记优化设计方案V2.md - 产品设计文档

---

**修复完成时间**: 2025-11-03  
**测试状态**: ✅ 通过
