package ru.dimaskama.radio.mixin.voicechat;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.voice.common.LocationSoundPacket;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    private void broadcastHead(LocationSoundPacket packet, CallbackInfo ci) {
        if (this.level.getServerLevel() instanceof ServerWorld world) {
            VoiceIntegration.onPluginLocationPacket(
                    world,
                    packet.getLocation(),
                    packet.getChannelId(),
                    packet.getData(),
                    packet.getDistance()
            );
        }
    }
}
