package cz.boosik.boosCooldown;

import static cz.boosik.boosCooldown.Managers.BoosItemCostManager.getItemStackJson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.coloredcarrot.mcapi.json.JSON;
import com.coloredcarrot.mcapi.json.JSONClickAction;
import com.coloredcarrot.mcapi.json.JSONColor;
import com.coloredcarrot.mcapi.json.JSONComponent;
import com.coloredcarrot.mcapi.json.JSONHoverAction;
import cz.boosik.boosCooldown.Managers.BoosAliasManager;
import cz.boosik.boosCooldown.Managers.BoosConfigManager;
import cz.boosik.boosCooldown.Managers.BoosCoolDownManager;
import cz.boosik.boosCooldown.Managers.BoosItemCostManager;
import cz.boosik.boosCooldown.Managers.BoosLimitManager;
import cz.boosik.boosCooldown.Managers.BoosPriceManager;
import cz.boosik.boosCooldown.Managers.BoosWarmUpManager;
import cz.boosik.boosCooldown.Managers.BoosXpCostManager;
import util.boosChat;

public class BoosCoolDownListener implements Listener {
    public static Map<String, Boolean> commandQueue = new ConcurrentHashMap<>();
    private static BoosCoolDown plugin;

    BoosCoolDownListener(BoosCoolDown instance) {
        plugin = instance;
    }

