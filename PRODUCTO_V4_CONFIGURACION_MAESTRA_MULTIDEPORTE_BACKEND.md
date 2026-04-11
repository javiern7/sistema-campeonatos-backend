# Producto Version 4 - Configuracion maestra multideporte - Backend

## Alcance cerrado

Este hilo backend cubre solo configuracion maestra multideporte:

- maestro de deportes sobre la entidad existente `sport`;
- posiciones por deporte como catalogo hijo `sport_position`;
- catalogo de formatos de competencia derivado del enum existente `TournamentFormat`;
- validaciones focalizadas de codigo, unicidad y restricciones de borrado.

No se reabren autenticacion, sesiones, permisos ni shell. No se incluyen reporterias, eventos de partido, estadisticas ni UX/UI avanzado.

## Modelo reutilizado

- `sport`: ya existia desde `V2__catalog_sport.sql`; se endurecio la entidad Java para mapear sus columnas reales: `team_based`, `max_players_on_field`, `score_label`, `created_at`.
- `TournamentFormat`: ya existia como enum usado por torneos; se expone como catalogo de lectura sin duplicar tabla.
- `sport_position`: nueva tabla dependiente de `sport`, con codigo unico por deporte y orden unico por deporte.

## Endpoints backend

Base protegida por los permisos existentes:

- lectura: `sports:read`;
- escritura: `configuration:basic:manage`, reutilizado deliberadamente para no abrir gobierno de permisos en este bloque.

Endpoints, relativos al prefijo local `/api`:

- `POST /sports`: crea deporte.
- `GET /sports?activeOnly=true`: lista deportes.
- `GET /sports/{id}`: obtiene deporte por id.
- `PUT /sports/{id}`: actualiza deporte.
- `DELETE /sports/{id}`: elimina deporte solo si no tiene torneos ni posiciones.
- `GET /sports/{sportId}/positions?activeOnly=true`: lista posiciones del deporte.
- `POST /sports/{sportId}/positions`: crea posicion.
- `PUT /sports/{sportId}/positions/{positionId}`: actualiza posicion.
- `DELETE /sports/{sportId}/positions/{positionId}`: elimina posicion.
- `GET /sports/competition-formats`: lista `LEAGUE`, `GROUPS_THEN_KNOCKOUT`, `KNOCKOUT`.

## Validaciones

- `code` de deporte y posicion se normaliza a mayusculas.
- `code` acepta letras, numeros y guion bajo.
- `sport.code` es unico global.
- `sport_position.code` es unico por deporte.
- `sport_position.display_order` es unico por deporte y mayor a cero.
- `max_players_on_field` debe ser mayor a cero cuando se informa.
- no se elimina un deporte si tiene torneos asociados.
- no se elimina un deporte si tiene posiciones configuradas.

## Riesgos y decisiones

- Borrado de deportes: queda restringido para proteger torneos existentes; para catalogos semilla se recomienda desactivar con `active=false`.
- Permisos: se reutiliza `configuration:basic:manage`. Si el maestro decide gobierno fino despues, el siguiente bloque puede crear `sports:manage` sin cambiar el contrato funcional.
- Posiciones en rosters: el modelo actual de roster conserva `position_name` como texto. Este bloque no migra rosters a FK para evitar impacto en inscripciones y partido.
- Formatos: no se crea tabla nueva porque `TournamentFormat` ya es contrato operacional activo.

## Pruebas ejecutadas

```powershell
mvn "-Dmaven.repo.local=.m2repo" "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=SportValidatorTest,SportServiceImplTest" test
mvn "-Dmaven.repo.local=.m2repo" "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=SecurityContractWebMvcTest" test
mvn "-Dmaven.repo.local=.m2repo" "-Dmaven.compiler.useIncrementalCompilation=false" -DskipTests compile
```

Resultado:

- `SportServiceImplTest`: 4 tests OK.
- `SportValidatorTest`: 4 tests OK.
- `SecurityContractWebMvcTest`: 17 tests OK.
- compilacion backend: OK.

## Checklist de cierre backend

- Alcance congelado respetado.
- Autenticacion, sesiones, permisos y shell sin reapertura funcional.
- Reporterias, eventos de partido, estadisticas y UX/UI avanzado fuera del bloque.
- Entidades existentes revisadas antes de crear nuevas piezas.
- `sport` reutilizado y endurecido.
- `TournamentFormat` reutilizado como catalogo de formatos.
- `sport_position` agregado solo como catalogo hijo necesario.
- Validaciones focalizadas agregadas.
- Pruebas Maven focalizadas ejecutadas.
- Paso a frontend recomendado con contrato minimo.

## Recomendacion de paso a frontend

Frontend puede avanzar con una pantalla simple de configuracion maestra:

- listar y editar deportes desde `/sports`;
- administrar posiciones dentro del detalle del deporte con `/sports/{sportId}/positions`;
- cargar selector de formatos desde `/sports/competition-formats`;
- usar `active=false` como baja operativa para deportes semilla.

No avanzar aun con reporterias, eventos, estadisticas ni migracion de rosters a posicion catalogada.
