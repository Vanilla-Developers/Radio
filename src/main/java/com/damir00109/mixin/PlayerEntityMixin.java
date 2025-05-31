package com.damir00109.mixin;

import com.damir00109.VanillaDamir00109;
import com.damir00109.zona.CustomBorderManager;
import com.damir00109.zona.PlayerState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    // Обязательный конструктор для LivingEntity, если компилятор жалуется
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // Инъекция в метод пробуждения
    // Убедимся, что сигнатура метода wakeUp в PlayerEntity правильная.
    // В 1.20.1+ wakeUp(boolean resetSleepTime, boolean updateSleepingPlayers) уже нет, просто wakeUp()
    @Inject(method = "wakeUp()V", at = @At("TAIL"))
    private void onWakeUp(CallbackInfo ci) {
        Object thisObj = this;
        if (thisObj instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) thisObj;
            PlayerState state = CustomBorderManager.getPlayerState(player.getUuid());

            if (state != null && state.hasEatenChamomileSoupRecently()) {
                long timeSinceSoupEaten = player.getWorld().getTime() - state.getTimeWhenSoupWasEaten();
                VanillaDamir00109.SVSP_LOGGER.info("Player {} woke up. Time since soup eaten: {} ticks. DURATION_UNTIL_SOUP_EFFECT_EXPIRES_TICKS: {}",
                                        player.getName().getString(), timeSinceSoupEaten, CustomBorderManager.DURATION_UNTIL_SOUP_EFFECT_EXPIRES_TICKS);

                if (timeSinceSoupEaten < CustomBorderManager.DURATION_UNTIL_SOUP_EFFECT_EXPIRES_TICKS) {
                    state.setAteChamomileSoupRecently(false);
                    state.setTimeWhenSoupWasEaten(0); // Сбрасываем время
                    VanillaDamir00109.SVSP_LOGGER.info("Player {} slept before soup effect expired. Debuff avoided.", player.getName().getString());
                    if (player.getServer() != null) {
                        CustomBorderManager.saveConsciousnessData(player.getServer(), CustomBorderManager.getAllPlayerStates());
                    }
                } else {
                    VanillaDamir00109.SVSP_LOGGER.info("Player {} slept AFTER soup effect should have expired (or DURATION is wrong). TimeSince: {}, DurationConst: {}",
                                            player.getName().getString(), timeSinceSoupEaten, CustomBorderManager.DURATION_UNTIL_SOUP_EFFECT_EXPIRES_TICKS);
                }
            }
        }
    }
} 