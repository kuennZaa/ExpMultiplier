package dev.expmulti;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ExpMultiplier extends JavaPlugin implements Listener {

    private double multiplier = 20.0;
    private boolean clumpEnabled = true;
    private double clumpRadius = 5.0;
    private int clumpInterval = 10;
    private long clumpMaxValue = Integer.MAX_VALUE;
    private BukkitTask clumpTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadSettings() {
        reloadConfig();
        multiplier = getConfig().getDouble("multiplier", 20.0);
        clumpEnabled = getConfig().getBoolean("clump.enabled", true);
        clumpRadius = Math.max(0.5, getConfig().getDouble("clump.radius", 5.0));
        clumpInterval = Math.max(1, getConfig().getInt("clump.interval-ticks", 10));
        long max = getConfig().getLong("clump.max-orb-value", 0L);
        clumpMaxValue = (max <= 0 || max > Integer.MAX_VALUE) ? Integer.MAX_VALUE : max;

        if (clumpTask != null) {
            clumpTask.cancel();
            clumpTask = null;
        }
        if (clumpEnabled) {
            clumpTask = Bukkit.getScheduler().runTaskTimer(this, this::clumpOrbs, clumpInterval, clumpInterval);
        }
    }

    /** รวมลูก XP ที่อยู่ใกล้ผู้เล่นให้เป็นลูกเดียว (ค่า XP รวมกัน) */
    private void clumpOrbs() {
        Set<UUID> handled = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            double range = 32.0;
            for (Entity e : p.getNearbyEntities(range, range, range)) {
                if (!(e instanceof ExperienceOrb orb)) continue;
                if (!orb.isValid() || handled.contains(orb.getUniqueId())) continue;

                long total = (long) orb.getExperience() * Math.max(1, orb.getCount());
                boolean merged = false;

                for (Entity n : orb.getNearbyEntities(clumpRadius, clumpRadius, clumpRadius)) {
                    if (!(n instanceof ExperienceOrb other)) continue;
                    if (!other.isValid() || handled.contains(other.getUniqueId())) continue;

                    long otherTotal = (long) other.getExperience() * Math.max(1, other.getCount());
                    if (total + otherTotal > clumpMaxValue) continue;

                    total += otherTotal;
                    other.remove();
                    handled.add(other.getUniqueId());
                    merged = true;
                }

                if (merged) {
                    orb.setCount(1);
                    orb.setExperience((int) total);
                }
                handled.add(orb.getUniqueId());
            }
        }
    }

    // ตัวคูณทำงานตอนเก็บ XP จึงคูณค่าที่รวมก้อนแล้วให้อัตโนมัติ
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExpChange(PlayerExpChangeEvent e) {
        int amount = e.getAmount();
        if (amount <= 0) return;
        long result = Math.round(amount * multiplier);
        e.setAmount((int) Math.min(result, Integer.MAX_VALUE));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            sender.sendMessage("ExpMultiplier: ตัวคูณ x" + multiplier
                    + " | รวมก้อน XP: " + (clumpEnabled ? "เปิด" : "ปิด"));
            return true;
        }
        sender.sendMessage("ใช้: /expmulti reload");
        return true;
    }

    @Override
    public void onDisable() {
        if (clumpTask != null) clumpTask.cancel();
    }
}
