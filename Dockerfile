FROM node:22-bookworm-slim AS frontend
WORKDIR /src/web/frontend
COPY web/frontend/package*.json ./
RUN npm ci --no-audit --no-fund
COPY web/frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /src
COPY . .
COPY --from=frontend /src/web/src/main/resources/public/ web/src/main/resources/public/
RUN mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true -pl web -am package

FROM eclipse-temurin:21-jre-noble
RUN groupadd --gid 10001 chess && useradd --uid 10001 --gid chess --no-create-home chess \
    && mkdir -p /app /var/lib/chess/stockfish && chown -R chess:chess /app /var/lib/chess
WORKDIR /app
COPY --from=backend --chown=chess:chess /src/web/target/chess-web-1.0-SNAPSHOT.jar ./
COPY --from=backend --chown=chess:chess /src/web/target/lib/ ./lib/
USER chess
ENV CHESS_HOST=0.0.0.0 PORT=8080 JAVA_TOOL_OPTIONS=-Dchess.stockfish.cache=/var/lib/chess/stockfish
EXPOSE 8080
VOLUME ["/var/lib/chess/stockfish"]
ENTRYPOINT ["java", "-jar", "chess-web-1.0-SNAPSHOT.jar"]
