# PostgreSQL Setup with Docker

## Quick Start

### 1. Start PostgreSQL
```bash
docker-compose up -d
```

### 2. Check PostgreSQL is Running
```bash
docker ps
docker logs maqoor-postgres
```

### 3. Run Application with PostgreSQL
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Database Connection Details

- **Host**: localhost
- **Port**: 5432
- **Database**: maqoor_db
- **Username**: maqoor
- **Password**: maqoor123

## Useful Docker Commands

### Stop PostgreSQL
```bash
docker-compose down
```

### Stop and Remove Data (Fresh Start)
```bash
docker-compose down -v
```

### View Logs
```bash
docker logs -f maqoor-postgres
```

### Access PostgreSQL CLI
```bash
docker exec -it maqoor-postgres psql -U maqoor -d maqoor_db
```

## Connect with Database Client

You can use any PostgreSQL client:
- **pgAdmin**: https://www.pgadmin.org/
- **DBeaver**: https://dbeaver.io/
- **DataGrip**: https://www.jetbrains.com/datagrip/

## Switch Back to H2 (Development)

Just run without the profile:
```bash
mvn spring-boot:run
```

## Data Persistence

PostgreSQL data is stored in a Docker volume named `postgres_data`.
This means your data persists even if you stop/restart the container.

To completely remove data:
```bash
docker-compose down -v
docker volume rm laundry_garment_tracker_postgres_data
```




