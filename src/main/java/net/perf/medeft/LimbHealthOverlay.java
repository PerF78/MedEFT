package net.perf.medeft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MedEFT.MOD_ID, value = Dist.CLIENT)
public class LimbHealthOverlay {
    private final Minecraft mc;
    private LimbHealth limbHealth;

    public LimbHealthOverlay(LimbHealth limbHealth) {
        this.mc = Minecraft.getInstance();
        this.limbHealth = limbHealth;
    }

    public void updateLimbHealth(LimbHealth limbHealth) {
        this.limbHealth = limbHealth;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        PoseStack poseStack = event.getPoseStack();
        int x = 10;
        int y = 10;


        Player player = mc.player;
        if (player != null) {
            float headHealth = limbHealth.getHeadHealth();
            float bodyHealth = limbHealth.getBodyHealth();
            float armsHealth = limbHealth.getArmsHealth();
            float legsHealth = limbHealth.getLegsHealth();

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (headHealth == 0) {
                mc.font.draw(poseStack, String.format("Head: %.1f (GOLOVA GLAZA)", headHealth), x, y, 0xFF0000);
            } else if (headHealth < 5) {
                mc.font.draw(poseStack, String.format("Head: %.1f (almost dead)", headHealth), x, y, 0xFFA500);
            } else {
                mc.font.draw(poseStack, String.format("Head: %.1f", headHealth), x, y, 0x00FF00);
            }
            y += 10;

            if (bodyHealth == 0) {
                mc.font.draw(poseStack, String.format("Body: %.1f (bleeding)", bodyHealth), x, y, 0xFF0000);

            } else if (bodyHealth < 6) {
                mc.font.draw(poseStack, String.format("Body: %.1f (shattered ribs)", bodyHealth), x, y, 0xFFA500);
            } else {
                mc.font.draw(poseStack, String.format("Body: %.1f", bodyHealth), x, y, 0x00FF00);
            }
            y += 10;

            if (armsHealth == 0) {
                mc.font.draw(poseStack, String.format("Arms: %.1f (broken)", armsHealth), x, y, 0xFF0000);
            } else if (armsHealth < 4.5) {
                mc.font.draw(poseStack, String.format("Arms: %.1f (pain)", armsHealth), x, y, 0xFFA500);
            } else {
                mc.font.draw(poseStack, String.format("Arms: %.1f", armsHealth), x, y, 0x00FF00);
            }
            y += 10;

            // Добавляем проверку для здоровья ног
            if (legsHealth == 0) {
                mc.font.draw(poseStack, String.format("Legs: %.1f (broken)", legsHealth), x, y, 0xFF0000);
            } else if (legsHealth < 4.5) {
                mc.font.draw(poseStack, String.format("Legs: %.1f (pain)", legsHealth), x, y, 0xFFA500);
            } else {
                mc.font.draw(poseStack, String.format("Legs: %.1f", legsHealth), x, y, 0x00FF00);
            }

        }
    }
}