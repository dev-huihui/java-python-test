package com.api.pythontest.dto;

import java.util.Map;

/**
 * Python 검증 결과. valid 가 true 면 통과, 아니면 errors 에 필드별 메시지가 담긴다.
 */
public record ValidationResult(
        boolean valid,
        Map<String, String> errors
) {
}
