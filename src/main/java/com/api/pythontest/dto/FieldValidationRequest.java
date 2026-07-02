package com.api.pythontest.dto;

/**
 * 다양한 검증기 요청. kind 로 검증 종류를, value 로 검증할 값을 전달한다.
 */
public record FieldValidationRequest(
        String kind,
        String value
) {
}
