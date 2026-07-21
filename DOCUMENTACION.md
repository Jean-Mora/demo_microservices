# Documentación del demo — qué construimos y por qué

Sandbox de **Arquitectura Empresarial** (PUCE, 2026-01, paralelo 1473).

Este documento explica, de principio a fin, el sistema completo: los dos microservicios,
cómo se autentican contra AWS Cognito, cómo el reverse proxy los une bajo un solo puerto,
cómo se empaquetan con Docker y cómo quedó organizado el repositorio.

---

## 1. La idea general

Partimos de dos microservicios independientes, escritos en Kotlin + Spring Boot, que **no se
conocen entre sí** y **no comparten base de datos**. Lo único que comparten es la confianza en
un tercero: el **User Pool de AWS Cognito** que emite los tokens.

```
                        ┌──────────────────────────┐
                        │      AWS Cognito         │
                        │  (User Pool + Hosted UI) │
                        └────────────┬─────────────┘
                                     │ emite JWT (access_token / id_token)
                                     │ publica JWKS (llaves públicas)
                                     ▼
   cliente ──► http://localhost:8888 ──►  nginx (reverse proxy)
                                            │
                        ┌───────────────────┴───────────────────┐
                        │                                       │
                  /users │                                       │ /diary
                        ▼                                       ▼
            ┌───────────────────────┐              ┌────────────────────────┐
            │  users-microservice   │              │  diary-microservice    │
            │  Spring Boot :8686    │              │  Spring Boot :8989     │
            │  H2 en memoria        │              │  H2 en memoria         │
            │  (perfiles de usuario)│              │  (entradas de diario)  │
            └───────────────────────┘              └────────────────────────┘
```

Cada servicio valida el token **por su cuenta**, descargando las llaves públicas (JWKS) del
issuer configurado. No hay una llamada de red entre servicios para "preguntar si el token es
válido": eso es lo que hace escalable el patrón de *Resource Server*.

---

## 2. Los componentes

| Componente | Carpeta | Puerto interno | Ruta pública | Rol |
|---|---|---|---|---|
| users | [users/](users/) | 8686 | `/users` | Asocia un `cognitoId` con el perfil del usuario en *nuestra* BD |
| diary | [diary_auth_practice/](diary_auth_practice/) | 8989 | `/diary` | Entradas de diario privadas por usuario |
| reverse proxy | [nginx/](nginx/) | 80 → 8888 | `/` | Única puerta de entrada al sistema |

---

## 3. Microservicio `users` — identidad propia vs. identidad de Cognito

**El problema que resuelve:** Cognito sabe quién eres (te da un `sub`, un UUID), pero no sabe
nada de tu negocio: no guarda tu teléfono, tu dirección, ni tus preferencias. Este micro es el
puente entre el identificador de Cognito y los datos que **sí** son de la aplicación.

La entidad es deliberadamente mínima ([User.kt](users/src/main/kotlin/com/pucetec/users/entities/User.kt)):

```kotlin
@Column(unique = true, nullable = false)
val cognitoId: String = "",   // el claim "sub" del token
val name: String = "",
val email: String? = null,
val phone: String? = null,
```

El `cognitoId` es **único**: un usuario de Cognito ↔ un perfil en este micro.

### Endpoints

| Método | Endpoint | De dónde sale el usuario |
|---|---|---|
| `POST` | `/api/users/me` | **del token** (`jwt.subject`) |
| `GET` | `/api/users/me` | **del token** |
| `PUT` | `/api/users/me` | **del token** |
| `GET` | `/api/users` | — (consulta administrativa) |
| `GET` | `/api/users/{id}` | path |
| `GET` | `/api/users/cognito/{cognitoId}` | path (útil para otros microservicios) |
| `DELETE` | `/api/users/{id}` | path |

La clave está en los tres primeros: el cliente **nunca envía su propio id**. Se lee de
`@AuthenticationPrincipal jwt: Jwt` → `jwt.subject`
([UserController.kt](users/src/main/kotlin/com/pucetec/users/controllers/UserController.kt)).
Si el id viniera en el body, cualquiera podría escribir el de otro.

### Errores propios

`GlobalExceptionHandler` traduce las excepciones de dominio a códigos HTTP:
`UserNotFoundException` → 404, `DuplicateCognitoIdException` → 409, `BlankNameException` → 400.

