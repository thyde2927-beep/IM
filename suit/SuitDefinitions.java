package com.ironmod.suit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ironmod.IronMod;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/** Reloads on /reload, so you can tweak suit JSON in a data pack without restarting. */
public final class SuitDefinitions implements SimpleSynchronousResourceReloadListener {
	private static volatile Map<String, SuitDefinition> definitions = Map.of();

	public static SuitDefinition get(String suitId) {
		return definitions.get(suitId);
	}

	@Override
	public ResourceLocation getFabricId() {
		return IronMod.id("suit_sets");
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		Map<String, SuitDefinition> loaded = new HashMap<>();
		manager.listResources("suit_sets", rl -> rl.getPath().endsWith(".json")).forEach((rl, resource) -> {
			String path = rl.getPath();
			String id = path.substring("suit_sets/".length(), path.length() - ".json".length());
			try (Reader reader = resource.openAsReader()) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				loaded.put(id, SuitDefinition.fromJson(id, json));
			} catch (Exception ex) {
				IronMod.LOGGER.error("Failed to load suit set {}", rl, ex);
			}
		});
		definitions = loaded;
		IronMod.LOGGER.info("Loaded {} suit sets", loaded.size());
	}
}
