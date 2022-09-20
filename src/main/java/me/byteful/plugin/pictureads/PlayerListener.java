package me.byteful.plugin.pictureads;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class PlayerListener implements Listener {
  private final PictureAdsPlugin plugin;

  public PlayerListener(PictureAdsPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (isPlayerViewing(event.getWhoClicked()) && event.getClickedInventory() == event.getWhoClicked().getInventory() && event.getSlot() == 40) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onOffhandSwitch(PlayerSwapHandItemsEvent event) {
    if (isPlayerViewing(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  private boolean isPlayerViewing(HumanEntity entity) {
    return plugin.offhandItems.containsKey(entity.getUniqueId());
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (isPlayerViewing(event.getEntity())) {
      plugin.cancel(event.getEntity());
    }
  }

  @EventHandler
  public void onWorldSwitch(PlayerChangedWorldEvent event) {
    if (isPlayerViewing(event.getPlayer())) {
      plugin.cancel(event.getPlayer());
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (isPlayerViewing(event.getPlayer())) {
      plugin.cancel(event.getPlayer());
    }
  }
}