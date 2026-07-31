FROM node:20-alpine AS build
WORKDIR /app
RUN apk add --no-cache ffmpeg
COPY package.json package-lock.json* ./
RUN npm install --include=dev
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
ARG PDE_FRONTEND_VERSION_ID=unknown
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.d/ /docker-entrypoint.d/
ENV PDE_BACKEND_UPSTREAM=pde-platform-backend:8096
ENV PDE_FRONTEND_VERSION_ID=${PDE_FRONTEND_VERSION_ID}
RUN printf '{"versionId":"%s"}\n' "${PDE_FRONTEND_VERSION_ID}" > /usr/share/nginx/html/pde-image-version.json
EXPOSE 80
