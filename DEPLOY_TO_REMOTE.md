# Deployment Instructions for Remote Server (GCP)

## Problem Root Cause
The application was not working due to hardcoded `localhost:8080` URLs in:
1. `.env.production` file (VUE_APP_API_BASE_URL=http://localhost:8080/api)
2. Multiple Vue components (Login.vue, ResetPassword.vue, etc.)

## Solution Applied
1. Created centralized API config (`frontend/src/config/api.config.js`)
2. Updated all components to use centralized config
3. Fixed `.env` files to use relative paths
4. Updated `.dockerignore` to exclude `.env` files from Docker build
5. Configure environment variables via docker-compose instead

## Deployment Steps on Remote Server

### 1. Connect to Remote Server
```bash
ssh your-user@your-gcp-server-ip
cd ~/ads-creative-content
```

### 2. Pull Latest Code
```bash
git pull origin main
```

### 3. Clean Old Docker Images & Containers
```bash
# Stop all containers
docker-compose down

# Remove old frontend images to force rebuild
docker rmi fbads-frontend:latest 2>/dev/null || true
docker rmi ads-creative-content-frontend:latest 2>/dev/null || true

# Clean build cache (optional but recommended)
docker builder prune -f
```

### 4. Rebuild Frontend with Correct Environment Variables
```bash
# Rebuild ONLY frontend (faster than rebuilding everything)
docker-compose build --no-cache frontend

# Or rebuild all services if needed
docker-compose build --no-cache
```

### 5. Start Services
```bash
docker-compose up -d

# Check logs
docker-compose logs -f frontend nginx backend
```

### 6. Verify Deployment

#### Check container status:
```bash
docker ps | grep fbads
```

Expected output:
```
fbads-nginx      Up x minutes   0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp
fbads-frontend   Up x minutes   80/tcp
fbads-backend    Up x minutes   8080/tcp
```

#### Check frontend environment variables in built code:
```bash
# Enter frontend container
docker exec -it fbads-frontend sh

# Check if API URLs are correct (should be /api, NOT localhost:8080)
cd /usr/share/nginx/html/js
grep -r "localhost:8080" . || echo "✅ No localhost:8080 found - GOOD!"

# Exit container
exit
```

#### Test in browser:
1. Open https://linhnh.site
2. Open DevTools → Network tab
3. Click "Đăng nhập bằng Facebook"
4. Verify redirect URL is: `https://linhnh.site/api/auth/oauth2/authorize/facebook` (NOT localhost:8080)

### 7. Troubleshooting

#### If still seeing localhost:8080:

**Check if .env files exist in container:**
```bash
docker exec -it fbads-frontend sh -c "cat /app/.env.production 2>/dev/null || echo 'No .env.production (good)'"
```

If .env files exist in container, rebuild with --no-cache:
```bash
docker-compose down
docker-compose build --no-cache frontend
docker-compose up -d
```

**Check Dockerfile ARG defaults:**
```bash
# Should see: ARG VUE_APP_API_BASE_URL=/api
grep "VUE_APP_API_BASE_URL" frontend/Dockerfile
```

**Check docker-compose args:**
```bash
# Should see: VUE_APP_API_BASE_URL: ${VUE_APP_API_BASE_URL:-/api}
grep "VUE_APP_API_BASE_URL" docker-compose.yml
```

## Important Files Changed

### Frontend files:
- ✅ `frontend/src/config/api.config.js` - NEW centralized config
- ✅ `frontend/src/views/Login.vue` - Uses getApiBaseUrl()
- ✅ `frontend/src/views/ResetPassword.vue` - Uses getApiBaseUrl()
- ✅ `frontend/src/services/api.js` - Fallback to /api
- ✅ `frontend/src/services/apiService.js` - Fallback to /api
- ✅ `frontend/src/services/aiProviderService.js` - Uses getApiBaseUrl()
- ✅ `frontend/Dockerfile` - ARG default = /api
- ✅ `frontend/.dockerignore` - Excludes .env files
- ⚠️ `frontend/.env.production` - Fixed but GITIGNORED (won't affect Docker build)
- ⚠️ `frontend/.env.development` - Fixed but GITIGNORED (won't affect Docker build)

### Backend files:
- ✅ `docker-compose.yml` - OAuth redirect URI and CORS origins

## Architecture After Fix

```
Browser (https://linhnh.site)
    ↓
Request: https://linhnh.site/api/campaigns
    ↓
Nginx Container (port 443/80)
    ↓
proxy_pass → Frontend Container (port 80) for /
proxy_pass → Backend Container (port 8080) for /api/*
    ↓
Vue App or Spring Boot API
```

## Environment Variable Priority

Docker build process:
1. ~~.env.production~~ (EXCLUDED via .dockerignore) ❌
2. Dockerfile ARG (VUE_APP_API_BASE_URL=/api) ✅
3. docker-compose build args ✅
4. Code fallback (`process.env.VUE_APP_API_BASE_URL || '/api'`) ✅

## Quick Verification Commands

```bash
# On remote server:
cd ~/ads-creative-content

# Check git status
git log -1 --oneline

# Check running containers
docker ps | grep fbads

# Check frontend logs for errors
docker logs fbads-frontend --tail 50

# Check nginx logs
docker logs fbads-nginx --tail 50

# Test API connectivity from frontend container
docker exec -it fbads-frontend wget -q -O- http://fbads-backend:8080/actuator/health

# Restart if needed
docker-compose restart frontend nginx
```

## Post-Deployment Checklist

- [ ] All containers running (docker ps)
- [ ] No errors in logs (docker-compose logs)
- [ ] Website loads at https://linhnh.site
- [ ] DevTools Network tab shows requests to `/api/*` (not localhost)
- [ ] Login redirects to `https://linhnh.site/api/auth/oauth2/authorize/facebook`
- [ ] OAuth callback works
- [ ] Dashboard loads after login

## Facebook App Configuration

Ensure Facebook Developer Console has correct OAuth redirect URI:
```
https://linhnh.site/api/auth/oauth2/callback/facebook
```

Remove any localhost URIs from Facebook App settings.

## Notes

- `.env` files are now **ignored by Docker** via `.dockerignore`
- All configuration comes from **Dockerfile ARG** and **docker-compose args**
- This ensures **consistent behavior** across all environments
- No need to manually edit `.env` files on remote server
- Code changes in `frontend/src/**` require rebuild to take effect