---

## 4. Microservicio `diary` — autorización SIN roles

Este es el micro con la lección más importante del demo, y tiene su propio detalle en
[diary_auth_practice/README.md](diary_auth_practice/README.md).

> *"Todos los usuarios de este sistema tienen exactamente los mismos permisos. Y aun así,
> ninguno puede leer el diario de otro. Spring Security no tiene absolutamente nada que ver
> con eso."*

### Las tres ideas

**1. El "yo" de una petición lo define el token, no el cliente.**
`GET /entries` no recibe **ningún** parámetro — ni query, ni path, ni body — y aun así sabe qué
entradas devolver. Es imposible de abusar porque no hay nada que manipular.

**2. Spring Security NO te protege de leer datos ajenos.**
Verifica firma, emisor y expiración del token, y dice "adelante". Saber **de quién es la fila 1**
no es su trabajo. Toda la autorización de la app vive en un `if` de tres líneas, escrito **una
sola vez**, en [EntryService.kt](diary_auth_practice/src/main/kotlin/com/pucetec/diary/services/EntryService.kt):

```kotlin
private fun findMineOrThrow(id: Long, author: String): Entry {
    val entry = entryRepository.findById(id)
        .orElseThrow { EntryNotFoundException("No existe la entrada con id $id") }
    if (entry.author != author) throw NotYourEntryException("La entrada $id no es tuya")
    return entry
}
```

Borrar ese `if` produce un **IDOR** (*Insecure Direct Object Reference*), el #1 del OWASP Top 10.
Compila, los tests del camino feliz pasan, y Beto lee el diario de Ana con un `200`.

**3. El email no está donde crees.**
`GET /me` devuelve `"email": null` a propósito: el email viaja en el **`id_token`**, no en el
**`access_token`** que valida el backend. El access token es una *llave* (permisos); el id token
es una *cédula* (identidad).

### Quién pone cada código HTTP

| Código | Significa | ¿Quién lo pone? |
|---|---|---|
| `401` | *No sé quién eres* | **Spring Security** |
| `403` | *Sé quién eres… y esa entrada no es tuya* | **tu código, en el service** |
| `404` | *Esa entrada no existe* | **tu código, en el service** |

---

## 5. Seguridad: los dos `SecurityConfig`

