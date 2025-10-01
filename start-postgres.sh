#!/bin/bash
echo "🐘 Starting PostgreSQL with Docker..."
docker-compose up -d
echo ""
echo "⏳ Waiting for PostgreSQL to be ready..."
sleep 5
docker exec maqoor-postgres pg_isready -U maqoor -d maqoor_db
echo ""
echo "✅ PostgreSQL is running!"
echo ""
echo "📊 Database Details:"
echo "   Host: localhost"
echo "   Port: 5432"
echo "   Database: maqoor_db"
echo "   Username: maqoor"
echo "   Password: maqoor123"
echo ""
echo "🚀 To start your application with PostgreSQL:"
echo "   mvn spring-boot:run -Dspring-boot.run.profiles=postgres"
