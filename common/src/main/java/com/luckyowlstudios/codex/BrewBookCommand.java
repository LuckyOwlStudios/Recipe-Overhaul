package com.luckyowlstudios.codex;

import com.luckyowlstudios.codex.brewing.BrewingKnowledge;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /brewbook unlockall | reset: operator shortcuts for testing the brewing recipe book. */
public final class BrewBookCommand {
    private BrewBookCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("brewbook")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("unlockall").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    int added = BrewingKnowledge.unlockAll(player);
                    context.getSource().sendSuccess(() -> Component.translatable("commands.codex.brewbook.unlockall", added), false);
                    return added;
                }))
                .then(Commands.literal("reset").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    BrewingKnowledge.reset(player);
                    context.getSource().sendSuccess(() -> Component.translatable("commands.codex.brewbook.reset"), false);
                    return 1;
                })));
    }
}
