FROM node:24.22.0-bookworm-slim AS build
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM nginx:1.28-alpine
COPY infra/nginx.conf /etc/nginx/nginx.conf
COPY infra/nginx.server.conf /etc/nginx/nexus-server.conf
COPY infra/nginx.locations.conf /etc/nginx/nexus-locations.conf
COPY infra/nginx.headers.conf /etc/nginx/nexus-headers.conf
COPY --from=build /build/frontend/dist/nexus-crm/browser /usr/share/nginx/html
USER 101:101
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
ENTRYPOINT ["nginx"]
CMD ["-g", "daemon off;"]
