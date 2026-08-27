# Stage 1: build the static assets using the project's own npm scripts -
# no source changes, just `npm run build` (tsc -b && vite build) as-is
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: serve the built dist/ output with nginx. No Node, no source, no
# node_modules in the final image - just the static bundle and the proxy config
FROM nginx:1.31-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
