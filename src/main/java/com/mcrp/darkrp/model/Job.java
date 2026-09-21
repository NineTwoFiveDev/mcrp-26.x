package com.mcrp.darkrp.model;

import java.util.List;

/**
 * Definition of a job/class, loaded from jobs.yml.
 */
public class Job {

    private final String id;
    private final String displayName;
    private final String description;
    private final double salary;
    private final int maxSlots;
    private final boolean police;
    private final boolean whitelisted;
    private final List<String> kit;

    public Job(String id, String displayName, String description, double salary,
               int maxSlots, boolean police, boolean whitelisted, List<String> kit) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.salary = salary;
        this.maxSlots = maxSlots;
        this.police = police;
        this.whitelisted = whitelisted;
        this.kit = kit;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getSalary() {
        return salary;
    }

    /** -1 means unlimited. */
    public int getMaxSlots() {
        return maxSlots;
    }

    public boolean isPolice() {
        return police;
    }

    public boolean isWhitelisted() {
        return whitelisted;
    }

    public List<String> getKit() {
        return kit;
    }

    public String whitelistPermission() {
        return "darkrp.job." + id;
    }
}
