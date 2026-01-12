package com.mental.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mental.health.entity.ConsultationSession;
import com.mental.health.entity.Message;
import com.mental.health.entity.User;
import com.mental.health.mapper.ConsultationSessionMapper;
import com.mental.health.mapper.MessageMapper;
import com.mental.health.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天消息服务
 */
@Service
public class ChatMessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ConsultationSessionMapper sessionMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取用户的聊天列表
     */
    public List<Map<String, Object>> getChatList(Long userId, String role) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 查询用户参与的所有会话
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        if ("PATIENT".equals(role)) {
            wrapper.eq(ConsultationSession::getPatientId, userId);
        } else if ("DOCTOR".equals(role)) {
            wrapper.eq(ConsultationSession::getDoctorId, userId);
        }
        wrapper.orderByDesc(ConsultationSession::getUpdateTime);

        List<ConsultationSession> sessions = sessionMapper.selectList(wrapper);

        for (ConsultationSession session : sessions) {
            Map<String, Object> chatInfo = new HashMap<>();
            chatInfo.put("sessionId", session.getId());
            chatInfo.put("sessionStatus", session.getSessionStatus());

            // 获取对方用户信息
            Long targetUserId = "PATIENT".equals(role) ?
                session.getDoctorId() : session.getPatientId();
            User targetUser = userMapper.selectById(targetUserId);

            if (targetUser != null) {
                chatInfo.put("userId", targetUser.getId());
                chatInfo.put("name", targetUser.getNickname() != null ?
                    targetUser.getNickname() : targetUser.getUsername());
                chatInfo.put("avatar", targetUser.getAvatar());
                chatInfo.put("online", false);
            }

            // 获取最后一条消息
            Message lastMsg = getLastMessage(session.getId());
            if (lastMsg != null) {
                chatInfo.put("lastMessage", lastMsg.getContent());
                chatInfo.put("lastTime", lastMsg.getCreateTime());
            }

            // 获取未读消息数
            int unreadCount = getUnreadCount(session.getId(), userId);
            chatInfo.put("unreadCount", unreadCount);

            result.add(chatInfo);
        }

        return result;
    }

    /**
     * 获取会话的最后一条消息
     */
    private Message getLastMessage(Long sessionId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConsultationId, sessionId)
               .orderByDesc(Message::getCreateTime)
               .last("LIMIT 1");
        return messageMapper.selectOne(wrapper);
    }

    /**
     * 获取未读消息数
     */
    private int getUnreadCount(Long sessionId, Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConsultationId, sessionId)
               .eq(Message::getReceiverId, userId)
               .eq(Message::getIsRead, 0);
        return Math.toIntExact(messageMapper.selectCount(wrapper));
    }

    /**
     * 获取消息历史
     */
    public Map<String, Object> getMessages(Long sessionId, Long userId, int pageNum, int pageSize) {
        Page<Message> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConsultationId, sessionId)
               .orderByDesc(Message::getCreateTime);

        Page<Message> resultPage = messageMapper.selectPage(page, wrapper);

        // 转换为前端需要的格式
        List<Map<String, Object>> messages = resultPage.getRecords().stream()
            .map(msg -> convertMessageToMap(msg, userId))
            .collect(Collectors.toList());

        // 反转顺序，让最新消息在最后
        java.util.Collections.reverse(messages);

        Map<String, Object> result = new HashMap<>();
        result.put("records", messages);
        result.put("total", resultPage.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return result;
    }

    /**
     * 转换消息为Map格式
     */
    private Map<String, Object> convertMessageToMap(Message msg, Long currentUserId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", msg.getId());
        map.put("content", msg.getContent());
        map.put("type", msg.getMessageType());
        map.put("createTime", msg.getCreateTime());
        map.put("isSelf", msg.getSenderId().equals(currentUserId));
        map.put("isRead", msg.getIsRead());

        // 获取发送者信息
        User sender = userMapper.selectById(msg.getSenderId());
        if (sender != null) {
            map.put("senderName", sender.getNickname() != null ?
                sender.getNickname() : sender.getUsername());
            map.put("avatar", sender.getAvatar());
        }

        return map;
    }

    /**
     * 发送消息
     */
    @Transactional
    public Map<String, Object> sendMessage(Long sessionId, Long senderId,
            Long receiverId, String content, String type) {

        Message message = new Message();
        message.setConsultationId(sessionId);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(type != null ? type : "text");
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());

        messageMapper.insert(message);

        // 更新会话时间
        ConsultationSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }

        return convertMessageToMap(message, senderId);
    }

    /**
     * 标记消息为已读
     */
    @Transactional
    public void markAsRead(Long sessionId, Long userId) {
        LambdaUpdateWrapper<Message> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Message::getConsultationId, sessionId)
               .eq(Message::getReceiverId, userId)
               .eq(Message::getIsRead, 0)
               .set(Message::getIsRead, 1);
        messageMapper.update(null, wrapper);
    }

    /**
     * 根据两个用户ID获取或创建会话
     */
    @Transactional
    public ConsultationSession getOrCreateSession(Long patientId, Long doctorId) {
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationSession::getPatientId, patientId)
               .eq(ConsultationSession::getDoctorId, doctorId)
               .eq(ConsultationSession::getSessionStatus, "ongoing")
               .last("LIMIT 1");

        ConsultationSession session = sessionMapper.selectOne(wrapper);

        if (session == null) {
            session = new ConsultationSession();
            session.setPatientId(patientId);
            session.setDoctorId(doctorId);
            session.setSessionStatus("ongoing");
            session.setStartTime(LocalDateTime.now());
            session.setCreateTime(LocalDateTime.now());
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.insert(session);
        }

        return session;
    }
}
