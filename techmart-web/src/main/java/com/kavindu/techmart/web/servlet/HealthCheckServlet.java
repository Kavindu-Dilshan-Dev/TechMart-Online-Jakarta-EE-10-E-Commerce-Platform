package com.kavindu.techmart.web.servlet;

import com.kavindu.techmart.common.dto.MetricsDTO;
import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import com.kavindu.techmart.ejb.session.singleton.SystemConfigBean;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/health")
public class HealthCheckServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private PerformanceMetricsBean metrics;

    @EJB
    private SystemConfigBean systemConfig;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        MetricsDTO m = metrics.getMetrics();
        boolean healthy = m.isHealthy() && !systemConfig.isMaintenanceMode();

        String body = Json.createObjectBuilder()
                .add("status", healthy ? "UP" : "DOWN")
                .add("uptime", m.getUptimeFormatted())
                .add("activeUsers", m.getActiveUsers())
                .add("totalRequests", m.getTotalRequests())
                .add("heapUsagePercent", m.getHeapUsagePercent())
                .add("maintenanceMode", systemConfig.isMaintenanceMode())
                .add("timestamp", System.currentTimeMillis())
                .build()
                .toString();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(healthy ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        resp.getWriter().write(body);
    }
}
