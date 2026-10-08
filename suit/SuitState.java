package com.ironmod.suit;

import net.minecraft.world.phys.Vec3;

/** Per-player runtime state of a worn suit (server side, not persisted: you re-spawn with a full reactor). */
public final class SuitState {
	public final SuitDefinition def;
	public final int[] cooldowns;
	public float energy;
	public boolean flightArmed;
	public Vec3 lastPos = Vec3.ZERO;
	public double speed;

	public SuitState(SuitDefinition def) {
		this.def = def;
		this.energy = def.maxEnergy();
		this.cooldowns = new int[def.abilities().size()];
	}

	public SuitDefinition.Ability flightAbility() {
		for (SuitDefinition.Ability a : def.abilities()) {
			if (a.type().equals("flight")) return a;
		}
		return null;
	}
}
