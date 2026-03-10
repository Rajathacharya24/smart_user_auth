# Deployment Guide - Smart User Auth

## Prerequisites

### Required Software
- Java 21 or later
- PostgreSQL 16+
- Maven 3.8+ (or use included wrapper)
- Docker (optional, for containerized deployment)

### Infrastructure Requirements
- Min 512 MB RAM
- Min 1 CPU core
- 1 GB disk space (including database)

## Environment Configuration

### Required Environment Variables

```bash
# Database
DB_URL=jdbc:postgresql://your-db-host:5432/smart_user_auth
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# JWT Configuration (generate with: openssl rand -base64 32)
JWT_SECRET=your-base64-encoded-secret-key
JWT_ACCESS_EXPIRATION=900000      # 15 minutes
JWT_REFRESH_EXPIRATION=604800000  # 7 days

# Server
SERVER_PORT=8080

# CORS (comma-separated list)
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://app.yourdomain.com

# Profile
SPRING_PROFILES_ACTIVE=prod
```

## Deployment Methods

### 1. Traditional Deployment (JAR)

#### Step 1: Build Application
```bash
./mvnw clean package -DskipTests
```

#### Step 2: Set Environment Variables
```bash
export DB_URL="jdbc:postgresql://production-db:5432/smart_user_auth"
export DB_USERNAME="prod_user"
export DB_PASSWORD="secure_password"
export JWT_SECRET="your-generated-secret"
export SPRING_PROFILES_ACTIVE="prod"
export CORS_ALLOWED_ORIGINS="https://yourdomain.com"
```

#### Step 3: Run Application
```bash
java -jar target/smart-user-auth-0.0.1-SNAPSHOT.jar
```

#### Step 4: Verify Deployment
```bash
curl http://localhost:8080/actuator/health
```

### 2. Docker Deployment

#### Create Dockerfile
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/smart-user-auth-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Build Docker Image
```bash
docker build -t smart-user-auth:1.0.0 .
```

#### Run Container
```bash
docker run -d \
  --name smart-user-auth \
  -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/smart_user_auth" \
  -e DB_USERNAME="smart_user" \
  -e DB_PASSWORD="secure_password" \
  -e JWT_SECRET="your-secret" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  smart-user-auth:1.0.0
```

### 3. Docker Compose (Full Stack)

#### docker-compose.prod.yml
```yaml
version: '3.9'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: smart_user_auth
      POSTGRES_USER: smart_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - app-network

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/smart_user_auth
      DB_USERNAME: smart_user
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}
    depends_on:
      - postgres
    networks:
      - app-network

volumes:
  postgres_data:

networks:
  app-network:
```

#### Deploy
```bash
docker-compose -f docker-compose.prod.yml up -d
```

## Database Setup

### Initialize Database
```sql
CREATE DATABASE smart_user_auth;
CREATE USER smart_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE smart_user_auth TO smart_user;
```

### Flyway Migrations
Migrations run automatically on application startup. To manually run:
```bash
./mvnw flyway:migrate
```

## Security Hardening

### 1. Generate Strong JWT Secret
```bash
openssl rand -base64 64
```

### 2. Use HTTPS in Production
Configure reverse proxy (nginx/Apache) with SSL certificate.

### 3. Database Security
- Use strong passwords
- Restrict network access to database
- Enable SSL/TLS for database connections

### 4. Rate Limiting
Already configured (5 req/min for auth endpoints).
Adjust in `RateLimitFilter` if needed.

### 5. CORS Configuration
Whitelist only trusted domains in production:
```bash
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

## Monitoring & Health Checks

### Health Endpoint
```bash
curl http://localhost:8080/actuator/health
```

### Database Connection Check
```bash
curl http://localhost:8080/actuator/health/db
```

### Logs Location
- Default: `logs/spring-boot-application.log`
- Configure in `application-prod.yml`

## Reverse Proxy (nginx)

### Example Configuration
```nginx
server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Troubleshooting

### Application Won't Start
- Check logs: `logs/spring-boot-application.log`
- Verify database connectivity
- Ensure Java 21+ is installed

### Database Connection Issues
- Verify credentials
- Check firewall rules
- Test connection: `psql -h host -U user -d database`

### JWT Token Issues
- Verify JWT_SECRET is properly base64 encoded
- Check token expiration settings
- Review logs for validation errors

## Backup Strategy

### Database Backup
```bash
pg_dump -h localhost -U smart_user smart_user_auth > backup_$(date +%Y%m%d).sql
```

### Restore
```bash
psql -h localhost -U smart_user smart_user_auth < backup_20260304.sql
```

## Scaling Considerations

### Horizontal Scaling
- App is stateless (JWT-based)
- Use load balancer (nginx, HAProxy, AWS ALB)
- Share database across instances

### Database Scaling
- Use connection pooling (configured in Hikari)
- Consider read replicas for high load
- Monitor and index query performance

## Production Checklist

- [ ] Strong JWT secret generated and set
- [ ] Database credentials secured
- [ ] HTTPS/TLS enabled
- [ ] CORS configured for production domains only
- [ ] Database backups scheduled
- [ ] Monitoring and alerting configured
- [ ] Logs aggregation setup
- [ ] Rate limiting tested
- [ ] Security headers verified
- [ ] Admin account password changed
- [ ] Environment variables validated
- [ ] Health checks working
