package com.luckyowlstudios.pinrecipe.network;

import com.luckyowlstudios.pinrecipe.Constants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Server to client: the full list of brewing recipe keys the player has unlocked. */
public record SyncKnownBrewsPayload(List<String> keys) implements CustomPacketPayload {
    public static final Type<SyncKnownBrewsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "known_brews"));
    public static final StreamCodec<ByteBuf, SyncKnownBrewsPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(SyncKnownBrewsPayload::new, SyncKnownBrewsPayload::keys);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
