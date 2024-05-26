package net.perf.medeft.registry;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.perf.medeft.MedEFT;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MedEFT.MOD_ID);

    public static final RegistryObject<Item> MEDICAL_SPLINT = ITEMS.register("medical_splint",
            () -> new Item(new Item.Properties().tab(ModCreativeTabs.MEDEFT_TAB).stacksTo(1)));

    public static final RegistryObject<Item> LIFESAVER_TOURNIQUET = ITEMS.register("lifesaver_tourniquet",
            () -> new Item(new Item.Properties().tab(ModCreativeTabs.MEDEFT_TAB).stacksTo(1)));

    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage",
            () -> new Item(new Item.Properties().tab(ModCreativeTabs.MEDEFT_TAB)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}