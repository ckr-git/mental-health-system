<!-- Generated: 2026-03-14 | Files scanned: 314 | Token estimate: ~400 -->

# Dependencies Codemap

## Backend (pom.xml)

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.2.0 | Web framework, Security, WebSocket, Validation, Redis |
| MyBatis-Plus | 3.5.5 | ORM + pagination + logical delete |
| Flyway | (managed) | Database schema migration |
| MySQL Connector | (managed) | JDBC driver |
| JJWT | 0.12.3 | JWT token generation/parsing |
| Hutool | 5.8.23 | Utility toolkit |
| FastJSON2 | 2.0.43 | JSON processing |
| Commons FileUpload | 1.5 | File upload handling |
| Commons Math3 | 3.6.1 | Collaborative filtering recommendation |
| Spring Data Redis | (managed) | Redis cache + distributed features |
| Lombok | (managed) | Boilerplate reduction |

## Frontend (package.json)

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Vue | 3.4.0 | UI framework |
| Vue Router | 4.2.5 | Client-side routing |
| Pinia | 2.1.7 | State management |
| Element Plus | 2.5.0 | UI component library |
| Axios | 1.6.2 | HTTP client |
| ECharts + vue-echarts | 5.4.3 / 6.6.1 | Data visualization |
| Day.js | 1.11.10 | Date manipulation |
| Howler | 2.2.4 | Audio playback (meditation/ambient) |
| STOMP.js + SockJS | 2.3.3 / 1.6.1 | WebSocket messaging |
| Vite | 5.0.0 | Build tool + dev server |
| TypeScript | 5.3.0 | Type safety |
| Sass | 1.93.3 | CSS preprocessing |

## External Services

| Service | Usage | Config |
|---------|-------|--------|
| MySQL | Primary data store | application.yml: localhost:3306/mental_health |
| Redis | Cache + distributed lock (optional, disabled by default) | REDIS_ENABLED=false |
| Alibaba DashScope | AI text generation (mock mode default) | AI_MOCK_ENABLED=true |

## Infrastructure Patterns
- Outbox pattern for async event processing (database-backed, no message broker)
- WebSocket/STOMP for real-time notifications
- Flyway for schema migrations (8 migration files)
- BCrypt password hashing via Spring Security
- Vite proxy for API/WS forwarding in development
