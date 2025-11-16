package com.example.travelerbackpack.backpack;

import com.example.travelerbackpack.TravelerBackpackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class BackpackManager {
    private final TravelerBackpackPlugin plugin;
    private final Map<UUID, Inventory> backpacks = new HashMap<>();
    private final Map<UUID, ItemStack[]> toolSlots = new HashMap<>();
    private final Map<UUID, FilterType> filters = new HashMap<>();
    private final Map<UUID, LocalDate> sleepingBagUsage = new HashMap<>();
    private final File backpackDataFolder;
    private final File sleepingBagFile;
    private final YamlConfiguration sleepingBagConfig;

    public enum FilterType {
        ORE, FOOD, WOOD, ALL
    }

    public BackpackManager(TravelerBackpackPlugin plugin) {
        this.plugin = plugin;
        this.backpackDataFolder = new File(plugin.getDataFolder(), "data");
        if (!backpackDataFolder.exists()) backpackDataFolder.mkdirs();

        this.sleepingBagFile = new File(plugin.getDataFolder(), "sleeping_bag_usage.yml");
        if (!sleepingBagFile.exists()) {
            try {
                sleepingBagFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать sleeping_bag_usage.yml");
            }
        }
        this.sleepingBagConfig = YamlConfiguration.loadConfiguration(sleepingBagFile);
    }

    public Inventory getOrCreateBackpack(Player player) {
        return backpacks.computeIfAbsent(player.getUniqueId(), k -> Bukkit.createInventory(null, 36, "🎒 Ваш рюкзак"));
    }

    public void openBackpack(Player player) {
        loadBackpack(player);
        Inventory gui = BackpackGUI.createGUI(this, player);
        player.openInventory(gui);

        Location loc = player.getLocation();
        player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, loc, 20, 0.5, 0.5, 0.5);
    }

    public ItemStack getToolSlot(Player player, int index) {
        if (index < 0 || index > 3) return null;
        UUID uuid = player.getUniqueId();
        ItemStack[] slots = toolSlots.computeIfAbsent(uuid, k -> new ItemStack[4]);
        return slots[index];
    }

    public void setToolSlot(Player player, int index, ItemStack item) {
        if (index < 0 || index > 3) return;
        UUID uuid = player.getUniqueId();
        ItemStack[] slots = toolSlots.computeIfAbsent(uuid, k -> new ItemStack[4]);
        slots[index] = item;
    }

    public void setFilter(Player player, FilterType filterType) {
        filters.put(player.getUniqueId(), filterType);
    }

    public FilterType getFilter(Player player) {
        return filters.getOrDefault(player.getUniqueId(), FilterType.ALL);
    }

    public boolean shouldPickup(ItemStack item, Player player) {
        FilterType filter = getFilter(player);
        Material mat = item.getType();

        switch (filter) {
            case ORE:
                return mat.name().contains("ORE") || mat.name().contains("INGOT");
            case FOOD:
                return mat.isEdible();
            case WOOD:
                return mat.name().contains("LOG") || mat.name().contains("PLANKS") || mat.name().contains("WOOD");
            case ALL:
            default:
                return true;
        }
    }

    public boolean canUseSleepingBag(Player player) {
        String key = player.getUniqueId().toString();
        if (!sleepingBagConfig.contains(key)) return true;

        String lastUsedStr = sleepingBagConfig.getString(key);
        try {
            LocalDate lastUsed = LocalDate.parse(lastUsedStr);
            return !lastUsed.equals(LocalDate.now());
        } catch (Exception e) {
            return true;
        }
    }

    public void setSleepingBagUsedToday(Player player) {
        String key = player.getUniqueId().toString();
        sleepingBagConfig.set(key, LocalDate.now().toString());
        try {
            sleepingBagConfig.save(sleepingBagFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить состояние спального мешка.");
        }
    }

    public void saveBackpack(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = backpacks.get(uuid);
        if (inv != null) {
            File file = new File(backpackDataFolder, uuid + ".yml");
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("items", inv.getContents());
            cfg.set("tools", Arrays.asList(toolSlots.getOrDefault(uuid, new ItemStack[4])));

            try {
                cfg.save(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Не удалось сохранить рюкзак для " + uuid);
                e.printStackTrace();
            }
        }
    }

    public void saveAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            saveBackpack(player);
        }
    }

    public void loadBackpack(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(backpackDataFolder, uuid + ".yml");
        if (file.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            ItemStack[] items = ((List<ItemStack>) cfg.getList("items")).toArray(new ItemStack[0]);
            List<?> rawList = cfg.getList("tools");
            List<ItemStack> toolList = (List<ItemStack>) (List<?>) rawList;
            if (toolList != null && toolList.size() == 4) {
                ItemStack[] tools = toolList.toArray(new ItemStack[0]);
                toolSlots.put(uuid, tools);
            }

            Inventory inv = getOrCreateBackpack(player);
            inv.setContents(items);
        }
    }
}
