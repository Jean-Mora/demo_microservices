# demo_microservices

Sandbox de Arquitectura Empresarial (PUCE, 2026-01): dos microservicios Kotlin/Spring Boot
detrás de un reverse proxy nginx, autenticados con JWT emitidos por AWS Cognito.

## Componentes

| Servicio | Ruta pública | Puerto interno | Descripción |
|---|---|---|---|
| `users` | `/users` | 8686 | CRUD de usuarios (H2 en memoria) |
| `diary_auth_practice` | `/diary` | 8989 | Entradas de diario por usuario autenticado |
| `nginx` | `:8888` | 80 | Reverse proxy que enruta por prefijo |

Ambos servicios actúan como *OAuth2 Resource Server*: validan la firma del JWT
contra el JWKS del User Pool de Cognito (`issuer-uri` en `application.yaml`).

> 📖 **[DOCUMENTACION.md](DOCUMENTACION.md)** — explicación completa: arquitectura, seguridad con
> Cognito, autorización sin roles (IDOR), el reverse proxy, Docker y cómo quedó armado el repo.

## Levantar el entorno

```bash
docker compose up --build
```

- http://localhost:8888/users
- http://localhost:8888/diary

## Desarrollo local (sin Docker)

```bash
cd users && ./gradlew bootRun
cd diary_auth_practice && ./gradlew bootRun
```

## Tests

```bash
cd users && ./gradlew test
cd diary_auth_practice && ./gradlew test
```

## Colecciones Postman

- `users/users.postman_collection.json`
- `diary_auth_practice/diary.postman_collection.json`
