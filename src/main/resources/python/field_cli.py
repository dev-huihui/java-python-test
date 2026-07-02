"""백엔드(ProcessBuilder)에서 다양한 검증기를 호출하는 진입점.

stdin 으로 {"kind": ..., "value": ...} JSON 을 받아
validators.validate(kind, value) 결과를 stdout 으로 JSON 출력한다.
"""
import sys
import json

from validators import validate

# Windows 파이프 기본 인코딩(CP949) 대신 UTF-8 로 고정
sys.stdin.reconfigure(encoding="utf-8")
sys.stdout.reconfigure(encoding="utf-8")


def main():
    raw = sys.stdin.read()
    data = json.loads(raw) if raw.strip() else {}
    result = validate(data.get("kind"), data.get("value"))
    sys.stdout.write(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
