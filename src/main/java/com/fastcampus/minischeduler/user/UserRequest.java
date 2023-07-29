package com.fastcampus.minischeduler.user;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.*;

public class UserRequest {

    @Getter
    public static class LoginDTO {

        @NotBlank(message = "이메일을 입력해주세요")
        @Pattern(
                regexp = "^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$",
                message = "이메일 형식으로 작성해주세요"
        )
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, max = 20)
        private String password;
    }

    @Getter
    public static class JoinDTO {

        @NotBlank(message = "이메일을 입력해주세요")
        @Pattern(
                regexp = "^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$",
                message = "이메일 형식으로 작성해주세요"
        )
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, max = 20, message = "8~20자 이내로 입력해주세요")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,20}$",
                message = "영문, 숫자, 특수문자 조합 8~20자 이내로 입력해주세요"
        )
        private String password;

        @NotBlank(message = "이름을 입력해주세요")
        @Pattern(
                regexp = "^[a-zA-Z가-힣]{1,20}$",
                message = "한글/영문 1~20자 이내로 작성해주세요"
        )
        private String fullName;

        @NotNull(message = "기획사 또는 팬을 선택해주세요")
        private Role role;

        public void setPassword(String password) { // password 인코딩용
            this.password = password;
        }

        public User toEntity() {
            return User.builder()
                    .email(email)
                    .password(password)
                    .fullName(fullName)
                    .role(role)
                    .build();
        }
    }

    @Setter
    @Getter
    public static class UpdateUserInfoDTO {

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, max = 20, message = "8~20자 이내로 입력해주세요")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,20}$",
                message = "영문, 숫자, 특수문자 조합 8~20자 이내로 입력해주세요"
        )
        private String password;

        private String profileImage;

        private String updateDate;
    }
}
