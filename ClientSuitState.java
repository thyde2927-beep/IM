package com.ironmod.client;

import com.ironmod.net.SuitStatePayload;

import java.util.List;

/** Client-side copy of the last SuitStatePayload the server sent. */
public final class ClientSuitState {
	public static int energy, maxEnergy;
	public static float flightSpeed;
	public static boolean flightOn;
	public static List<String> abilities = List.of();
	public static List<Integer> cooldowns = List.of();

	private ClientSuitState() {}

	public static boolean active() {
		return maxEnergy > 0;
	}

	public static void update(SuitStatePayload p) {
		energy = p.energy();
		maxEnergy = p.maxEnergy();
		flightSpeed = p.flightSpeed();
		flightOn = p.flightOn();
		abilities = p.abilities();
		cooldowns = p.cooldowns();
	}

	public static void clear() {
		update(SuitStatePayload.empty());
	}
}