    private void checkRestrictions(PlayerCommandPreprocessEvent event,
                                   Player player, String regexCommad, String originalCommand,
                                   int warmupTime, int cooldownTime, double price, String item,
                                   int count, String name, List<String> lore, List<String> enchants, int limit, int xpPrice, int xpRequirement) {
        boolean blocked = false;
        String perm = BoosConfigManager.getPermission(player, regexCommad);
        if (!(perm == null)) {
            if (!player.hasPermission(perm)) {
                String msg = BoosConfigManager.getPermissionMessage(player, regexCommad);
                if (!(msg == null)) {
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
                    & BoosItemCostManager.has(player, item, count, name, lore, enchants)
                    & BoosXpCostManager.has(player, xpPrice)
                    & BoosXpCostManager.has(player, xpRequirement)) {
                if (BoosCoolDownManager.coolDown(player, regexCommad,
                        originalCommand, cooldownTime)) {
                    event.setCancelled(true);
                }
            }
            if (BoosPriceManager.has(player, price)
                    & BoosItemCostManager.has(player, item, count, name, lore, enchants)
                    & BoosXpCostManager.has(player, xpPrice)
                    & BoosXpCostManager.has(player, xpRequirement)) {
                if (!event.isCancelled()) {
                    BoosPriceManager.payForCommand(event, player, regexCommad,
                            originalCommand, price);
                }
                if (!event.isCancelled()) {
                    BoosItemCostManager.payItemForCommand(event, player,
                            regexCommad, originalCommand, item, count, name, lore, enchants);
                }
                if (!event.isCancelled()) {
                    BoosXpCostManager.payXPForCommand(event, player,
                            regexCommad, originalCommand, xpPrice);
                }
            } else {
                boolean warmupInProgress = BoosWarmUpManager.isWarmUpProcess(player, regexCommad);
                boolean cooldownInProgress = BoosCoolDownManager.isCoolingdown(player, regexCommad, cooldownTime);
                if (!BoosPriceManager.has(player, price)
                        && !warmupInProgress && !cooldownInProgress) {
                    String msg;
                    msg = String.format(
                            BoosConfigManager.getInsufficientFundsMessage(),
                            BoosCoolDown.getEconomy().format(price),
                            BoosCoolDown.getEconomy().format(
                                    BoosCoolDown.getEconomy()
                                            .getBalance(player)));
                    msg = msg.replaceAll("&command&", originalCommand);
                    boosChat.sendMessageToPlayer(player, msg);
                }
                if (!BoosItemCostManager.has(player, item, count, name, lore, enchants)
                        && !warmupInProgress && !cooldownInProgress) {
                    String msg;
                    msg = String.format(
                            BoosConfigManager.getInsufficientItemsMessage(), "");
                    JSON json = getItemStackJson(1, item, count, name, lore, enchants);
                    msg = msg.replaceAll("&command&", originalCommand);
                    boosChat.sendMessageToPlayer(player, msg);
                    json.send(player);
                }
                if (!BoosXpCostManager.has(player, xpRequirement)
                        && !warmupInProgress && !cooldownInProgress) {
                    String msg;
                    msg = String.format(
                            BoosConfigManager.getInsufficientXpRequirementMessage(),
                            (xpRequirement));
                    msg = msg.replaceAll("&command&", originalCommand);
                    boosChat.sendMessageToPlayer(player, msg);
                }
                if (!BoosXpCostManager.has(player, xpPrice)
                        && !warmupInProgress && !cooldownInProgress) {
                    String msg;
                    msg = String.format(
                            BoosConfigManager.getInsufficientXpMessage(),
                            (xpPrice));
                    msg = msg.replaceAll("&command&", originalCommand);
                    boosChat.sendMessageToPlayer(player, msg);
                }
                event.setCancelled(true);
            }
            if (!event.isCancelled()) {
                String msg = BoosConfigManager.getMessage(
                        regexCommad, player);
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
        for (String key : commandQueue.keySet()) {
            if (key.startsWith(String.valueOf(player.getUniqueId()))) {
                commandQueue.remove(key);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (BoosConfigManager.getSyntaxBlocker() && !player.isOp() && !player.hasPermission("booscooldowns.syntaxblockerexception")) {
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
            }
        }
        if (BoosConfigManager.getConfirmCommandEnabled(player)) {
            for (String key : commandQueue.keySet()) {
                String[] keyList = key.split("@");
                if (keyList[0].equals(String.valueOf(uuid))) {
                    if (!keyList[1].equals(event.getMessage())) {
                        commandQueue.remove(key);
                        String commandCancelMessage = BoosConfigManager.getCommandCanceledMessage();
                        commandCancelMessage = commandCancelMessage.replace("&command&", keyList[1]);
                        boosChat.sendMessageToPlayer(player, commandCancelMessage);
                        event.setCancelled(true);
                        return;
                    }
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
        String name = "";
        List<String> lore = new ArrayList<>();
        List<String> enchants = new ArrayList<>();
        int count = 0;
        int warmupTime = 0;
        double price = 0;
        int limit = -1;
        int cooldownTime = 0;
        int xpPrice = 0;
        int xpRequirement = 0;
        on = BoosCoolDown.isPluginOnForPlayer(player);
        if (aliases != null) {
            originalCommand = BoosAliasManager.checkCommandAlias(
                    originalCommand, aliases, player);
            event.setMessage(originalCommand);
        }
        if (on && commands != null) {
            for (String group : commands) {
                String group2 = group.replace("*", ".*");
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
                        xpRequirement = BoosConfigManager.getXpRequirement(regexCommad, player);
                    }
                    if (BoosConfigManager.getItemCostEnabled()) {
                        item = BoosConfigManager.getItemCostItem(regexCommad,
                                player);
                        name = BoosConfigManager.getItemCostName(regexCommad,
                                player);
                        lore = BoosConfigManager.getItemCostLore(regexCommad,
                                player);
                        count = BoosConfigManager.getItemCostCount(regexCommad,
                                player);
                        enchants = BoosConfigManager.getItemCostEnchants(regexCommad,
                                player);
                    }
                    if (BoosConfigManager.getLimitEnabled()) {
                        limit = BoosConfigManager.getLimit(regexCommad, player);
                    }
                    break;
                }
            }
            if (!BoosConfigManager.getConfirmCommandEnabled(player) || (commandQueue
                    .keySet()
                    .contains(uuid + "@" + originalCommand) && commandQueue.get(uuid + "@" + originalCommand))) {
                this.checkRestrictions(event, player, regexCommad, originalCommand,
                        warmupTime, cooldownTime, price, item, count, name, lore, enchants, limit,
                        xpPrice, xpRequirement);
            } else {
                if ((price > 0 || xpPrice > 0 || count > 0 || limit > 0) && !BoosWarmUpManager.isWarmUpProcess(player,
                        regexCommad) && !BoosCoolDownManager.isCoolingdown(player, regexCommad, cooldownTime)) {
                    if (BoosConfigManager.getConfirmCommandEnabled(player)) {
                        commandQueue.put(uuid + "@" + originalCommand, false);
                        String questionMessage = BoosConfigManager.getQuestionMessage();
                        questionMessage = questionMessage.replace("&command&", originalCommand);
                        boosChat.sendMessageToPlayer(player, questionMessage);
                    }
                    if (BoosCoolDown.getEconomy() != null) {
                        if (BoosConfigManager.getPriceEnabled()) {
                            if (price > 0) {
                                String priceMessage = BoosConfigManager.getItsPriceMessage();
                                priceMessage = priceMessage.replace("&price&", BoosCoolDown.getEconomy().format(price))
                                        .replace("&balance&", BoosCoolDown.getEconomy().format(BoosCoolDown.getEconomy().getBalance(player)));
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
                        itemMessage = itemMessage.replace("&itemprice&", "").replace("&itemname&", "");
                        JSON json = getItemStackJson(2, item, count, name, lore, enchants);
                        boosChat.sendMessageToPlayer(player, "    " + itemMessage);
                        json.send(player);
                    }
                    if (limit > 0) {
                        int uses = BoosLimitManager.getUses(player, regexCommad);
                        String limitMessage = BoosConfigManager.getItsLimitMessage();
                        limitMessage = limitMessage.replace("&limit&", String.valueOf(limit))
                                .replace("&uses&", String.valueOf(limit - uses));
                        boosChat.sendMessageToPlayer(player, "    " + limitMessage);
                    }
                    String yesString = BoosConfigManager.getConfirmCommandMessage();
                    JSONClickAction yesClick = new JSONClickAction.RunCommand(yesString);
                    JSONHoverAction yesHover = new JSONHoverAction.ShowStringText(BoosConfigManager.getConfirmCommandHint());
                    JSONComponent yes = new JSONComponent("    " + yesString);
                    yes.setColor(JSONColor.GREEN).setBold(true);
                    yes.setClickAction(yesClick);
                    yes.setHoverAction(yesHover);
                    yes.send(player);

                    String noString = BoosConfigManager.getCancelCommandMessage();
                    JSONClickAction noClick = new JSONClickAction.RunCommand(noString);
                    JSONHoverAction noHover = new JSONHoverAction.ShowStringText(BoosConfigManager.getCancelCommandHint());
                    JSONComponent no = new JSONComponent("    " + noString);
                    no.setColor(JSONColor.RED).setBold(true);
                    no.setClickAction(noClick);
                    no.setHoverAction(noHover);
                    no.send(player);

                    event.setCancelled(true);
                    return;
                } else {
                    this.checkRestrictions(event, player, regexCommad, originalCommand,
                            warmupTime, cooldownTime, price, item, count, name, lore, enchants, limit,
                            xpPrice, xpRequirement);
                }
            }
        }
        originalCommand = originalCommand.replace("SdollarS", "$");
        event.setMessage(originalCommand);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onPlayerChatEvent(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (BoosConfigManager.getConfirmCommandEnabled(player)) {
            for (String key : commandQueue.keySet()) {
                final String[] keyList = key.split("@");
                if (keyList[0].equals(String.valueOf(uuid))) {
                    if (event.getMessage().equalsIgnoreCase(BoosConfigManager.getConfirmCommandMessage())) {
                        commandQueue.put(key, true);
                        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                player.chat(keyList[1]);
                            }
                        });
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