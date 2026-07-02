package com.api.pythontest.service;

import com.api.pythontest.dto.FieldValidationResult;
import com.api.pythontest.dto.SignupForm;
import com.api.pythontest.dto.ValidationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 백엔드 검증: classpath 의 Python 스크립트를 임시 폴더로 꺼낸 뒤
 * {@link ProcessBuilder} 로 실행한다. (stdin 으로 JSON 전달 → stdout 으로 JSON 결과 수신)
 *
 * <p>검증 규칙 자체는 Python 파일에만 있고, 이 클래스는 실행 통로 역할만 한다.
 */
@Service
public class PythonValidationService {

    /** 임시 폴더로 꺼내 두고 실행할 Python 스크립트 목록. */
    private static final String[] SCRIPTS = {
            "validation.py", "cli.py",       // 회원가입 검증
            "validators.py", "field_cli.py"  // 다양한 검증기
    };

    private final ObjectMapper mapper;
    private final String pythonCommand;

    /** Python 스크립트를 꺼내 둔 임시 디렉터리 (최초 호출 시 1회 생성). */
    private volatile Path scriptDir;

    public PythonValidationService(ObjectMapper mapper,
                                   @Value("${app.python.command:python}") String pythonCommand) {
        this.mapper = mapper;
        this.pythonCommand = pythonCommand;
    }

    /** 회원가입 폼 검증 (cli.py → validation.py). */
    public ValidationResult validate(SignupForm form) {
        return run("cli.py", form, ValidationResult.class);
    }

    /** 종류별 단일 값 검증 (field_cli.py → validators.py). */
    public FieldValidationResult validateField(String kind, String value) {
        Map<String, String> input = Map.of(
                "kind", kind == null ? "" : kind,
                "value", value == null ? "" : value);
        return run("field_cli.py", input, FieldValidationResult.class);
    }

    /** 입력 객체를 JSON 으로 직렬화해 stdin 으로 넘기고, stdout JSON 을 결과 타입으로 역직렬화한다. */
    private <T> T run(String script, Object input, Class<T> resultType) {
        try {
            Path dir = ensureScripts();
            String json = mapper.writeValueAsString(input);

            ProcessBuilder pb = new ProcessBuilder(pythonCommand, script);
            pb.directory(dir.toFile());
            // Python stdin/stdout 을 UTF-8 로 강제 (Windows 기본 로케일 인코딩 방지)
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            Process proc = pb.start();

            try (OutputStream os = proc.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String err = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!proc.waitFor(10, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                throw new IllegalStateException("Python 실행 시간 초과(10초)");
            }
            if (proc.exitValue() != 0) {
                throw new IllegalStateException("Python 실행 오류: " + err.trim());
            }

            return mapper.readValue(out, resultType);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(
                    "Python 검증 실패: " + e.getMessage()
                            + " (python 실행 파일이 설치·PATH 등록되어 있는지, "
                            + "또는 app.python.command 설정을 확인하세요)", e);
        }
    }

    /** classpath:python/*.py 를 임시 디렉터리로 1회 추출한다. */
    private Path ensureScripts() throws IOException {
        Path dir = scriptDir;
        if (dir == null) {
            synchronized (this) {
                if (scriptDir == null) {
                    Path created = Files.createTempDirectory("py-validation");
                    for (String name : SCRIPTS) {
                        copyResource(created, name);
                    }
                    created.toFile().deleteOnExit();
                    scriptDir = created;
                }
                dir = scriptDir;
            }
        }
        return dir;
    }

    private void copyResource(Path targetDir, String name) throws IOException {
        try (InputStream in = new ClassPathResource("python/" + name).getInputStream()) {
            Files.copy(in, targetDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
