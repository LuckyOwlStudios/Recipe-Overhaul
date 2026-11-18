package com.luckyowlstudios.pinrecipe.network;

import com.luckyowlstudios.pinrecipe.Constants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: the full list of brewing recipe keys the player has unlocked, plus the keys that were
 * just learned from a scroll (empty for a plain sync) so the client can show a toast.
 */
public record SyncKnownBrewsPayload(List<String> keys, List<String> learned) implements CustomPacketPayload {
    public static final Type<SyncKnownBrewsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "known_brews"));
    private static final StreamCodec<ByteBuf, List<String>> KEYS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
    public static final StreamCodec<ByteBuf, SyncKnownBrewsPayload> STREAM_CODEC = StreamCodec.composite(
            KEYS, SyncKnownBrewsPayload::keys,
            KEYS, SyncKnownBrewsPayload::learned,
            SyncKnownBrewsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
