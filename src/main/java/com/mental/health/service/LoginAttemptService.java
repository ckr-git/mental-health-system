package com.mental.health.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录尝试限制服务
 * 防止暴力破解密码
 */
@Service
public class LoginAttemptService {

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.lock-duration:300}")
    private int lockDurationSeconds;

    private final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lockCache = new ConcurrentHashMap<>();

    /**
     * 登录失败时调用
     */
    public void loginFailed(String username) {
        int attempts = attemptsCache.getOrDefault(username, 0) + 1;
        attemptsCache.put(username, attempts);

        if (attempts >= maxAttempts) {
            lockCache.put(username, System.currentTimeMillis());
        }
    }

    /**
     * 登录成功时调用，清除记录
     */
    public void loginSucceeded(String username) {
        attemptsCache.remove(username);
        lockCache.remove(username);
    }

    /**
     * 检查账户是否被锁定
     */
    public boolean isLocked(String username) {
        Long lockTime = lockCache.get(username);
        if (lockTime == null) {
            return false;
        }

        long elapsed = (System.currentTimeMillis() - lockTime) / 1000;
        if (elapsed >= lockDurationSeconds) {
            // 锁定时间已过，解锁
            lockCache.remove(username);
            attemptsCache.remove(username);
            return false;
        }
        return true;
    }

    /**
     * 获取剩余锁定时间（秒）
     */
    public int getRemainingLockTime(String username) {
        Long lockTime = lockCache.get(username);
        if (lockTime == null) {
            return 0;
        }
        long elapsed = (System.currentTimeMillis() - lockTime) / 1000;
        return Math.max(0, lockDurationSeconds - (int) elapsed);
    }

    /**
     * 获取剩余尝试次数
     */
    public int getRemainingAttempts(String username) {
        int attempts = attemptsCache.getOrDefault(username, 0);
        return Math.max(0, maxAttempts - attempts);
    }
}
