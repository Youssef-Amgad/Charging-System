# Database Configuration

## ⚠️ Security Notice

The database credentials have been moved from hardcoded values to environment variables for security.

## Setting Environment Variables

### For Local Development

Set these environment variables before running your application:

```bash
export DB_URL="jdbc:postgresql://your-host:5432/your-database?sslmode=require&channel_binding=require"
export DB_USER="your_username"
export DB_PASSWORD="your_password"
```

### For Docker/Container Deployment

Add to your `docker-compose.yml` or container environment:

```yaml
environment:
  - DB_URL=jdbc:postgresql://your-host:5432/your-database?sslmode=require&channel_binding=require
  - DB_USER=your_username
  - DB_PASSWORD=your_password
```

### For Tomcat Server (context.xml)

Add to `src/main/webapp/META-INF/context.xml`:

```xml
<Context>
  <Environment name="DB_URL" value="jdbc:postgresql://your-host:5432/your-database?sslmode=require&channel_binding=require" type="java.lang.String" />
  <Environment name="DB_USER" value="your_username" type="java.lang.String" />
  <Environment name="DB_PASSWORD" value="your_password" type="java.lang.String" />
</Context>
```

## Default Fallback Values

If environment variables are not set, the application will use these fallback values:
- `DB_URL`: `jdbc:postgresql://localhost/neondb`
- `DB_USER`: `postgres`
- `DB_PASSWORD`: (empty string)

⚠️ **Note**: These defaults are for development only. Always set proper environment variables in production.

## Database Schema

The application expects a `customers` table with the following structure:

```sql
CREATE TABLE customers (
    msisdn VARCHAR(20) PRIMARY KEY,
    balance DOUBLE PRECISION NOT NULL
);
```

## For Neon PostgreSQL

If using Neon, your connection string will look like:

```
jdbc:postgresql://ep-xxxxx-pooler.region.aws.neon.tech/neondb?sslmode=require&channel_binding=require
```

Get your credentials from the Neon dashboard and set them as environment variables.

## Important Security Reminders

1. ✅ **Never commit credentials** to version control
2. ✅ **Always use environment variables** in production
3. ✅ **Use secrets management** tools (HashiCorp Vault, AWS Secrets Manager, etc.) in enterprise environments
4. ✅ **Rotate credentials regularly**, especially if they've been exposed
