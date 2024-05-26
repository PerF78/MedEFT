package net.perf.medeft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.perf.medeft.MedEFT;
import net.perf.medeft.SyncHealthPacket;
import org.lwjgl.glfw.GLFW;

public class LimbHealingScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("medeft", "textures/gui/limb_healing.png");
    private final Player player;
    private final Minecraft minecraft;
    private MedEFT medEFT;

    public LimbHealingScreen(Component title, Player player, Minecraft minecraft,MedEFT medEFT) {
        super(title);
        this.player = player;
        this.minecraft = minecraft;
        this.medEFT = medEFT;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        RenderSystem.setShaderTexture(0, BACKGROUND);
        int centerX = (this.width - 256) / 2;
        int centerY = (this.height - 256) / 2;
        this.blit(poseStack, centerX, centerY, 0, 0, 256, 256);

        drawCenteredString(poseStack, this.font, "Head", centerX + 128, centerY + 30, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, "Arms", centerX + 128, centerY + 60, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, "Body", centerX + 128, centerY + 90, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, "Legs", centerX + 128, centerY + 150, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.minecraft != null) {
            int centerX = (this.width - 256) / 2;
            int centerY = (this.height - 256) / 2;


            if (mouseX >= centerX && mouseX <= centerX + 256) {
                if (mouseY >= centerY + 20 && mouseY <= centerY + 40) {
                    medEFT.healLimb(player, "head");
                } else if (mouseY >= centerY + 50 && mouseY <= centerY + 70) {
                    medEFT.healLimb(player, "arms");
                } else if (mouseY >= centerY + 80 && mouseY <= centerY + 100) {
                    medEFT.healLimb(player, "body");
                } else if (mouseY >= centerY + 140 && mouseY <= centerY + 160) {
                    medEFT.healLimb(player, "legs");
                }
                this.minecraft.setScreen(null);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
