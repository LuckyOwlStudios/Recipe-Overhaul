package com.luckyowlstudios.codex.mixin;

import com.luckyowlstudios.codex.brewing.KnownBrewsHolder;
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
    private static final String codex$TAG = "codex:known_brews";

    @Unique
    private final Set<String> codex$knownBrews = new LinkedHashSet<>();

    @Override
    public Set<String> codex$getKnownBrews() {
        return codex$knownBrews;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void codex$save(CompoundTag tag, CallbackInfo ci) {
        ListTag list = new ListTag();
        for (String key : codex$knownBrews) {
            list.add(StringTag.valueOf(key));
        }
        tag.put(codex$TAG, list);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void codex$load(CompoundTag tag, CallbackInfo ci) {
        codex$knownBrews.clear();
        // Fall back to the tag saved before the mod was renamed from Pin Recipe.
        String key = tag.contains(codex$TAG) ? codex$TAG : "pinrecipe:known_brews";
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            codex$knownBrews.add(list.getString(i));
        }
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void codex$copyOnRespawn(ServerPlayer old, boolean keepEverything, CallbackInfo ci) {
        codex$knownBrews.clear();
        codex$knownBrews.addAll(((KnownBrewsHolder) old).codex$getKnownBrews());
    }
}
