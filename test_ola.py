#!/usr/bin/env python3
"""Self-checks for ola.py's hand-rolled protobuf and address helpers.

Run (from the repo root):  python test_ola.py
These are the parts that silently corrupt if wrong (varints, response decode,
target building, the sh wrapper), so they get a test even though the repo has
no test framework.
"""
import ola


def _resp(status: int, message: str) -> bytes:
    """Build a wire response the way the daemon does: field1=status, field2=msg."""
    raw = message.encode()
    return (ola._vi((1 << 3) | 0) + ola._vi(status)
            + ola._vi((2 << 3) | 2) + ola._vi(len(raw)) + raw)


def test_varint_roundtrip():
    for n in (0, 1, 127, 128, 300, 16384, 1 << 31):
        value, nxt = ola._read_varint(ola._vi(n), 0)
        assert value == n, f"varint {n} -> {value}"
        assert nxt == len(ola._vi(n))


def test_varint_truncated_does_not_crash():
    # high bit set means "more bytes follow", but there are none
    value, nxt = ola._read_varint(b"\x80", 0)
    assert nxt == 1  # returned instead of IndexError


def test_decode_response_roundtrip():
    for status, msg in ((0, ""), (1, "ok"), (255, "line1\nline2"), (1, "spaced path /a b")):
        got_status, got_msg = ola._decode_response(_resp(status, msg))
        assert got_status == status, f"{status} -> {got_status}"
        assert got_msg == msg, f"{msg!r} -> {got_msg!r}"


def test_build_target():
    assert ola.build_target("192.168.43.50", 50005, None) == "192.168.43.50:50005"
    assert ola.build_target("2001:db8::1", 50005, None) == "ipv6:[2001:db8::1]:50005"
    # link-local gets its scope, and % is escaped to %25 for gRPC
    assert ola.build_target("fe80::1", 50005, 6) == "ipv6:[fe80::1%256]:50005"
    # ipv6: prefix and [brackets] are stripped before rebuilding
    assert ola.build_target("ipv6:[2001:db8::2]:50005", 50005, None) == "ipv6:[2001:db8::2]:50005"


def test_build_target_linklocal_needs_scope():
    try:
        ola.build_target("fe80::1", 50005, None)
    except SystemExit:
        return
    raise AssertionError("link-local without scope should raise SystemExit")


def test_sh_wrap_is_reversible():
    import base64
    cmd = 'echo "a b" > /sdcard/My File.txt'
    wrapped = ola.sh_wrap(cmd)
    b64 = wrapped.split("echo${IFS}", 1)[1].split("|", 1)[0]
    assert base64.b64decode(b64).decode() == cmd


if __name__ == "__main__":
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    for t in tests:
        t()
        print(f"ok  {t.__name__}")
    print(f"\n{len(tests)} checks passed.")
