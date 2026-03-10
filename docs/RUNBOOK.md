# Incident Response Runbook

## Overview
This runbook provides step-by-step procedures for handling common authentication service incidents.

---

## 1. Users Cannot Login

### Symptoms
- HTTP 401 responses on `/api/v1/auth/login`
- "Invalid credentials" errors for valid accounts

### Diagnosis Steps
1. **Check Service Health**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **Verify Database Connection**
   ```bash
   docker exec smart_user_auth_db pg_isready
   ```

3. **Check Application Logs**
   ```bash
   tail -f logs/spring-boot-application.log | grep "Login"
   ```

4. **Test with Known Good Credentials**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@smartauth.com","password":"AdminPass123!"}'
   ```

### Resolution Steps

#### If Database is Down
```bash
# Restart database container
docker restart smart_user_auth_db

# Or start if stopped
docker-compose up -d postgres
```

#### If Account is Locked (Rate Limit)
Account lockout clears automatically after 15 minutes. To clear manually:
```java
// Clear via code (requires deployment)
loginAttemptService.loginSucceeded(email);
```

#### If JWT Secret Invalid
```bash
# Verify JWT_SECRET is set
echo $JWT_SECRET

# Restart with correct secret
export JWT_SECRET="correct-secret"
./restart-app.sh
```

---

## 2. Token Validation Failures

### Symptoms
- HTTP 401 on protected endpoints
- "Invalid token" or "Token expired" messages

### Diagnosis Steps
1. **Verify Token Format**
   ```bash
   # Token should have 3 parts separated by dots
   echo "YOUR_TOKEN" | tr '.' '\n' | wc -l  # Should output 3
   ```

2. **Check Token Expiry**
   ```bash
   # Decode JWT (header.payload.signature)
   echo "PAYLOAD_PART" | base64 -d | jq .exp
   ```

3. **Review Logs**
   ```bash
   grep "JWT authentication failed" logs/spring-boot-application.log
   ```

### Resolution Steps

#### If Tokens Expired
- Access tokens expire in 15 minutes (expected behavior)
- Client should use refresh token endpoint
- No action needed on server side

#### If JWT Secret Mismatch
```bash
# This happens after server restart with different secret
# All existing tokens become invalid

# Solution: Users must re-login
# Communicate to users: "Please log in again"
```

#### If Token Blacklisted/Revoked
- Refresh tokens are revoked on logout
- Users must obtain new tokens via login

---

## 3. High Load / Rate Limiting

### Symptoms
- HTTP 429 (Too Many Requests)
- Slow response times
- Connection pool exhaustion

### Diagnosis Steps
1. **Check Active Connections**
   ```bash
   # PostgreSQL connections
   docker exec smart_user_auth_db psql -U smart_user -d smart_user_auth \
     -c "SELECT count(*) FROM pg_stat_activity;"
   ```

2. **Monitor Request Rate**
   ```bash
   # Check nginx/access logs
   tail -f /var/log/nginx/access.log | grep "/api/v1/auth"
   ```

3. **Check Application Metrics**
   ```bash
   curl http://localhost:8080/actuator/metrics/http.server.requests
   ```

### Resolution Steps

#### If Legitimate Traffic Spike
```java
// Increase rate limits temporarily
// Edit: RateLimitFilter.java
private int getRateLimitCapacity(String path) {
    if (path.contains("/login") || path.contains("/register")) {
        return 10; // Increased from 5
    }
    return 40; // Increased from 20
}
```

#### If Under Attack (DDoS)
```bash
# Block offending IPs at firewall level
sudo iptables -A INPUT -s ATTACKER_IP -j DROP

