package net.perf.medeft.registry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.perf.medeft.MedEFT;

@Mod.EventBusSubscriber(modid = MedEFT.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeTabs {
    public static final CreativeModeTab MEDEFT_TAB = new CreativeModeTab("medeft_tab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.MEDICAL_SPLINT.get());

        }
    };
}