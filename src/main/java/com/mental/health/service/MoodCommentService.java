package com.mental.health.service;

import com.alibaba.fastjson2.JSON;
import com.mental.health.entity.MoodComment;
import com.mental.health.entity.MoodDiary;
import com.mental.health.mapper.MoodCommentMapper;
import com.mental.health.mapper.MoodDiaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 心情留言服务
 */
@Service
public class MoodCommentService {

    @Autowired
    private MoodCommentMapper commentMapper;

    @Autowired
    private MoodDiaryMapper diaryMapper;

    /**
     * 添加留言
     */
    @Transactional
    public MoodComment addComment(MoodComment comment) {
        // 获取日记信息
        MoodDiary diary = diaryMapper.selectById(comment.getDiaryId());
        if (diary != null) {
            // 记录日记时的心情
            comment.setMoodAtDiary(diary.getMoodScore());
            
            // 计算心情差值
            if (comment.getMoodAtComment() != null) {
                comment.setMoodGap(comment.getMoodAtComment() - diary.getMoodScore());
            }
        }

        commentMapper.insert(comment);
        return comment;
    }

    /**
     * 获取日记的留言列表
     */
    public List<MoodComment> getCommentsByDiaryId(Long diaryId) {
        return commentMapper.getByDiaryId(diaryId);
    }

    /**
     * 更新留言的互动标记
     */
    @Transactional
    public boolean updateInteractions(Long commentId, Long userId, List<String> interactions) {
        MoodComment comment = commentMapper.selectById(commentId);
        if (comment != null && comment.getUserId().equals(userId)) {
            comment.setInteractions(JSON.toJSONString(interactions));
            
            // 同时更新日记的互动统计
            updateDiaryInteractionCount(comment.getDiaryId(), interactions);
            
            return commentMapper.updateById(comment) > 0;
        }
        return false;
    }

    /**
     * 更新日记的互动统计
     */
    private void updateDiaryInteractionCount(Long diaryId, List<String> interactions) {
        MoodDiary diary = diaryMapper.selectById(diaryId);
        if (diary != null && diary.getInteractionCount() != null) {
            try {
                Map<String, Integer> counts = JSON.parseObject(diary.getInteractionCount(), Map.class);
                
                // 增加各类互动的计数
                for (String interaction : interactions) {
                    counts.put(interaction, counts.getOrDefault(interaction, 0) + 1);
                }
                
                diary.setInteractionCount(JSON.toJSONString(counts));
                diaryMapper.updateById(diary);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 删除留言
     */
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        MoodComment comment = commentMapper.selectById(commentId);
        if (comment != null && comment.getUserId().equals(userId)) {
            return commentMapper.deleteById(commentId) > 0;
        }
        return false;
    }
}
