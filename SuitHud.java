package com.ironmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Arc reactor energy bar + ability list with live keybind names and cooldowns. */
public final class SuitHud {
	private SuitHud() {}

	public static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || !ClientSuitState.active()) return;

		int count = Math.min(ClientSuitState.abilities.size(), IronModClient.KEYS.length);
		int x = 8;
		int y = g.guiHeight() - 24 - count * 11 - 12;

		int barW = 90, barH = 6;
		float frac = Math.max(0f, Math.min(1f, ClientSuitState.energy / (float) ClientSuitState.maxEnergy));
		g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0xAA000000);
		g.fill(x, y, x + barW, y + barH, 0xFF14232B);
		g.fill(x, y, x + (int) (barW * frac), y + barH, frac > 0.25f ? 0xFF55E6FF : 0xFFFF5555);
		g.drawString(mc.font, Component.translatable("hud.ironmod.arc", ClientSuitState.energy, ClientSuitState.maxEnergy),
				x + barW + 6, y - 1, 0xFFFFFF, true);

		y += barH + 5;
		for (int i = 0; i < count; i++) {
			String type = ClientSuitState.abilities.get(i);
			int cooldown = i < ClientSuitState.cooldowns.size() ? ClientSuitState.cooldowns.get(i) : 0;
			String line = "[" + IronModClient.keyName(i) + "] " + Component.translatable("ability.ironmod." + type).getString();
			int color = 0xFFFFFF;
			if (type.equals("flight")) {
				line += ClientSuitState.flightOn ? " : ON" : " : OFF";
				if (ClientSuitState.flightOn) color = 0x55FF55;
			} else if (cooldown > 0) {
				line += String.format(" (%.1fs)", cooldown / 20f);
				color = 0xFF8888;
			}
			g.drawString(mc.font, line, x, y, color, true);
			y += 11;
		}
	}
}
