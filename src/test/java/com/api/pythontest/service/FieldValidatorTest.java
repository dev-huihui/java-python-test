package com.api.pythontest.service;

import com.api.pythontest.dto.FieldValidationResult;
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
 * 다양한 검증기(validators.py)를 백엔드로 실행하는 통합 테스트.
 * 실제 Python 을 호출하므로 Python 이 없는 환경에서는 건너뛴다.
 */
@SpringBootTest
class FieldValidatorTest {

    @Autowired
    private PythonValidationService service;

    @Value("${app.python.command}")
    private String pythonCommand;

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
        Assumptions.assumeTrue(available, "Python 실행 불가 — 테스트 건너뜀");
    }

    @Test
    @DisplayName("Luhn 체크섬: 유효한 카드번호 통과, 변조된 번호 실패")
    void 신용카드_Luhn_검증() {
        assertThat(service.validateField("credit_card", "4111 1111 1111 1111").valid()).isTrue();
        assertThat(service.validateField("credit_card", "4111 1111 1111 1112").valid()).isFalse();
    }

    @Test
    @DisplayName("사업자등록번호 체크섬 검증")
    void 사업자등록번호_검증() {
        assertThat(service.validateField("business_no", "123-45-67891").valid()).isTrue();
        assertThat(service.validateField("business_no", "123-45-67890").valid()).isFalse();
    }

    @Test
    @DisplayName("IP 주소: 표준 라이브러리로 IPv4/IPv6 판별")
    void IP주소_검증() {
        FieldValidationResult v4 = service.validateField("ip_address", "192.168.0.1");
        assertThat(v4.valid()).isTrue();
        assertThat(v4.detail()).containsEntry("version", 4);
        assertThat(service.validateField("ip_address", "999.1.1.1").valid()).isFalse();
    }

    @Test
    @DisplayName("날짜: 실제 존재하지 않는 날짜는 실패")
    void 날짜_검증() {
        assertThat(service.validateField("date", "2026-07-02").valid()).isTrue();
        assertThat(service.validateField("date", "2026-02-30").valid()).isFalse();
    }

    @Test
    @DisplayName("비밀번호 강도: 점수와 등급을 detail 로 반환")
    void 비밀번호_강도() {
        FieldValidationResult weak = service.validateField("password_strength", "abc");
        assertThat(weak.valid()).isFalse();
        assertThat(weak.detail()).containsEntry("level", "약함");

        FieldValidationResult strong = service.validateField("password_strength", "P@ssw0rd!2026");
        assertThat(strong.valid()).isTrue();
    }

    @Test
    @DisplayName("한글 이름은 한글이 아니면 실패")
    void 한글이름_검증() {
        assertThat(service.validateField("korean_name", "홍길동").valid()).isTrue();
        assertThat(service.validateField("korean_name", "Hong").valid()).isFalse();
    }

    @Test
    @DisplayName("알 수 없는 종류는 valid=false")
    void 알수없는_종류() {
        assertThat(service.validateField("unknown_kind", "x").valid()).isFalse();
    }
}
