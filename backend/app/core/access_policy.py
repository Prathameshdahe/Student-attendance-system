"""
Admin access guard for the deployed backend.

By default all access is open so students and admins can log in from any device
or network without any extra configuration.

To activate restrictions, set these Render environment variables:
  ADMIN_ALLOWED_CLIENT_TYPES  -> comma-separated list, e.g. "android-app,web-portal"
  ADMIN_ALLOWED_NETWORKS      -> comma-separated CIDRs, e.g. "192.168.1.0/24"
  ADMIN_ALLOWED_DEVICE_IDS    -> comma-separated device IDs from the Android admin app

Leave all three unset to allow unrestricted login from anywhere.
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
    Enforce access policy for admin users only.
    Students always pass through freely.

    If no environment variables are configured, the backend stays open.
    Set the Render environment only when you want to lock down admin access.
    """
    if user.role != "ADMIN":
        return

    allowed_networks = _parse_networks(_parse_csv_env("ADMIN_ALLOWED_NETWORKS"))
    allowed_device_ids = set(_parse_csv_env("ADMIN_ALLOWED_DEVICE_IDS"))
    allowed_client_types = set(
        value.lower() for value in _parse_csv_env("ADMIN_ALLOWED_CLIENT_TYPES")
    )

    if not allowed_networks and not allowed_device_ids and not allowed_client_types:
        return

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

    client_type = request.headers.get("X-KIWI-Client-Type", "").strip().lower()
    if allowed_client_types:
        if "*" in allowed_client_types or client_type in allowed_client_types:
            return
        raise HTTPException(
            status_code=403,
            detail=(
                f"Admin access is restricted. Client '{client_type}' is not in the "
                f"allowed list: {', '.join(sorted(allowed_client_types))}. "
                "Set ADMIN_ALLOWED_CLIENT_TYPES in Render to adjust."
            ),
        )

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
