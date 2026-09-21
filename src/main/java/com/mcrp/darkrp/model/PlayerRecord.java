package com.mcrp.darkrp.model;

import java.util.UUID;

/**
 * Persistent per-player state: economy, job and crime status.
 */
public class PlayerRecord {

    private final UUID uuid;
    private String name;
    private double balance;
    private String jobId;

    // Jail state
    private long jailedUntilMillis;
    private String preJailLocation;

    // Wanted state
    private boolean wanted;
    private String wantedReason = "";
    private long wantedExpiryMillis;

    public PlayerRecord(UUID uuid, String name, double balance, String jobId) {
        this.uuid = uuid;
        this.name = name;
        this.balance = balance;
        this.jobId = jobId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = Math.max(0.0, balance);
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public boolean isJailed() {
        return jailedUntilMillis > System.currentTimeMillis();
    }

    public long getJailedUntilMillis() {
        return jailedUntilMillis;
    }

    public void setJailedUntilMillis(long jailedUntilMillis) {
        this.jailedUntilMillis = jailedUntilMillis;
    }

    public String getPreJailLocation() {
        return preJailLocation;
    }

    public void setPreJailLocation(String preJailLocation) {
        this.preJailLocation = preJailLocation;
    }

    public boolean isWanted() {
        return wanted && wantedExpiryMillis > System.currentTimeMillis();
    }

    public void setWanted(boolean wanted) {
        this.wanted = wanted;
    }

    public String getWantedReason() {
        return wantedReason;
    }

    public void setWantedReason(String wantedReason) {
        this.wantedReason = wantedReason == null ? "" : wantedReason;
    }

    public long getWantedExpiryMillis() {
        return wantedExpiryMillis;
    }

    public void setWantedExpiryMillis(long wantedExpiryMillis) {
        this.wantedExpiryMillis = wantedExpiryMillis;
    }
}
