# pythonTest — Java ↔ Python 검증 연동 데모

입력값 검증 **규칙은 Python 파일에만** 두고, 그 Python 을 **프론트엔드(브라우저) 또는 백엔드(서버)** 에서 실행하는 데모입니다. Java(Spring Boot) 프로젝트는 Python 을 실행하는 통로 역할만 합니다.

- **프론트엔드 실행**: 브라우저가 [Pyodide](https://pyodide.org)(WASM)로 `.py` 를 직접 실행
- **백엔드 실행**: 서버가 `ProcessBuilder` 로 `python` 을 실행 (stdin JSON → stdout JSON)

두 가지 데모가 있습니다.

| 데모 | Python 파일 | 내용 |
|------|-------------|------|
| 회원가입 검증 | `validation.py` | 아이디·이메일·비밀번호·나이 등 폼 검증 |
| 다양한 검증기 | `validators.py` | 정규식·체크섬·표준 라이브러리·점수 기반 등 10종 |

기술 스택: **Spring Boot 4.1.0 / Java 21 / WAR 패키징**.

> 이 프로젝트는 [Claude Code](https://claude.com/claude-code) 를 사용해 작성했습니다.
>
> 설계 배경, 프론트/백엔드 방식의 **주의점·단점**, **오프라인 동작**은 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 참고.

## 실행 전 준비물

- **JDK 21** — 설치 후 `JAVA_HOME` 이 JDK 21 을 가리키도록 설정하세요.
- **Python 3** (백엔드 실행용) — 설치한 뒤 `src/main/resources/application.properties` 의 `app.python.command` 를 Python 실행 파일 경로로 지정하세요. (PATH 에 `python` 이 정상적으로 잡혀 있으면 `python` 값 그대로 두어도 됩니다.)

## 실행

```powershell
# JAVA_HOME 이 JDK 21 을 가리키도록 설정 (경로는 환경에 맞게)
$env:JAVA_HOME = "<JDK 21 설치 경로>"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
.\mvnw.cmd spring-boot:run
```

브라우저에서 http://localhost:8080 접속 → 아래 페이지 중 선택.

### 페이지

| 데모 | 실행 위치 | 페이지 |
|------|-----------|--------|
| 회원가입 | 프론트 · 온라인(CDN) | `/frontend.html` |
| 회원가입 | 프론트 · 오프라인(로컬 번들) | `/frontend-offline.html` |
| 회원가입 | 백엔드(서버) | `/backend.html` |
| 검증기 | 프론트 (화면에서 온라인/오프라인 선택) | `/validators-frontend.html` |
| 검증기 | 백엔드(서버) | `/validators-backend.html` |

회원가입 입력값 예시:

- **통과**: 아이디 `user_01`, 이메일 `a@b.com`, 비밀번호 `abc12345`(확인 동일), 나이 `20`
- **실패**: 아이디 `ab`, 이메일 `bad`, 비밀번호 `short`, 나이 `9` → 필드별 에러 표시

## 오프라인 환경

- **백엔드 실행**은 로컬 Python 을 쓰므로 인터넷이 필요 없습니다.
- **프론트 실행**은 Pyodide 가 필요합니다. **온라인** 페이지는 CDN 에서, **오프라인** 페이지·선택은 프로젝트에 번들된 `src/main/resources/static/pyodide/` 에서 로드합니다.
- 번들 파일을 다시 받거나 버전을 올릴 때(인터넷 되는 환경에서):

  ```powershell
  powershell -File scripts/fetch-pyodide.ps1
  ```

자세한 내용은 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 의 "오프라인 환경에서의 동작" 참고.

## HTTP 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/validate/backend` | 회원가입 폼 검증 (→ `cli.py` → `validation.py`) |
| POST | `/api/validate/field` | 종류별 단일 값 검증 (→ `field_cli.py` → `validators.py`) |
| GET | `/python/{name}` | 프론트(Pyodide)용 `.py` 소스 제공 (`validation.py`, `validators.py` 화이트리스트) |

## 테스트

```powershell
$env:JAVA_HOME = "<JDK 21 설치 경로>"
.\mvnw.cmd test
```

실제 Java→Python 경로를 태우는 통합 테스트이며, Python 이 없는 환경에서는 자동으로 건너뜁니다.

- `PythonValidationServiceTest` — 회원가입 검증(유효/실패/비밀번호 불일치/한글 UTF-8)
- `FieldValidatorTest` — 다양한 검증기(Luhn·사업자번호 체크섬, IP, 날짜, 비밀번호 강도, 한글 이름, 알 수 없는 종류)
- `PythonTestApplicationTests` — 스프링 컨텍스트 로딩

## 구조

```
src/main/
├─ java/com/api/pythontest/
│  ├─ controller/
│  │  ├─ ValidationController.java      # POST /api/validate/backend, /field
│  │  └─ PythonScriptController.java    # GET /python/{name}
│  ├─ service/
│  │  └─ PythonValidationService.java   # ProcessBuilder 로 python 실행 (제네릭 실행기)
│  └─ dto/
│     ├─ SignupForm.java                # 회원가입 입력
│     ├─ ValidationResult.java          # 회원가입 결과 { valid, errors }
│     ├─ FieldValidationRequest.java    # 검증기 요청 { kind, value }
│     └─ FieldValidationResult.java     # 검증기 결과 { valid, message, detail }
└─ resources/
   ├─ python/
   │  ├─ validation.py                  # ★ 회원가입 검증 로직 (프론트/백엔드 공용)
   │  ├─ cli.py                         # 회원가입 백엔드 진입점 (stdin→stdout JSON)
   │  ├─ validators.py                  # ★ 다양한 검증기 로직 (프론트/백엔드 공용)
   │  └─ field_cli.py                   # 검증기 백엔드 진입점 (stdin→stdout JSON)
   └─ static/
      ├─ index.html                     # 메뉴
      ├─ frontend.html                  # 회원가입 · 프론트 온라인
      ├─ frontend-offline.html          # 회원가입 · 프론트 오프라인
      ├─ backend.html                   # 회원가입 · 백엔드
      ├─ validators-frontend.html       # 검증기 · 프론트 (온라인/오프라인 선택)
      ├─ validators-backend.html        # 검증기 · 백엔드
      ├─ css/form.css
      └─ pyodide/                       # 오프라인용 Pyodide 로컬 번들 (약 14MB)

scripts/fetch-pyodide.ps1               # Pyodide 번들 재생성/버전업 스크립트
docs/ARCHITECTURE.md                    # 설계·주의점·오프라인 설명

src/test/java/com/api/pythontest/service/
├─ PythonValidationServiceTest.java     # 회원가입 검증 통합 테스트
└─ FieldValidatorTest.java              # 다양한 검증기 통합 테스트
```

핵심은 **검증 규칙을 Python 파일에만** 둔다는 점입니다. `validation.py` / `validators.py` 하나를 프론트·백엔드가 공용으로 쓰므로, 규칙을 바꾸려면 Python 파일만 수정하면 양쪽에 반영됩니다.

## 검증 규칙

**회원가입 (`validation.py`)**

- 아이디: 영문/숫자/`_` 4~20자
- 이메일: `user@domain.tld` 형식
- 비밀번호: 8자 이상, 영문+숫자 포함
- 비밀번호 확인: 비밀번호와 일치
- 나이: 14~120 사이 정수

**다양한 검증기 (`validators.py`)**

| 종류(kind) | 기법 |
|------------|------|
| `email`, `phone_kr`, `korean_name`, `hex_color` | 정규식 |
| `credit_card` | Luhn 체크섬 |
| `business_no` | 사업자등록번호 체크섬 |
| `ip_address` | `ipaddress` 모듈 |
| `date` | `datetime` 파싱 |
| `url` | `urllib` 파싱 |
| `password_strength` | 점수 기반(약함/보통/강함) |

## 참고 (구현 메모)

- Spring Boot 4 는 **Jackson 3** 를 사용하므로 `ObjectMapper` 패키지가 `tools.jackson.databind` 입니다(구 `com.fasterxml.jackson.databind` 아님).
- Windows 에서 Python 은 stdout 이 파이프일 때 기본 인코딩이 시스템 로케일(CP949)이라, `cli.py`/`field_cli.py` 에서 stdin/stdout 을 UTF-8 로 고정하고 서버에서도 `PYTHONIOENCODING=utf-8` 을 지정해 한글 깨짐을 방지합니다.
