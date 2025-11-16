package com.example.travelerbackpack.listener;

import com.example.travelerbackpack.TravelerBackpackPlugin;
import com.example.travelerbackpack.backpack.BackpackManager;
import com.example.travelerbackpack.backpack.BackpackGUI;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final BackpackManager backpackManager;

    public PlayerListener(BackpackManager backpackManager) {
        this.backpackManager = backpackManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getHand() == EquipmentSlot.HAND && TravelerBackpackPlugin.isBackpack(item)) {
                event.setCancelled(true);
                backpackManager.openBackpack(player);
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        Item itemEntity = event.getItem();
        ItemStack item = itemEntity.getItemStack();

        if (backpackManager.shouldPickup(item, player)) {
            Inventory backpack = backpackManager.getOrCreateBackpack(player);
            if (backpack.firstEmpty() != -1) {
                backpack.addItem(item);
                event.setCancelled(true);
                itemEntity.remove();
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("🎒 Рюкзак Путешественника")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot >= 0 && slot < 36) {
            Inventory backpack = backpackManager.getOrCreateBackpack(player);
            backpack.setItem(slot, event.getCurrentItem());
        }
        else if (slot >= 39 && slot <= 42) { // Инструментальные слоты
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();

            if (cursor != null && isTool(cursor)) {
                backpackManager.setToolSlot(player, slot - 39, cursor);
                event.setCursor(current);
            } else if (cursor != null) {
                player.sendMessage("§cТолько инструменты можно поместить сюда.");
                event.setCancelled(true);
            } else if (current != null) {
                backpackManager.setToolSlot(player, slot - 39, null);
                event.setCursor(current);
            }
        }
        else {
            BackpackGUI.handleClick(backpackManager, player, slot);
        }

        if (event.getClickedInventory() != null && event.getWhoClicked().getOpenInventory().getTitle().equals("🎒 Рюкзак Путешественника")) {
            player.openInventory(BackpackGUI.createGUI(backpackManager, player));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(".setLayoutManager(Backpack Путешественника")) return;

        Player player = (Player) event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (isTool(inHand)) {
            int currentHotbarSlot = player.getInventory().getHeldItemSlot();
            backpackManager.setToolSlot(player, currentHotbarSlot, inHand);
            player.getInventory().setItemInMainHand(null);
        }
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int newSlot = event.getNewSlot();
        int oldSlot = event.getPreviousSlot();

        ItemStack tool = backpackManager.getToolSlot(player, newSlot);

        if (tool != null) {
            player.getInventory().setItemInMainHand(tool);

            ItemStack oldItem = player.getInventory().getItem(oldSlot);
            if (oldItem != null && isTool(oldItem)) {
                backpackManager.setToolSlot(player, oldSlot, oldItem);
            }
        }
    }

    private boolean isTool(ItemStack item) {
        if (item == null) return false;

        Material type = item.getType();
        String name = type.name();

        return name.contains("_PICKAXE") ||
               name.contains("_AXE") ||
               name.contains("_SHOVEL") ||
               name.contains("_HOE") ||
               name.contains("_SWORD");
    }
}