# Or configure rate limiting in nginx
limit_req_zone $binary_remote_addr zone=auth:10m rate=5r/m;
```

#### Scale Horizontally
```bash
# Deploy additional instances
docker-compose up -d --scale app=3
```

---

## 4. Database Connection Issues

### Symptoms
- "Unable to acquire JDBC Connection"
- Application startup failures
- Intermittent 500 errors

### Diagnosis Steps
1. **Test Database Connectivity**
   ```bash
   psql -h localhost -U smart_user -d smart_user_auth -c "SELECT 1"
   ```

2. **Check Connection Pool**
   ```bash
   curl http://localhost:8080/actuator/metrics/hikaricp.connections
   ```

3. **Review Database Logs**
   ```bash
   docker logs smart_user_auth_db | tail -50
   ```

### Resolution Steps

#### If Max Connections Reached
```sql
-- Check current connections
SELECT count(*), state FROM pg_stat_activity GROUP BY state;

-- Terminate idle connections
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = 'smart_user_auth' 
  AND state = 'idle' 
  AND state_change < NOW() - INTERVAL '10 minutes';
```

#### If Database is Down
```bash
# Restart database
docker restart smart_user_auth_db

# Check logs
docker logs smart_user_auth_db
```

#### Increase Connection Pool
```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase from default 10
      minimum-idle: 5
```

---

## 5. Refresh Token Issues

### Symptoms
- "Invalid refresh token" on `/api/v1/auth/refresh`
- Tokens work once then fail

### Diagnosis Steps
1. **Check Token in Database**
   ```sql
   SELECT id, user_id, issued_at, expires_at, revoked_at 
   FROM refresh_tokens 
   WHERE token_hash = 'HASH_VALUE' 
   LIMIT 1;
   ```

2. **Verify Token Rotation**
   ```bash
   # Check logs for rotation events
   grep "Refresh token rotated" logs/spring-boot-application.log
   ```

### Resolution Steps

#### If Token Already Used (Rotation)
- Expected behavior: old refresh token is revoked after use
- Client must use the NEW refresh token from response

#### If Token Expired
```sql
-- Check expiration
SELECT expires_at FROM refresh_tokens WHERE token_hash = 'HASH';

-- Expired tokens are invalid (expected)
-- User must re-login
```

#### If Token Revoked
```sql
-- Check revocation status
SELECT revoked_at FROM refresh_tokens WHERE token_hash = 'HASH';

-- If revoked_at is set, token was logged out
-- User must re-login
```

---

## 6. Account Lockout Issues

### Symptoms
- "Account is temporarily locked" message
- Users unable to login after failed attempts

### Diagnosis Steps
1. **Check Lockout Status (Logs)**
   ```bash
   grep "Account locked" logs/spring-boot-application.log
   ```

2. **Verify Failed Attempt Count**
   - Lockout occurs after 5 failed attempts
   - Lock duration: 15 minutes

### Resolution Steps

#### Manual Unlock (Emergency)
```bash
# Restart application to clear in-memory cache
docker restart smart-user-auth-app

# Or wait 15 minutes for automatic unlock
```

#### Prevent False Positives
- Verify user is entering correct credentials
- Check for CAPS LOCK issues
- Consider increasing max attempts (security trade-off)

---

## 7. CORS Errors

### Symptoms
- "CORS policy" errors in browser console
- Preflight OPTIONS requests failing

### Diagnosis Steps
1. **Check CORS Configuration**
   ```bash
   echo $CORS_ALLOWED_ORIGINS
   ```

2. **Test CORS Headers**
   ```bash
   curl -X OPTIONS http://localhost:8080/api/v1/auth/login \
     -H "Origin: https://yourdomain.com" \
     -H "Access-Control-Request-Method: POST" \
     -v
   ```

### Resolution Steps

#### Add Missing Origin
```bash
# Update environment variable
export CORS_ALLOWED_ORIGINS="https://yourdomain.com,https://newdomain.com"

# Restart application
docker restart smart-user-auth-app
```

#### Verify Configuration
```yaml
# application.yml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

---

## Emergency Contacts

- **On-Call Engineer**: [Contact Info]
- **Database Admin**: [Contact Info]
- **Security Team**: [Contact Info]

## Escalation Path

1. Level 1: On-call engineer (30 min response)
2. Level 2: Senior engineer (1 hour response)
3. Level 3: Engineering manager (2 hour response)

## Post-Incident

- Document incident in wiki
- Update runbook with learnings
- Schedule post-mortem if severe
- Review and update monitoring alerts
