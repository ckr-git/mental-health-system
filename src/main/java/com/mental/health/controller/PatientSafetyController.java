package com.mental.health.controller;

import com.mental.health.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient/safety")
public class PatientSafetyController {

    @GetMapping("/resources")
    public Result<List<Map<String, String>>> getResources() {
        List<Map<String, String>> resources = List.of(
                Map.of("type", "HOTLINE", "name", "24小时心理援助热线", "value", "400-161-9995", "description", "全国心理援助热线，24小时在线"),
                Map.of("type", "HOTLINE", "name", "北京心理危机研究与干预中心", "value", "010-82951332", "description", "专业心理危机干预"),
                Map.of("type", "HOTLINE", "name", "生命热线", "value", "400-821-1215", "description", "24小时生命热线"),
                Map.of("type", "TOOL", "name", "呼吸练习", "value", "/patient/meditation", "description", "通过呼吸练习缓解焦虑"),
                Map.of("type", "TOOL", "name", "情绪日记", "value", "/patient/mood-diary", "description", "记录和表达您的感受")
        );
        return Result.success(resources);
    }
}
