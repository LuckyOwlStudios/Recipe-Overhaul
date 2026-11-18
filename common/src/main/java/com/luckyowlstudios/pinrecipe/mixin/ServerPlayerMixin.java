package com.luckyowlstudios.pinrecipe.mixin;

import com.luckyowlstudios.pinrecipe.brewing.KnownBrewsHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/** Stores the player's unlocked brewing recipes in their save data and keeps them through death. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements KnownBrewsHolder {
    @Unique
    private static final String pinrecipe$TAG = "pinrecipe:known_brews";

    @Unique
    private final Set<String> pinrecipe$knownBrews = new LinkedHashSet<>();

    @Override
    public Set<String> pinrecipe$getKnownBrews() {
        return pinrecipe$knownBrews;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void pinrecipe$save(CompoundTag tag, CallbackInfo ci) {
        ListTag list = new ListTag();
        for (String key : pinrecipe$knownBrews) {
            list.add(StringTag.valueOf(key));
        }
        tag.put(pinrecipe$TAG, list);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void pinrecipe$load(CompoundTag tag, CallbackInfo ci) {
        pinrecipe$knownBrews.clear();
        ListTag list = tag.getList(pinrecipe$TAG, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            pinrecipe$knownBrews.add(list.getString(i));
        }
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void pinrecipe$copyOnRespawn(ServerPlayer old, boolean keepEverything, CallbackInfo ci) {
        pinrecipe$knownBrews.clear();
        pinrecipe$knownBrews.addAll(((KnownBrewsHolder) old).pinrecipe$getKnownBrews());
    }
}
