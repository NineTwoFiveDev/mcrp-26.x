package com.mcrp.darkrp.model;

import java.util.UUID;

/**
 * A money printer placed in the world at a specific block.
 */
public class PrinterRecord {

    private final String blockKey;
    private final UUID owner;
    private final double price;

    public PrinterRecord(String blockKey, UUID owner, double price) {
        this.blockKey = blockKey;
        this.owner = owner;
        this.price = price;
    }

    public String getBlockKey() {
        return blockKey;
    }

    public UUID getOwner() {
        return owner;
    }

    public double getPrice() {
        return price;
    }
}
