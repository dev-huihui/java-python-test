package com.api.pythontest.dto;

/**
 * 회원가입 입력 폼. 프론트엔드에서 JSON 으로 전달된다.
 * age 는 미입력/문자열 입력도 Python 쪽에서 처리하도록 String 으로 받는다.
 */
public record SignupForm(
        String username,
        String email,
        String password,
        String passwordConfirm,
        String age
) {
}
