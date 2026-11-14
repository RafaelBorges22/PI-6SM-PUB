# Smart Parking - frontend Web (Kotlin/Wasm + Compose) servido pelo Nginx.
# Build:  docker compose --profile app build frontend

# ---- 1) Compilacao ---------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# O Node.js baixado pelo plugin Kotlin/Wasm precisa da libatomic (ausente na imagem do JDK).
RUN apt-get update && apt-get install -y --no-install-recommends libatomic1 \
    && rm -rf /var/lib/apt/lists/*
COPY . .
# smartparking.semAndroid=true: o container nao tem Android SDK e so precisa do alvo Web.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew --no-daemon -Psmartparking.semAndroid=true \
       :composeApp:wasmJsBrowserDistribution

# ---- 2) Servidor estatico --------------------------------------------------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/composeApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html
EXPOSE 80
