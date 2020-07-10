package cz.boosik.boosCooldown.Managers;

import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import cz.boosik.boosCooldown.BoosCoolDown;
import util.BoosChat;

public class BoosPlayerPointsManager {
    private static final PlayerPoints playerPoints = BoosCoolDown.getPlayerPoints();

    private static boolean payForCommand(
            final Player player,
            final String originalCommand, final int price) {
        if (playerPoints == null) {
            return true;
        }
        String msg = "";
        if(playerPoints.getAPI().take(player.getUniqueId(), price)) {
            msg = String.format(BoosConfigManager.getPlayerPointsForCommandMessage(),
                    price, playerPoints.getAPI().look(player.getUniqueId()));
            msg = msg.replaceAll("&command&", originalCommand);
            BoosChat.sendMessageToPlayer(player, msg);
            return true;
        } else {
            msg = String.format(BoosConfigManager.getInsufficientPlayerPointsMessage(),
                    price, playerPoints.getAPI().look(player.getUniqueId()));
            BoosChat.sendMessageToPlayer(player, msg);
            return false;
        }
    }

    public static void payForCommand(
            final PlayerCommandPreprocessEvent event,
            final Player player, final String regexCommand, final String originalCommand,
            final int price) {
        if (price > 0) {
            if (!player.hasPermission("booscooldowns.noplayerpoints")
                    && !player.hasPermission("booscooldowns.noplayerpoints."
                    + originalCommand)) {
                if (!payForCommand(player, originalCommand, price)) {
                    BoosCoolDownManager.cancelCooldown(player, regexCommand);
                    event.setCancelled(true);
                }
            }
        }
    }

    public static boolean has(final Player player, final int price) {
        return playerPoints == null || price <= 0 || playerPoints.getAPI().look(player.getUniqueId()) >= price;
    }
}
