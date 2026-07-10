# NetOps Console — Telecom Prepaid Customer Web Interface

A standalone servlet-based web application (Jakarta Servlet 6, JDBC, PostgreSQL) implementing
full CRUD management for the `public.customers` table (`msisdn`, `balance`). This is the
**third application** of the Telecom Prepaid Charging System — the web management console.

No Spring, no Hibernate, no JS/CSS frameworks. Pure Servlets + JDBC + vanilla HTML/CSS/JS.

## Tech stack

- Java 17, Jakarta Servlet API 6.0 (Tomcat 10+)
- PostgreSQL via JDBC (`org.postgresql:postgresql` driver)
- Maven (`war` packaging)
- HTML5 / CSS3 / vanilla JavaScript (no frameworks, no CDNs except Google Fonts)

## Project layout

```
TelecomCustomerWeb/
├── pom.xml
├── src/main/java/
│   ├── model/          Customer.java, DashboardStats.java
│   ├── dao/             CustomerDAO.java, DBConnection.java, exceptions
│   ├── servlet/         HomeServlet, CustomerListServlet, AddCustomerServlet,
│   │                    EditCustomerServlet, DeleteCustomerServlet, SearchCustomerServlet
│   └── util/            JsonUtil, CustomerListResponder
├── src/main/webapp/
│   ├── css/             style.css, dashboard.css, table.css, form.css, modal.css, responsive.css
│   ├── js/               app.js, theme.js, validation.js
│   ├── error/            404.html, 500.html
│   ├── WEB-INF/web.xml
│   └── index.html
```

## Database

Uses the Neon-hosted PostgreSQL instance already provided in the brief. The JDBC URL, user, and
password are configured in `src/main/java/dao/DBConnection.java`:

```
jdbc:postgresql://ep-twilight-river-a2lb7my7-pooler.eu-central-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
```

Table used (already exists, no other tables are touched):

```sql
CREATE TABLE public.customers (
    msisdn  varchar(20)   NOT NULL,
    balance numeric(12,2) NOT NULL,
    CONSTRAINT customers_pkey PRIMARY KEY (msisdn)
);
```

If you need to point the app at a different database, just edit the constants at the top of
`DBConnection.java` (`HOST`, `DATABASE`, `USER`, `PASSWORD`).

## Build & run

1. Import the project into **IntelliJ IDEA** or **Eclipse** as an existing Maven project.
2. Let Maven resolve dependencies (`jakarta.servlet-api` is `provided` — Tomcat 10+ supplies it;
   `org.postgresql:postgresql` is bundled into the WAR).
3. Build the WAR:
   ```
   mvn clean package
   ```
   This produces `target/TelecomCustomerWeb.war`.
4. Deploy the WAR to **Apache Tomcat 10+** (drop it in `webapps/`, or configure a run
   configuration in your IDE pointing at a Tomcat 10+ server).
5. Open `http://localhost:8080/TelecomCustomerWeb/` in your browser.

No additional configuration is required — the JDBC credentials are already wired in.

## REST-ish JSON endpoints (consumed by the front end)

| Endpoint                     | Method | Purpose                                   |
|-------------------------------|--------|--------------------------------------------|
| `/api/dashboard`               | GET    | Aggregate stats (total/avg/max/min balance) |
| `/api/customers`               | GET    | Paginated + sorted customer list            |
| `/api/customers/search`        | GET    | Live search by MSISDN (paginated)           |
| `/api/customers/add`           | POST   | Create a customer                           |
| `/api/customers/edit`          | POST   | Update a customer's balance                 |
| `/api/customers/delete`        | POST   | Delete a customer                           |

All list/search endpoints accept `q`, `sort` (`msisdn`|`balance`), `dir` (`asc`|`desc`),
`page`, and `size` query parameters.

## Notes

- All SQL uses `PreparedStatement` — no string-concatenated queries.
- MSISDN is treated as immutable (it's the primary key); only `balance` is editable after creation.
- Friendly HTML error pages are wired up for HTTP 404 and 500 via `web.xml`.
- Dark/Light mode preference is remembered in the browser between visits.
