package dev.expmulti;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ExpMultiplier extends JavaPlugin implements Listener {

    private double multiplier = 20.0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMultiplier();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadMultiplier() {
        reloadConfig();
        multiplier = getConfig().getDouble("multiplier", 20.0);
    }

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
            loadMultiplier();
            sender.sendMessage("ExpMultiplier: ตัวคูณตอนนี้ x" + multiplier);
            return true;
        }
        sender.sendMessage("ใช้: /expmulti reload");
        return true;
    }
}
