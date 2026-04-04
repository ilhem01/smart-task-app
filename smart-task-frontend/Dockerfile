FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund
COPY . .
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
# Only inject VITE_API_URL when set. Empty = use runtime resolution in api-url.ts (same host as SPA :8080).
RUN if [ -n "$VITE_API_URL" ]; then printf 'VITE_API_URL=%s\n' "$VITE_API_URL" > .env.production; fi \
  && npm run build -- --configuration production

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/smart-task-frontend /usr/share/nginx/html
EXPOSE 80
