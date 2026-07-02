# pythonTest — 회원가입 입력값 검증 데모 (Java ↔ Python 연동)

회원가입 폼의 입력값을 **Python 파일(`validation.py`)** 로 검증하는 데모입니다.
같은 검증 로직을 두 가지 방식으로 실행합니다.

| 버전 | 실행 위치 | 연동 방식 | Python 설치 |
|------|-----------|-----------|-------------|
| ① 프론트엔드 | 브라우저 | Pyodide(WASM) 가 `validation.py` 실행 | 불필요 (단, Pyodide CDN 접속 필요) |
| ② 백엔드 | 서버(JVM) | `ProcessBuilder` 로 `python cli.py` 실행 | **필요** |

기술 스택: Spring Boot 4.1.0 / Java 21 / WAR 패키징.

## 실행 전 준비물

- **JDK 21** — 설치 후 `JAVA_HOME` 이 JDK 21 을 가리키도록 설정하세요.
- **Python 3** (백엔드 버전만) — 설치한 뒤 `application.properties` 의 `app.python.command` 를
  Python 실행 파일 경로로 지정하세요. (PATH 에 `python` 이 정상적으로 잡혀 있으면 `python` 값 그대로 두어도 됩니다.)

## 실행

PowerShell:

```powershell
# JAVA_HOME 이 JDK 21 을 가리키도록 설정 (경로는 환경에 맞게)
$env:JAVA_HOME = "<JDK 21 설치 경로>"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
.\mvnw.cmd spring-boot:run
```

브라우저에서 접속:

- 홈(버전 선택): http://localhost:8080
- ① 프론트엔드 검증: http://localhost:8080/frontend.html
- ② 백엔드 검증: http://localhost:8080/backend.html

입력값 예시:

- **통과**: 아이디 `user_01`, 이메일 `a@b.com`, 비밀번호 `abc12345`(확인 동일), 나이 `20`
- **실패**: 아이디 `ab`, 이메일 `bad`, 비밀번호 `short`, 나이 `9` → 필드별 에러 표시

## 테스트

```powershell
$env:JAVA_HOME = "<JDK 21 설치 경로>"
.\mvnw.cmd test
```

- `PythonValidationServiceTest` — 실제 Java→Python 경로를 태우는 백엔드 검증 통합 테스트
  (유효 입력 통과 / 잘못된 입력 필드별 에러 / 비밀번호 불일치 / 한글 UTF-8 확인).
  Python 이 없는 환경에서는 자동으로 건너뜁니다.
- `PythonTestApplicationTests` — 스프링 컨텍스트 로딩 확인.

## 구조

```
src/main/
├─ java/com/api/pythontest/
│  ├─ controller/
│  │  ├─ ValidationController.java     # POST /api/validate/backend (백엔드 버전 API)
│  │  └─ PythonScriptController.java   # GET /python/validation.py (프론트에 스크립트 제공)
│  ├─ service/
│  │  └─ PythonValidationService.java  # ProcessBuilder 로 python 실행
│  └─ dto/
│     ├─ SignupForm.java               # 입력 폼
│     └─ ValidationResult.java         # 검증 결과 { valid, errors }
└─ resources/
   ├─ python/
   │  ├─ validation.py                 # ★ 검증 로직 (프론트/백엔드 공용)
   │  └─ cli.py                        # 백엔드용 진입점 (stdin JSON → stdout JSON)
   └─ static/
      ├─ index.html                    # 두 버전 선택 화면
      ├─ frontend.html                 # ① 프론트엔드(Pyodide) 검증
      ├─ backend.html                  # ② 백엔드(ProcessBuilder) 검증
      └─ css/form.css

src/test/java/com/api/pythontest/
└─ service/PythonValidationServiceTest.java   # 백엔드 검증 통합 테스트
```

핵심은 **`validation.py` 하나를 두 버전이 공용**으로 쓴다는 점입니다.
검증 규칙을 바꾸려면 이 파일만 수정하면 양쪽에 모두 반영됩니다.

## 검증 규칙

- 아이디: 영문/숫자/`_` 4~20자
- 이메일: `user@domain.tld` 형식
- 비밀번호: 8자 이상, 영문+숫자 포함
- 비밀번호 확인: 비밀번호와 일치
- 나이: 14~120 사이 정수

## 참고 (구현 메모)

- Spring Boot 4 는 **Jackson 3** 를 사용하므로 `ObjectMapper` 패키지가 `tools.jackson.databind` 입니다(구 `com.fasterxml.jackson.databind` 아님).
- Windows 에서 Python 은 stdout 이 파이프일 때 기본 인코딩이 시스템 로케일(CP949)이라, `cli.py` 에서 stdin/stdout 을 UTF-8 로 고정하고 서버에서도 `PYTHONIOENCODING=utf-8` 을 지정해 한글 깨짐을 방지합니다.
