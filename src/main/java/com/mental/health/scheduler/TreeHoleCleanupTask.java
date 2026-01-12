package com.mental.health.scheduler;

import com.mental.health.service.TreeHoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 心情树洞定时清理任务
 * 每5分钟执行一次,标记已过期的倾诉记录
 */
@Component
public class TreeHoleCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(TreeHoleCleanupTask.class);

    @Autowired
    private TreeHoleService treeHoleService;

    /**
     * 清理过期内容
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void cleanExpiredContent() {
        try {
            int count = treeHoleService.cleanExpiredContent();
            if (count > 0) {
                logger.info("标记了 {} 条过期的树洞记录", count);
            }
        } catch (Exception e) {
            logger.error("清理过期树洞记录失败", e);
        }
    }
}
