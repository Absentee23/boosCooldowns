package cz.boosik.boosCooldown;

import cz.boosik.boosCooldown.Managers.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import util.boosChat;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoosCoolDownListener implements Listener {
    private static BoosCoolDown plugin;

    public BoosCoolDownListener(BoosCoolDown instance) {
        plugin = instance;
    }

    public static ConcurrentHashMap<String,Boolean> commandQueue = new ConcurrentHashMap();

    private void checkRestrictions(PlayerCommandPreprocessEvent event,
                                   Player player, String regexCommad, String originalCommand,
                                   int warmupTime, int cooldownTime, double price, String item,
                                   int count, int limit, int xpPrice) {
        boolean blocked = false;
        String perm = BoosConfigManager.getPermission(player, regexCommad);
        if (!(perm == null)) {
            if (!player.hasPermission(perm)) {
                String msg = BoosConfigManager.getPermissionMessage(player, regexCommad);
                if (!(msg == null)){
                    boosChat.sendMessageToPlayer(player, msg);
                }
                event.setCancelled(true);
            }
        }
        if (limit != -1) {
            blocked = BoosLimitManager.blocked(player, regexCommad,
                    originalCommand, limit);
        }
        if (!blocked && !event.isCancelled()) {
            if (warmupTime > 0) {
                if (!player.hasPermission("booscooldowns.nowarmup")
                        && !player.hasPermission("booscooldowns.nowarmup."
                        + originalCommand)) {
                    start(event, player, regexCommad, originalCommand,
                            warmupTime, cooldownTime);
                }
            } else if (BoosPriceManager.has(player, price)
                    & BoosItemCostManager.has(player, item, count)
                    & BoosXpCostManager.has(player, xpPrice)) {
                if (BoosCoolDownManager.coolDown(player, regexCommad,
                        originalCommand, cooldownTime)) {
                    event.setCancelled(true);
                }
            }
            if (BoosPriceManager.has(player, price)
                    & BoosItemCostManager.has(player, item, count)
                    & BoosXpCostManager.has(player, xpPrice)) {
                if (!event.isCancelled()) {
                    BoosPriceManager.payForCommand(event, player, regexCommad,
                            originalCommand, price);
                }
                if (!event.isCancelled()) {
                    BoosItemCostManager.payItemForCommand(event, player,
                            regexCommad, originalCommand, item, count);
                }
                if (!event.isCancelled()) {
                    BoosXpCostManager.payXPForCommand(event, player,
                            regexCommad, originalCommand, xpPrice);
                }
            } else {
                if (!BoosPriceManager.has(player, price)
                        & !BoosWarmUpManager.isWarmUpProcess(player,
                        regexCommad)) {
                    String unit;
                    String msg = "";
                    if (price == 1) {
                        unit = BoosCoolDown.getEconomy().currencyNameSingular();
                    } else {
                        unit = BoosCoolDown.getEconomy().currencyNamePlural();
                    }
                    msg = String.format(
                            BoosConfigManager.getInsufficientFundsMessage(),
                            (price + " " + unit),
                            BoosCoolDown.getEconomy().format(
                                    BoosCoolDown.getEconomy()
                                            .getBalance(player)));
                    msg = msg.replaceAll("&command&", originalCommand);
                    boosChat.sendMessageToPlayer(player, msg);
                }
                if (!BoosItemCostManager.has(player, item, count)
                        & !BoosWarmUpManager.isWarmUpProcess(player,
                        regexCommad)) {
                    String msg = "";
                    msg = String.format(
                            BoosConfigManager.getInsufficientItemsMessage(),
                            (count + " " + item));
                    msg = msg.replaceAll("&command&", originalCommand);
                    boosChat.sendMessageToPlayer(player, msg);
                }
                if (!BoosXpCostManager.has(player, xpPrice)
                        & !BoosWarmUpManager.isWarmUpProcess(player,
                        regexCommad)) {
                    String msg = "";
                    msg = String.format(
                            BoosConfigManager.getInsufficientXpMessage(),
                            (xpPrice));
                    msg = msg.replaceAll("&command&", originalCommand);
                    boosChat.sendMessageToPlayer(player, msg);
                }
                event.setCancelled(true);
            }
            if (!event.isCancelled()) {
                String msg = String.format(BoosConfigManager.getMessage(
                        regexCommad, player));
                if (!msg.equals("")) {
                    boosChat.sendMessageToPlayer(player, msg);
                }
            }
        } else {
            event.setCancelled(true);
        }
        if (!event.isCancelled()) {
            List<String> linkGroup = BoosConfigManager.getSharedLimits(
                    regexCommad, player);
                if (linkGroup.isEmpty()) {
                    BoosLimitManager.setUses(player, regexCommad);
                } else {
                    BoosLimitManager.setUses(player, regexCommad);
                    for (String a : linkGroup) {
                        BoosLimitManager.setUses(player, a);
                    }
                }
            if (BoosConfigManager.getCommandLogging()) {
                BoosCoolDown.commandLogger(player.getName(), originalCommand);
            }
        }
        for (String key : commandQueue.keySet()){
            if (key.startsWith(String.valueOf(player.getUniqueId()))){
                commandQueue.remove(key);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

	/* Removed because we use these -Absentee23
        if (event.getMessage().contains(":")) {
            Pattern p = Pattern.compile("^/([a-zA-Z0-9_]+):");
            Matcher m = p.matcher(event.getMessage());
            if (m.find()) {
                {
                    boosChat.sendMessageToPlayer(player, BoosConfigManager
                            .getInvalidCommandSyntaxMessage());
                    event.setCancelled(true);
                    return;
                }
            }
        } */
        for (String key : commandQueue.keySet()) {
            String[] keyList = key.split("@");
            if (keyList[0].equals(String.valueOf(uuid))) {
                if (!keyList[1].equals(event.getMessage())){
                    commandQueue.remove(key);
                    String commandCancelMessage = BoosConfigManager.getCommandCanceledMessage();
                    commandCancelMessage = commandCancelMessage.replace("&command&", keyList[1]);
                    boosChat.sendMessageToPlayer(player, commandCancelMessage);
                    event.setCancelled(true);
                    return;
                }
            }
        }

        String originalCommand = event.getMessage().replace("\\", "\\\\");
        originalCommand = originalCommand.replace("$", "SdollarS");
        originalCommand = originalCommand.trim().replaceAll(" +", " ");
        String regexCommad = "";
        Set<String> aliases = BoosConfigManager.getAliases();
        Set<String> commands = BoosConfigManager.getCommands(player);
        boolean on;
        String item = "";
        int count = 0;
        int warmupTime = 0;
        double price = 0;
        int limit = -1;
        int cooldownTime = 0;
        int xpPrice = 0;
        on = BoosCoolDown.isPluginOnForPlayer(player);
        if (aliases != null) {
            originalCommand = BoosAliasManager.checkCommandAlias(
                    originalCommand, aliases, player);
            event.setMessage(originalCommand);
        }
        if (on && commands != null) {
            for (String group : commands) {
                String group2 = group.replace("*", ".+");
                if (originalCommand.matches("(?i)" + group2)) {
                    regexCommad = group;
                    if (BoosConfigManager.getWarmupEnabled()) {
                        warmupTime = BoosConfigManager.getWarmUp(regexCommad,
                                player);
                    }
                    if (BoosConfigManager.getCooldownEnabled()) {
                        cooldownTime = BoosConfigManager.getCoolDown(
                                regexCommad, player);
                    }
                    if (BoosConfigManager.getPriceEnabled()) {
                        price = BoosConfigManager.getPrice(regexCommad, player);
                    }
                    if (BoosConfigManager.getXpPriceEnabled()) {
                        xpPrice = BoosConfigManager.getXpPrice(regexCommad,
                                player);
                    }
                    if (BoosConfigManager.getItemCostEnabled()) {
                        item = BoosConfigManager.getItemCostItem(regexCommad,
                                player);
                        count = BoosConfigManager.getItemCostCount(regexCommad,
                                player);
                    }
                    if (BoosConfigManager.getLimitEnabled()) {
                        limit = BoosConfigManager.getLimit(regexCommad, player);
                    }
                    break;
                }
            }
            if (commandQueue.keySet().contains(uuid + "@" + originalCommand) && commandQueue.get(uuid + "@" + originalCommand)) {
                this.checkRestrictions(event, player, regexCommad, originalCommand,
                        warmupTime, cooldownTime, price, item, count, limit,
                        xpPrice);
            } else {
                if (price > 0 || xpPrice > 0 || count > 0 || limit > 0) {
                    commandQueue.put(uuid + "@" + originalCommand, false);
                    String questionMessage = BoosConfigManager.getQuestionMessage();
                    questionMessage = questionMessage.replace("&command&", originalCommand);
                    boosChat.sendMessageToPlayer(player, questionMessage);
                    if (BoosCoolDown.getEconomy() != null) {
                        if (BoosConfigManager.getPriceEnabled()) {
                            if (price > 0) {
                                String priceMessage = BoosConfigManager.getItsPriceMessage();
                                if (price > 1) {
                                    priceMessage = priceMessage.replace("&price&", String.valueOf(price))
                                            .replace("&currency&", BoosCoolDown.getEconomy().currencyNamePlural())
                                            .replace("&balance&", String.valueOf(BoosCoolDown.getEconomy().getBalance(player)));
                                } else {
                                    priceMessage = priceMessage.replace("&price&", String.valueOf(price))
                                            .replace("&currency&", BoosCoolDown.getEconomy().currencyNameSingular())
                                            .replace("&balance&", String.valueOf(BoosCoolDown.getEconomy().getBalance(player)));
                                }
                                boosChat.sendMessageToPlayer(player, "    " + priceMessage);
                            }
                        }
                    }
                    if (xpPrice > 0) {
                        String xpMessage = BoosConfigManager.getItsXpPriceMessage();
                        xpMessage = xpMessage.replace("&xpprice&", String.valueOf(xpPrice));
                        boosChat.sendMessageToPlayer(player, "    " + xpMessage);
                    }
                    if (count > 0) {
                        String itemMessage = BoosConfigManager.getItsItemCostMessage();
                        itemMessage = itemMessage.replace("&itemprice&", String.valueOf(count)).replace("&itemname&", item);
                        boosChat.sendMessageToPlayer(player, "    " + itemMessage);
                    }
                    if (limit > 0) {
                        int uses = BoosLimitManager.getUses(player, regexCommad);
                        String limitMessage = BoosConfigManager.getItsLimitMessage();
                        limitMessage = limitMessage.replace("&limit&", String.valueOf(limit))
                                .replace("&uses&", String.valueOf(limit - uses));
                        boosChat.sendMessageToPlayer(player, "    " + limitMessage);
                    }
                    boosChat.sendMessageToPlayer(player, "    &2" + BoosConfigManager.getConfirmCommandMessage());
                    boosChat.sendMessageToPlayer(player, "    &c" + BoosConfigManager.getCancelCommandMessage());
                    event.setCancelled(true);
                    return;
                } else {
                    commandQueue.put(player.getUniqueId() + "@" + originalCommand, true);
                }
            }
        }

        originalCommand = originalCommand.replace("SdollarS", "$");
        event.setMessage(originalCommand);
    }
    @EventHandler(priority = EventPriority.NORMAL)
    private void onPlayerChatEvent(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        for(String key : commandQueue.keySet()) {
            String[] keyList = key.split("@");
            if (keyList[0].equals(String.valueOf(uuid))) {
                if (event.getMessage().equalsIgnoreCase(BoosConfigManager.getConfirmCommandMessage())) {
                    commandQueue.put(key, true);
                    player.chat(keyList[1]);
                    event.setCancelled(true);
                } else {
                    commandQueue.remove(key);
                    String commandCancelMessage = BoosConfigManager.getCommandCanceledMessage();
                    commandCancelMessage = commandCancelMessage.replace("&command&", keyList[1]);
                    boosChat.sendMessageToPlayer(player, commandCancelMessage);
                    event.setCancelled(true);
                }
            }
        }
    }
    private void start(PlayerCommandPreprocessEvent event, Player player,
                       String regexCommad, String originalCommand, int warmupTime,
                       int cooldownTime) {
        if (!BoosWarmUpManager.checkWarmUpOK(player, regexCommad)) {
            if (BoosCoolDownManager.checkCoolDownOK(player, regexCommad,
                    originalCommand, cooldownTime)) {
                BoosWarmUpManager.startWarmUp(plugin, player, regexCommad,
                        originalCommand, warmupTime);
                event.setCancelled(true);
            } else {
                event.setCancelled(true);
            }
        } else if (BoosCoolDownManager.coolDown(player, regexCommad,
                originalCommand, cooldownTime)) {
            event.setCancelled(true);
        } else {
            BoosWarmUpManager.removeWarmUpOK(player, regexCommad);
        }
    }
}
