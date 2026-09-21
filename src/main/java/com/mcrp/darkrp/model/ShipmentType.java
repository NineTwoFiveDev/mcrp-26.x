package com.mcrp.darkrp.model;

import org.bukkit.Material;

import java.util.List;

/**
 * A buyable shipment definition (a crate filled with a configured item), loaded from shipments.yml.
 */
public class ShipmentType {

    private final String id;
    private final String displayName;
    private final Material item;
    private final int amount;
    private final double price;
    private final List<String> allowedJobs;

    public ShipmentType(String id, String displayName, Material item, int amount, double price, List<String> allowedJobs) {
        this.id = id;
        this.displayName = displayName;
        this.item = item;
        this.amount = amount;
        this.price = price;
        this.allowedJobs = allowedJobs;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }

    public double getPrice() {
        return price;
    }

    public List<String> getAllowedJobs() {
        return allowedJobs;
    }
}
