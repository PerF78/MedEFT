package net.perf.medeft;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.perf.medeft.client.gui.LimbHealingScreen;
import net.perf.medeft.registry.ModItems;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.*;

import static net.minecraft.world.item.ArmorMaterials.*;

@Mod(MedEFT.MOD_ID)
public class MedEFT {
    public static final String MOD_ID = "medeft";
    private static final Logger LOGGER = LogManager.getLogger();
    public Minecraft mc = Minecraft.getInstance();
    Component title = Component.translatable("Limb Healing");



    static final float headHP = 10.0f;
    static final float bodyHP = 12.0f;
    static final float armsHP = 9.0f;
    static final float legsHP = 9.0f;

    private LimbHealth limbHealth;
    final LimbHealthOverlay limbHealthOverlay;
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public MedEFT() {
        this.limbHealthOverlay = new LimbHealthOverlay(this.limbHealth);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();


        ModItems.register(modEventBus);

        modEventBus.addListener(this::setup);


        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(limbHealthOverlay);

        CHANNEL.registerMessage(0, SyncHealthPacket.class, SyncHealthPacket::encode, SyncHealthPacket::decode, SyncHealthPacket::handle);
    }

    private void setup(final FMLCommonSetupEvent event) {
    }


    @SubscribeEvent
    public void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!player.getPersistentData().contains("head_health")) {
                initializeLimbHealth(player);
            } else {
                syncHealthFromPlayer(player);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                sendHealthToClient(serverPlayer);
            }

            updatePlayerHealth(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player player = event.getEntity();
            initializeLimbHealth(player);

            if (player instanceof ServerPlayer serverPlayer) {
                sendHealthToClient(serverPlayer);
            }

            updatePlayerHealth(player);
        }
    }

    // логика получения урона от разных мобов
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            float damage = event.getAmount();

            if (event.getSource().isFall()) {
                handleFallDamage(player, damage);
                event.setCanceled(true); // Отменяем стандартный урон от падения
                return;
            } else if (event.getSource().isExplosion()) {
                handleExplosionDamage(player, damage, event.getSource());
                event.setCanceled(true); // Отменяем стандартный урон от взрыва
                return;

            } else if (event.getSource().isFire()) {
                handleFireDamage(player, damage);


            } else if (event.getSource().isMagic()) {
                handleFireDamage(player, damage);
                //event.setCanceled(true);
                return;

            } else if (event.getSource().getEntity() != null) {
                EntityType<?> entityType = event.getSource().getEntity().getType();
                ResourceLocation entityTypeName = ForgeRegistries.ENTITY_TYPES.getKey(entityType);

                if (entityTypeName != null) {
                    if (entityTypeName.toString().equals("minecraft:zombie")) {
                        handleZombieAttack(player, damage);
                        event.setCanceled(true);
                        return;
                    } else if (entityTypeName.toString().equals("minecraft:player")) {
                        handleSwordAttack(player, damage, (Player) event.getSource().getEntity());
                        event.setCanceled(true);
                        return;
                    } else if (entityTypeName.toString().equals("minecraft:skeleton")) {

                    } else if (entityTypeName.toString().equals("minecraft:spider")) {
                        handleSpiderAttack(player, damage);
                        event.setCanceled(true);
                        return;
                    } else if (entityTypeName.toString().equals("minecraft:enderman")) {
                        handleEndermanAttack(player, damage);
                        event.setCanceled(true);
                        return;
                    } else if (entityTypeName.toString().equals("minecraft:wolf")) {
                        handleWolfAttack(player, damage);
                        event.setCanceled(true);
                        return;
                    } else {
                        handleZombieAttack(player, damage);
                        event.setCanceled(true);
                    }
                }
            }

            // Получаем источник урона и направление атаки
            if (event.getSource().getDirectEntity() != null) {
                EntityType<?> entityType = event.getSource().getDirectEntity().getType();
                ResourceLocation entityTypeName = ForgeRegistries.ENTITY_TYPES.getKey(entityType);

                if (entityTypeName != null && entityTypeName.toString().equals("minecraft:arrow")) {
                    handleArrowAttack(player, damage, event.getSource().getDirectEntity().getY(), event.getSource());
                    event.setCanceled(true); // Отменяем стандартный урон от стрелы
                    return;
                }
            }

            displayLimbHealth(player);
            syncHealthWithPlayer(player);

            if (player instanceof ServerPlayer serverPlayer) {
                sendHealthToClient(serverPlayer);
            }

            updatePlayerHealth(player);
        }
    }

    private void updateLimbHealthWithArmor(Player player, String limb, float damage) {
        float protection = getArmorProtection(player, limb);
        float reducedDamage = damage * (1.0f - protection);
        updateLimbHealth(player, limb, reducedDamage);
    }

    private void handleEndermanAttack(Player player, float damage) {
        if (Math.random() < 0.5) {
            updateLimbHealthWithArmor(player, "body_health", damage * 0.6f);
        } else {
            updateLimbHealthWithArmor(player, "arms_health", damage * 0.4f);
        }

        displayLimbHealth(player);
        syncHealthWithPlayer(player);

        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        updatePlayerHealth(player);
    }

    private void handleSpiderAttack(Player player, float damage) {
        if (Math.random() < 0.5) {
            updateLimbHealthWithArmor(player, "legs_health", damage * 0.6f);
        } else {
            updateLimbHealthWithArmor(player, "arms_health", damage * 0.6f);
        }
        updateLimbHealthWithArmor(player, "body_health", damage * 0.4f);

        displayLimbHealth(player);
        syncHealthWithPlayer(player);

        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        updatePlayerHealth(player);
    }

    private void handleWolfAttack(Player player, float damage) {
        if (Math.random() < 0.5) {
            updateLimbHealthWithArmor(player, "legs_health", damage);
        } else {
            updateLimbHealthWithArmor(player, "arms_health", damage * 0.8f);
        }
        updateLimbHealthWithArmor(player, "body_health", damage * 0.3f);

        displayLimbHealth(player);
        syncHealthWithPlayer(player);

        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        updatePlayerHealth(player);
    }

    private void handleFireDamage(Player player, float damage) {
        updateLimbHealthWithArmor(player, "head_health", damage * 0.3f);
        updateLimbHealthWithArmor(player, "body_health", damage * 0.4f);
        updateLimbHealthWithArmor(player, "arms_health", damage * 0.3f);
        updateLimbHealthWithArmor(player, "legs_health", damage * 0.3f);

        displayLimbHealth(player);
        syncHealthWithPlayer(player);

        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        updatePlayerHealth(player);
    }

    private void handleFallDamage(Player player, float damage) {
        double fallDistance = player.fallDistance;
        float multiplier = getFallDamageMultiplier(player);

        if (fallDistance >= 5) { // Проверяем падение считается ли высоким
            if (fallDistance > 10) { // Высокое падение
                multiplier *= 1.4f; // увеличиваем множитель для высокого падения
            }

            // Рассчитываем урон для каждой конечности с учетом брони
            float legsDamage = damage * multiplier;
            float bodyDamage = legsDamage * 0.5f;
            float armsDamage = legsDamage * 0.4f;
            float headDamage = legsDamage * 0.3f;

            // Обновляем здоровье каждой конечности
            updateLimbHealthWithArmor(player, "legs_health", legsDamage);
            updateLimbHealthWithArmor(player, "body_health", bodyDamage);
            updateLimbHealthWithArmor(player, "arms_health", armsDamage);
            updateLimbHealthWithArmor(player, "head_health", headDamage);

            displayLimbHealth(player);
            syncHealthWithPlayer(player);

            if (player instanceof ServerPlayer serverPlayer) {
                sendHealthToClient(serverPlayer);
            }

            updatePlayerHealth(player);
        }
    }

    private void handleExplosionDamage(Player player, float damage, DamageSource source) {
        double yDifference = source.getEntity() != null ? source.getEntity().getY() - player.getY() : 0;
        double distance = player.distanceToSqr(source.getEntity() != null ? source.getEntity() : player);

        float proximityFactor = (float) Math.max(1.0 - (distance / 26.0), 0);

        if (yDifference > 1.5) {
            updateLimbHealthWithArmor(player, "head_health", damage * proximityFactor * 2.0f);
            updateLimbHealthWithArmor(player, "body_health", damage * proximityFactor * 0.5f);
            updateLimbHealthWithArmor(player, "arms_health", damage * proximityFactor * 0.3f);
            updateLimbHealthWithArmor(player, "legs_health", damage * proximityFactor * 0.1f);
        } else {
            updateLimbHealthWithArmor(player, "legs_health", damage * proximityFactor * 0.5f);
            updateLimbHealthWithArmor(player, "body_health", damage * proximityFactor * 0.3f);
            updateLimbHealthWithArmor(player, "arms_health", damage * proximityFactor * 0.4f);
            updateLimbHealthWithArmor(player, "head_health", damage * proximityFactor * 0.2f);
        }

        displayLimbHealth(player);
        syncHealthWithPlayer(player);

        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        updatePlayerHealth(player);
    }

    private void handleZombieAttack(Player player, float damage) {
        if (Math.random() < 0.25) {
            updateLimbHealthWithArmor(player, "head_health", damage * 0.2f);
        }
        updateLimbHealthWithArmor(player, "body_health", damage * 0.4f);
        updateLimbHealthWithArmor(player, "arms_health", damage * 0.2f);
        updateLimbHealthWithArmor(player, "legs_health", damage * 0.2f);

        displayLimbHealth(player);
        syncHealthWithPlayer(player);

        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        updatePlayerHealth(player);
    }

    private void handleSwordAttack(Player player, float damage, Player attacker) {
        double yDifference = attacker.getY() - player.getY();

        if (yDifference > 1.5) {
            updateLimbHealthWithArmor(player, "head_health", damage * 0.5f);
        } else if (yDifference > 0.5) {
            updateLimbHealthWithArmor(player, "body_health", damage * 0.5f);
        } else if (Math.random() < 0.5) {
            updateLimbHealthWithArmor(player, "arms_health", damage * 0.5f);
        } else {
            updateLimbHealthWithArmor(player, "legs_health", damage * 0.5f);
        }

        displayLimbHealth(player);
        syncHealthWithPlayer(player);

        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        updatePlayerHealth(player);
    }

    private void handleArrowAttack(Player player, float damage, double arrowY, DamageSource source) {
        double yDifference = arrowY - player.getY();
        boolean isLethalHeadshot = false;

        if (yDifference > 1.5) {
            if (player.getInventory().armor.get(3).isEmpty()) { // проверяем надет ли шлем
                player.getPersistentData().putFloat("head_health", 0.0f);// устанавливаем хп головы 0
                isLethalHeadshot = true;
            } else {
                updateLimbHealthWithArmor(player, "head_health", damage * 0.7f);
            }
        } else if (yDifference > 0.5) {
            updateLimbHealthWithArmor(player, "body_health", damage * 0.6f);
        } else if (Math.random() < 0.5) {
            updateLimbHealthWithArmor(player, "arms_health", damage * 0.5f);
        } else {
            updateLimbHealthWithArmor(player, "legs_health", damage * 0.5f);
        }

        displayLimbHealth(player);
        syncHealthWithPlayer(player);

        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        updatePlayerHealth(player);

        if (isLethalHeadshot && !player.isDeadOrDying()) {
            player.hurt(source, Float.MAX_VALUE); // сносим все хп если не убил предыдущий урон
        }
    }


    // обновляем значения каждый тик
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            syncHealthWithPlayer(player);
            updatePlayerHealth(player);
        }
    }

    private void initializeLimbHealth(Player player) {
        player.getPersistentData().putFloat("head_health", headHP);
        player.getPersistentData().putFloat("body_health", bodyHP);
        player.getPersistentData().putFloat("arms_health", armsHP);
        player.getPersistentData().putFloat("legs_health", legsHP);
    }

    private void updateLimbHealth(Player player, String limb, float damage) {
        float currentHealth = player.getPersistentData().getFloat(limb);

        if (currentHealth > 0) {
            float newHealth = Math.max(currentHealth - damage, 0);

            player.getPersistentData().putFloat(limb, newHealth);
            LOGGER.info("Updated " + limb + " to " + newHealth);
            this.limbHealth = new LimbHealth(
                    player.getPersistentData().getFloat("head_health"),
                    player.getPersistentData().getFloat("body_health"),
                    player.getPersistentData().getFloat("arms_health"),
                    player.getPersistentData().getFloat("legs_health")
            );
            limbHealthOverlay.updateLimbHealth(this.limbHealth); // обновляем оверлей с новыми значениями
            syncHealthWithPlayer(player);


            if (limb.equals("body_health") && ((newHealth <= bodyHP / 2) && (newHealth > 0))) {

                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, Integer.MAX_VALUE, 0, false, false));
            }
            if (limb.equals("body_health") && newHealth <= 0) {
                startWitherTimer(player,0.3f);
                //player.addEffect(new MobEffectInstance(MobEffects.WITHER, Integer.MAX_VALUE, 0, false, false));
            }
            if (limb.equals("legs_health") && ((newHealth <= legsHP / 2) && (newHealth > 0))) {

                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Integer.MAX_VALUE, 0, false, false));
            }
            if (limb.equals("legs_health") && newHealth <= 0) {

                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Integer.MAX_VALUE, 3, false, false));
            }
            if (limb.equals("arms_health") && ((newHealth <= armsHP / 2) && (newHealth > 0))) {

                //player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Integer.MAX_VALUE, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, Integer.MAX_VALUE, 0, false, false));
            }
            if (limb.equals("arms_health") && newHealth <= 0) {

                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Integer.MAX_VALUE, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, Integer.MAX_VALUE, 1, false, false));
            }
            if (limb.equals("head_health") && ((newHealth <= headHP / 4) && (newHealth > 0))) {
                //player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Integer.MAX_VALUE, 0, false, false));
            }
            if (limb.equals("head_health") && (newHealth <= 0)) {
                player.hurt(DamageSource.GENERIC, Float.MAX_VALUE);
                player.setHealth(0);// мгновенная смерть, если здоровье головы <= 0
            }
            if ((getHeadHealth(player) == 0) || (getLegsHealth(player) == 0)) {
                //redistributeDamage(player, limb, damage);
            }
        }
    }

    private float getHeadHealth(Player player) {
        float headHealth = player.getPersistentData().getFloat("head_health");
        return headHealth;
    }
    private float getLegsHealth(Player player) {
        float legsHealth = player.getPersistentData().getFloat("legs_health");
        return legsHealth;
    }


    public static void displayLimbHealth(Player player) {
        float headHealth = player.getPersistentData().getFloat("head_health");
        float bodyHealth = player.getPersistentData().getFloat("body_health");
        float armsHealth = player.getPersistentData().getFloat("arms_health");
        float legsHealth = player.getPersistentData().getFloat("legs_health");
    }

    public LimbHealth getLimbHealth() {
        return this.limbHealth;
    }

    public void syncHealthWithPlayer(Player player) {
        this.limbHealth = new LimbHealth(
                player.getPersistentData().getFloat("head_health"),
                player.getPersistentData().getFloat("body_health"),
                player.getPersistentData().getFloat("arms_health"),
                player.getPersistentData().getFloat("legs_health")
        );
        limbHealthOverlay.updateLimbHealth(this.limbHealth); // обновляем оверлей
    }

    private void syncHealthFromPlayer(Player player) {
        this.limbHealth = new LimbHealth(
                player.getPersistentData().getFloat("head_health"),
                player.getPersistentData().getFloat("body_health"),
                player.getPersistentData().getFloat("arms_health"),
                player.getPersistentData().getFloat("legs_health")
        );
        limbHealthOverlay.updateLimbHealth(this.limbHealth);
        LOGGER.info("Loaded health from player - Head: " + this.limbHealth.getHeadHealth()
                + ", Body: " + this.limbHealth.getBodyHealth()
                + ", Arms: " + this.limbHealth.getArmsHealth()
                + ", Legs: " + this.limbHealth.getLegsHealth());
    }

    private static void sendHealthToClient(ServerPlayer player) {
        LimbHealth health = new LimbHealth(
                player.getPersistentData().getFloat("head_health"),
                player.getPersistentData().getFloat("body_health"),
                player.getPersistentData().getFloat("arms_health"),
                player.getPersistentData().getFloat("legs_health")
        );
        CHANNEL.sendTo(new SyncHealthPacket(health), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static void updatePlayerHealth(Player player) {
        // делаем так что бы хп игрока зависило от хп конечностей
        float totalHealth = player.getPersistentData().getFloat("head_health") +
                player.getPersistentData().getFloat("body_health") +
                player.getPersistentData().getFloat("arms_health") +
                player.getPersistentData().getFloat("legs_health");

        // Устанавливаем максимальное хп и текущее хп игрока
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(totalHealth);
        player.setHealth(totalHealth);
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            ItemStack itemStack = event.getItem();
            if (event.getItem().getItem() == Items.GOLDEN_APPLE) { // теперь золотые яблоки восстанавливают хп
                healLimbWithLowestHealth(player);

            }
        }
    }

    public static float getLimbHealth(Player player, String limb) {

        switch (limb) {
            case "head":
                return player.getPersistentData().getFloat("head_health");
            case "body":
                return player.getPersistentData().getFloat("body_health");
            case "legs":
                return player.getPersistentData().getFloat("legs_health");
            case "arms":
                return player.getPersistentData().getFloat("arms_health");
            default:
                return 0.0f;
        }
    }

    // обработка выбора из оверлея
    public void healLimb(Player player, String limb) {
        switch (limb) {
            case "head":
                healSelectedLimb(player,"head_health", 2.0f);
                break;
            case "body":
                healSelectedLimb(player,"body_health", 2.0f);
                break;
            case "legs":
                healSelectedLimb(player,"legs_health", 2.0f);
                break;
            case "arms":
                healSelectedLimb(player,"arms_health", 2.0f);
                break;
        }
        displayLimbHealth(player);
        //syncHealthWithPlayer(player);

        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }
    }

    // новый метод хила с помощью бинта
    private void healSelectedLimb(Player player, String limb, float healAmount) {
        ItemStack bandageStack = null;

        // проверка на наличие бинта
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() == ModItems.BANDAGE.get()) {
                bandageStack = itemStack;
                break;
            }
        }

        // Если бинтов нет
        if (bandageStack == null) {
            LOGGER.info("No bandages available to heal " + limb + ".");
            return;
        }

        float limbHealth = player.getPersistentData().getFloat(limb);
        float limbMaxHealth = 0;
        switch (limb) {
            case "head_health":
                limbMaxHealth = headHP;
                break;
            case "body_health":
                limbMaxHealth = bodyHP;
                break;
            case "arms_health":
                limbMaxHealth = armsHP;
                break;
            case "legs_health":
                limbMaxHealth = legsHP;
                break;
        }

        if (limbHealth < 0.1f) {
            LOGGER.info("Cannot heal " + limb + " because its health is below the threshold.");
            return;
        }

        if (limbHealth >= limbMaxHealth) {
            LOGGER.info(limb + " is already at maximum health.");
            return;
        }

        // Убираем дебаффы с выбранной конечности
        switch (limb) {
            case "head_health":
                player.removeEffect(MobEffects.CONFUSION);
                break;
            case "body_health":
                player.removeEffect(MobEffects.HUNGER);
                player.removeEffect(MobEffects.WITHER);
                cancelWitherTimer();
                break;
            case "arms_health":
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
                break;
            case "legs_health":
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                break;
        }

        // Хилим выбранную конечность
        float newHealth = Math.min(limbHealth + healAmount, limbMaxHealth);
        player.getPersistentData().putFloat(limb, newHealth);
        LOGGER.info("Healed " + limb + " to " + newHealth);

        // Уменьшаем количество бинтов на 1
        bandageStack.shrink(1);

        // Обновляем общее хп конечностей и оверлей
        this.limbHealth = new LimbHealth(
                player.getPersistentData().getFloat("head_health"),
                player.getPersistentData().getFloat("body_health"),
                player.getPersistentData().getFloat("arms_health"),
                player.getPersistentData().getFloat("legs_health")
        );
        limbHealthOverlay.updateLimbHealth(this.limbHealth);

        // Синхронизируем хп с игроком
        syncHealthWithPlayer(player);

        // Отправляем хп клиенту
        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        // Обновляем хп игрока
        updatePlayerHealth(player);
    }


    // старый метод хила с помощью бинта
    private void healLimbWithBandage(Player player) {
        float headHealth = player.getPersistentData().getFloat("head_health");
        float bodyHealth = player.getPersistentData().getFloat("body_health");
        float armsHealth = player.getPersistentData().getFloat("arms_health");
        float legsHealth = player.getPersistentData().getFloat("legs_health");

        float[] healthArray = {headHealth, bodyHealth, armsHealth, legsHealth};
        String[] limbNames = {"head_health", "body_health", "arms_health", "legs_health"};
        float[] maxHealthArray = {headHP, bodyHP, armsHP, legsHP};

        float minHealth = Float.MAX_VALUE;
        int minIndex = -1;

        for (int i = 0; i < healthArray.length; i++) {
            if (healthArray[i] > 0.1 && healthArray[i] < minHealth) {
                minHealth = healthArray[i];
                minIndex = i;
            }
        }

        if (minIndex != -1) {
            float currentHealth = healthArray[minIndex];
            float newHealth = Math.min(currentHealth + (maxHealthArray[minIndex] / 2), maxHealthArray[minIndex]);
            player.getPersistentData().putFloat(limbNames[minIndex], newHealth);

            updatePlayerHealth(player);
        }
    }

    // хилл с помощью шины
    private void healLimbWithSplint(Player player) {
        ItemStack splintStack = null;

        // проверка на наличие шины
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() == ModItems.MEDICAL_SPLINT.get()) {
                splintStack = itemStack;
                break;
            }
        }

        // Если шины нет
        if (splintStack == null) {
            LOGGER.info("No splints available.");
            return;
        }

        float armsHealth = player.getPersistentData().getFloat("arms_health");
        float legsHealth = player.getPersistentData().getFloat("legs_health");
        float healAmount = 0.1f;

        if (armsHealth == 0) {
            float newHealth = healAmount;
            player.getPersistentData().putFloat("arms_health", newHealth);
            player.removeEffect(MobEffects.WEAKNESS);
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
        } else if (legsHealth == 0) {
            float newHealth = healAmount;
            player.getPersistentData().putFloat("legs_health", newHealth);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        } else {
            LOGGER.info("No limbs are critically damaged.");
            return;
        }

        // Уменьшаем количество шин на 1
        splintStack.shrink(1);

        // Обновляем общее хп конечностей и оверлей
        this.limbHealth = new LimbHealth(
                player.getPersistentData().getFloat("head_health"),
                player.getPersistentData().getFloat("body_health"),
                player.getPersistentData().getFloat("arms_health"),
                player.getPersistentData().getFloat("legs_health")
        );
        limbHealthOverlay.updateLimbHealth(this.limbHealth);

        // Синхронизируем хп с игроком
        syncHealthWithPlayer(player);

        // Отправляем хп клиенту
        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        // Обновляем хп игрока
        updatePlayerHealth(player);
    }

    // хилл с помощью жгута
    private void healLimbWithLifesaver(Player player) {
        ItemStack lifesavertStack = null;


        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() == ModItems.LIFESAVER_TOURNIQUET.get()) {
                lifesavertStack = itemStack;
                break;
            }
        }


        if (lifesavertStack == null) {

            return;
        }

        float bodyHealth = player.getPersistentData().getFloat("body_health");

        float healAmount = 0.1f;

        if (bodyHealth == 0) {
            float newHealth = healAmount;
            player.getPersistentData().putFloat("body_health", newHealth);
            cancelWitherTimer();
        } else {
            return;
        }


        lifesavertStack.shrink(1);

        // Обновляем общее хп конечностей и оверлей
        this.limbHealth = new LimbHealth(
                player.getPersistentData().getFloat("head_health"),
                player.getPersistentData().getFloat("body_health"),
                player.getPersistentData().getFloat("arms_health"),
                player.getPersistentData().getFloat("legs_health")
        );
        limbHealthOverlay.updateLimbHealth(this.limbHealth);

        // Синхронизируем хп с игроком
        syncHealthWithPlayer(player);

        // Отправляем хп клиенту
        if (player instanceof ServerPlayer serverPlayer) {
            sendHealthToClient(serverPlayer);
        }

        // Обновляем хп игрока
        updatePlayerHealth(player);
    }


    // хилл с помощью яблока
    private void healLimbWithLowestHealth(Player player) {
        float headHealth = player.getPersistentData().getFloat("head_health");
        float bodyHealth = player.getPersistentData().getFloat("body_health");
        float armsHealth = player.getPersistentData().getFloat("arms_health");
        float legsHealth = player.getPersistentData().getFloat("legs_health");

        float headHealthPercent = headHealth / headHP;
        float bodyHealthPercent = bodyHealth / bodyHP;
        float armsHealthPercent = armsHealth / armsHP;
        float legsHealthPercent = legsHealth / legsHP;

        Map<String, Float> limbHealthMap = new HashMap<>();
        if (headHealthPercent > 0.1) {
            limbHealthMap.put("head_health", headHealthPercent);
        }
        if (bodyHealthPercent > 0.1) {
            limbHealthMap.put("body_health", bodyHealthPercent);
        }
        if (armsHealthPercent > 0.1) {
            limbHealthMap.put("arms_health", armsHealthPercent);
        }
        if (legsHealthPercent > 0.1) {
            limbHealthMap.put("legs_health", legsHealthPercent);
        }

        if (limbHealthMap.isEmpty()) {
            LOGGER.info("No limb was healed because all limbs have health below the threshold.");
            return;
        }

        // поиск конечности с минимальный хп по отношению с ее макс. хп
        String limbToHeal = Collections.min(limbHealthMap.entrySet(), Map.Entry.comparingByValue()).getKey();
        float maxHealth = 0;
        switch (limbToHeal) {
            case "head_health":
                maxHealth = headHP;
                player.removeEffect(MobEffects.CONFUSION); // снимаем эффект
                break;
            case "body_health":
                maxHealth = bodyHP;
                player.removeEffect(MobEffects.HUNGER);
                player.removeEffect(MobEffects.WITHER);
                cancelWitherTimer();// снимаем эффект кровотечения
                break;
            case "arms_health":
                maxHealth = armsHP;
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
                break;
            case "legs_health":
                maxHealth = legsHP;
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                break;
        }
    }


    private Timer witherTimer;

    // метод для начала таймера с уроном по остальным конечностям игрока
    private void startWitherTimer(Player player, float damage) {
        witherTimer = new Timer();

        // планируем задачу таймера, которая будет выполняться каждую секунду
        witherTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                if (getHeadHealth(player) >= 0) {
                    // наносим урон по остальным конечностям игрока
                    updateLimbHealth(player, "body_health", damage);
                    updateLimbHealth(player, "arms_health", damage);
                    updateLimbHealth(player, "legs_health", damage);
                    updateLimbHealth(player, "head_health", damage);


                    // после каждого удара проверяем не стало ли здоровье головы равным нулю
                    if (getHeadHealth(player) <= 0) {
                        // если здоровье головы равно нулю отменяем таймер
                        cancelWitherTimer();
                    }
                    syncHealthWithPlayer(player);
                    if (player instanceof ServerPlayer serverPlayer) {
                        sendHealthToClient(serverPlayer);
                    }

                    updatePlayerHealth(player);
                }
            }
        }, 1000, 1500); // задержка перед первым запуском и интервал выполнения задачи
    }

    // метод для отмены таймера
    private void cancelWitherTimer() {
        if (witherTimer != null) {
            witherTimer.cancel();
            witherTimer = null;
        }
    }
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();

        if (itemStack.getItem() == ModItems.BANDAGE.get()) {
            if (event.getHand() == player.getUsedItemHand()) {
                if (mc != null) {
                    mc.setScreen(new LimbHealingScreen(title,player, mc,this));
                }
            }
        } else if (itemStack.getItem() == ModItems.MEDICAL_SPLINT.get()) {
            healLimbWithSplint(player);
        } else if (itemStack.getItem() == ModItems.LIFESAVER_TOURNIQUET.get()) {
            healLimbWithLifesaver(player);
        }
    }
    // новый метод обработки урона с учетом брони
    private float getArmorProtection(Player player, String limb) {
        ItemStack armorItem = null;
        switch (limb) {
            case "head_health":
                armorItem = player.getInventory().armor.get(3); // шлем
                break;
            case "body_health", "arms_health":
                armorItem = player.getInventory().armor.get(2); // нагрудник
                break;
            case "legs_health":
                armorItem = player.getInventory().armor.get(1); // штаны
                break;
        }

        if (armorItem != null && !armorItem.isEmpty()) {
            if (armorItem.getItem() instanceof ArmorItem armor) {
                ArmorMaterial material = armor.getMaterial();
                if (material.equals(DIAMOND)) {
                    return 0.7f;
                } else if (material.equals(IRON)) {
                    return 0.5f;
                } else if (material.equals(LEATHER)) {
                    return 0.3f;
                } else if (material.equals(GOLD)) {
                    return 0.4f;
                } else if (material.equals(NETHERITE)) {
                    return 0.8f;
                }
                return 0.2f; // базовая защита
            }
        }
        return 0.0f; // нет брони нет защиты
    }
    // метод который увеличивает множитель урона в зависимости от брони
    private float getFallDamageMultiplier(Player player) {
        float multiplier = 1.0f;

        for (ItemStack armorItem : player.getArmorSlots()) {
            if (!armorItem.isEmpty()) {
                ArmorItem armor = (ArmorItem) armorItem.getItem();
                ArmorMaterial material = armor.getMaterial();
                if (material.equals(LEATHER)) {
                    multiplier += 0.1f; // легкая броня
                } else if (material.equals(CHAIN)) {
                    multiplier += 0.2f; // средняя броня
                } else if (material.equals(IRON)) {
                    multiplier += 0.3f; // средняя броня
                } else if (material.equals(DIAMOND)) {
                    multiplier += 0.4f; // тяжелая броня
                } else if (material.equals(NETHERITE)) {
                    multiplier += 0.5f; // тяжелая броня
                } else {
                    multiplier += 0.1f; // по умолчанию
                }
            }
        }

        return multiplier;
    }

}
