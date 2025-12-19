package ru.dimaskama.radio.mixin;

import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.dimaskama.radio.WorldRadioManager;
import ru.dimaskama.radio.extend.ServerWorldExtend;

@Mixin({ServerWorld.class})
abstract class ServerWorldMixin implements ServerWorldExtend {

    @Unique
    @Nullable
    @Nullable
    private WorldRadioManager radio_radioManager;

    @Inject(
            method = {"<init>"},
            at = {@At("TAIL")}
    )
    private void initTail(CallbackInfo ci) {
        ServerWorld thisServerWorld = (ServerWorld) this;
        this.radio_radioManager = new WorldRadioManager(thisServerWorld);
    }

    @Nullable
    @Override
    public WorldRadioManager radio_getRadioManager() {
        return this.radio_radioManager;
    }
}
