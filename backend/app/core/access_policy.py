import ipaddress
import os
from functools import lru_cache

from fastapi import HTTPException, Request

from app.models.user import User


def _parse_csv_env(name: str) -> list[str]:
    raw = os.getenv(name)
    if raw is None:
        return []
    return [item.strip() for item in raw.split(",") if item.strip()]


def _parse_networks(values: list[str]) -> list[ipaddress._BaseNetwork]:
    networks: list[ipaddress._BaseNetwork] = []
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


@lru_cache(maxsize=1)
def _policy() -> dict:
    configured_client_types = _parse_csv_env("ADMIN_ALLOWED_CLIENT_TYPES")
    configured_networks = _parse_csv_env("ADMIN_ALLOWED_NETWORKS")
    configured_device_ids = _parse_csv_env("ADMIN_ALLOWED_DEVICE_IDS")

    return {
        "allowed_client_types": {
            client_type.lower()
            for client_type in (configured_client_types or ["android-app"])
        },
        "allowed_networks": _parse_networks(configured_networks),
        "allowed_device_ids": set(configured_device_ids),
    }


def resolve_client_ip(request: Request) -> str | None:
    forwarded_for = request.headers.get("x-forwarded-for", "")
    if forwarded_for:
        first_hop = forwarded_for.split(",")[0].strip()
        if first_hop:
            return first_hop
    return request.client.host if request.client else None


def _ip_is_allowed(client_ip: str | None, allowed_networks: list[ipaddress._BaseNetwork]) -> bool:
    if not client_ip or not allowed_networks:
        return False
    try:
        parsed_ip = ipaddress.ip_address(client_ip)
    except ValueError:
        return False
    return any(parsed_ip in network for network in allowed_networks)


def _client_type_is_allowed(client_type: str, allowed_client_types: set[str]) -> bool:
    if not allowed_client_types:
        return True
    return "*" in allowed_client_types or client_type.lower() in allowed_client_types


def enforce_admin_access(request: Request, user: User) -> None:
    if user.role != "ADMIN":
        return

    policy = _policy()
    client_ip = resolve_client_ip(request)
    client_type = request.headers.get("X-KIWI-Client-Type", "unknown").strip().lower()
    device_id = request.headers.get("X-KIWI-Device-ID", "").strip()

    if _ip_is_allowed(client_ip, policy["allowed_networks"]):
        return

    if not _client_type_is_allowed(client_type, policy["allowed_client_types"]):
        allowed = ", ".join(sorted(policy["allowed_client_types"])) or "trusted clients"
        raise HTTPException(
            status_code=403,
            detail=(
                f"Admin access is restricted. This request came from '{client_type}'. "
                f"Use an approved admin client ({allowed}) or an allowlisted network."
            ),
        )

    if policy["allowed_device_ids"] and device_id not in policy["allowed_device_ids"]:
        raise HTTPException(
            status_code=403,
            detail=(
                "Admin access is restricted to trusted devices. "
                "Configure ADMIN_ALLOWED_DEVICE_IDS with your approved Android device ID."
            ),
        )
