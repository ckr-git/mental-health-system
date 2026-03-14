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
 * 用户接口测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static String token;

    @BeforeAll
    static void setup(@Autowired MockMvc mockMvc) throws Exception {
        // 注册并登录获取token
        Map<String, String> regData = new HashMap<>();
        regData.put("username", "usertest001");
        regData.put("password", "123456");
        regData.put("nickname", "用户测试");
        regData.put("role", "PATIENT");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(regData)));

        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "usertest001");
        loginData.put("password", "123456");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(loginData)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> res = JSON.parseObject(response, Map.class);
        if (res.get("data") != null) {
            Map<String, Object> data = (Map<String, Object>) res.get("data");
            token = (String) data.get("token");
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试获取用户信息 - 无Token")
    void testGetProfileWithoutToken() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("测试获取用户信息 - 有Token")
    void testGetProfileWithToken() throws Exception {
        if (token == null) return;

        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
