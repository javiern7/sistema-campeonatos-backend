# Backend local con Docker Compose

## 1. Preparar variables de entorno

```powershell
Copy-Item .env.example .env
```

## 2. Levantar servicios

```powershell
docker compose up --build
```

Servicios:

- Backend: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`
- Health: `http://localhost:8080/api/actuator/health`
- PostgreSQL: `localhost:5433`
- Adminer: `http://localhost:8081`

## 3. Credenciales de prueba

- Usuario: `devadmin`
- Password: `admin123`

El usuario se inserta mediante Flyway en `V7__seed_local_dev_admin.sql`.

## 4. Probar CRUD de tournament

Ejemplo `POST /api/tournaments`

```bash
curl -X POST "http://localhost:8080/api/tournaments" \
  -u devadmin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "sportId": 1,
    "name": "Liga Apertura",
    "seasonName": "2026",
    "format": "LEAGUE",
    "status": "DRAFT",
    "description": "Primer torneo de prueba local",
    "startDate": "2026-04-01",
    "endDate": "2026-06-30",
    "registrationOpenAt": "2026-03-20T08:00:00Z",
    "registrationCloseAt": "2026-03-31T23:59:59Z",
    "maxTeams": 12,
    "pointsWin": 3,
    "pointsDraw": 1,
    "pointsLoss": 0
  }'
```

Listar torneos:

```bash
curl -X GET "http://localhost:8080/api/tournaments?page=0&size=20&sort=id,desc" \
  -u devadmin:admin123
```

Obtener uno:

```bash
curl -X GET "http://localhost:8080/api/tournaments/1" \
  -u devadmin:admin123
```

Actualizar:

```bash
curl -X PUT "http://localhost:8080/api/tournaments/1" \
  -u devadmin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "sportId": 1,
    "name": "Liga Apertura Actualizada",
    "seasonName": "2026",
    "format": "LEAGUE",
    "status": "OPEN",
    "description": "Torneo ajustado para pruebas",
    "startDate": "2026-04-01",
    "endDate": "2026-06-30",
    "registrationOpenAt": "2026-03-20T08:00:00Z",
    "registrationCloseAt": "2026-03-31T23:59:59Z",
    "maxTeams": 12,
    "pointsWin": 3,
    "pointsDraw": 1,
    "pointsLoss": 0
  }'
```

Eliminar:

```bash
curl -X DELETE "http://localhost:8080/api/tournaments/1" \
  -u devadmin:admin123
```

## 5. Datos útiles ya sembrados

La migracion `V6__seed_roles_admin_sports.sql` ya deja deportes base:

- `FOOTBALL`
- `FUTSAL`
- `BASKETBALL`
- `VOLLEYBALL`

## 6. Comandos utiles

Ver logs:

```powershell
docker compose logs -f backend
```

Recrear limpio:

```powershell
docker compose down -v
docker compose up --build
```
