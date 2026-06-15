package com.kavindu.techmart.common.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouses")
@NamedQueries({
        @NamedQuery(name = "Warehouse.findActive",
                query = "SELECT w FROM Warehouse w WHERE w.active = true ORDER BY w.name")
})
public class Warehouse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "location", length = 120)
    private String location;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "warehouse", fetch = FetchType.LAZY)
    private List<Inventory> inventories = new ArrayList<>();

    public Warehouse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Inventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<Inventory> inventories) {
        this.inventories = inventories;
    }
}
