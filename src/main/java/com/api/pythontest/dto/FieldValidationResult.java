package com.api.pythontest.dto;

import java.util.Map;

/**
 * 다양한 검증기 결과.
 * valid: 통과 여부, message: 사람이 읽을 메시지, detail: 검증기별 부가 정보(정규화 값·점수 등).
 */
public record FieldValidationResult(
        boolean valid,
        String message,
        Map<String, Object> detail
) {
}
