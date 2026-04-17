"""
access_policy.py — Admin access guard.

By default ALL access is OPEN so students and admins can log in from any device
or network without any configuration.

To activate restrictions, set these Railway environment variables:
  ADMIN_ALLOWED_CLIENT_TYPES  → comma-separated list e.g. "android-app,web-portal"
  ADMIN_ALLOWED_NETWORKS      → comma-separated CIDRs  e.g. "192.168.1.0/24"
  ADMIN_ALLOWED_DEVICE_IDS    → comma-separated device IDs from the Android admin app

Leave ALL of them unset (the default) to allow unrestricted login from anywhere.
"""

import ipaddress
import os

from fastapi import HTTPException, Request

from app.models.user import User


def _parse_csv_env(name: str) -> list[str]:
    raw = os.getenv(name)
    if raw is None:
        return []
    return [item.strip() for item in raw.split(",") if item.strip()]


def _parse_networks(values: list[str]) -> list:
    networks = []
    for value in values:
        try:
            if "/" in value:
                networks.append(ipaddress.ip_network(value, strict=False))
            else:
                ip = ipaddress.ip_address(value)
                suffix = "/32" if ip.version == 4 else "/128"
                networks.append(ipaddress.ip_network(f"{value}{suffix}", strict=False))
        except ValueError:
            continue
    return networks


def enforce_admin_access(request: Request, user: User) -> None:
    """
    Enforces access policy for ADMIN users only.
    Students always pass through freely.

    If NO environment variables are configured → OPEN (anyone can log in).
    Configure variables on Railway to lock down admin access.
    """
    if user.role != "ADMIN":
        # Students are never restricted
        return

    allowed_networks = _parse_networks(_parse_csv_env("ADMIN_ALLOWED_NETWORKS"))
    allowed_device_ids = set(_parse_csv_env("ADMIN_ALLOWED_DEVICE_IDS"))
    allowed_client_types = set(
        t.lower() for t in _parse_csv_env("ADMIN_ALLOWED_CLIENT_TYPES")
    )

    # If no restrictions are configured at all → open access for admins too
    if not allowed_networks and not allowed_device_ids and not allowed_client_types:
        return

    # ── network allowlist check ──────────────────────────────────────────────
    forwarded_for = request.headers.get("x-forwarded-for", "")
    client_ip = forwarded_for.split(",")[0].strip() if forwarded_for else (
        request.client.host if request.client else None
    )
    if allowed_networks and client_ip:
        try:
            parsed = ipaddress.ip_address(client_ip)
            if any(parsed in net for net in allowed_networks):
                return
        except ValueError:
            pass

    # ── client-type allowlist check ──────────────────────────────────────────
    client_type = request.headers.get("X-KIWI-Client-Type", "").strip().lower()
    if allowed_client_types:
        if "*" in allowed_client_types or client_type in allowed_client_types:
            return
        raise HTTPException(
            status_code=403,
            detail=(
                f"Admin access is restricted. Client '{client_type}' is not in the "
                f"allowed list: {', '.join(sorted(allowed_client_types))}. "
                "Set ADMIN_ALLOWED_CLIENT_TYPES on Railway to adjust."
            ),
        )

    # ── device-ID allowlist check ────────────────────────────────────────────
    device_id = request.headers.get("X-KIWI-Device-ID", "").strip()
    if allowed_device_ids:
        if device_id in allowed_device_ids:
            return
        raise HTTPException(
            status_code=403,
            detail=(
                "Admin access is restricted to trusted devices. "
                "Your device ID is not in ADMIN_ALLOWED_DEVICE_IDS."
            ),
        )
