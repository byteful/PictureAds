package me.byteful.plugin.pictureads;

import org.apache.commons.validator.routines.UrlValidator;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import redempt.redlib.commandmanager.CommandHook;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class CommandHandler {
  private final PictureAdsPlugin plugin;

  public CommandHandler(PictureAdsPlugin plugin) {
    this.plugin = plugin;
  }

  @CommandHook("reload")
  public void onReload(CommandSender sender) {
    plugin.reloadConfig();
    plugin.loadMessages();
    plugin.scheduledAds.close();
    plugin.scheduledAds = new ScheduledAds(plugin);
    plugin.updateTask.cancel();
    plugin.updateChecker = new UpdateChecker(plugin);
    if (plugin.getConfig().getBoolean("update", true)) {
      plugin.updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> plugin.updateChecker.check(), 0L, 20L * TimeUnit.DAYS.toSeconds(1)); // every day
    }
    sender.sendMessage(plugin.getMessages().get("reload"));
  }

  @CommandHook("debug")
  public void onDebug(CommandSender sender) {
    plugin.getUpdateChecker().check();
    sender.sendMessage("PictureAds Debug Information:");
    sender.sendMessage("- Server Version: " + Bukkit.getVersion());
    sender.sendMessage("- Server Type: " + Bukkit.getBukkitVersion());
    sender.sendMessage("- Plugin Version: " + plugin.getDescription().getVersion());
    sender.sendMessage("- Latest Version: " + plugin.getUpdateChecker().getLastCheckedVersion());
    sender.sendMessage("- Buyer: %%__USER__%%");
    sender.sendMessage("- Resource ID: %%__RESOURCE__%%");
    sender.sendMessage("- MC-Market?: %%__BUILTBYBIT__%%");
    sender.sendMessage("{!} Please include your configuration with this when asking for help. You MAY OMIT credentials. Please COPY AND PASTE configuration into discord server. {!}");
  }

  @CommandHook("broadcast")
  public void onBroadcast(CommandSender sender, String image) {
    CompletableFuture.runAsync(() -> {
      BufferedImage bufferedImage;
      if (UrlValidator.getInstance().isValid(image)) {
        // is URL
        if (!plugin.getConfig().getBoolean("urls")) {
          sender.sendMessage(plugin.getMessages().get("urls_disabled"));

          return;
        }

        try {
          sender.sendMessage(plugin.getMessages().get("loading_url"));
          bufferedImage = ImageIO.read(new URL(image));
        } catch (IOException ignored) {
          sender.sendMessage(plugin.getMessages().get("failed_to_load_url"));
          return;
        }

        if (bufferedImage == null) {
          sender.sendMessage(plugin.getMessages().get("failed_to_load_url"));
          return;
        }
      } else {
        // maybe file
        try {
          sender.sendMessage(plugin.getMessages().get("loading_file"));
          bufferedImage = ImageIO.read(new File(plugin.getDataFolder(), image));
        } catch (IOException ignored) {
          sender.sendMessage(plugin.getMessages().get("failed_to_find_file"));
          return;
        }

        if (bufferedImage == null) {
          sender.sendMessage(plugin.getMessages().get("failed_to_find_file"));
          return;
        }
      }

      // loaded image, now need to set up renderer to display for all players
      sender.sendMessage(plugin.getMessages().get("broadcasting_advertisement"));
      Bukkit.getScheduler().runTask(plugin, () -> plugin.broadcast(bufferedImage));
    });
  }
}