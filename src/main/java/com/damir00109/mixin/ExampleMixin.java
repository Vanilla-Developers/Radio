package com.damir00109.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(MinecraftServer.class)
public class ExampleMixin {
	@Inject(at = @At("HEAD"), method = "loadWorld")
	public void init(CallbackInfo info) {
		// This code is injected into the start of MinecraftServer.loadWorld()
	}
}