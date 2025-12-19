package ru.dimaskama.radio;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;

public class VoicePlugin implements VoicechatPlugin {
	public String getPluginId() {
		return "radio";
	}

	public void registerEvents(EventRegistration registration) {
		VoiceIntegration.registerEvents(registration);
	}
}
