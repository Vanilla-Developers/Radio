package com.damir00109.zona;

import com.damir00109.vpl;
import com.damir00109.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.Formatting;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.github.cliftonlabs.json_simple.JsonException;
import java.io.FileWriter;
import java.nio.file.Files;
import net.minecraft.world.World;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class CustomBorderManager {

    public static final Logger LOGGER = LoggerFactory.getLogger(vpl.MOD_ID + " CustomBorder");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create(); // Gson для сохранения/загрузки зон

    private static MinecraftServer server;
    private static final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();
    private static final CustomBorder emergencyBorder = new CustomBorder();
    private static final CustomBorder borderNormal = new CustomBorder();
    private static final CustomBorder borderReduced = new CustomBorder();
    private static boolean bordersConfigured = false;

    private static ModConfig currentConfig;
    private static List<MentalHealthZone> mentalHealthZones = new ArrayList<>(); // Хранилище для активных зон
    private static final Random random = new Random();

    // Константа для механики супа (длительность ожидания до утра, если не поспал)
    public static final long DURATION_UNTIL_SOUP_EFFECT_EXPIRES_TICKS = 13000; // Примерно до утра, если съел днем (13000 тиков = 10.8 минут)
    private static final int SOUP_DEBUFF_DURATION_TICKS = 10 * 20; // 10 секунд

    public static final RegistryKey<DamageType> MYSTERIOUS_DAMAGE_TYPE_KEY =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(vpl.MOD_ID, "mysterious_death"));

    public static void initialize(ModConfig config) {
        currentConfig = config;
        LOGGER.info("CustomBorderManager initialized with ModConfig. Allowed dimensions: {}", currentConfig.allowedDimensions);
        LOGGER.info("Border system active based on ModConfig: {}", currentConfig.enableBorderSystem);
        setupBordersFromConfig();
        // loadMentalHealthZonesFromConfig(); // Загрузка будет происходить при setServer, когда известен мир
    }

    // Публичный метод для получения состояния конкретного игрока
    public static PlayerState getPlayerState(UUID playerUuid) {
        return playerStates.get(playerUuid);
    }

    // Публичный метод для получения всех состояний игроков (например, для сохранения)
    public static Map<UUID, PlayerState> getAllPlayerStates() {
        return playerStates;
    }

    private static void setupBordersFromConfig() {
        if (currentConfig == null) {
            LOGGER.error("setupBordersFromConfig called but currentConfig is null!");
            bordersConfigured = false;
            return;
        }

        if (!currentConfig.enableBorderSystem) {
            LOGGER.info("Border system is disabled in ModConfig. Clearing all border configurations.");
            borderNormal.setVertices(new ArrayList<>());
            borderReduced.setVertices(new ArrayList<>());
            emergencyBorder.setVertices(new ArrayList<>());
            mentalHealthZones.clear(); // Также очищаем зоны, если система выключена
            bordersConfigured = false;
            return;
        }

        // Основная граница (Normal Border)
        List<Point> normalVertices = Arrays.asList(
            new Point(currentConfig.borderMinX, currentConfig.borderMinZ),
            new Point(currentConfig.borderMaxX, currentConfig.borderMinZ),
            new Point(currentConfig.borderMaxX, currentConfig.borderMaxZ),
            new Point(currentConfig.borderMinX, currentConfig.borderMaxZ)
        );
        borderNormal.setVertices(normalVertices);

        // Уменьшенная граница (Reduced Border - "зона проклятых")
        List<Point> reducedVertices = Arrays.asList(
            new Point(currentConfig.borderMinX + 50, currentConfig.borderMinZ + 50),
            new Point(currentConfig.borderMaxX - 50, currentConfig.borderMinZ + 50),
            new Point(currentConfig.borderMaxX - 50, currentConfig.borderMaxZ - 50),
            new Point(currentConfig.borderMinX + 50, currentConfig.borderMaxZ - 50)
        );
        borderReduced.setVertices(reducedVertices);

        // Аварийная граница (Emergency Border)
        List<Point> emergencyVertices = Arrays.asList(
            new Point(currentConfig.borderMinX - 100, currentConfig.borderMinZ - 100),
            new Point(currentConfig.borderMaxX + 100, currentConfig.borderMinZ - 100),
            new Point(currentConfig.borderMaxX + 100, currentConfig.borderMaxZ + 100),
            new Point(currentConfig.borderMinX - 100, currentConfig.borderMaxZ + 100)
        );
        emergencyBorder.setVertices(emergencyVertices);

        bordersConfigured = true;
        LOGGER.info("Borders configured from ModConfig. System Enabled: {}.", currentConfig.enableBorderSystem);
    }

    private static File getMentalHealthZonesFilePath(MinecraftServer currentSrv) { // Возвращаем File
        if (currentSrv == null) return null;
        String levelName = currentSrv.getSaveProperties().getLevelName();
        Path runDirectory = currentSrv.getRunDirectory().toAbsolutePath(); // Получаем Path
        Path worldPath;
        if (currentSrv instanceof DedicatedServer) { // Используем instanceof
            worldPath = runDirectory.resolve(levelName);
        } else { // Если это клиентский (интегрированный) сервер или другой не-дедикейтед
            worldPath = runDirectory.resolve("saves").resolve(levelName);
        }
        File worldDir = worldPath.toFile(); // Директория мира
        File targetFile = new File(worldDir, vpl.MOD_ID + "_mental_health_zones.json");
        LOGGER.info("Path for mental health zones for world '{}': {}", levelName, targetFile.getAbsolutePath());
        return targetFile;
    }

    private static void loadMentalHealthZonesForCurrentWorld(MinecraftServer currentSrv) {
        if (currentConfig == null || !currentConfig.enableBorderSystem) {
            mentalHealthZones.clear();
            LOGGER.info("Border system disabled, not loading mental health zones.");
            return;
        }
        mentalHealthZones.clear(); // Очищаем старые зоны перед загрузкой новых
        File filePath = getMentalHealthZonesFilePath(currentSrv); // Стало File
        if (filePath == null) {
            LOGGER.error("Cannot load mental health zones: server instance is null for path generation.");
            return;
        }

        if (filePath.exists()) { // Используем File.exists()
            try (FileReader reader = new FileReader(filePath)) { // filePath уже File
                Type listType = new TypeToken<ArrayList<MentalHealthZone>>() {}.getType();
                List<MentalHealthZone> loadedZones = GSON.fromJson(reader, listType);
                if (loadedZones != null) {
                    mentalHealthZones.addAll(loadedZones);
                    LOGGER.info("Successfully loaded {} mental health zones from {}", mentalHealthZones.size(), filePath.getAbsolutePath());
                }
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.error("Failed to load or parse mental health zones from " + filePath.getAbsolutePath(), e);
            }
        } else {
            LOGGER.info("Mental health zones file not found at {}. No zones loaded.", filePath.getAbsolutePath());
            // Файл будет создан при первом добавлении зоны
        }
    }

    private static void saveMentalHealthZonesToWorldFile(MinecraftServer currentSrv) {
        if (currentConfig == null || !currentConfig.enableBorderSystem) {
            LOGGER.info("Border system disabled, not saving mental health zones.");
            return;
        }
        File filePath = getMentalHealthZonesFilePath(currentSrv); // Стало File
        if (filePath == null) {
            LOGGER.error("Cannot save mental health zones: server instance is null for path generation.");
            return;
        }

        try {
            File parentDirFile = filePath.getParentFile(); // Используем getParentFile()
            if (parentDirFile != null && !parentDirFile.exists()) {
                parentDirFile.mkdirs(); // Используем mkdirs() для File
            }
            try (FileWriter writer = new FileWriter(filePath)) { // filePath уже File
                GSON.toJson(mentalHealthZones, writer);
                LOGGER.info("Successfully saved {} mental health zones to {}", mentalHealthZones.size(), filePath.getAbsolutePath());
            } 
        } catch (IOException e) {
            LOGGER.error("Failed to save mental health zones to " + filePath.getAbsolutePath(), e);
        }
    }

    public static void setServer(MinecraftServer s) {
        server = s;
        if (server != null) {
            if (currentConfig != null && currentConfig.enableBorderSystem) {
                LOGGER.info("Server instance set. Border system ENABLED. Loading consciousness data and mental health zones...");
                Map<UUID, Integer> consciousnessDataFromFile = loadConsciousnessData(server);
                for (Map.Entry<UUID, Integer> entry : consciousnessDataFromFile.entrySet()) {
                    UUID playerUuid = entry.getKey();
                    int consciousnessFromFile = entry.getValue();
                    PlayerState state = playerStates.computeIfAbsent(playerUuid, uuid -> {
                        LOGGER.info("Pre-loading state for UUID {} from file (consciousness file).", playerUuid);
                        return new PlayerState(false);
                    });
                    state.setConsciousness(consciousnessFromFile);
                    LOGGER.info("Set consciousness for UUID {} to {} from consciousness file.", playerUuid, consciousnessFromFile);
                }
                LOGGER.info("Finished loading {} entries from consciousness file into playerStates.", consciousnessDataFromFile.size());
                loadMentalHealthZonesForCurrentWorld(server);
            } else {
                LOGGER.info("Server instance set. Border system DISABLED. Skipping data load for zones and consciousness.");
                playerStates.clear();
                mentalHealthZones.clear();
            }
        }
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (currentConfig == null || !currentConfig.enableBorderSystem) {
            return;
        }
        UUID playerUuid = player.getUuid();
        PlayerState state = playerStates.get(playerUuid);

        if (state == null) {
            LOGGER.info("Player {} creating new PlayerState (not found in pre-load or existing states). Initializing with random consciousness.", player.getName().getString());
            state = new PlayerState(true);
            playerStates.put(playerUuid, state);
        } else {
            LOGGER.info("Player {} joined. Using existing PlayerState. Consciousness: {}.",
                        player.getName().getString(), state.getConsciousness());
        }

        state.setOutsideBorder(false);
        state.setTimeOutsideStart(0);
        state.resetBorderEffectsState();
        state.setDeathTick(Long.MAX_VALUE);
        String allowedDims = (currentConfig != null && currentConfig.allowedDimensions != null) ? currentConfig.allowedDimensions.toString() : "CONFIG_NOT_LOADED_OR_NO_DIMS";
        LOGGER.info("Player {} joined/state updated. Consciousness: {}. Allowed dimensions: {}",
                player.getName().getString(), state.getConsciousness(), allowedDims);
    }

    public static void onPlayerDisconnect(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        if (currentConfig == null || !currentConfig.enableBorderSystem) {
            playerStates.remove(playerUuid);
            return;
        }
        LOGGER.info("Player {} disconnected.", player.getName().getString());
        if (server != null && playerStates.containsKey(playerUuid)) {
            saveConsciousnessData(server, playerStates);
        }
    }

    public static void clearPlayerStates() {
        LOGGER.info("Clearing all player states from memory.");
        playerStates.clear();
        if (currentConfig != null && currentConfig.enableBorderSystem) {
             LOGGER.info("Border system was enabled, states cleared.");
        } else {
            LOGGER.info("Border system is disabled, states cleared from memory.");
        }
    }

    private static DamageSource getMysteriousDamageSource(ServerWorld world) {
        if (world == null) {
            LOGGER.error("Cannot get mysterious damage source, ServerWorld is null!");
            return null;
        }
        return world.getDamageSources().create(MYSTERIOUS_DAMAGE_TYPE_KEY);
    }

    public static void onServerTick(MinecraftServer currentServer) {
        if (currentConfig == null || !currentConfig.enableBorderSystem || !bordersConfigured) {
            return;
        }
        if (server == null && currentServer != null) {
            server = currentServer;
        } else if (currentServer == null && server == null) {
             LOGGER.warn("onServerTick called but server context is null.");
             return;
        }

        if (currentConfig == null) {
            LOGGER.warn("CustomBorderManager.onServerTick: currentConfig is null! Aborting tick.");
            return;
        }
        
        long currentTick = server.getOverworld().getTime();

        // --- Начало цикла обработки общих механик для каждого игрока ---
        for (ServerPlayerEntity playerEntity : server.getPlayerManager().getPlayerList()) {
            UUID playerUuid = playerEntity.getUuid();
            PlayerState state = playerStates.computeIfAbsent(playerUuid, uuid -> {
                LOGGER.info("PlayerState for {} created on the fly in onServerTick (general mechanics). Initializing with random consciousness.", playerEntity.getName().getString());
                return new PlayerState(true);
            });
            ServerWorld playerWorld = playerEntity.getServerWorld(); // Мир игрока для общих механик

            // 1. Логика восстановления в зонах ментального здоровья
            if (!mentalHealthZones.isEmpty() && state.getConsciousness() < 100) {
                Vec3d playerPosVec = playerEntity.getPos();
                String playerDimensionId = playerWorld.getRegistryKey().getValue().toString();
                for (MentalHealthZone zone : mentalHealthZones) {
                    if (zone.isInside(playerPosVec.x, playerPosVec.z, playerDimensionId)) {
                        double recoveryPerTick = zone.recoveryRatePerMinute / 1200.0;
                        state.addAccumulatedRecovery(recoveryPerTick);
                        break; 
                    }
                }
            }

            // 2. Логика для ромашкового супа
            if (state.hasEatenChamomileSoupRecently()) {
                if (currentTick > state.getTimeWhenSoupWasEaten() + DURATION_UNTIL_SOUP_EFFECT_EXPIRES_TICKS) {
                    LOGGER.info("Player {} did not sleep after eating chamomile soup. Applying debuff.", playerEntity.getName().getString());
                    state.setConsciousness(Math.max(0, state.getConsciousness() - 10));
                    state.setAteChamomileSoupRecently(false);
                    state.setTimeWhenSoupWasEaten(0);
                    state.setChamomileDebuffActive(true);
                    state.setChamomileDebuffEndTick(currentTick + SOUP_DEBUFF_DURATION_TICKS);
                    if (playerEntity.getServer() != null) {
                        saveConsciousnessData(playerEntity.getServer(), playerStates);
                    }
                }
            }

            if (state.isChamomileDebuffActive()) {
                if (currentTick < state.getChamomileDebuffEndTick()) {
                    double offsetX = (random.nextDouble() - 0.5) * 0.15;
                    double offsetZ = (random.nextDouble() - 0.5) * 0.15;
                    playerEntity.teleport(playerWorld, playerEntity.getX() + offsetX, playerEntity.getY(), playerEntity.getZ() + offsetZ, Collections.emptySet(), playerEntity.getYaw(), playerEntity.getPitch(), true);
                    if (currentTick % 4 == 0) {
                        for (int i = 0; i < 5; i++) {
                            playerWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                                     playerEntity.getX() + (random.nextDouble() - 0.5) * 1.5,
                                                     playerEntity.getY() + random.nextDouble() * 2.0,
                                                     playerEntity.getZ() + (random.nextDouble() - 0.5) * 1.5,
                                                     1, 0.0, 0.0, 0.0, 0.02);
                        }
                    }
                    if (currentTick % 30 == 0) {
                         playerWorld.playSound(null, playerEntity.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.3f, 1.5f);
                    }
                } else {
                    state.setChamomileDebuffActive(false);
                    LOGGER.info("Chamomile soup debuff ended for player {}.", playerEntity.getName().getString());
                }
            }
        } // --- Конец цикла обработки общих механик ---
        

        // --- Начало логики системы границ (если включена) ---
        if (!currentConfig.enableBorderSystem || !bordersConfigured) {
            return; 
        }

        for (ServerPlayerEntity playerBorderSystem : server.getPlayerManager().getPlayerList()) {
            UUID playerUuid = playerBorderSystem.getUuid();
            // PlayerState state уже был получен и, возможно, изменен в предыдущем цикле. Получаем его снова или используем тот же экземпляр.
            // Надежнее получать его снова, т.к. computeIfAbsent создаст новый, если его вдруг нет.
            // Но т.к. мы только что прошли по всем игрокам, он должен быть.
            PlayerState state = playerStates.get(playerUuid); 
            if (state == null) { // На всякий случай, хотя этого не должно происходить
                LOGGER.warn("PlayerState for {} (border system) was null after general mechanics processing. Skipping border logic for this player.", playerBorderSystem.getName().getString());
                continue;
            }

            String playerDimension = playerBorderSystem.getWorld().getRegistryKey().getValue().toString();
            if (!currentConfig.allowedDimensions.contains(playerDimension)) {
                if (state.isOutsideBorder()) {
                    state.setOutsideBorder(false);
                    state.setTimeOutsideStart(0);
                    state.resetBorderEffectsState();
                    state.setDeathTick(Long.MAX_VALUE);
                    LOGGER.info("Player {} is in a non-allowed dimension ({}) for border system. Resetting active border state.", playerBorderSystem.getName().getString(), playerDimension);
                }
                // Если дебафф супа активен, он продолжит работать из предыдущего блока кода.
                // Здесь мы просто пропускаем остальную логику ГРАНИЦ для этого игрока.
                continue; 
            }

            // Если система границ включена и игрок в разрешенном измерении:
            Vec3d playerPosVec = playerBorderSystem.getPos();
            Point playerPosPoint = new Point(playerPosVec.x, playerPosVec.z);
            ServerWorld playerWorldForBorder = playerBorderSystem.getServerWorld();

            // Проверка на выход за аварийную границу (EMERGENCY ZONE)
            if (!emergencyBorder.isInside(playerPosPoint)) {
                LOGGER.info("Player {} is outside the EMERGENCY BORDER. Scheduling immediate death. Consc: {}.", 
                            playerBorderSystem.getName().getString(), state.getConsciousness());
                // DamageSource mysteriousDamage = getMysteriousDamageSource(playerWorldForBorder);
                // if (mysteriousDamage != null && playerWorldForBorder != null) {
                //    playerBorderSystem.damage(playerWorldForBorder, mysteriousDamage, Float.MAX_VALUE); 
                // }
                // Устанавливаем, что игрок за границей и его время истекло
                if (!state.isOutsideBorder()) { // Если он только что пересек
                    state.setOutsideBorder(true);
                    state.setTimeOutsideStart(currentTick);
                    // Устанавливаем deathTick на сейчас, чтобы он умер в этом же тике или следующем при проверке state.getDeathTick()
                    state.setDeathTick(currentTick); 
                    state.setCurrentDeathTimerDurationTicks(1); // Минимальное время для таймера, если он используется где-то еще
                    // Спавним частицы один раз при пересечении аварийной зоны
                    spawnParticlesOnBorderCross(playerBorderSystem, playerWorldForBorder, true); // true для isEmergencyZone
                    // Можно добавить звук
                    playerWorldForBorder.playSound(null, playerBorderSystem.getBlockPos(), SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 1.0f, 0.5f);
                } else {
                    // Если он уже был за границей, убедимся, что deathTick все еще актуален для немедленной смерти
                    if (state.getDeathTick() > currentTick + 1) { // Если вдруг deathTick был установлен на будущее
                         state.setDeathTick(currentTick);
                    }
                }
                // Дальнейшая логика в этом тике для этого игрока будет обрабатывать смерть
                // через state.getDeathTick()
            } // Конец проверки на аварийную границу

            CustomBorder activeBorder = state.getConsciousness() < 50 ? borderReduced : borderNormal;
            String activeBorderName = state.getConsciousness() < 50 ? "Reduced Border" : "Normal Border";

            // Важно: предыдущая проверка на emergencyBorder НЕ должна прерывать выполнение так, 
            // чтобы эта логика не сработала, если игрок оказался между emergency и active границей.
            // Но если игрок уже был обработан как вышедший за emergency, то isCurrentlyOutsideRelevantBorder будет true из-за этого.

            // boolean isOutsideEmergency = !emergencyBorder.isInside(playerPosPoint); // Эта переменная теперь дублирует проверку выше
            // Для ясности, будем использовать результат первой проверки.
            boolean isStrictlyInsideEmergencyButOutsideActive = emergencyBorder.isInside(playerPosPoint) && !activeBorder.isInside(playerPosPoint);
            boolean isCurrentlyOutsideRelevantBorder = !emergencyBorder.isInside(playerPosPoint) || isStrictlyInsideEmergencyButOutsideActive;
            
            String currentZoneType = !emergencyBorder.isInside(playerPosPoint) ? "EMERGENCY ZONE" : activeBorderName;
            // Если мы уже в EMERGENCY ZONE по первой проверке, currentZoneType будет EMERGENCY ZONE.
            // Если мы внутри EMERGENCY, но за activeBorder, currentZoneType будет activeBorderName (Normal/Reduced).

            if (isCurrentlyOutsideRelevantBorder) {
                if (!state.isOutsideBorder()) {
                    state.setOutsideBorder(true);
                    state.setTimeOutsideStart(currentTick);

                    long warningPeriodTicks = (long)currentConfig.borderWarningTime * 20;
                    long animationDurationTicks = 80; // 4 секунды для анимации землетрясения/эффектов

                    state.setDeathTick(currentTick + warningPeriodTicks + animationDurationTicks);
                    state.setCurrentDeathTimerDurationTicks((int)(warningPeriodTicks + animationDurationTicks));
                    LOGGER.info("Player {} crossed into {}. Warning (animation start) in {}s ({} ticks), animation for {}s ({} ticks). Death at tick {}. Consc: {}.", 
                                playerBorderSystem.getName().getString(), 
                                currentZoneType, 
                                currentConfig.borderWarningTime, warningPeriodTicks,
                                animationDurationTicks / 20, animationDurationTicks,
                                state.getDeathTick(), 
                                state.getConsciousness());
                    spawnParticlesOnBorderCross(playerBorderSystem, playerWorldForBorder, isStrictlyInsideEmergencyButOutsideActive);
                }
            } else {
                if (state.isOutsideBorder()) {
                    LOGGER.info("Player {} is back inside ALL borders. Resetting state.", playerBorderSystem.getName().getString());
                    state.setOutsideBorder(false);
                    state.setTimeOutsideStart(0);
                    state.resetBorderEffectsState();
                    state.setDeathTick(Long.MAX_VALUE);
                }
            }

            if (state.isOutsideBorder()) {
                long timeOutsideTicks = currentTick - state.getTimeOutsideStart();

                if (timeOutsideTicks > 0 && timeOutsideTicks % 20 == 0) {
                    state.setConsciousness(state.getConsciousness() - 1);
                    LOGGER.info("Player {} losing consciousness. New: {}. Time outside: {}t.", playerBorderSystem.getName().getString(), state.getConsciousness(), timeOutsideTicks);
                }
                
                applyBorderEffects(playerBorderSystem, state, currentTick, playerWorldForBorder, currentZoneType);

                if (currentTick >= state.getDeathTick()) {
                    LOGGER.info("Player {}'s time up. Applying mysterious damage. Consciousness: {}. Zone: {}",
                                playerBorderSystem.getName().getString(), state.getConsciousness(), currentZoneType);
                    DamageSource mysteriousDamage = getMysteriousDamageSource(playerWorldForBorder);
                    if (mysteriousDamage != null && playerWorldForBorder != null) {
                         playerBorderSystem.damage(playerWorldForBorder, mysteriousDamage, Float.MAX_VALUE);
                    } else {
                         LOGGER.error("MysteriousDamageSource is null or playerWorld is null for player {}! Cannot apply damage.", playerBorderSystem.getName().getString());
                    }
                }
            }
        } // --- Конец логики системы границ ---
    }

    private static void spawnParticlesOnBorderCross(ServerPlayerEntity player, ServerWorld world, boolean isEmergencyZone) {
        if (world == null) return;
        ParticleEffect particleEffect = isEmergencyZone ? ParticleTypes.LAVA : ParticleTypes.SMOKE;
        for (int i = 0; i < 20; i++) {
            world.spawnParticles(particleEffect,
                               player.getX() + (random.nextDouble() - 0.5) * 1.5,
                               player.getY() + random.nextDouble() * 2.0,
                               player.getZ() + (random.nextDouble() - 0.5) * 1.5,
                               1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    private static void applyBorderEffects(ServerPlayerEntity player, PlayerState state, long currentTick, ServerWorld playerWorld, String activeBorderName) {
        if (playerWorld == null) return;
        long timeSinceCrossingBorderTicks = currentTick - state.getTimeOutsideStart();

        if (timeSinceCrossingBorderTicks >= 20) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 40, 0, true, false, false));
        }

        long warningTimeTicks = (long)currentConfig.borderWarningTime * 20;
        if (timeSinceCrossingBorderTicks > warningTimeTicks) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 40, 0, true, false, false));

            long timeInIntensePhaseTicks = timeSinceCrossingBorderTicks - warningTimeTicks;
            if (timeInIntensePhaseTicks > 0) {
                int darknessAmplifier = Math.min(2, (int)(timeInIntensePhaseTicks / 40));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 40, darknessAmplifier, true, false, false));
            }

            if (!state.hasWarnedAboutIntenseEffects()) {
                 state.setWarnedAboutIntenseEffects(true);
                 LOGGER.info("Player {} entered intense effects phase in {}. Time outside: {}t.", player.getName().getString(), activeBorderName, timeSinceCrossingBorderTicks);
            }

            double offsetX = (random.nextDouble() - 0.5) * 0.1;
            double offsetZ = (random.nextDouble() - 0.5) * 0.1;
            player.teleport(player.getServerWorld(), player.getX() + offsetX, player.getY(), player.getZ() + offsetZ, Collections.emptySet(), player.getYaw(), player.getPitch(), true);

            if (currentTick % 2 == 0) {
                ParticleEffect effectParticle = "EMERGENCY ZONE".equals(activeBorderName) ? ParticleTypes.FLAME : ParticleTypes.PORTAL;
                for (int i = 0; i < 10; i++) {
                    playerWorld.spawnParticles(effectParticle,
                                             player.getX() + (random.nextDouble() - 0.5) * 2.5,
                                             player.getY() + random.nextDouble() * 2.5,
                                             player.getZ() + (random.nextDouble() - 0.5) * 2.5,
                                             5, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }
    }

    public static void onPlayerRespawn(ServerPlayerEntity newPlayer) { 
        UUID playerUuid = newPlayer.getUuid();
        PlayerState state = playerStates.get(playerUuid); 

        if (state != null) {
            LOGGER.info("Player {} respawned. Consciousness before: {}",
                        newPlayer.getName().getString(), state.getConsciousness());
            
            state.resetBorderEffectsState();
            state.setDeathTick(Long.MAX_VALUE);
            state.setOutsideBorder(false); 
            state.setTimeOutsideStart(0);
        } else {
            LOGGER.warn("PlayerState for {} (respawn) was unexpectedly null. Creating new with random consciousness.", newPlayer.getName().getString());
            PlayerState newState = new PlayerState(true);
            playerStates.put(playerUuid, newState); 
        }
    }
    
    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("customborder")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("setConsciousness")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .then(CommandManager.argument("consciousness", IntegerArgumentType.integer(0, 100))
                        .executes(context -> {
                            if (currentConfig == null || !currentConfig.enableBorderSystem) {
                                context.getSource().sendError(Text.literal("Border system is disabled. Command unavailable."));
                                return 0;
                            }
                            return setConsciousnessCommand(context);
                        })
                    )
                    .then(CommandManager.argument("mode", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(Arrays.asList("set", "add", "remove"), builder))
                        .then(CommandManager.argument("value", IntegerArgumentType.integer())
                            .executes(context -> {
                                if (currentConfig == null || !currentConfig.enableBorderSystem) {
                                    context.getSource().sendError(Text.literal("Border system is disabled. Command unavailable."));
                                    return 0;
                                }
                                return setConsciousnessCommandWithStringArgument(context);
                            })
                        )
                    )
                )
            )
            .then(CommandManager.literal("checkMental")
                .executes(CustomBorderManager::checkMentalPlayerSelf)
                .then(CommandManager.argument("target", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        List<String> suggestions = new ArrayList<>();
                        suggestions.add("all");
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            MinecraftServer currentServer = player.getServer();
                            if (currentServer != null) {
                                currentServer.getPlayerManager().getPlayerList().forEach(p -> suggestions.add(p.getName().getString()));
                            }
                        }
                        return CommandSource.suggestMatching(suggestions, builder);
                    })
                    .executes(context -> {
                        if (currentConfig == null || !currentConfig.enableBorderSystem) {
                            context.getSource().sendError(Text.literal("Border system is disabled. Command unavailable."));
                            return 0;
                        }
                        return checkMentalTarget(context);
                    })
                )
            )
            .then(CommandManager.literal("setConfigBorder")
                .then(CommandManager.argument("minX", DoubleArgumentType.doubleArg())
                    .then(CommandManager.argument("minZ", DoubleArgumentType.doubleArg())
                        .then(CommandManager.argument("maxX", DoubleArgumentType.doubleArg())
                            .then(CommandManager.argument("maxZ", DoubleArgumentType.doubleArg())
                                .executes(CustomBorderManager::setBorderInConfigCommand)
                            )
                        )
                    )
                )
            )
            .then(CommandManager.literal("addMentalZone")
                .then(CommandManager.argument("name", StringArgumentType.string())
                .then(CommandManager.argument("x1", DoubleArgumentType.doubleArg())
                .then(CommandManager.argument("z1", DoubleArgumentType.doubleArg())
                .then(CommandManager.argument("x2", DoubleArgumentType.doubleArg())
                .then(CommandManager.argument("z2", DoubleArgumentType.doubleArg())
                .then(CommandManager.argument("ratePerMinute", DoubleArgumentType.doubleArg())
                .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                    .executes(context -> {
                        if (currentConfig == null || !currentConfig.enableBorderSystem) {
                            context.getSource().sendError(Text.literal("Border system is disabled. Command unavailable."));
                            return 0;
                        }
                        return addMentalHealthZoneCommand(context);
                    })
                )))))))
            )
            .then(CommandManager.literal("listMentalZones")
                .executes(context -> {
                    if (currentConfig == null || !currentConfig.enableBorderSystem) {
                        context.getSource().sendError(Text.literal("Border system is disabled. Command unavailable."));
                        return 0;
                    }
                    return listMentalHealthZonesCommand(context);
                })
            )
            .then(CommandManager.literal("deleteMentalZone")
                .then(CommandManager.argument("zoneName", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        List<String> zoneNames = new ArrayList<>();
                        if (currentConfig != null && currentConfig.enableBorderSystem) {
                           for (MentalHealthZone zone : mentalHealthZones) {
                               zoneNames.add(zone.name);
                           }
                        }
                        return CommandSource.suggestMatching(zoneNames, builder);
                    })
                    .executes(context -> {
                        if (currentConfig == null || !currentConfig.enableBorderSystem) {
                            context.getSource().sendError(Text.literal("Border system is disabled. Command unavailable."));
                            return 0;
                        }
                        return deleteMentalHealthZoneCommand(context);
                    })
                )
            )
        );
    }

    private static int setConsciousnessCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        int value = IntegerArgumentType.getInteger(context, "value");
        PlayerState state = playerStates.computeIfAbsent(player.getUuid(), uuid -> new PlayerState(false));
        state.setConsciousness(value);
        context.getSource().sendFeedback(() -> Text.literal("Сознание для " + player.getName().getString() + " -> " + value), true);
        if (server != null) {
            saveConsciousnessData(server, playerStates);
        }
        return 1;
    }

    private static int setConsciousnessCommandWithStringArgument(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "playerName");
        ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);

        if (player == null) {
            context.getSource().sendError(Text.literal("Игрок '" + playerName + "' не найден или не в сети."));
            return 0;
        }

        int value = IntegerArgumentType.getInteger(context, "value");
        PlayerState state = playerStates.computeIfAbsent(player.getUuid(), uuid -> new PlayerState(false)); // Existing state or new
        state.setConsciousness(value);
        context.getSource().sendFeedback(() -> Text.literal("Сознание для " + player.getName().getString() + " установлено на " + value), true);
        if (server != null) {
            saveConsciousnessData(server, playerStates); // Save data after modification
        }
        return 1;
    }

    private static int checkMentalPlayerSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow(); // Получаем игрока, вызвавшего команду
        PlayerState state = playerStates.get(player.getUuid());

        if (state != null) {
            Text coloredName = Text.literal(player.getName().getString()).formatted(Formatting.GREEN);
            Text message = Text.empty().append(coloredName).append(Text.literal(": " + state.getConsciousness()));
            source.sendFeedback(() -> message, false);
        } else {
            source.sendFeedback(() -> Text.literal("Данные о вашем ментальном здоровье не найдены."), false);
        }
        return 1;
    }

    public static Map<UUID, Integer> loadConsciousnessData(MinecraftServer server) {
        if (server == null) {
            LOGGER.error("Attempted to load consciousness data with a null server instance.");
            return new HashMap<>();
        }
        String levelName = server.getSaveProperties().getLevelName(); // Получаем имя текущего мира
        Path runDirectory = server.getRunDirectory().toAbsolutePath(); // Получаем Path
        Path worldPath;
        if (server instanceof DedicatedServer) { // Используем instanceof
            worldPath = runDirectory.resolve(levelName);
        } else { // Если это клиентский (интегрированный) сервер или другой не-дедикейтед
            worldPath = runDirectory.resolve("saves").resolve(levelName);
        }
        File worldDir = worldPath.toFile(); // Директория мира
        File dataFile = new File(worldDir, vpl.MOD_ID + "_consciousness_data.json"); // Используем File

        LOGGER.info("Path for consciousness data (load) for world '{}': {}", levelName, dataFile.getAbsolutePath());

        Map<UUID, Integer> consciousnessData = new HashMap<>();
        if (dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                JsonObject jsonObject = (JsonObject) Jsoner.deserialize(reader);

                for (Object key : jsonObject.keySet()) {
                    String uuidString = (String) key;
                    try {
                        UUID playerUuid = UUID.fromString(uuidString);
                        Object value = jsonObject.get(key);
                        if (value instanceof Number) {
                            consciousnessData.put(playerUuid, ((Number) value).intValue());
                        } else if (value instanceof String) {
                            try {
                                consciousnessData.put(playerUuid, Integer.parseInt((String) value));
                            } catch (NumberFormatException nfe) {
                                LOGGER.warn("Could not parse consciousness value for UUID {} from string: {}", playerUuid, value);
                            }
                        } else {
                             LOGGER.warn("Unexpected type for consciousness value for UUID {}: {}", playerUuid, value.getClass().getName());
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Invalid UUID string in consciousness data: {}", uuidString);
                    }
                }
                LOGGER.info("Successfully loaded consciousness data for {} players from {}", consciousnessData.size(), dataFile.getAbsolutePath());
            } catch (FileNotFoundException e) {
                LOGGER.warn("Consciousness data file not found (should not happen if dataFile.exists() is true): {}", dataFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("IOException while reading consciousness data from {}: {}", dataFile.getAbsolutePath(), e.getMessage());
            } catch (JsonException e) {
                LOGGER.error("JsonException (parse error) while reading consciousness data from {}: {}", dataFile.getAbsolutePath(), e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Unexpected exception while loading consciousness data from {}: {}", dataFile.getAbsolutePath(), e.getMessage(), e);
            }
        } else {
            LOGGER.info("Consciousness data file not found at {}. A new one will be created if needed.", dataFile.getAbsolutePath());
        }
        return consciousnessData;
    }

    public static void saveConsciousnessData(MinecraftServer currentServer, Map<UUID, PlayerState> statesToSave) {
        if (currentServer == null) {
            LOGGER.error("Attempted to save consciousness data with a null server instance.");
            return;
        }
        String levelName = currentServer.getSaveProperties().getLevelName(); // Имя текущего мира
        Path runDirectory = currentServer.getRunDirectory().toAbsolutePath(); // Получаем Path
        Path worldPath;
        if (currentServer instanceof DedicatedServer) { // Используем instanceof
            worldPath = runDirectory.resolve(levelName);
        } else { // Если это клиентский (интегрированный) сервер или другой не-дедикейтед
            worldPath = runDirectory.resolve("saves").resolve(levelName);
        }
        File worldDir = worldPath.toFile(); // Директория мира
        File dataFile = new File(worldDir, vpl.MOD_ID + "_consciousness_data.json"); // Используем File

        LOGGER.info("Path for consciousness data (save) for world '{}': {}", levelName, dataFile.getAbsolutePath());
        File backupFile = new File(dataFile.getAbsolutePath() + ".bak");

        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<UUID, PlayerState> entry : statesToSave.entrySet()) {
            jsonObject.put(entry.getKey().toString(), entry.getValue().getConsciousness());
        }

        try {
            File parentDir = dataFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.mkdirs()) {
                    LOGGER.info("Created directory for consciousness data: {}", parentDir.getAbsolutePath());
                } else {
                    LOGGER.error("Failed to create directory for consciousness data: {}", parentDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error creating parent directories for {}: {}", dataFile.getAbsolutePath(), e.getMessage());
        }

        if (dataFile.exists()) {
            try {
                Path backupFilePath = backupFile.toPath(); 
                Files.copy(dataFile.toPath(), backupFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Backup of consciousness data created at {}", backupFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.warn("Could not create backup of consciousness data file {}: {}", dataFile.getAbsolutePath(), e.getMessage());
            }
        }

        try (FileWriter writer = new FileWriter(dataFile)) {
            writer.write(jsonObject.toJson());
            LOGGER.info("Successfully saved consciousness data for {} players to {}", statesToSave.size(), dataFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Could not save consciousness data to {}: {}", dataFile.getAbsolutePath(), e.getMessage());
            if (backupFile.exists()) {
                try {
                    Path backupFilePath = backupFile.toPath(); 
                    Files.copy(backupFilePath, dataFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Restored consciousness data from backup {} due to write error.", backupFile.getAbsolutePath());
                } catch (IOException restoreEx) {
                    LOGGER.error("Could not restore consciousness data from backup {}: {}", backupFile.getAbsolutePath(), restoreEx.getMessage());
                }
            }
        }
    }

    private static int checkMentalTarget(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer currentServer = source.getServer();
        String targetArgument = StringArgumentType.getString(context, "target");

        if (targetArgument.equalsIgnoreCase("all")) {
            List<Text> onlinePlayerMessages = new ArrayList<>();
            List<Text> offlinePlayerMessages = new ArrayList<>();

            LOGGER.info("Executing checkmentall all. Total states in playerStates: {}", playerStates.size());

            for (Map.Entry<UUID, PlayerState> entry : playerStates.entrySet()) {
                UUID playerUuid = entry.getKey();
                PlayerState state = entry.getValue();
                if (state == null) {
                    LOGGER.warn("Found null PlayerState for UUID {} in playerStates during checkmentall all.", playerUuid);
                    continue;
                }

                int consciousness = state.getConsciousness();
                ServerPlayerEntity onlinePlayer = currentServer.getPlayerManager().getPlayer(playerUuid);
                Text message;
                String playerName;

                if (onlinePlayer != null) {
                    playerName = onlinePlayer.getName().getString();
                    Text coloredName = Text.literal(playerName).formatted(Formatting.GREEN);
                    message = Text.empty().append(coloredName).append(Text.literal(": " + consciousness));
                    onlinePlayerMessages.add(message);
                } else {
                    Optional<GameProfile> gameProfileOpt = currentServer.getUserCache().getByUuid(playerUuid);
                    playerName = gameProfileOpt.map(GameProfile::getName)
                                                   .orElse("OfflinePlayer_" + playerUuid.toString().substring(0, 8));
                    Text coloredName = Text.literal(playerName).formatted(Formatting.RED);
                    message = Text.empty().append(coloredName).append(Text.literal(": " + consciousness));
                    offlinePlayerMessages.add(message);
                }
                LOGGER.debug("Processed player for checkmentall all: {} (UUID: {}), Online: {}, Consciousness: {}", playerName, playerUuid, (onlinePlayer != null), consciousness);
            }

            if (onlinePlayerMessages.isEmpty() && offlinePlayerMessages.isEmpty()) {
                 source.sendFeedback(() -> Text.literal("Нет данных о ментальном здоровье игроков (playerStates пуст)."), false);
            } else {
                onlinePlayerMessages.sort(Comparator.comparing(Text::getString));
                offlinePlayerMessages.sort(Comparator.comparing(Text::getString));

                onlinePlayerMessages.forEach(msg -> source.sendFeedback(() -> msg, false));
                offlinePlayerMessages.forEach(msg -> source.sendFeedback(() -> msg, false));
            }
        } else {
            ServerPlayerEntity targetOnlinePlayer = currentServer.getPlayerManager().getPlayer(targetArgument);
            boolean found = false;

            if (targetOnlinePlayer != null) {
                PlayerState state = playerStates.get(targetOnlinePlayer.getUuid());
                if (state != null) {
                    Text coloredName = Text.literal(targetOnlinePlayer.getName().getString()).formatted(Formatting.GREEN);
                    Text message = Text.empty().append(coloredName).append(Text.literal(": " + state.getConsciousness()));
                    source.sendFeedback(() -> message, false);
                    found = true;
                } else {
                    source.sendError(Text.literal("Состояние для онлайн игрока " + targetOnlinePlayer.getName().getString() + " не найдено в playerStates (ошибка!)."));
                }
            } else {
                UUID targetUuid = null;
                Optional<GameProfile> profileByName = currentServer.getUserCache().findByName(targetArgument);
                if (profileByName.isPresent()) {
                    targetUuid = profileByName.get().getId();
                }

                if (targetUuid != null) {
                    PlayerState state = playerStates.get(targetUuid);
                    if (state != null) {
                        String nameToDisplay = profileByName.get().getName();
                        Text coloredName = Text.literal(nameToDisplay).formatted(Formatting.RED);
                        Text message = Text.empty().append(coloredName).append(Text.literal(": " + state.getConsciousness()));
                        source.sendFeedback(() -> message, false);
                        found = true;
                    }
                }
            }

            if (!found) {
                 source.sendError(Text.literal("Игрок '" + targetArgument + "' не найден в активных состояниях или для него нет данных."));
            }
        }
        return 1;
    }

    private static int setBorderInConfigCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (currentConfig == null) {
            context.getSource().sendError(Text.literal("Конфигурация мода не загружена."));
            return 0;
        }

        float minX = FloatArgumentType.getFloat(context, "minX");
        float maxX = FloatArgumentType.getFloat(context, "maxX");
        float minZ = FloatArgumentType.getFloat(context, "minZ");
        float maxZ = FloatArgumentType.getFloat(context, "maxZ");

        if (minX >= maxX) {
            context.getSource().sendError(Text.literal("minX должен быть меньше maxX."));
            return 0;
        }
        if (minZ >= maxZ) {
            context.getSource().sendError(Text.literal("minZ должен быть меньше maxZ."));
            return 0;
        }

        currentConfig.borderMinX = minX;
        currentConfig.borderMaxX = maxX;
        currentConfig.borderMinZ = minZ;
        currentConfig.borderMaxZ = maxZ;

        ModConfig.save(currentConfig);
        setupBordersFromConfig(); // Применяем изменения немедленно

        context.getSource().sendFeedback(() -> Text.literal(String.format("Границы мира в конфиге обновлены: X(%.1f до %.1f), Z(%.1f до %.1f). Перезагрузка конфига и применение границ выполнены.", minX, maxX, minZ, maxZ)), true);
        LOGGER.info("World border in config updated via command: minX={}, maxX={}, minZ={}, maxZ={}. Config saved and borders re-applied.", minX, maxX, minZ, maxZ);
        return 1;
    }

    private static int addMentalHealthZoneCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        double x1 = DoubleArgumentType.getDouble(context, "x1");
        double z1 = DoubleArgumentType.getDouble(context, "z1");
        double x2 = DoubleArgumentType.getDouble(context, "x2");
        double z2 = DoubleArgumentType.getDouble(context, "z2");
        double ratePerMinute = DoubleArgumentType.getDouble(context, "ratePerMinute");
        ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
        String dimensionId = dimension.getRegistryKey().getValue().toString();

        // currentConfig больше не хранит зоны, используем server для определения мира и сохранения
        MinecraftServer currentSrv = context.getSource().getServer();
        if (currentSrv == null) {
            context.getSource().sendError(Text.literal("Сервер не доступен для сохранения зоны."));
            return 0;
        }

        // Проверка на уникальность имени в текущем списке mentalHealthZones
        for (MentalHealthZone existingZone : mentalHealthZones) {
            if (existingZone.name.equalsIgnoreCase(name)) {
                context.getSource().sendError(Text.literal("Зона с именем '" + name + "' уже существует в этом мире."));
                return 0;
            }
        }
        if (ratePerMinute <= 0) {
            context.getSource().sendError(Text.literal("Скорость восстановления должна быть положительным числом."));
            return 0;
        }

        MentalHealthZone newZone = new MentalHealthZone(name, x1, z1, x2, z2, ratePerMinute, dimensionId);
        mentalHealthZones.add(newZone); // Добавляем в текущий активный список
        saveMentalHealthZonesToWorldFile(currentSrv); // Сохраняем в файл мира

        context.getSource().sendFeedback(() -> Text.literal("Зона восстановления '" + name + "' добавлена в этот мир."), true);
        LOGGER.info("Added mental health zone to current world: {}", newZone.toString());
        return 1;
    }

    private static int listMentalHealthZonesCommand(CommandContext<ServerCommandSource> context) {
        // Используем напрямую mentalHealthZones
        if (mentalHealthZones.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("Нет настроенных зон восстановления ментального здоровья в этом мире."), false);
            return 1;
        }

        context.getSource().sendFeedback(() -> Text.literal("--- Зоны восстановления ментального здоровья в этом мире ---"), false);
        for (int i = 0; i < mentalHealthZones.size(); i++) {
            MentalHealthZone zone = mentalHealthZones.get(i);
            Text message = Text.literal((i + 1) + ". ")
                .append(Text.literal(zone.name).formatted(Formatting.YELLOW))
                .append(String.format(": (Измерение: %s, X1: %.1f, Z1: %.1f, X2: %.1f, Z2: %.1f, Скорость: %.2f/мин)",
                                zone.dimensionId, zone.x1, zone.z1, zone.x2, zone.z2, zone.recoveryRatePerMinute));
            context.getSource().sendFeedback(() -> message, false);
        }
        return 1;
    }

    private static int deleteMentalHealthZoneCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        MinecraftServer currentSrv = context.getSource().getServer();
        if (currentSrv == null) {
            context.getSource().sendError(Text.literal("Сервер не доступен для удаления зоны."));
            return 0;
        }

        // Удаляем из текущего списка mentalHealthZones
        boolean removed = mentalHealthZones.removeIf(zone -> zone.name.equalsIgnoreCase(name));

        if (removed) {
            saveMentalHealthZonesToWorldFile(currentSrv); // Сохраняем изменения в файл мира
            context.getSource().sendFeedback(() -> Text.literal("Зона восстановления '" + name + "' удалена из этого мира."), true);
            LOGGER.info("Removed mental health zone from current world: {}", name);
        } else {
            context.getSource().sendError(Text.literal("Зона восстановления с именем '" + name + "' не найдена в этом мире."));
        }
        return removed ? 1 : 0;
    }
}