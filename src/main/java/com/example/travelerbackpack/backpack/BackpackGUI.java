package com.example.travelerbackpack.backpack;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BackpackGUI {

    public static Inventory createGUI(BackpackManager manager, Player player) {
        Inventory inv = manager.getOrCreateBackpack(player);
        Inventory gui = Bukkit.createInventory(null, 45, "🎒 Рюкзак Путешественника");

        // Копируем содержимое рюкзака
        for (int i = 0; i < 36; i++) {
            gui.setItem(i, inv.getItem(i));
        }

        // Слоты для инструментов (39-42)
        for (int i = 0; i < 4; i++) {
            ItemStack toolSlot = manager.getToolSlot(player, i);
            if (toolSlot != null) {
                if (player.getInventory().getItemInMainHand().isSimilar(toolSlot) && player.getInventory().getHeldItemSlot() == i) {
                    ItemStack activeTool = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                    ItemMeta meta = activeTool.getItemMeta();
                    meta.setDisplayName("🟢 Активный: " + toolSlot.getItemMeta().getDisplayName());
                    activeTool.setItemMeta(meta);
                    gui.setItem(39 + i, activeTool);
                } else {
                    gui.setItem(39 + i, toolSlot);
                }
            } else {
                ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = placeholder.getItemMeta();
                meta.setDisplayName(" ");
                placeholder.setItemMeta(meta);
                gui.setItem(39 + i, placeholder);
            }
        }

        // Кнопка "Сортировать"
        ItemStack sortButton = new ItemStack(Material.HOPPER);
        ItemMeta meta = sortButton.getItemMeta();
        meta.setDisplayName("§eСортировать");
        sortButton.setItemMeta(meta);
        gui.setItem(43, sortButton);

        // Кнопка "Фильтр"
        ItemStack filterButton = new ItemStack(Material.REDSTONE);
        ItemMeta filterMeta = filterButton.getItemMeta();
        filterMeta.setDisplayName("§bФильтр: " + manager.getFilter(player).name());
        filterButton.setItemMeta(filterMeta);
        gui.setItem(44, filterButton);

        // Спальный мешок
        gui.setItem(45, createSleepingBag());

        return gui;
    }

    public static ItemStack createSleepingBag() {
        ItemStack item = new ItemStack(Material.RED_BED);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("🛏 Спальный мешок");
        List<String> lore = Arrays.asList(
                "§7Кликните ПКМ, чтобы отдохнуть до рассвета.",
                "§7Используется 1 раз в день."
        );
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static void handleClick(BackpackManager manager, Player player, int slot) {
        if (slot >= 0 && slot < 36) {
            // Обработка основного инвентаря
        }
        else if (slot == 43) { // Сортировать
            Inventory backpack = manager.getOrCreateBackpack(player);
            ItemStack[] contents = backpack.getContents();
            Arrays.sort(contents, (a, b) -> {
                if (a == null && b == null) return 0;
                if (a == null) return 1;
                if (b == null) return -1;
                return a.getType().name().compareTo(b.getType().name());
            });
            backpack.setContents(contents);
            player.openInventory(createGUI(manager, player));
        }
        else if (slot == 44) { // Сменить фильтр
            BackpackManager.FilterType current = manager.getFilter(player);
            BackpackManager.FilterType next = switch (current) {
                case ALL -> BackpackManager.FilterType.ORE;
                case ORE -> BackpackManager.FilterType.FOOD;
                case FOOD -> BackpackManager.FilterType.WOOD;
                case WOOD -> BackpackManager.FilterType.ALL;
            };
            manager.setFilter(player, next);
            player.openInventory(createGUI(manager, player));
        }
        else if (slot == 45) { // Спальный мешок
            if (manager.canUseSleepingBag(player)) {
                World world = player.getWorld();
                long time = world.getTime();
                if (time > 12541 && time < 23458) { // Ночное время
                    world.setTime(0); // Установить на рассвет

                    // Добавляем эффекты
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 0)); // 10 сек
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 20 * 5, 1));  // 5 сек, усиленный

                    // Визуальный эффект частиц
                    org.bukkit.Location loc = player.getLocation().add(0, 1, 0);
                    world.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, loc, 30, 0.5, 0.5, 0.5, 0.05);

                    player.sendMessage(ChatColor.YELLOW + "Вы немного отдохнули и проснулись с рассветом.");
                    manager.setSleepingBagUsedToday(player);
                } else {
                    player.sendMessage(ChatColor.RED + "Спальный мешок можно использовать только ночью.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Спальный мешок уже использован сегодня.");
            }
        }
    }
}
