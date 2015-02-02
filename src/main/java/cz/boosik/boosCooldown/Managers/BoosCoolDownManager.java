package cz.boosik.boosCooldown.Managers;

import org.bukkit.entity.Player;
import util.boosChat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BoosCoolDownManager {

    static void cancelCooldown(Player player, String regexCommand) {
        int pre2 = regexCommand.toLowerCase().hashCode();
        BoosConfigManager.getConfusers().set(
                "users." + player.getUniqueId() + ".cooldown." + pre2, null);
    }

    private static boolean cd(Player player, String regexCommand,
                              String originalCommand, int coolDownSeconds) {
        Date lastTime = getTime(player, regexCommand);
        List<String> linkGroup = BoosConfigManager.getSharedCooldowns(
                regexCommand, player);
        if (lastTime == null) {
            if (linkGroup.isEmpty()) {
                setTime(player, regexCommand);
            } else {
                setTime(player, regexCommand);
                for (String a : linkGroup) {
                    setTime(player, a);
                }
            }
            return false;
        } else {
            Calendar calcurrTime = Calendar.getInstance();
            calcurrTime.setTime(getCurrTime());
            Calendar callastTime = Calendar.getInstance();
            callastTime.setTime(lastTime);
            long secondsBetween = secondsBetween(callastTime, calcurrTime);
            long waitSeconds = coolDownSeconds - secondsBetween;
            long waitMinutes = (long) Math.ceil(waitSeconds / 60.0);
            long waitHours = (long) Math.ceil(waitMinutes / 60.0);
            if (secondsBetween > coolDownSeconds) {
                if (linkGroup.isEmpty()) {
                    setTime(player, regexCommand);
                } else {
                    setTime(player, regexCommand);
                    for (String a : linkGroup) {
                        setTime(player, a);
                    }
                }
                return false;
            } else {
                String msg = BoosConfigManager.getCoolDownMessage();
                msg = msg.replaceAll("&command&", originalCommand);
                if (waitSeconds >= 60 && 3600 >= waitSeconds) {
                    msg = msg.replaceAll("&seconds&",
                            Long.toString(waitMinutes));
                    msg = msg.replaceAll("&unit&",
                            BoosConfigManager.getUnitMinutesMessage());
                } else if (waitMinutes >= 60) {
                    msg = msg.replaceAll("&seconds&", Long.toString(waitHours));
                    msg = msg.replaceAll("&unit&",
                            BoosConfigManager.getUnitHoursMessage());
                } else {
                    String secs = Long.toString(waitSeconds);
                    if (secs.equals("0")) {
                        secs = "1";
                    }
                    msg = msg.replaceAll("&seconds&", secs);
                    msg = msg.replaceAll("&unit&",
                            BoosConfigManager.getUnitSecondsMessage());
                }
                boosChat.sendMessageToPlayer(player, msg);
                return true;
            }
        }
    }

    public static boolean coolDown(Player player, String regexCommand,
                                   String originalCommand, int time) {
        regexCommand = regexCommand.toLowerCase();
        return time > 0 && !player.hasPermission("booscooldowns.nocooldown") && !player.hasPermission("booscooldowns.nocooldown." + originalCommand) && cd(player, regexCommand, originalCommand, time);
    }

    private static Date getCurrTime() {
        String currTime = "";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        currTime = sdf.format(cal.getTime());
        Date time = null;

        try {
            time = sdf.parse(currTime);
            return time;
        } catch (ParseException e) {
            return null;
        }
    }

    private static Date getTime(Player player, String regexCommand) {
        int pre2 = regexCommand.toLowerCase().hashCode();
        String confTime = "";
        confTime = BoosConfigManager.getConfusers().getString(
                "users." + player.getUniqueId() + ".cooldown." + pre2, null);

        if (confTime != null && !confTime.equals("")) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            Date lastDate = null;

            try {
                lastDate = sdf.parse(confTime);
                return lastDate;
            } catch (ParseException e) {
                return null;
            }
        }
        return null;
    }

    public static boolean checkCoolDownOK(Player player, String regexCommand,
                                          String originalCommand, int time) {
        regexCommand = regexCommand.toLowerCase();
        if (time > 0) {
            Date lastTime = getTime(player, regexCommand);
            if (lastTime == null) {
                return true;
            } else {
                Calendar calcurrTime = Calendar.getInstance();
                calcurrTime.setTime(getCurrTime());
                Calendar callastTime = Calendar.getInstance();
                callastTime.setTime(lastTime);
                long secondsBetween = secondsBetween(callastTime, calcurrTime);
                long waitSeconds = time - secondsBetween;
                long waitMinutes = (long) Math.ceil(waitSeconds / 60.0);
                long waitHours = (long) Math.ceil(waitMinutes / 60.0);
                if (secondsBetween > time) {
                    return true;
                } else {
                    String msg = BoosConfigManager.getCoolDownMessage();
                    msg = msg.replaceAll("&command&", originalCommand);
                    if (waitSeconds >= 60 && 3600 >= waitSeconds) {
                        msg = msg.replaceAll("&seconds&",
                                Long.toString(waitMinutes));
                        msg = msg.replaceAll("&unit&",
                                BoosConfigManager.getUnitMinutesMessage());
                    } else if (waitMinutes >= 60) {
                        msg = msg.replaceAll("&seconds&",
                                Long.toString(waitHours));
                        msg = msg.replaceAll("&unit&",
                                BoosConfigManager.getUnitHoursMessage());
                    } else {
                        msg = msg.replaceAll("&seconds&",
                                Long.toString(waitSeconds));
                        msg = msg.replaceAll("&unit&",
                                BoosConfigManager.getUnitSecondsMessage());
                    }
                    boosChat.sendMessageToPlayer(player, msg);
                    return false;
                }
            }
        }
        return true;
    }

    private static long secondsBetween(Calendar startDate, Calendar endDate) {
        long secondsBetween = 0;
        secondsBetween = (endDate.getTimeInMillis() - startDate
                .getTimeInMillis()) / 1000;
        return secondsBetween;
    }

    private static void setTime(Player player, String regexCommand) {
        int pre2 = regexCommand.toLowerCase().hashCode();
        String currTime = "";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        currTime = sdf.format(cal.getTime());
        BoosConfigManager.getConfusers()
                .set("users." + player.getUniqueId() + ".cooldown." + pre2,
                        currTime);
    }

    public static void startAllCooldowns(Player player, String message) {
        for (String a : BoosConfigManager.getCooldowns(player)) {
            int cooldownTime = BoosConfigManager.getCoolDown(a, player);
            coolDown(player, a, message, cooldownTime);
        }

    }

}
