# Build stage
FROM node:18-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Remove .env files to prevent Vue CLI from reading them
# This ensures environment variables come from ARG/ENV only
RUN rm -f .env .env.local .env.production .env.development .env.staging || true

# Accept build arguments for environment variables
ARG VUE_APP_API_BASE_URL=/api
ARG VUE_APP_SENTRY_DSN
ARG VUE_APP_ENVIRONMENT=production

# Set environment variables for build
ENV VUE_APP_API_BASE_URL=$VUE_APP_API_BASE_URL
ENV VUE_APP_SENTRY_DSN=$VUE_APP_SENTRY_DSN
ENV VUE_APP_ENVIRONMENT=$VUE_APP_ENVIRONMENT

# Build the application
RUN npm run build

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