Ambos servicios son **OAuth2 Resource Servers**. La configuración cabe en unas pocas líneas
porque el trabajo pesado lo hace Spring a partir del `issuer-uri`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_yzwNALI2A
```

Con esa sola línea Spring descarga el JWKS del User Pool y, en cada request, verifica
**firma, emisor y expiración** del `Bearer` token. No hay secreto compartido ni llamada extra.

Diferencias entre los dos:

| | `users` | `diary` |
|---|---|---|
| Rutas públicas | `/h2-console/**` (solo para clase) | **ninguna** — todo exige token |
| CSRF | deshabilitado (API stateless) | deshabilitado |
| `frameOptions` | `sameOrigin` (la consola H2 usa `<iframe>`) | — |
| Roles / `hasRole` | no | no |

Ninguno de los dos usa `JwtAuthenticationConverter`: ese converter solo existe para traducir
`cognito:groups` → `ROLE_...`, y solo sirve si vas a usar `hasRole(...)`. Aquí no hay roles.

---

## 6. El reverse proxy nginx

Un solo puerto público (`8888`) para todo el sistema. El cliente nunca habla directamente con
un microservicio ([nginx/nginx.conf](nginx/nginx.conf)):

```nginx
location /users { proxy_pass http://users-microservice:8686; include /etc/nginx/proxy_headers.conf; }
location /diary { proxy_pass http://diary-microservice:8989; include /etc/nginx/proxy_headers.conf; }
```

Tres detalles que hacen que esto funcione de verdad:

1. **`resolver 127.0.0.11`** — es el DNS interno de Docker. Sin él, nginx resuelve los nombres
   de los contenedores **una sola vez al arrancar** y se queda con una IP vieja si el contenedor
   se reinicia.

2. **`SERVER_SERVLET_CONTEXT_PATH`** (en el `docker-compose.yml`) — cada app se monta bajo su
   propio prefijo (`/users`, `/diary`), así que el path que llega del proxy coincide con el que
   la app espera. No hace falta reescribir la URL en nginx.

3. **[proxy_headers.conf](nginx/proxy_headers.conf) + `SERVER_FORWARD_HEADERS_STRATEGY=FRAMEWORK`** —
   el proxy reenvía `Host`, `X-Real-IP`, `X-Forwarded-For/Proto/Host`, y Spring los honra. Sin
   esto, cualquier URL que la app genere (redirects, links) apuntaría al puerto interno y no al
   `8888` que el cliente realmente ve.

`GET /` devuelve un mensaje de texto plano indicando a dónde ir.

---

## 7. Docker

Cada servicio tiene un `Dockerfile` **multi-stage** idéntico en estructura:

```dockerfile
FROM eclipse-temurin:21-jdk AS build
COPY gradlew ./ ; COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew --no-daemon dependencies      # ← capa cacheada: solo cambia si cambian las deps
COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jdk AS runtime
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

El truco importante es **bajar las dependencias antes de copiar `src/`**: mientras solo cambies
código, Docker reutiliza la capa de dependencias y el build es mucho más rápido.

En el [docker-compose.yml](docker-compose.yml) los microservicios usan `expose` (no `ports`):
son alcanzables **solo desde la red interna de Docker**. El único puerto publicado al host es el
`8888` del proxy.

---

## 8. Cómo levantarlo y probarlo

```bash
docker compose up --build
```

- http://localhost:8888/ → mensaje del sandbox
- http://localhost:8888/users/api/users
- http://localhost:8888/diary/entries

Sin `Authorization: Bearer <access_token>` todo responde `401`. Los tokens se obtienen del
Hosted UI de Cognito. Las colecciones de Postman ya traen las peticiones armadas:

- [users.postman_collection.json](users/users.postman_collection.json)
- [diary.postman_collection.json](diary_auth_practice/diary.postman_collection.json)

Sin Docker:

```bash
cd users && ./gradlew bootRun               # :8686
cd diary_auth_practice && ./gradlew bootRun # :8989
```

Tests (no requieren AWS: en `diary` los JWT se simulan con `jwt()` de `spring-security-test`,
y en `users` las pruebas son de service con mocks):

```bash
cd users && ./gradlew test
cd diary_auth_practice && ./gradlew test
```

---

## 9. Cómo quedó el repositorio

Antes, `users/` y `diary_auth_practice/` eran **dos repositorios git independientes** con sus
propios remotes (`jsaavedra815/users` y `jsaavedra815/diary_auth_practice`). Para el demo se
consolidó todo en un **monorepo**:

- Se eliminaron los `.git` internos de cada subproyecto → el código quedó como archivos normales
  dentro de este repo. Los repos originales en GitHub siguen intactos.
- Un `.gitignore` en la raíz excluye artefactos de build (`build/`, `.gradle/`, `.kotlin/`),
  configuración de IDE y `.claude/`. Se versiona solo código fuente (~66 MB de artefactos
  quedaron fuera).
- Se conservan los `gradle-wrapper.jar` de cada subproyecto para que `./gradlew` funcione en
  cualquier máquina sin instalar Gradle.

Estructura final:

```
demo_microservices/
├── docker-compose.yml          orquestación de los 3 contenedores
├── nginx/                      nginx.conf + proxy_headers.conf
├── users/                      microservicio de perfiles (:8686)
│   └── guia_microservicio_users.md
├── diary_auth_practice/        microservicio de diario (:8989)
│   └── README.md               la clase de autorización sin roles
├── README.md
└── DOCUMENTACION.md            este archivo
```

---

## 10. Los conceptos que deja el demo

| Concepto | Dónde se ve |
|---|---|
| **Resource Server** / validación de JWT por JWKS | `SecurityConfig` + `issuer-uri` de ambos servicios |
| **Identidad desde el token, nunca desde el cliente** | `jwt.subject` en users, `jwt.username()` en diary |
| **access_token ≠ id_token** | `MeController` (`email` → `null`) |
| **Autorización de datos ≠ autenticación** | `EntryService.findMineOrThrow()` |
| **IDOR / Broken Access Control (OWASP #1)** | el `if` que no se puede borrar, y sus tests |
| **API Gateway / punto único de entrada** | nginx + `expose` en lugar de `ports` |
| **Servicios autónomos** | una BD por servicio, sin FKs cruzadas |
| **Build reproducible** | Dockerfile multi-stage + Gradle wrapper |
