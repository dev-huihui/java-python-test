"""백엔드(ProcessBuilder)에서 호출하는 진입점.

stdin 으로 JSON 을 받아 validation.validate() 결과를 stdout 으로 JSON 출력한다.
"""
import sys
import json

from validation import validate

# Windows 에서는 stdin/stdout 이 파이프일 때 기본 인코딩이 시스템 로케일(예: CP949)이라
# Java 와 주고받는 JSON 을 UTF-8 로 고정한다.
sys.stdin.reconfigure(encoding="utf-8")
sys.stdout.reconfigure(encoding="utf-8")


def main():
    raw = sys.stdin.read()
    data = json.loads(raw) if raw.strip() else {}
    result = validate(data)
    sys.stdout.write(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
