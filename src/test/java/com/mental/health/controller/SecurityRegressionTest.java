package com.mental.health.controller;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    private String login(String username, String password) throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(loginData)))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> response = JSON.parseObject(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return (String) data.get("token");
    }

    @Test
    @DisplayName("patient doctors API must not expose password")
    void patientDoctorsShouldNotExposePassword() throws Exception {
        String token = login("patient001", "123456");
        MvcResult result = mockMvc.perform(get("/api/patient/doctors")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = JSON.parseObject(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
        assertFalse(records.isEmpty());
        assertTrue(records.stream().noneMatch(row -> row.containsKey("password") && row.get("password") != null));
    }

    @Test
    @DisplayName("admin users API must not expose password")
    void adminUsersShouldNotExposePassword() throws Exception {
        String token = login("admin", "123456");
        MvcResult result = mockMvc.perform(get("/api/admin/users")
                        .param("pageNum", "1")
                        .param("pageSize", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> response = JSON.parseObject(result.getResponse().getContentAsString(), Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");
        assertFalse(records.isEmpty());
        assertTrue(records.stream().noneMatch(row -> row.containsKey("password") && row.get("password") != null));
    }
}
