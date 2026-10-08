package com.ironmod.net;

import com.ironmod.IronMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server -> client: everything the HUD and flight thrust need. maxEnergy == 0 means "no suit worn". */
public record SuitStatePayload(int energy, int maxEnergy, float flightSpeed, List<String> abilities,
							   List<Integer> cooldowns, boolean flightOn) implements CustomPacketPayload {
	public static final Type<SuitStatePayload> TYPE = new Type<>(IronMod.id("suit_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SuitStatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SuitStatePayload::energy,
			ByteBufCodecs.VAR_INT, SuitStatePayload::maxEnergy,
			ByteBufCodecs.FLOAT, SuitStatePayload::flightSpeed,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SuitStatePayload::abilities,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SuitStatePayload::cooldowns,
			ByteBufCodecs.BOOL, SuitStatePayload::flightOn,
			SuitStatePayload::new);

	public static SuitStatePayload empty() {
		return new SuitStatePayload(0, 0, 0f, List.of(), List.of(), false);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
