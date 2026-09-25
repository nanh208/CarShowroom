package com.ares.dto;

public class AuthDtos {

    public static class SendCodeRequest {
        private String target;
        private boolean isEmail;

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public boolean isEmail() { return isEmail; }
        public void setEmail(boolean email) { isEmail = email; }
    }

    public static class VerifyCodeRequest {
        private String target;
        private String code;

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class RegisterRequest {
        private String username;
        private Integer birthDay;
        private Integer birthMonth;
        private String gender;
        private String emailOrPhone;
        private String password;
        private String confirmPassword;
        private String verificationCode;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Integer getBirthDay() { return birthDay; }
        public void setBirthDay(Integer birthDay) { this.birthDay = birthDay; }
        public Integer getBirthMonth() { return birthMonth; }
        public void setBirthMonth(Integer birthMonth) { this.birthMonth = birthMonth; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getEmailOrPhone() { return emailOrPhone; }
        public void setEmailOrPhone(String emailOrPhone) { this.emailOrPhone = emailOrPhone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        public String getVerificationCode() { return verificationCode; }
        public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    }

    public static class ResetPasswordRequest {
        private String target;
        private String code;
        private String newPassword;

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
