package com.mental.health.controller;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证接口测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static String testToken;

    @Test
    @Order(1)
    @DisplayName("测试用户注册 - 正常注册")
    void testRegister() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("username", "testuser001");
        data.put("password", "123456");
        data.put("nickname", "测试用户");
        data.put("role", "PATIENT");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(2)
    @DisplayName("测试用户注册 - 重复用户名")
    void testRegisterDuplicate() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("username", "testuser001");
        data.put("password", "123456");
        data.put("nickname", "测试用户2");
        data.put("role", "PATIENT");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @Order(3)
    @DisplayName("测试用户登录 - 正确密码")
    void testLoginSuccess() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("username", "testuser001");
        data.put("password", "123456");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> res = JSON.parseObject(response, Map.class);
        Map<String, Object> resData = (Map<String, Object>) res.get("data");
        testToken = (String) resData.get("token");
    }

    @Test
    @Order(4)
    @DisplayName("测试用户登录 - 错误密码")
    void testLoginWrongPassword() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("username", "testuser001");
        data.put("password", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @Order(5)
    @DisplayName("测试用户登录 - 不存在的用户")
    void testLoginUserNotExist() throws Exception {
        Map<String, String> data = new HashMap<>();
        data.put("username", "nonexistentuser");
        data.put("password", "123456");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
