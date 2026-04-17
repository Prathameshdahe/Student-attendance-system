# KIWI Backend Deployment Alternatives

## Best Practical Option
Use:

- Render for the FastAPI backend
- Neon or Render Postgres for the database
- Cloudflare DNS with your own custom domain, for example `api.kiwiattendance.in`

Why this is the best fit:

- your app becomes reachable through a normal custom domain instead of a provider-generated hostname
- Android and the admin portal can both point to one stable API URL
- Cloudflare-backed DNS is usually much more reliable across different mobile and ISP networks

## Option A: Render + Custom Domain

This repo now includes:

- `render.yaml`
- `backend/Dockerfile`
- `backend/.env.example`

### Steps

1. Push the repo to GitHub when you are ready.
2. In Render, create a new Blueprint from this repository.
3. When Render asks for `DATABASE_URL`, provide:
   - a Neon Postgres connection string, or
   - a Render Postgres connection string
4. Let Render generate `SECRET_KEY`.
5. After deploy, add a custom domain such as `api.kiwiattendance.in`.
6. Put that domain behind Cloudflare DNS.
7. Point Android and the portal to the custom domain instead of `up.railway.app`.

### Important

- `starter` is used in `render.yaml` so the API stays awake.
- If you change to `free`, expect cold starts and sleep behavior.

## Option B: Fly.io + Custom Domain

Fly.io is also a strong choice if you want:

- Docker-based deploys
- a stable `fly.dev` hostname
- your own custom domain later

The Dockerfile in `backend/Dockerfile` is also suitable for Fly.io.

## Option C: VPS + Cloudflare Tunnel

If you want maximum control:

- rent a small VPS
- run the Docker container yourself
- use Cloudflare Tunnel to expose `api.yourdomain.com`

This avoids provider subdomain issues entirely, but it is more manual to maintain.

## Recommended Final API URL

Do not use a provider hostname long-term.

Use something like:

```text
https://api.kiwiattendance.in
```

Then set:

- Android build property `kiwiBackendUrl`
- Admin portal `Backend API` field

to that single custom domain.
