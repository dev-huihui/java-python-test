package com.api.pythontest.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 프론트엔드 검증 버전을 위해 Python 검증 로직 소스를 그대로 내려준다.
 * 브라우저의 Pyodide 가 이 텍스트를 fetch 해서 실행한다. (백엔드와 동일한 .py 재사용)
 */
@RestController
public class PythonScriptController {

    /** 외부로 제공을 허용하는 스크립트 (경로 조작 방지용 화이트리스트). */
    private static final Set<String> ALLOWED = Set.of("validation.py", "validators.py");

    @GetMapping(value = "/python/{name}", produces = MediaType.TEXT_PLAIN_VALUE + "; charset=UTF-8")
    public ResponseEntity<String> script(@PathVariable String name) throws IOException {
        if (!ALLOWED.contains(name)) {
            return ResponseEntity.notFound().build();
        }
        try (InputStream in = new ClassPathResource("python/" + name).getInputStream()) {
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(body);
        }
    }
}
