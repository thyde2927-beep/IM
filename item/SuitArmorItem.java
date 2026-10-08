package com.ironmod.item;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/** One piece of a suit. The suit only "powers up" when all four pieces share the same suitId. */
public class SuitArmorItem extends ArmorItem {
	private final String suitId;

	public SuitArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties, String suitId) {
		super(material, type, properties);
		this.suitId = suitId;
	}

	public String suitId() {
		return suitId;
	}

	/** @return the suit id if the player wears a full matching set, otherwise null. */
	public static String wornSuit(Player player) {
		String id = null;
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack stack = player.getItemBySlot(slot);
			if (!(stack.getItem() instanceof SuitArmorItem piece)) return null;
			if (id == null) id = piece.suitId();
			else if (!id.equals(piece.suitId())) return null;
		}
		return id;
	}
}
