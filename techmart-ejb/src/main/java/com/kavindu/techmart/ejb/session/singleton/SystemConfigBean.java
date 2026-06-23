package com.kavindu.techmart.ejb.session.singleton;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Singleton(name = "SystemConfigBean")
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Lock(LockType.READ)
public class SystemConfigBean {

    private static final Logger LOG = Logger.getLogger(SystemConfigBean.class.getName());

    public static final String MAX_CONCURRENT_USERS = "max.concurrent.users";
    public static final String MAINTENANCE_MODE = "maintenance.mode";
    public static final String PAYHERE_MERCHANT_ID = "payhere.merchant.id";
    public static final String PAYHERE_API_KEY = "payhere.api.key";
    public static final String PAYHERE_MERCHANT_SECRET = "payhere.merchant.secret";
    public static final String PAYHERE_SANDBOX = "payhere.sandbox";

    public static final String APP_PUBLIC_BASE_URL = "app.public.base.url";
    public static final String EMAIL_ENABLED = "email.enabled";
    public static final String SMTP_HOST = "smtp.host";
    public static final String SMTP_PORT = "smtp.port";
    public static final String SMTP_FROM = "smtp.from";
    public static final String SMTP_AUTH = "smtp.auth";
    public static final String SMTP_STARTTLS = "smtp.starttls";
    public static final String SMTP_USERNAME = "smtp.username";
    public static final String SMTP_PASSWORD = "smtp.password";
    public static final String TAX_RATE = "order.tax.rate";
    public static final String SHIPPING_FLAT = "order.shipping.flat";
    public static final String CB_FAILURE_THRESHOLD = "circuitbreaker.failure.threshold";
    public static final String CB_OPEN_TIMEOUT_MS = "circuitbreaker.open.timeout.ms";

    private final Map<String, String> config = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        LOG.info("Initialising SystemConfigBean (singleton @Startup)...");
        loadDefaults();
        overlayEnvironment();
        LOG.info("SystemConfigBean ready: maxConcurrentUsers=" + getMaxConcurrentUsers()
                + ", maintenanceMode=" + isMaintenanceMode());
    }

    private void loadDefaults() {
        config.put(MAX_CONCURRENT_USERS, "10000");
        config.put(MAINTENANCE_MODE, "false");
        config.put(PAYHERE_MERCHANT_ID, "1225785");
        config.put(PAYHERE_API_KEY, "DEMO-SANDBOX-KEY");
        config.put(PAYHERE_MERCHANT_SECRET, "3814300462406772644435150277681462884977");
        config.put(PAYHERE_SANDBOX, "true");
        config.put(APP_PUBLIC_BASE_URL, "https://007c-2402-4000-b2c1-fb8-408b-ed9a-afb0-8ec6.ngrok-free.app/techmart");
        config.put(EMAIL_ENABLED, "false");
        config.put(SMTP_HOST, "localhost");
        config.put(SMTP_PORT, "25");
        config.put(SMTP_FROM, "no-reply@techmart.lk");
        config.put(SMTP_AUTH, "false");
        config.put(SMTP_STARTTLS, "true");
        config.put(SMTP_USERNAME, "");
        config.put(SMTP_PASSWORD, "");
        config.put(TAX_RATE, "0.08");
        config.put(SHIPPING_FLAT, "350.00");
        config.put(CB_FAILURE_THRESHOLD, "5");
        config.put(CB_OPEN_TIMEOUT_MS, "30000");
    }

    private void overlayEnvironment() {
        for (String key : config.keySet()) {
            String envKey = key.toUpperCase().replace('.', '_');
            String envVal = System.getenv(envKey);
            if (envVal != null && !envVal.isBlank()) {
                config.put(key, envVal.trim());
                LOG.info("Config override from env: " + key + "=" + envVal);
            }
        }
    }

    public String getConfig(String key) {
        return config.get(key);
    }

    public String getConfig(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public int getIntConfig(String key, int defaultValue) {
        try {
            String v = config.get(key);
            return v != null ? Integer.parseInt(v.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDoubleConfig(String key, double defaultValue) {
        try {
            String v = config.get(key);
            return v != null ? Double.parseDouble(v.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String v = config.get(key);
        return v != null ? Boolean.parseBoolean(v.trim()) : defaultValue;
    }

    public int getMaxConcurrentUsers() {
        return getIntConfig(MAX_CONCURRENT_USERS, 10000);
    }

    public boolean isMaintenanceMode() {
        return getBooleanConfig(MAINTENANCE_MODE, false);
    }

    public String getPayhereMerchantId() {
        return getConfig(PAYHERE_MERCHANT_ID, "");
    }

    public String getPayhereApiKey() {
        return getConfig(PAYHERE_API_KEY, "");
    }

    public String getPayhereMerchantSecret() {
        return getConfig(PAYHERE_MERCHANT_SECRET, "");
    }

    public boolean isPayhereSandbox() {
        return getBooleanConfig(PAYHERE_SANDBOX, true);
    }

    public String getPublicBaseUrl() {
        String base = getConfig(APP_PUBLIC_BASE_URL, "http://localhost:8080/techmart");
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public boolean isEmailEnabled() {
        return getBooleanConfig(EMAIL_ENABLED, false);
    }

    public String getSmtpHost() {
        return getConfig(SMTP_HOST, "localhost");
    }

    public int getSmtpPort() {
        return getIntConfig(SMTP_PORT, 25);
    }

    public String getSmtpFrom() {
        return getConfig(SMTP_FROM, "no-reply@techmart.lk");
    }

    public boolean isSmtpAuth() {
        return getBooleanConfig(SMTP_AUTH, false);
    }

    public boolean isSmtpStartTls() {
        return getBooleanConfig(SMTP_STARTTLS, true);
    }

    public String getSmtpUsername() {
        return getConfig(SMTP_USERNAME, "");
    }

    public String getSmtpPassword() {
        return getConfig(SMTP_PASSWORD, "");
    }

    public double getTaxRate() {
        return getDoubleConfig(TAX_RATE, 0.08);
    }

    public double getShippingFlat() {
        return getDoubleConfig(SHIPPING_FLAT, 350.00);
    }

    public Map<String, String> getAllConfig() {
        return Collections.unmodifiableMap(config);
    }

    @Lock(LockType.WRITE)
    public void setConfig(String key, String value) {
        config.put(key, value);
        LOG.info("Config updated: " + key + "=" + value);
    }

    @Lock(LockType.WRITE)
    public void setMaintenanceMode(boolean enabled) {
        config.put(MAINTENANCE_MODE, Boolean.toString(enabled));
        LOG.warning("Maintenance mode set to " + enabled);
    }

    @Lock(LockType.WRITE)
    public void reloadConfig() {
        LOG.info("Reloading configuration (exclusive WRITE lock)...");
        loadDefaults();
        overlayEnvironment();
    }
}
