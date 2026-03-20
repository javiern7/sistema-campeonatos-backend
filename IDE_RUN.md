# Levantar backend desde IDE

## 1. Asegurar PostgreSQL limpio en Docker

```powershell
docker compose down -v
docker compose up -d postgres
```

Esto debe levantar PostgreSQL con:

- host: `localhost`
- puerto: `5433`
- base: `multisport_db`
- usuario: `multisport_user`
- password: `multisport_pass`

## 2. Verificar que PostgreSQL realmente quedo con esas credenciales

Si tienes `psql` instalado:

```powershell
$env:PGPASSWORD="multisport_pass"
psql -h localhost -p 5433 -U multisport_user -d multisport_db -c "select current_user, current_database();"
```

Si eso falla, el problema no es Spring Boot ni el IDE: el problema sigue siendo la base o el puerto.

## 3. Run Configuration en IntelliJ

- Main class: `com.multideporte.backend.MultisportBackendApplication`
- Active profiles: `ide`
- Working directory: la raiz del proyecto

No necesitas variables de entorno para la conexion si usas el perfil `ide`, porque ya estan en `application-ide.yml`.

## 4. Alternativa si prefieres variables explicitas en IDE

Puedes usar el perfil `local` y setear:

```text
DB_URL=jdbc:postgresql://localhost:5433/multisport_db;DB_USERNAME=multisport_user;DB_PASSWORD=multisport_pass
```

Pero recuerda: `.env` no se inyecta automaticamente en IntelliJ.

## 5. Señales de que todo ya arranco bien

Debes poder abrir:

- `http://localhost:8080/api/actuator/health`
- `http://localhost:8080/api/swagger-ui/index.html`

## 6. Si todavia falla

Revisa estas dos causas primero:

1. Otro PostgreSQL local esta ocupando `5432`.
2. El contenedor viejo no se elimino realmente y sigue con otra password.

## 7. Verificacion rapida del puerto 5433 en Windows

```powershell
netstat -ano | findstr :5433
```

Si aparece un proceso ajeno a Docker, probablemente Spring esta entrando a otra base distinta.
