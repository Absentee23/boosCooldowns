package cz.boosik.boosCooldown.Listeners;

import cz.boosik.boosCooldown.BoosConfigManager;
import cz.boosik.boosCooldown.BoosWarmUpManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import util.boosChat;

public class BoosPlayerToggleSneakListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player != null
                && !player.hasPermission("booscooldowns.nocancel.sneak")) {
            if (BoosWarmUpManager.hasWarmUps(player)) {
                boosChat.sendMessageToPlayer(player,
                        BoosConfigManager.getCancelWarmupOnSneakMessage());
                BoosWarmUpManager.cancelWarmUps(player);
            }

        }
    }
}