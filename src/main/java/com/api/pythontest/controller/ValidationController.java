package com.api.pythontest.controller;

import com.api.pythontest.dto.SignupForm;
import com.api.pythontest.dto.ValidationResult;
import com.api.pythontest.service.PythonValidationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 백엔드 검증 버전 API.
 * 프론트엔드는 폼 값을 JSON 으로 보내고, 서버가 Python 으로 검증해 결과를 돌려준다.
 */
@RestController
@RequestMapping("/api/validate")
public class ValidationController {

    private final PythonValidationService service;

    public ValidationController(PythonValidationService service) {
        this.service = service;
    }

    @PostMapping("/backend")
    public ValidationResult backend(@RequestBody SignupForm form) {
        return service.validate(form);
    }
}
