# TechMart Online — Jakarta EE 10 E-Commerce Platform

> Full setup guide: **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)**

---

## Option A — Docker (recommended)

Requires only **Docker Desktop** (or Docker Engine + Compose plugin). No JDK, Maven, WildFly, or MySQL installation needed.

```bash
docker compose up --build
```

This builds the EAR, configures WildFly with the MySQL datasource and JMS resources, starts MySQL, and deploys the app automatically. Once you see `Deployed "techmart.ear"` in the logs, open **http://localhost:8080/techmart/**

On later runs (image already built, data already seeded):
```bash
docker compose up
```

---

## Option B — Manual setup (WildFly + MySQL installed locally)

### Prerequisites

| Software | Version |
|---|---|
| JDK | 17 |
| Apache Maven | 3.9+ |
| MySQL Server | 8.x |
| WildFly | 31.x |

Set `JAVA_HOME` and `WILDFLY_HOME` environment variables before starting.

### Build

```bash
mvn clean install
```

### One-time setup

**1. Database**
```powershell
cmd /c "mysql -u root -p < database\schema.sql"
mysql -u root -p -e "CREATE USER IF NOT EXISTS 'techmart'@'%' IDENTIFIED BY 'techmart'; GRANT ALL PRIVILEGES ON techmart.* TO 'techmart'@'%'; FLUSH PRIVILEGES;"
cmd /c "mysql -u root -p techmart < database\sample-data.sql"
```

**2. Edit MySQL connector path** — open `wildfly-config\create-datasource.cli` and set `--resources=` to your connector jar, e.g.  
`C:/Users/<you>/.m2/repository/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar`

**3. Start WildFly** (keep this window open)
```powershell
& "$env:WILDFLY_HOME\bin\standalone.bat" -c standalone-full.xml
```

**4. Register datasource and JMS resources** (second terminal, WildFly must be running)
```powershell
& "$env:WILDFLY_HOME\bin\jboss-cli.bat" --connect --file=wildfly-config\create-datasource.cli
& "$env:WILDFLY_HOME\bin\jboss-cli.bat" --connect --file=wildfly-config\create-jms-resources.cli
```

**5. Deploy**
```powershell
Copy-Item techmart-ear\target\techmart.ear "$env:WILDFLY_HOME\standalone\deployments\"
```

### Subsequent runs

Database, resources, and deployment already exist — just start WildFly:
```powershell
& "$env:WILDFLY_HOME\bin\standalone.bat" -c standalone-full.xml
```

Then open **http://localhost:8080/techmart/**

---

## Demo accounts

| Role | Username | Password |
|---|---|---|
| Customer | `customer` | `password123` |
| Admin | `admin` | `admin123` |
| Developer | `devuser` | `dev123` |

For troubleshooting, Linux/macOS commands, and optional integrations (email, payment sandbox) see **[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)**.
