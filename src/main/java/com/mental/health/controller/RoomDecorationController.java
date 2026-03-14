package com.mental.health.controller;

import com.mental.health.common.JwtUtil;
import com.mental.health.common.Result;
import com.mental.health.entity.DecorationConfig;
import com.mental.health.entity.RoomDecoration;
import com.mental.health.service.RoomDecorationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 房间装饰控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/patient/room")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class RoomDecorationController {

    @Autowired
    private RoomDecorationService decorationService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserIdFromHeader(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("缺少或无效的认证信息");
        }
        return jwtUtil.getUserIdFromToken(authorization.substring(7));
    }

    private boolean isAuthError(IllegalArgumentException e) {
        String message = e.getMessage();
        return message != null && message.contains("认证");
    }

    private Integer parseInteger(Object value, String fieldName, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer intValue) {
            return intValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        if (value instanceof String strValue) {
            if (!StringUtils.hasText(strValue)) {
                return defaultValue;
            }
            try {
                double parsed = Double.parseDouble(strValue);
                return (int) Math.round(parsed);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("字段 " + fieldName + " 格式不正确");
            }
        }
        throw new IllegalArgumentException("字段 " + fieldName + " 类型不受支持");
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number numberValue) {
            return BigDecimal.valueOf(numberValue.doubleValue());
        }
        if (value instanceof String strValue && StringUtils.hasText(strValue)) {
            try {
                return new BigDecimal(strValue);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("字段 scale 格式不正确");
            }
        }
        return null;
    }

    /**
     * 获取用户所有装饰物（包括未放置的）
     */
    @GetMapping("/decorations")
    public Result<List<RoomDecoration>> getUserDecorations(@RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromHeader(token);
            // 返回所有装饰记录（包括未放置的）用于商店和解锁显示
            List<RoomDecoration> decorations = decorationService.getAllUserDecorations(userId);
            return Result.success(decorations);
        } catch (IllegalArgumentException e) {
            if (isAuthError(e)) {
                log.warn("Invalid authorization header when loading decorations: {}", e.getMessage());
                return Result.unauthorized("登录状态已失效，请重新登录");
            }
            log.warn("Invalid parameter when loading decorations: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get user decorations", e);
            return Result.error("获取装饰物失败");
        }
    }

    /**
     * 获取所有可用装饰物配置
     */
    @GetMapping("/configs")
    public Result<List<DecorationConfig>> getAllConfigs() {
        try {
            List<DecorationConfig> configs = decorationService.getAllConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("Failed to get configs", e);
            return Result.error("获取配置失败");
        }
    }

    /**
     * 获取推荐装饰
     */
    @GetMapping("/recommended")
    public Result<List<DecorationConfig>> getRecommended() {
        try {
            List<DecorationConfig> recommended = decorationService.getRecommendedConfigs();
            return Result.success(recommended);
        } catch (Exception e) {
            log.error("Failed to get recommended configs", e);
            return Result.error("获取推荐失败");
        }
    }

    /**
     * 检查并解锁新装饰物
     */
    @PostMapping("/check-unlock")
    public Result<Map<String, Object>> checkAndUnlock(@RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromHeader(token);
            Map<String, Object> result = decorationService.checkAndUnlockDecorations(userId);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            if (isAuthError(e)) {
                log.warn("Invalid authorization header when checking unlock: {}", e.getMessage());
                return Result.unauthorized("登录状态已失效，请重新登录");
            }
            log.warn("Invalid parameter when checking unlock: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to check and unlock", e);
            return Result.error("检查解锁失败");
        }
    }

    /**
     * 添加装饰物到房间
     */
    @PostMapping("/add")
    public Result<String> addToRoom(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = getUserIdFromHeader(token);
            String decorationType = (String) request.get("decorationType");

            if (!StringUtils.hasText(decorationType)) {
                return Result.error("装饰类型不能为空");
            }

            Integer x = parseInteger(request.get("x"), "x", 50);
            Integer y = parseInteger(request.get("y"), "y", 50);

            boolean success = decorationService.addDecorationToRoom(userId, decorationType, x, y);
            return success ? Result.success("添加成功", null) : Result.error("添加失败");
        } catch (IllegalArgumentException e) {
            if (isAuthError(e)) {
                log.warn("Invalid authorization header when adding decoration: {}", e.getMessage());
                return Result.unauthorized("登录状态已失效，请重新登录");
            }
            log.warn("Invalid parameter when adding decoration: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to add decoration to room", e);
            return Result.error("添加失败");
        }
    }

    /**
     * 更新装饰物位置
     */
    @PutMapping("/position/{id}")
    public Result<String> updatePosition(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = getUserIdFromHeader(token);
            Integer x = request.containsKey("x") ? parseInteger(request.get("x"), "x", null) : null;
            Integer y = request.containsKey("y") ? parseInteger(request.get("y"), "y", null) : null;
            Integer z = request.containsKey("z") ? parseInteger(request.get("z"), "z", null) : null;
            BigDecimal scale = request.containsKey("scale") ? parseBigDecimal(request.get("scale")) : null;
            Integer rotation = request.containsKey("rotation") ? parseInteger(request.get("rotation"), "rotation", null) : null;

            boolean success = decorationService.updateDecorationPosition(userId, id, x, y, z, scale, rotation);
            return success ? Result.success("更新成功", null) : Result.error("更新失败");
        } catch (IllegalArgumentException e) {
            if (isAuthError(e)) {
                log.warn("Invalid authorization header when updating decoration: {}", e.getMessage());
                return Result.unauthorized("登录状态已失效，请重新登录");
            }
            log.warn("Invalid parameter when updating decoration: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update decoration position", e);
            return Result.error("更新失败");
        }
    }

    /**
     * 移除装饰
     */
    @DeleteMapping("/{id}")
    public Result<String> removeDecoration(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromHeader(token);
            boolean success = decorationService.removeDecoration(userId, id);
            return success ? Result.success("移除成功", null) : Result.error("移除失败");
        } catch (IllegalArgumentException e) {
            if (isAuthError(e)) {
                log.warn("Invalid authorization header when removing decoration: {}", e.getMessage());
                return Result.unauthorized("登录状态已失效，请重新登录");
            }
            log.warn("Invalid parameter when removing decoration: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to remove decoration", e);
            return Result.error("移除失败");
        }
    }

    /**
     * 与装饰物互动
     */
    @PostMapping("/interact/{id}")
    public Result<Map<String, Object>> interact(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = getUserIdFromHeader(token);
            Map<String, Object> result = decorationService.interactWithDecoration(userId, id);
            if (result.isEmpty()) {
                return Result.error("互动失败");
            }
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            if (isAuthError(e)) {
                log.warn("Invalid authorization header when interacting with decoration: {}", e.getMessage());
                return Result.unauthorized("登录状态已失效，请重新登录");
            }
            log.warn("Invalid parameter when interacting with decoration: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to interact with decoration", e);
            return Result.error("互动失败");
        }
    }
}
