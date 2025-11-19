# Build stage
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* .npmrc* ./
RUN npm install

COPY . .
RUN npm run build

# Runtime stage
FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 4173
CMD ["nginx", "-g", "daemon off;"]
