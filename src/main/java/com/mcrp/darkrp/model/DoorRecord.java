package com.mcrp.darkrp.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A door (or group of connected double-door blocks) that has been bought.
 */
public class DoorRecord {

    private final String id;
    private final Set<String> blockKeys;
    private UUID owner;
    private final Set<UUID> coOwners = new HashSet<>();
    private boolean locked;
    /** Base purchase price paid to the bank; also used to compute resale refunds. */
    private double price;
    /** If > 0, this door is being offered for direct sale to other players at this price. */
    private double forSalePrice;

    public DoorRecord(String id, Set<String> blockKeys, UUID owner, double price) {
        this.id = id;
        this.blockKeys = blockKeys;
        this.owner = owner;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public Set<String> getBlockKeys() {
        return blockKeys;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Set<UUID> getCoOwners() {
        return coOwners;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getForSalePrice() {
        return forSalePrice;
    }

    public void setForSalePrice(double forSalePrice) {
        this.forSalePrice = forSalePrice;
    }

    public boolean isOwnedBy(UUID uuid) {
        return owner != null && (owner.equals(uuid) || coOwners.contains(uuid));
    }
}
