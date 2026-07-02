package com.api.pythontest.service;

import com.api.pythontest.dto.SignupForm;
import com.api.pythontest.dto.ValidationResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 백엔드 검증(ProcessBuilder → Python) 통합 테스트.
 * 실제 Python 을 호출하므로, Python 이 없는 환경에서는 각 테스트를 건너뛴다.
 */
@SpringBootTest
class PythonValidationServiceTest {

    @Autowired
    private PythonValidationService service;

    @Value("${app.python.command}")
    private String pythonCommand;

    /** 설정된 python 실행 파일이 동작할 때만 테스트를 진행한다. */
    @BeforeEach
    void assumePythonAvailable() {
        boolean available = false;
        try {
            Process p = new ProcessBuilder(pythonCommand, "--version").start();
            if (p.waitFor(5, TimeUnit.SECONDS)) {
                available = p.exitValue() == 0;
            } else {
                p.destroyForcibly();
            }
        } catch (Exception e) {
            available = false;
        }
        Assumptions.assumeTrue(available,
                "Python(" + pythonCommand + ") 실행 불가 — 통합 테스트 건너뜀");
    }

    @Test
    @DisplayName("유효한 입력은 valid=true, errors 비어 있음")
    void 유효한_입력은_통과한다() {
        SignupForm form = new SignupForm("user_01", "a@b.com", "abc12345", "abc12345", "20");

        ValidationResult result = service.validate(form);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("모두 잘못된 입력은 필드별 에러를 반환")
    void 잘못된_입력은_필드별_에러를_반환한다() {
        SignupForm form = new SignupForm("ab", "bad", "short", "nope", "9");

        ValidationResult result = service.validate(form);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .containsKeys("username", "email", "password", "passwordConfirm", "age");
    }

    @Test
    @DisplayName("비밀번호 불일치만 있을 때 passwordConfirm 만 에러")
    void 비밀번호_불일치를_감지한다() {
        SignupForm form = new SignupForm("user_01", "a@b.com", "abc12345", "different1", "20");

        ValidationResult result = service.validate(form);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).containsKey("passwordConfirm");
        assertThat(result.errors()).doesNotContainKey("password");
    }

    @Test
    @DisplayName("한글 에러 메시지가 깨지지 않는다(UTF-8)")
    void 한글_에러_메시지가_깨지지_않는다() {
        SignupForm form = new SignupForm("ab", "a@b.com", "abc12345", "abc12345", "20");

        ValidationResult result = service.validate(form);

        assertThat(result.errors().get("username")).contains("아이디");
    }
}
