package nms;

import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_9_R1.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_9_R1.PacketPlayOutChat;

/**
 * All rights reserved.
 *
 * @author ColoredCarrot
 */
public class NMSHook_v1_9_R1
        implements NMSHook {

    public void sendJSON(String json, Player player) {

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(ChatSerializer.a(json)));

    }

    public void sendActionBar(String json, Player player) {

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(ChatSerializer.a(json), (byte) 2));

    }

}
