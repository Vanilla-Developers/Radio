package com.damir00109.mixin.voicechat;

import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.damir00109.VoiceIntegration;

@Pseudo
@Mixin(
        targets = {"de.maxhenkel.voicechat.plugins.impl.audiochannel.LocationalAudioChannelImpl"}
)
abstract class LocationalAudioChannelImplMixin {

    @Shadow(remap = false)
    protected ServerLevel level;

    @Inject(
            method = {"broadcast"},
            at = @At("HEAD"),
            remap = false
    )
    private void broadcastHead(@Coerce Object packet, CallbackInfo ci) {
        if (this.level.getServerLevel() instanceof ServerWorld world) {
            try {
                VoiceIntegration.onPluginLocationPacket(
                        world,
                        Vec3d.of((BlockPos) packet.getClass().getMethod("getPosition").invoke(packet)),
                        (java.util.UUID) packet.getClass().getMethod("getChannelId").invoke(packet),
                        (byte[]) packet.getClass().getMethod("getOpusEncodedData").invoke(packet),
                        ((Number) packet.getClass().getMethod("getDistance").invoke(packet)).floatValue()
                );
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
