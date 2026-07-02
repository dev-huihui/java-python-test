"""회원가입 입력값 검증 로직.

프론트엔드(Pyodide)와 백엔드(ProcessBuilder) 두 버전이 공용으로 사용한다.
표준 라이브러리(re)만 사용하므로 브라우저의 Pyodide에서도 추가 설치 없이 동작한다.
"""
import re

USERNAME_RE = re.compile(r"^[A-Za-z0-9_]{4,20}$")
EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


def validate(data):
    """입력값 dict를 받아 {"valid": bool, "errors": {field: message}} 를 반환한다."""
    errors = {}

    username = (data.get("username") or "").strip()
    email = (data.get("email") or "").strip()
    password = data.get("password") or ""
    password_confirm = data.get("passwordConfirm") or ""
    age_raw = data.get("age")

    if not USERNAME_RE.match(username):
        errors["username"] = "아이디는 영문/숫자/_ 4~20자여야 합니다."

    if not EMAIL_RE.match(email):
        errors["email"] = "올바른 이메일 형식이 아닙니다."

    if len(password) < 8 or not re.search(r"[A-Za-z]", password) or not re.search(r"\d", password):
        errors["password"] = "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다."

    if password != password_confirm:
        errors["passwordConfirm"] = "비밀번호가 일치하지 않습니다."

    try:
        age = int(age_raw)
        if age < 14 or age > 120:
            errors["age"] = "나이는 14~120 사이여야 합니다."
    except (TypeError, ValueError):
        errors["age"] = "나이는 숫자여야 합니다."

    return {"valid": len(errors) == 0, "errors": errors}
