package com.mental.health.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateProfileCommand {
    // User基础信息
    private String nickname;
    private String email;
    private String phone;
    private Integer gender;
    private Integer age;

    // PatientProfile临床信息
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
}
