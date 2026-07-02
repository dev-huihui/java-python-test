"""다양한 입력값 검증기 모음. (프론트 Pyodide / 백엔드 ProcessBuilder 공용)

정규식, 체크섬 알고리즘, 표준 라이브러리(ipaddress/datetime/urllib), 점수 기반 등
서로 다른 검증 기법을 한곳에서 보여준다. 표준 라이브러리만 사용한다.
"""
import re
import ipaddress
from datetime import datetime
from urllib.parse import urlparse

EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")
PHONE_RE = re.compile(r"^01[016789]-?\d{3,4}-?\d{4}$")
KOREAN_NAME_RE = re.compile(r"^[가-힣]{2,5}$")
HEX_COLOR_RE = re.compile(r"^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$")


def _ok(message, **detail):
    return {"valid": True, "message": message, "detail": detail}


def _fail(message, **detail):
    return {"valid": False, "message": message, "detail": detail}


# --- 정규식 기반 ---------------------------------------------------------

def check_email(value):
    if EMAIL_RE.match(value.strip()):
        return _ok("올바른 이메일 형식입니다.")
    return _fail("이메일 형식이 아닙니다. 예) user@example.com")


def check_phone_kr(value):
    v = value.strip()
    if not PHONE_RE.match(v):
        return _fail("휴대폰 번호 형식이 아닙니다. 예) 010-1234-5678")
    digits = re.sub(r"\D", "", v)
    normalized = "{}-{}-{}".format(digits[:3], digits[3:-4], digits[-4:])
    return _ok("올바른 휴대폰 번호입니다.", normalized=normalized)


def check_korean_name(value):
    if KOREAN_NAME_RE.match(value.strip()):
        return _ok("올바른 한글 이름입니다.")
    return _fail("한글 2~5자여야 합니다. 예) 홍길동")


def check_hex_color(value):
    if HEX_COLOR_RE.match(value.strip()):
        return _ok("유효한 HEX 색상 코드입니다.")
    return _fail("HEX 색상 형식이 아닙니다. 예) #4f7cff")


# --- 체크섬 알고리즘 기반 -------------------------------------------------

def check_credit_card(value):
    digits = re.sub(r"\D", "", value)
    if not (13 <= len(digits) <= 19):
        return _fail("카드 번호 자릿수가 올바르지 않습니다. (13~19자리)")
    total = 0
    for i, ch in enumerate(reversed(digits)):
        d = int(ch)
        if i % 2 == 1:  # Luhn: 뒤에서 짝수 번째 자리를 2배
            d *= 2
            if d > 9:
                d -= 9
        total += d
    if total % 10 == 0:
        return _ok("Luhn 체크섬을 통과한 카드 번호입니다.")
    return _fail("Luhn 체크섬에 실패했습니다. 번호를 확인하세요.")


def check_business_no(value):
    digits = re.sub(r"\D", "", value)
    if len(digits) != 10:
        return _fail("사업자등록번호는 10자리입니다. 예) 123-45-67890")
    weights = [1, 3, 7, 1, 3, 7, 1, 3, 5]
    nums = [int(c) for c in digits]
    s = sum(nums[i] * weights[i] for i in range(9))
    s += (nums[8] * 5) // 10
    check = (10 - (s % 10)) % 10
    if check == nums[9]:
        return _ok("유효한 사업자등록번호입니다.")
    return _fail("사업자등록번호 검증(체크섬)에 실패했습니다.")


# --- 표준 라이브러리 기반 -------------------------------------------------

def check_ip_address(value):
    try:
        ip = ipaddress.ip_address(value.strip())
        return _ok(
            "유효한 IPv{} 주소입니다.".format(ip.version),
            version=ip.version,
            is_private=ip.is_private,
        )
    except ValueError:
        return _fail("올바른 IP 주소가 아닙니다. 예) 192.168.0.1")


def check_date(value):
    try:
        d = datetime.strptime(value.strip(), "%Y-%m-%d")
        return _ok("유효한 날짜입니다.", weekday=d.strftime("%A"))
    except ValueError:
        return _fail("YYYY-MM-DD 형식의 실제 존재하는 날짜여야 합니다. 예) 2026-07-02")


def check_url(value):
    parsed = urlparse(value.strip())
    if parsed.scheme in ("http", "https") and parsed.netloc:
        return _ok("유효한 URL 입니다.", scheme=parsed.scheme, host=parsed.netloc)
    return _fail("http/https URL 형식이 아닙니다. 예) https://example.com")


# --- 점수 기반 -----------------------------------------------------------

def check_password_strength(value):
    score = 0
    if len(value) >= 8:
        score += 1
    if len(value) >= 12:
        score += 1
    if re.search(r"[a-z]", value):
        score += 1
    if re.search(r"[A-Z]", value):
        score += 1
    if re.search(r"\d", value):
        score += 1
    if re.search(r"[^A-Za-z0-9]", value):
        score += 1
    level = "약함" if score <= 2 else ("보통" if score <= 4 else "강함")
    return {
        "valid": score >= 3,
        "message": "비밀번호 강도: {} (점수 {}/6)".format(level, score),
        "detail": {"score": score, "level": level},
    }


VALIDATORS = {
    "email": check_email,
    "phone_kr": check_phone_kr,
    "korean_name": check_korean_name,
    "hex_color": check_hex_color,
    "credit_card": check_credit_card,
    "business_no": check_business_no,
    "ip_address": check_ip_address,
    "date": check_date,
    "url": check_url,
    "password_strength": check_password_strength,
}


def validate(kind, value):
    """kind 에 해당하는 검증기를 찾아 실행한다."""
    fn = VALIDATORS.get(kind)
    if fn is None:
        return {"valid": False, "message": "알 수 없는 검증 종류: {}".format(kind), "detail": {}}
    return fn(str(value if value is not None else ""))
