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

    public static class LoginRequest {
        private String username;
        private String usernameOrEmail;
        private String password;

        public String getUsername() { return username != null ? username : usernameOrEmail; }
        public void setUsername(String username) { this.username = username; }
        public String getUsernameOrEmail() { return usernameOrEmail != null ? usernameOrEmail : username; }
        public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class PhoneLoginRequest {
        private String phone;
        private String code;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class UserDto {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
        private Integer birthDay;
        private Integer birthMonth;
        private String gender;
        private String verifiedType;

        public UserDto() {}

        public UserDto(com.ares.model.User user) {
            if (user != null) {
                this.id = user.getId();
                this.username = user.getUsername();
                this.fullName = user.getFullName();
                this.email = user.getEmail();
                this.phone = user.getPhone();
                this.avatarUrl = user.getAvatarUrl();
                this.birthDay = user.getBirthDay();
                this.birthMonth = user.getBirthMonth();
                this.gender = user.getGender();
                this.verifiedType = user.getVerifiedType();
            }
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public Integer getBirthDay() { return birthDay; }
        public void setBirthDay(Integer birthDay) { this.birthDay = birthDay; }
        public Integer getBirthMonth() { return birthMonth; }
        public void setBirthMonth(Integer birthMonth) { this.birthMonth = birthMonth; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getVerifiedType() { return verifiedType; }
        public void setVerifiedType(String verifiedType) { this.verifiedType = verifiedType; }
    }
}
