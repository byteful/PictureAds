package me.byteful.plugin.pictureads;

import net.coobird.thumbnailator.Thumbnails;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.commandmanager.Messages;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class PictureAdsPlugin extends JavaPlugin {
  final Map<UUID, ItemStack> offhandItems = new HashMap<>();
  ScheduledAds scheduledAds;
  private Messages messages;
  UpdateChecker updateChecker;
  BukkitTask updateTask;

  /**
   * Puts the provided image on a map in players' offhand.
   * Uses settings configured in the config.yml such as the delay, ignore_holding, etc.
   * <p>
   * This method is for developers to externally call broadcasts in PictureAds.
   *
   * @param image the image to display
   */
  public static void sendBroadcast(BufferedImage image) {
    JavaPlugin.getPlugin(PictureAdsPlugin.class).broadcast(image);
  }

  /**
   * Forcefully closes the displayed broadcast for the specified player.
   * This method will not do anything if the player doesn't have an active advertisement open.
   * <p>
   * This method is for developers to externally forcefully close broadcasts in PictureAds.
   *
   * @param player the player to force-close the advertisement
   */
  public static void forceEndBroadcast(Player player) {
    JavaPlugin.getPlugin(PictureAdsPlugin.class).cancel(player);
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    loadMessages();
    new CommandParser(getResource("commands.rdcml")).parse().register(this, "pictureads", new CommandHandler(this));
    getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    scheduledAds = new ScheduledAds(this);
    updateChecker = new UpdateChecker(this);
    if (getConfig().getBoolean("update", true)) {
      updateTask = Bukkit.getScheduler().runTaskTimer(this, () -> updateChecker.check(), 0L, 20L * TimeUnit.DAYS.toSeconds(1)); // every day
    }
  }

  @Override
  public void onDisable() {
    if (scheduledAds != null) {
      scheduledAds.close();
    }
  }

  void loadMessages() {
    messages = Messages.load(this);
  }

  Messages getMessages() {
    return messages;
  }

  void broadcast(BufferedImage image) {
    try {
      image = Thumbnails.of(image).size(128, 128).asBufferedImage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    final ImageRenderer renderer = new ImageRenderer(Bukkit.getOnlinePlayers().stream().map(Entity::getUniqueId).collect(Collectors.toSet()), image);
    final ItemStack map = new ItemStack(Material.FILLED_MAP);
    final MapMeta meta = (MapMeta) map.getItemMeta();
    final MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
    view.getRenderers().forEach(view::removeRenderer);
    view.addRenderer(renderer);
    meta.setMapView(view);
    map.setItemMeta(meta);

    Collection<? extends Player> players = Bukkit.getOnlinePlayers();
    if (getConfig().getBoolean("ignore_holding")) {
      players = players.stream().filter(x -> x.getInventory().getItemInOffHand().getType() == Material.AIR).collect(Collectors.toSet());
    }

    for (Player player : players) {
      if (offhandItems.containsKey(player.getUniqueId())) {
        cancel(player);
      }

      offhandItems.put(player.getUniqueId(), player.getInventory().getItemInOffHand());
      player.getInventory().setItemInOffHand(map);
      if (getConfig().getBoolean("message")) {
        player.sendMessage(messages.get("ad_alert"));
      }
    }

    Collection<? extends Player> finalPlayers = players;
    Bukkit.getScheduler().runTaskLater(this, () -> {
      for (Player player : finalPlayers) {
        cancel(player);
      }
    }, getConfig().getLong("delay"));
  }

  void cancel(Player player) {
    final ItemStack item = offhandItems.remove(player.getUniqueId());
    if (item == null) {
      return;
    }

    player.getInventory().setItemInOffHand(item);
  }

  public UpdateChecker getUpdateChecker() {
    return updateChecker;
  }
}
