package com.api.pythontest.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 프론트엔드 검증 버전을 위해 Python 검증 로직 소스를 그대로 내려준다.
 * 브라우저의 Pyodide 가 이 텍스트를 fetch 해서 실행한다. (백엔드와 동일한 validation.py 재사용)
 */
@RestController
public class PythonScriptController {

    @GetMapping(value = "/python/validation.py", produces = MediaType.TEXT_PLAIN_VALUE + "; charset=UTF-8")
    public ResponseEntity<String> validationScript() throws IOException {
        try (InputStream in = new ClassPathResource("python/validation.py").getInputStream()) {
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(body);
        }
    }
}
