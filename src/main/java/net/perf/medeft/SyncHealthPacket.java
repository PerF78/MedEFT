package net.perf.medeft;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncHealthPacket {
    private final LimbHealth limbHealth;



    public SyncHealthPacket(LimbHealth limbHealth) {
        this.limbHealth = limbHealth;
    }

    public static void encode(SyncHealthPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.limbHealth.getHeadHealth());
        buffer.writeFloat(packet.limbHealth.getBodyHealth());
        buffer.writeFloat(packet.limbHealth.getArmsHealth());
        buffer.writeFloat(packet.limbHealth.getLegsHealth());
    }

    public static SyncHealthPacket decode(FriendlyByteBuf buffer) {
        float headHealth = buffer.readFloat();
        float bodyHealth = buffer.readFloat();
        float armsHealth = buffer.readFloat();
        float legsHealth = buffer.readFloat();
        return new SyncHealthPacket(new LimbHealth(headHealth, bodyHealth, armsHealth, legsHealth));
    }

    public static void handle(SyncHealthPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getPersistentData().putFloat("head_health", packet.limbHealth.getHeadHealth());
                player.getPersistentData().putFloat("body_health", packet.limbHealth.getBodyHealth());
                player.getPersistentData().putFloat("arms_health", packet.limbHealth.getArmsHealth());
                player.getPersistentData().putFloat("legs_health", packet.limbHealth.getLegsHealth());

                MedEFT modInstance = new MedEFT();


                if (modInstance != null) {
                    modInstance.limbHealthOverlay.updateLimbHealth(packet.limbHealth);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
