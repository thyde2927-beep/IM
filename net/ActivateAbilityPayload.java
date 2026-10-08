package com.ironmod.net;

import com.ironmod.IronMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the player pressed ability key #slot. */
public record ActivateAbilityPayload(int slot) implements CustomPacketPayload {
	public static final Type<ActivateAbilityPayload> TYPE = new Type<>(IronMod.id("activate_ability"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, ActivateAbilityPayload::slot, ActivateAbilityPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
