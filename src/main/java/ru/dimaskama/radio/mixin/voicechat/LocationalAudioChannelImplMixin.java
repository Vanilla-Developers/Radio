package ru.dimaskama.radio.mixin.voicechat;

import de.maxhenkel.voicechat.api.ServerLevel;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.radio.RadioMod;
import ru.dimaskama.radio.VoiceIntegration;

@Pseudo
@Mixin(
        targets = {"de.maxhenkel.voicechat.plugins.impl.audiochannel.LocationalAudioChannelImpl"}
)
abstract class LocationalAudioChannelImplMixin {

    @Shadow(remap = false)
    protected ServerLevel level;

    // В новых версиях Simple Voice Chat API, вероятно, изменился метод broadcast
    @Inject(
            method = {"broadcast"},
            at = @At("HEAD"),
            remap = false
    )
    private void broadcastHead(Object rawPacket, CallbackInfo ci) {
        if (this.level.getServerLevel() instanceof ServerWorld world) {
            // Попробуйте определить актуальный тип пакета
            try {
                // Временное решение - рефлексия для определения структуры пакета
                Class<?> packetClass = rawPacket.getClass();

                // Попробуйте найти методы для получения данных
                java.lang.reflect.Method getLocationMethod = packetClass.getMethod("getLocation");
                java.lang.reflect.Method getChannelIdMethod = packetClass.getMethod("getChannelId");
                java.lang.reflect.Method getDataMethod = packetClass.getMethod("getData");
                java.lang.reflect.Method getDistanceMethod = packetClass.getMethod("getDistance");

                Object location = getLocationMethod.invoke(rawPacket);
                Object channelId = getChannelIdMethod.invoke(rawPacket);
                Object data = getDataMethod.invoke(rawPacket);
                Object distance = getDistanceMethod.invoke(rawPacket);

                VoiceIntegration.onPluginLocationPacket(
                        world,
                        location,  // Может быть Vec3 или что-то подобное
                        channelId,
                        data,
                        distance
                );
            } catch (Exception e) {
                // Если не удалось получить данные через рефлексию
                RadioMod.LOGGER.error("Failed to process voice chat packet: " + e.getMessage());
            }
        }
    }
}