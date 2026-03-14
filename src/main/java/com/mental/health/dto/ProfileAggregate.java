package com.mental.health.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProfileAggregate implements Serializable {
    private static final long serialVersionUID = 1L;

    // User基础信息
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private Integer gender;
    private Integer age;
    private String role;
    private String specialization;

    // PatientProfile临床信息
    private Long profileId;
    private String realName;
    private LocalDate birthDate;
    private String maritalStatus;
    private String occupation;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String introduction;
    private String medicalHistory;
    private String allergyHistory;
    private String familyHistory;
    private String consentFlags;
    private String intakeTags;
    private Integer profileVersion;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
