# Build stage
FROM node:18-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Accept build arguments for environment variables BEFORE copying source
# This ensures they are available when webpack processes the code
ARG VUE_APP_API_BASE_URL=/api
ARG VUE_APP_SENTRY_DSN
ARG VUE_APP_ENVIRONMENT=production

# Set environment variables for build BEFORE copying source
ENV VUE_APP_API_BASE_URL=$VUE_APP_API_BASE_URL
ENV VUE_APP_SENTRY_DSN=$VUE_APP_SENTRY_DSN
ENV VUE_APP_ENVIRONMENT=$VUE_APP_ENVIRONMENT

# Copy source code (.env files are excluded by .dockerignore)
COPY . .

# Double-check: Remove any .env files that might have been copied despite .dockerignore
RUN rm -f .env .env.local .env.production .env.development .env.staging || true

# Verify environment variables are set correctly
RUN echo "Building with VUE_APP_API_BASE_URL=$VUE_APP_API_BASE_URL"

# Build the application
RUN npm run build

# Verify the build output doesn't contain localhost:8080
RUN if grep -r "localhost:8080" /app/dist/js/ 2>/dev/null; then \
      echo "ERROR: Found localhost:8080 in built files!"; \
      exit 1; \
    else \
      echo "SUCCESS: No localhost:8080 found in built files"; \
    fi

# Production stage
FROM nginx:alpine

# Copy built files to nginx
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

RUN chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /var/log/nginx /etc/nginx/conf.d

RUN mkdir -p /tmp && chown -R nginx:nginx /tmp

# Switch to non-root user
USER nginx

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]