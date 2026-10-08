package com.ironmod.suit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A suit "set" loaded from data/<ns>/suit_sets/<suit_id>.json  (the same idea as Fisk's Superheroes:
 * the stats and powers are data, so they can be rebalanced or extended with a data pack).
 */
public record SuitDefinition(String id, float maxEnergy, float regenPerTick, boolean fallImmune,
							 List<PassiveEffect> passives, List<Ability> abilities) {

	public record PassiveEffect(ResourceLocation effect, int amplifier) {}

	/** One bindable power. Unused fields for a given type are simply ignored. */
	public record Ability(String type, float damage, float range, float speed, int cooldown, float energyCost,
						  float energyPerTick, float explosionPower, int igniteSeconds, boolean blockDamage, int count) {}

	public static SuitDefinition fromJson(String id, JsonObject o) {
		float max = GsonHelper.getAsFloat(o, "max_energy", 1000f);
		float regen = GsonHelper.getAsFloat(o, "energy_regen_per_tick", 0.5f);
		boolean fallImmune = GsonHelper.getAsBoolean(o, "fall_damage_immunity", true);

		List<PassiveEffect> passives = new ArrayList<>();
		for (JsonElement e : GsonHelper.getAsJsonArray(o, "passive_effects", new JsonArray())) {
			JsonObject p = e.getAsJsonObject();
			passives.add(new PassiveEffect(ResourceLocation.parse(GsonHelper.getAsString(p, "effect")),
					GsonHelper.getAsInt(p, "amplifier", 0)));
		}

		List<Ability> abilities = new ArrayList<>();
		for (JsonElement e : GsonHelper.getAsJsonArray(o, "abilities", new JsonArray())) {
			JsonObject a = e.getAsJsonObject();
			abilities.add(new Ability(
					GsonHelper.getAsString(a, "type"),
					GsonHelper.getAsFloat(a, "damage", 0f),
					GsonHelper.getAsFloat(a, "range", 32f),
					GsonHelper.getAsFloat(a, "speed", 1f),
					GsonHelper.getAsInt(a, "cooldown", 0),
					GsonHelper.getAsFloat(a, "energy_cost", 0f),
					GsonHelper.getAsFloat(a, "energy_per_tick", 0f),
					GsonHelper.getAsFloat(a, "explosion_power", 0f),
					GsonHelper.getAsInt(a, "ignite_seconds", 0),
					GsonHelper.getAsBoolean(a, "block_damage", false),
					GsonHelper.getAsInt(a, "count", 1)));
		}
		return new SuitDefinition(id, max, regen, fallImmune, List.copyOf(passives), List.copyOf(abilities));
	}
}
