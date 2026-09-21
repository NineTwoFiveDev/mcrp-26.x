package com.mcrp.darkrp.job;

import com.mcrp.darkrp.model.Job;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.ItemUtil;
import com.mcrp.darkrp.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the job-selection GUI.
 */
public class JobMenu {

    public static final NamespacedKey JOB_ID_KEY = new NamespacedKey("darkrp", "job_id");

    private final Plugin plugin;
    private final JobManager jobManager;
    private final DataStore dataStore;

    public JobMenu(Plugin plugin, JobManager jobManager, DataStore dataStore) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.dataStore = dataStore;
    }

    public void open(Player player) {
        List<Job> jobs = new ArrayList<>(jobManager.getJobs());
        int size = Math.min(54, Math.max(9, (int) (Math.ceil(jobs.size() / 9.0) * 9)));

        JobMenuHolder holder = new JobMenuHolder();
        Inventory inv = plugin.getServer().createInventory(holder, size, Component.text("Job Menu"));
        holder.setInventory(inv);

        PlayerRecord record = dataStore.get(player.getUniqueId());
        String currentJobId = record != null ? record.getJobId() : null;

        for (Job job : jobs) {
            inv.addItem(buildIcon(job, job.getId().equalsIgnoreCase(currentJobId)));
        }
        player.openInventory(inv);
    }

    private ItemStack buildIcon(Job job, boolean current) {
        Material material = iconMaterial(job);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        Component name = Msg.parse(job.getDisplayName()).decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
        if (!job.getDescription().isBlank()) {
            lore.add(Component.text(job.getDescription(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Salary: ", NamedTextColor.DARK_GRAY)
                .append(Component.text("$" + String.format("%,.2f", job.getSalary()), NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        String slots = job.getMaxSlots() < 0 ? "Unlimited" : String.valueOf(job.getMaxSlots());
        lore.add(Component.text("Slots: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(slots, NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        if (job.isPolice()) {
            lore.add(Component.text("Law Enforcement", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false));
        }
        if (job.isWhitelisted()) {
            lore.add(Component.text("Whitelisted", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        if (current) {
            lore.add(Component.empty());
            lore.add(Component.text("✓ Current job", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.empty());
            lore.add(Component.text("Click to become this job", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(JOB_ID_KEY, PersistentDataType.STRING, job.getId());
        item.setItemMeta(meta);
        return item;
    }

    private Material iconMaterial(Job job) {
        for (String def : job.getKit()) {
            ItemStack parsed = ItemUtil.parseSingle(def);
            if (parsed != null) {
                return parsed.getType();
            }
        }
        if (job.isPolice()) {
            return Material.IRON_SWORD;
        }
        return Material.PLAYER_HEAD;
    }
}
