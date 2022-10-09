package me.byteful.plugin.pictureads;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.UUID;

public class ImageRenderer extends MapRenderer {
  private final Set<UUID> displayingTo;
  private final BufferedImage image;
  private boolean rendered = false;

  public ImageRenderer(Set<UUID> displayingTo, BufferedImage image) {
    this.displayingTo = displayingTo;
    this.image = image;
  }

  @Override
  public void render(MapView map, MapCanvas canvas, Player player) {
    if (!displayingTo.contains(player.getUniqueId())) {
      return;
    }
    if (rendered) {
      return;
    }

    rendered = true;
    canvas.drawImage(0, 0, image);
  }
}