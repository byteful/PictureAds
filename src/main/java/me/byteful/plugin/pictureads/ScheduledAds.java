package me.byteful.plugin.pictureads;

import org.apache.commons.validator.routines.UrlValidator;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledAds implements Closeable {
  private final DateTimeFormatter formatter;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public ScheduledAds(PictureAdsPlugin plugin) {
    // schedule err thing
    final ZoneId timezone = ZoneId.of(plugin.getConfig().getString("timezone", "CST"), ZoneId.SHORT_IDS);
    this.formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a", Locale.US).withZone(timezone);

    final ConfigurationSection schedule = plugin.getConfig().getConfigurationSection("schedule");
    if (schedule == null) {
      return;
    }

    schedule.getValues(false).forEach((date, picture) -> {
      final ZonedDateTime then;
      try {
        then = formatter.parse(date, ZonedDateTime::from);
      } catch (Exception e) {
        plugin.getLogger().severe("Failed to parse date: " + date + " | Please follow format listed in config!");

        return;
      }
      final ZonedDateTime now = ZonedDateTime.now(timezone);

      if (then.isBefore(now)) {
        plugin.getLogger().warning("Found old scheduled date: " + date + " | Please remove this date from your config soon.");

        return;
      }

      scheduler.schedule(() -> {
        final String image = picture.toString();
        BufferedImage bufferedImage;
        if (UrlValidator.getInstance().isValid(image) && plugin.getConfig().getBoolean("urls")) {
          try {
            bufferedImage = ImageIO.read(new URL(image));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        } else {
          try {
            bufferedImage = ImageIO.read(new File(plugin.getDataFolder(), image));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }

        plugin.getLogger().info("Scheduled advertisement for '" + date + "' is being broadcast now...");
        Bukkit.getScheduler().runTask(plugin, () -> plugin.broadcast(bufferedImage));
      }, Duration.between(now, then).toMillis(), TimeUnit.MILLISECONDS);
    });
  }

  @Override
  public void close() throws SecurityException {
    scheduler.shutdownNow();
  }
}