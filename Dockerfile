# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy POMs first so this layer is cached when only source changes
COPY pom.xml .
COPY techmart-common/pom.xml techmart-common/pom.xml
COPY techmart-ejb/pom.xml     techmart-ejb/pom.xml
COPY techmart-web/pom.xml     techmart-web/pom.xml
COPY techmart-ear/pom.xml     techmart-ear/pom.xml

RUN mvn dependency:go-offline -B --no-transfer-progress 2>/dev/null || true

# Copy source and build
COPY . .
RUN mvn clean install -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM quay.io/wildfly/wildfly:31.0.0.Final-jdk17

USER root

# Add MySQL JDBC driver as a WildFly module
RUN mkdir -p $JBOSS_HOME/modules/com/mysql/main && \
    curl -sSL \
    "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar" \
    -o $JBOSS_HOME/modules/com/mysql/main/mysql-connector-j-8.3.0.jar
COPY docker/mysql-module.xml $JBOSS_HOME/modules/com/mysql/main/module.xml

# Configure datasource + JMS in offline embedded mode (no running server needed)
COPY docker/configure.cli /tmp/configure.cli
RUN $JBOSS_HOME/bin/jboss-cli.sh --file=/tmp/configure.cli

# Deploy the EAR (WildFly deployment scanner picks it up on startup)
COPY --from=builder /build/techmart-ear/target/techmart.ear \
    $JBOSS_HOME/standalone/deployments/techmart.ear

# Add JMX Prometheus exporter agent for Prometheus metrics scraping
ARG JMX_EXPORTER_VERSION=0.20.0
RUN curl -sSL \
    "https://github.com/prometheus/jmx_exporter/releases/download/${JMX_EXPORTER_VERSION}/jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar" \
    -o $JBOSS_HOME/jmx-exporter.jar
COPY docker/jmx-config.yml $JBOSS_HOME/jmx-config.yml

RUN chown -R jboss:jboss $JBOSS_HOME/modules/com/mysql $JBOSS_HOME/standalone/ \
    $JBOSS_HOME/jmx-exporter.jar $JBOSS_HOME/jmx-config.yml

USER jboss

EXPOSE 8080 9990 9404

CMD ["/opt/jboss/wildfly/bin/standalone.sh", \
     "-b",           "0.0.0.0", \
     "-bmanagement", "0.0.0.0", \
     "-c",           "standalone-full.xml"]
