package com.kavindu.techmart.web.servlet;

import com.kavindu.techmart.web.metrics.MicrometerRegistryProducer;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/actuator/prometheus")
public class PrometheusMetricsServlet extends HttpServlet {

    @Inject
    private MicrometerRegistryProducer producer;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String text = producer.getRegistry().scrape();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain; version=0.0.4; charset=utf-8");
        resp.getWriter().write(text);
    }
}
