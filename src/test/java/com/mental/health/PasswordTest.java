package com.mental.health;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希生成测试
 */
public class PasswordTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "123456";
        String hash = encoder.encode(password);
        
        System.out.println("===========================================");
        System.out.println("明文密码: " + password);
        System.out.println("BCrypt哈希: " + hash);
        System.out.println("===========================================");
        
        // 验证
        boolean matches = encoder.matches(password, hash);
        System.out.println("验证新哈希: " + matches);
        
        // 验证旧哈希
        String oldHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z6pQE0eojCGnJQ0bXK4Ak9Ae";
        boolean oldMatches = encoder.matches(password, oldHash);
        System.out.println("验证旧哈希: " + oldMatches);
        System.out.println("===========================================");
        
        if (!oldMatches) {
            System.out.println("警告：旧哈希无法验证123456！");
            System.out.println("请使用新生成的哈希值更新数据库。");
        }
    }
}
