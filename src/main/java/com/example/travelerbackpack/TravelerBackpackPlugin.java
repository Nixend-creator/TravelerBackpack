package com.example.travelerbackpack;

import com.example.travelerbackpack.backpack.BackpackManager;
import com.example.travelerbackpack.listener.PlayerListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TravelerBackpackPlugin extends JavaPlugin {

    public static final NamespacedKey BACKPACK_IDENTIFIER = new NamespacedKey(this, "traveler_backpack");

    private BackpackManager backpackManager;

    @Override
    public void onEnable() {
        this.backpackManager = new BackpackManager(this);

        getCommand("backpack").setExecutor((sender, command, label, args) -> {
            if (sender.hasPermission("travelerbackpack.use") && sender instanceof Player player) {
                backpackManager.openBackpack(player);
                return true;
            }
            sender.sendMessage("§cНет прав.");
            return false;
        });

        getCommand("backpackgive").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("travelerbackpack.give")) {
                sender.sendMessage("§cНет прав.");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage("§cИспользование: /backpackgive <player>");
                return true;
            }

            Player target = getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cИгрок не найден.");
                return true;
            }

            ItemStack backpackItem = createBackpackItem();
            target.getInventory().addItem(backpackItem);
            sender.sendMessage("§aРюкзак выдан игроку " + target.getName());
            target.sendMessage("§eВы получили рюкзак! Используйте ПКМ, чтобы открыть.");

            return true;
        });

        getServer().getPluginManager().registerEvents(new PlayerListener(backpackManager), this);

        getLogger().info("TravelerBackpack включён!");
    }

    @Override
    public void onDisable() {
        backpackManager.saveAll();
        getLogger().info("TravelerBackpack выключен.");
    }

    private ItemStack createBackpackItem() {
        ItemStack item = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "🎒 Рюкзак Путешественника");

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(BACKPACK_IDENTIFIER, PersistentDataType.STRING, "true");

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBackpack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(BACKPACK_IDENTIFIER, PersistentDataType.STRING);
    }

    public BackpackManager getBackpackManager() {
        return backpackManager;
    }
}
