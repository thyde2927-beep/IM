package com.ironmod.item;

import com.ironmod.IronMod;
import com.ironmod.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.*;
import java.util.function.Supplier;

public final class ModItems {
	/** Every suit id here needs: textures, a lang entry, and a data/ironmod/suit_sets/<id>.json file. */
	public static final List<String> SUIT_IDS = List.of("mark_3", "mark_7", "mark_42", "mark_50", "mark_85");
	private static final ArmorItem.Type[] PIECES = {
			ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};

	public static final Map<String, Map<ArmorItem.Type, Item>> SUIT_PIECES = new LinkedHashMap<>();
	public static Item ARC_REACTOR;

	private static String suffix(ArmorItem.Type t) {
		return switch (t) {
			case HELMET -> "helmet";
			case CHESTPLATE -> "chestplate";
			case LEGGINGS -> "leggings";
			case BOOTS -> "boots";
			default -> "body";
		};
	}

	private static Holder<ArmorMaterial> material(String id, int helmet, int chest, int legs, int boots,
												   float toughness, float knockbackResistance, Supplier<Ingredient> repair) {
		Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		defense.put(ArmorItem.Type.HELMET, helmet);
		defense.put(ArmorItem.Type.CHESTPLATE, chest);
		defense.put(ArmorItem.Type.LEGGINGS, legs);
		defense.put(ArmorItem.Type.BOOTS, boots);
		defense.put(ArmorItem.Type.BODY, chest);
		// The Layer id points at assets/ironmod/textures/models/armor/<id>_layer_1.png / _layer_2.png
		ArmorMaterial mat = new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_NETHERITE, repair,
				List.of(new ArmorMaterial.Layer(IronMod.id(id))), toughness, knockbackResistance);
		return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, IronMod.id(id), mat);
	}

	public static void register() {
		ModBlocks.register();
		ARC_REACTOR = Registry.register(BuiltInRegistries.ITEM, IronMod.id("arc_reactor"),
				new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

		registerSuit("mark_3", 33, material("mark_3", 3, 7, 5, 3, 2.0f, 0.0f, () -> Ingredient.of(Items.IRON_INGOT)));
		registerSuit("mark_7", 38, material("mark_7", 3, 8, 6, 3, 3.0f, 0.05f, () -> Ingredient.of(Items.DIAMOND)));
		registerSuit("mark_42", 40, material("mark_42", 3, 8, 6, 3, 3.0f, 0.1f, () -> Ingredient.of(Items.EMERALD)));
		registerSuit("mark_50", 44, material("mark_50", 3, 8, 6, 3, 3.5f, 0.1f, () -> Ingredient.of(Items.AMETHYST_SHARD)));
		registerSuit("mark_85", 46, material("mark_85", 3, 8, 6, 3, 4.0f, 0.15f, () -> Ingredient.of(Items.NETHERITE_INGOT)));

		CreativeModeTab tab = FabricItemGroup.builder()
				.title(Component.translatable("itemGroup.ironmod.suits"))
				.icon(() -> new ItemStack(SUIT_PIECES.get("mark_3").get(ArmorItem.Type.HELMET)))
				.displayItems((params, out) -> {
					out.accept(ARC_REACTOR);
					out.accept(ModBlocks.SUIT_STAND_ITEM);
					SUIT_PIECES.values().forEach(m -> m.values().forEach(out::accept));
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, IronMod.id("suits"), tab);
	}

	private static void registerSuit(String suitId, int durabilityFactor, Holder<ArmorMaterial> material) {
		Map<ArmorItem.Type, Item> pieces = new LinkedHashMap<>();
		for (ArmorItem.Type type : PIECES) {
			Item item = new SuitArmorItem(material, type,
					new Item.Properties().durability(type.getDurability(durabilityFactor)).rarity(Rarity.RARE), suitId);
			pieces.put(type, Registry.register(BuiltInRegistries.ITEM, IronMod.id(suitId + "_" + suffix(type)), item));
		}
		SUIT_PIECES.put(suitId, pieces);
	}

	private ModItems() {}
}
