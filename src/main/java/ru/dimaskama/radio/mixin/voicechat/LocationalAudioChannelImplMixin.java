package ru.dimaskama.radio.mixin.voicechat;

import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.radio.VoiceIntegration;

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
    private void broadcastHead(LocationalSoundPacket packet, CallbackInfo ci) {
        if (this.level.getServerLevel() instanceof ServerWorld world) {
            VoiceIntegration.onPluginLocationPacket(
                    world,
                    Vec3d.of((BlockPos) packet.getPosition()),
                    packet.getChannelId(),
                    packet.getOpusEncodedData(),
                    packet.getDistance()
            );
        }
    }
}
