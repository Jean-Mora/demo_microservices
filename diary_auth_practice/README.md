# diary — Autorización SIN roles 📔

Proyecto de la clase **"Mi Diario Privado"** — Arquitectura Empresarial, PUCE.
Kotlin + Spring Boot 4 + H2 en memoria.

> **La frase de la clase:**
> *"Todos los usuarios de este sistema tienen exactamente los mismos permisos. Y aun así, ninguno puede
> leer el diario de otro. Spring Security no tiene absolutamente nada que ver con eso."*

## Cómo correrlo

```bash
./gradlew bootRun      # http://localhost:8787
./gradlew test         # 15 tests, sin necesidad de AWS
```

La consola de H2 queda en `http://localhost:8787/h2-console`
(JDBC URL `jdbc:h2:mem:diarydb`, usuario `admin`, clave `admin`).

> ⚠️ Todos los endpoints exigen token. **No hay nada público.** Sin `Authorization: Bearer <access_token>`
> de Cognito, todo devuelve `401` — incluida la consola de H2.

## Endpoints

| Método | Endpoint | Sin token | Con **tu** token | Con el token de **otro** |
|---|---|---|---|---|
| `GET` | `/me` | `401` | `200` | `200` (sus datos) |
| `GET` | `/entries` | `401` | `200` (**las tuyas**) | `200` (**las suyas**) |
| `POST` | `/entries` | `401` | `201` | `201` |
| `GET` | `/entries/{id}` | `401` | `200` | **`403`** |
| `PUT` | `/entries/{id}` | `401` | `200` | **`403`** |
| `DELETE` | `/entries/{id}` | `401` | `204` | **`403`** |

Fíjate que en toda esa tabla **no aparece la palabra "rol"**. Ni una vez.

## Las tres ideas de la clase

**1. El "yo" de una petición lo define el token, no el cliente.**
`GET /entries` no recibe **ningún** parámetro — ni query, ni path, ni body — y aun así sabe qué entradas
devolver. Es imposible de abusar porque **no hay nada que manipular**. La mejor forma de prevenir un bug
es borrar el lugar donde el bug podía existir.

**2. Spring Security NO te protege de leer datos ajenos.**
Verifica la firma, el emisor y la expiración del token, y dice "adelante". Saber **de quién es la fila 1**
no es su trabajo: es el tuyo. Ese chequeo vive en `EntryService.findMineOrThrow()`, y es un `if` de tres
líneas escrito **una sola vez**.

**3. El email no está donde crees.**
`GET /me` devuelve `"email": null`. No es un bug: el email viaja en el **`id_token`**, no en el
**`access_token`**. El access token es una *llave* (permisos); el id token es una *cédula* (identidad).

## Los tres códigos, y quién pone cada uno

| Código | Significa | ¿Quién lo pone? |
|---|---|---|
| `401` | *No sé quién eres* | **Spring Security** |
| `403` | *Sé quién eres… y esa entrada no es tuya* | **TÚ**, en el service |
| `404` | *Esa entrada no existe, ni para ti ni para nadie* | **TÚ**, en el service |

En esta app **no existe ni un solo `403` por rol** — porque no hay roles. **Todos los `403` son tuyos.**
Si no los escribes, no existen, y Beto lee el diario de Ana.

## El bug que hay que introducir en clase (a propósito)

En `EntryService.findMineOrThrow()`, **borra el `if` del dueño** y deja solo el `findById(id)` — que es lo
que JPA te da gratis y lo que cualquiera escribiría. Compila, los tests del camino feliz pasan, y de pronto:

```
GET /entries/1   (con el token de BETO)   → 200 💀   el diario completo de Ana
```

Eso se llama **IDOR** (*Insecure Direct Object Reference*), y vive en el #1 del OWASP Top 10
(*Broken Access Control*). Los tests `NO puedo leer/editar/borrar la entrada de otro usuario` existen
justamente para que ese bug **no pueda volver**.

## Cognito

Este proyecto valida tokens contra el User Pool de pruebas configurado en `application.yaml`.

| Paso | Qué haces |
|---|---|
| **Grupos** | **Ninguno.** Hoy no hay roles 🎉 |
| Usuarios | `ana` y `beto`, con contraseña permanente y **email** |
| Tokens | Hosted UI → copia el **`access_token`** *y* el **`id_token`** de cada uno |

Necesitas los dos tokens para el Checkpoint 6: se abren lado a lado en [jwt.io](https://jwt.io) y se ve
que el `email` **solo** aparece en el `id_token`.

## Estructura

```
config/       SecurityConfig — 5 líneas. Sin hasRole. Sin JwtAuthenticationConverter.
controllers/  EntryController (el username sale del JWT) · MeController (el misterio del email)
services/     EntryService — aquí vive TODA la autorización de la app
repositories/ EntryRepository — findByAuthor...: para listar, nunca findAll()
entities/     Entry — el `author` es un varchar, NO una FK. No hay tabla `users`.
```
