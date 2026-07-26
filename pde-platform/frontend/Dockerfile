FROM node:20-alpine AS build
WORKDIR /app
RUN apk add --no-cache ffmpeg
COPY package.json package-lock.json* ./
RUN npm install --include=dev
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.d/ /docker-entrypoint.d/
ENV PDE_BACKEND_UPSTREAM=pde-platform-backend:8096
EXPOSE 80
