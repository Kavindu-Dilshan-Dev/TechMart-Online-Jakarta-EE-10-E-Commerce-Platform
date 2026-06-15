package com.kavindu.techmart.common.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_alerts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_stock_alerts_user_product",
                        columnNames = {"user_id", "product_id"})
        },
        indexes = {
                @Index(name = "idx_stock_alerts_product_notified", columnList = "product_id,notified")
        })
@NamedQueries({
        @NamedQuery(name = "StockAlert.findPendingByProduct",
                query = "SELECT s FROM StockAlert s WHERE s.product.id = :productId AND s.notified = false"),
        @NamedQuery(name = "StockAlert.findByUserAndProduct",
                query = "SELECT s FROM StockAlert s WHERE s.user.id = :userId AND s.product.id = :productId")
})
public class StockAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "notified", nullable = false)
    private boolean notified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }
}
