package com.zanchi.zanchi_backend.web.auth.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}