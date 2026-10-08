package com.ironmod.suit;

import com.ironmod.item.SuitArmorItem;
import com.ironmod.net.SuitStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** All server-side suit behaviour: energy, passives, flight state, and ability execution. */
public final class SuitManager {
	private static final Map<UUID, SuitState> STATES = new HashMap<>();
	private static final float DEFAULT_FLY_SPEED = 0.05f;

	private SuitManager() {}

	public static void remove(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	// ------------------------------------------------------------------ tick

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickPlayer(player);
		}
	}

	private static void tickPlayer(ServerPlayer player) {
		String suitId = SuitArmorItem.wornSuit(player);
		SuitDefinition def = suitId == null ? null : SuitDefinitions.get(suitId);
		SuitState state = STATES.get(player.getUUID());

		// Suit taken off (or definition missing): clean up.
		if (def == null) {
			if (state != null) {
				unequip(player, state);
				STATES.remove(player.getUUID());
				ServerPlayNetworking.send(player, SuitStatePayload.empty());
			}
			return;
		}
		// Newly equipped, or switched to a different suit.
		if (state == null || !state.def.id().equals(def.id())) {
			if (state != null) unequip(player, state);
			state = new SuitState(def);
			STATES.put(player.getUUID(), state);
			state.lastPos = player.position();
			sync(player, state);
		}

		Vec3 pos = player.position();
		state.speed = pos.distanceTo(state.lastPos);
		state.lastPos = pos;

		for (int i = 0; i < state.cooldowns.length; i++) {
			if (state.cooldowns[i] > 0) state.cooldowns[i]--;
		}
		state.energy = Math.min(def.maxEnergy(), state.energy + def.regenPerTick());

		// Passive effects (strength, resistance, ...), refreshed once a second.
		if (player.tickCount % 20 == 0) {
			for (SuitDefinition.PassiveEffect passive : def.passives()) {
				BuiltInRegistries.MOB_EFFECT.getHolder(passive.effect()).ifPresent(holder ->
						player.addEffect(new MobEffectInstance(holder, 260, passive.amplifier(), true, false, false)));
			}
		}

		// Flight upkeep.
		SuitDefinition.Ability flight = state.flightAbility();
		if (state.flightArmed && flight != null) {
			Abilities abilities = player.getAbilities();
			if (!abilities.mayfly) {
				abilities.mayfly = true;
				player.onUpdateAbilities();
			}
			if (abilities.flying) {
				state.energy -= flight.energyPerTick();
				player.fallDistance = 0;
				if (state.speed > 0.25) {
					player.serverLevel().sendParticles(ParticleTypes.FLAME,
							player.getX(), player.getY() + 0.1, player.getZ(), 3, 0.15, 0.05, 0.15, 0.01);
				}
				if (state.energy <= 0) {
					state.energy = 0;
					setFlight(player, state, false);
					player.displayClientMessage(Component.translatable("message.ironmod.low_energy"), true);
				}
			}
		}

		if (player.tickCount % 5 == 0) sync(player, state);
	}

	private static void unequip(ServerPlayer player, SuitState state) {
		if (state.flightArmed) setFlight(player, state, false);
		for (SuitDefinition.PassiveEffect passive : state.def.passives()) {
			BuiltInRegistries.MOB_EFFECT.getHolder(passive.effect()).ifPresent(holder -> {
				MobEffectInstance inst = player.getEffect(holder);
				if (inst != null && inst.isAmbient()) player.removeEffect(holder);
			});
		}
	}

	private static void sync(ServerPlayer player, SuitState state) {
		SuitDefinition.Ability flight = state.flightAbility();
		List<String> names = state.def.abilities().stream().map(SuitDefinition.Ability::type).toList();
		List<Integer> cooldowns = java.util.stream.IntStream.of(state.cooldowns).boxed().toList();
		ServerPlayNetworking.send(player, new SuitStatePayload(
				Math.round(state.energy), Math.round(state.def.maxEnergy()),
				flight != null ? flight.speed() : 0f, names, cooldowns, state.flightArmed));
	}

	// ---------------------------------------------------------------- damage

	/** Fall damage / wall-splat immunity while a suit with fall_damage_immunity is active. */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (entity instanceof ServerPlayer player) {
			SuitState state = STATES.get(player.getUUID());
			if (state != null && state.def.fallImmune()
					&& (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.FLY_INTO_WALL))) {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------- abilities

	public static void activate(ServerPlayer player, int slot) {
		SuitState state = STATES.get(player.getUUID());
		if (state == null) return;
		List<SuitDefinition.Ability> abilities = state.def.abilities();
		if (slot < 0 || slot >= abilities.size()) return;
		if (state.cooldowns[slot] > 0) return;

		SuitDefinition.Ability ability = abilities.get(slot);
		switch (ability.type()) {
			case "flight" -> {
				if (state.flightArmed) {
					setFlight(player, state, false);
				} else if (state.energy > 20) {
					setFlight(player, state, true);
				} else {
					player.displayClientMessage(Component.translatable("message.ironmod.low_energy"), true);
				}
				state.cooldowns[slot] = 10; // debounce
			}
			case "repulsor" -> {
				if (!spend(player, state, ability.energyCost())) return;
				fireBeam(player, ability, false);
				state.cooldowns[slot] = ability.cooldown();
			}
			case "unibeam" -> {
				if (!spend(player, state, ability.energyCost())) return;
				fireBeam(player, ability, true);
				state.cooldowns[slot] = ability.cooldown();
			}
			case "missiles" -> {
				if (!spend(player, state, ability.energyCost())) return;
				fireMissiles(player, ability);
				state.cooldowns[slot] = ability.cooldown();
			}
			case "boost" -> {
				if (!spend(player, state, ability.energyCost())) return;
				player.setDeltaMovement(player.getLookAngle().scale(ability.speed()));
				player.hurtMarked = true; // makes the server push the new velocity to the client
				player.fallDistance = 0;
				player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
						SoundSource.PLAYERS, 1.0f, 0.7f);
				state.cooldowns[slot] = ability.cooldown();
			}
			default -> { /* unknown type in JSON: ignore */ }
		}
	}

	private static boolean spend(ServerPlayer player, SuitState state, float cost) {
		if (state.energy < cost) {
			player.displayClientMessage(Component.translatable("message.ironmod.low_energy"), true);
			return false;
		}
		state.energy -= cost;
		return true;
	}

	/**
	 * Flight uses vanilla "mayfly" abilities (no kick-for-flying on servers, hover for free).
	 * The client mod adds forward thrust along your look direction while you hold W.
	 */
	private static void setFlight(ServerPlayer player, SuitState state, boolean on) {
		Abilities abilities = player.getAbilities();
		state.flightArmed = on;
		if (on) {
			SuitDefinition.Ability flight = state.flightAbility();
			float boost = flight != null ? flight.speed() : 1f;
			abilities.mayfly = true;
			abilities.setFlyingSpeed(DEFAULT_FLY_SPEED + 0.04f * boost);
			player.displayClientMessage(Component.translatable("message.ironmod.flight_on"), true);
		} else {
			if (!player.isCreative() && !player.isSpectator()) {
				abilities.mayfly = false;
				abilities.flying = false;
			}
			abilities.setFlyingSpeed(DEFAULT_FLY_SPEED);
			player.displayClientMessage(Component.translatable("message.ironmod.flight_off"), true);
		}
		player.onUpdateAbilities();
	}

	/** Hitscan beam: stops at the first block or entity, damages, ignites, optionally explodes. */
	private static void fireBeam(ServerPlayer player, SuitDefinition.Ability a, boolean heavy) {
		ServerLevel level = player.serverLevel();
		Vec3 dir = player.getLookAngle();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(dir.scale(a.range()));

		BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

		AABB box = new AABB(eye, stop).inflate(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, stop, box,
				e -> !e.isSpectator() && e.isPickable());
		if (entityHit != null) {
			stop = entityHit.getLocation();
			Entity target = entityHit.getEntity();
			target.hurt(level.damageSources().playerAttack(player), a.damage());
			if (a.igniteSeconds() > 0) target.setRemainingFireTicks(a.igniteSeconds() * 20);
		}

		// Visual: a line of particles from just in front of the "palm" to the impact point.
		Vec3 start = eye.add(dir.scale(0.6)).add(0, -0.25, 0);
		double length = start.distanceTo(stop);
		Vec3 step = stop.subtract(start).normalize().scale(0.5);
		ParticleOptions core = heavy ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK;
		Vec3 p = start;
		for (double d = 0; d < length; d += 0.5) {
			level.sendParticles(core, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			if (heavy) level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
			p = p.add(step);
		}

		if (a.explosionPower() > 0) {
			level.explode(player, stop.x, stop.y, stop.z, a.explosionPower(),
					a.blockDamage() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
		}

		if (heavy) {
			level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.8f);
			level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.5f, 0.6f);
		} else {
			level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.8f, 1.7f);
		}
	}

	/** Micro-missile volley: `count` slightly spread hitscan shots, each with its own small explosion. */
	private static void fireMissiles(ServerPlayer player, SuitDefinition.Ability a) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		for (int i = 0; i < Math.max(1, a.count()); i++) {
			Vec3 dir = player.getLookAngle().add(
					(level.random.nextDouble() - 0.5) * 0.14,
					(level.random.nextDouble() - 0.5) * 0.14,
					(level.random.nextDouble() - 0.5) * 0.14).normalize();
			Vec3 end = eye.add(dir.scale(a.range()));
			BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
			EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, player, eye, stop,
					new AABB(eye, stop).inflate(1.0), e -> !e.isSpectator() && e.isPickable());
			if (entityHit != null) {
				stop = entityHit.getLocation();
				entityHit.getEntity().hurt(level.damageSources().playerAttack(player), a.damage());
			}
			Vec3 start = eye.add(dir.scale(0.8)).add(0, -0.3, 0);
			double length = start.distanceTo(stop);
			Vec3 step = stop.subtract(start).normalize().scale(1.0);
			Vec3 p = start;
			for (double d = 0; d < length; d += 1.0) {
				level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
				p = p.add(step);
			}
			if (a.explosionPower() > 0) {
				level.explode(player, stop.x, stop.y, stop.z, a.explosionPower(),
						a.blockDamage() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
			}
		}
		level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2f, 0.9f);
	}
}
