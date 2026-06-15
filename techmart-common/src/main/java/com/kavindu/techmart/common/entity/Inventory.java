package com.kavindu.techmart.common.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_inventory_product_warehouse",
                        columnNames = {"product_id", "warehouse_id"})
        },
        indexes = {
                @Index(name = "idx_inventory_product", columnList = "product_id"),
                @Index(name = "idx_inventory_warehouse", columnList = "warehouse_id")
        })
@NamedQueries({
        @NamedQuery(name = "Inventory.findByProduct",
                query = "SELECT i FROM Inventory i WHERE i.product.id = :productId"),
        @NamedQuery(name = "Inventory.findByProductAndWarehouse",
                query = "SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId"),
        @NamedQuery(name = "Inventory.totalAvailableForProduct",
                query = "SELECT COALESCE(SUM(i.quantityAvailable), 0) FROM Inventory i WHERE i.product.id = :productId"),
        @NamedQuery(name = "Inventory.findLowStock",
                query = "SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.reorderThreshold ORDER BY i.quantityAvailable ASC")
})
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold = 10;

    @Column(name = "reorder_quantity", nullable = false)
    private int reorderQuantity = 50;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    @PreUpdate
    protected void touch() {
        lastUpdated = LocalDateTime.now();
    }

    public Inventory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public int getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(int reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public int getReorderQuantity() {
        return reorderQuantity;
    }

    public void setReorderQuantity(int reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
