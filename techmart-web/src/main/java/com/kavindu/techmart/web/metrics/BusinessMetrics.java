package com.kavindu.techmart.web.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BusinessMetrics {

    @Inject
    private MicrometerRegistryProducer registryProducer;

    private Counter ordersPlaced;
    private Counter ordersCancelled;
    private Counter paymentsInitiated;
    private Counter paymentsSucceeded;
    private Counter paymentsFailed;
    private Counter cartItemsAdded;
    private Counter usersRegistered;
    private Counter userLogins;

    @PostConstruct
    public void init() {
        MeterRegistry registry = registryProducer.getRegistry();

        ordersPlaced = Counter.builder("techmart.orders.placed")
                .description("Orders successfully placed")
                .register(registry);

        ordersCancelled = Counter.builder("techmart.orders.cancelled")
                .description("Orders cancelled by customer")
                .register(registry);

        paymentsInitiated = Counter.builder("techmart.payments.initiated")
                .description("Payment sessions initiated")
                .register(registry);

        paymentsSucceeded = Counter.builder("techmart.payments.succeeded")
                .description("Successful payment notifications received")
                .register(registry);

        paymentsFailed = Counter.builder("techmart.payments.failed")
                .description("Failed payment notifications received")
                .register(registry);

        cartItemsAdded = Counter.builder("techmart.cart.items.added")
                .description("Items added to shopping carts")
                .register(registry);

        usersRegistered = Counter.builder("techmart.users.registered")
                .description("New customer account registrations")
                .register(registry);

        userLogins = Counter.builder("techmart.users.logins")
                .description("Successful user logins")
                .register(registry);
    }

    public void recordOrderPlaced()      { ordersPlaced.increment(); }
    public void recordOrderCancelled()   { ordersCancelled.increment(); }
    public void recordPaymentInitiated() { paymentsInitiated.increment(); }
    public void recordPaymentSucceeded() { paymentsSucceeded.increment(); }
    public void recordPaymentFailed()    { paymentsFailed.increment(); }
    public void recordCartItemAdded()    { cartItemsAdded.increment(); }
    public void recordUserRegistered()   { usersRegistered.increment(); }
    public void recordUserLogin()        { userLogins.increment(); }
}
