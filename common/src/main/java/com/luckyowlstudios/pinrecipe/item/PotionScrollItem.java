package com.luckyowlstudios.pinrecipe.item;

import com.luckyowlstudios.pinrecipe.brewing.BrewingKnowledge;
import com.luckyowlstudios.pinrecipe.brewing.BrewingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Using it teaches the player a random brewing recipe they don't know yet. */
public class PotionScrollItem extends Item {
    public PotionScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        Optional<BrewingRecipe> learned = BrewingKnowledge.unlockRandom(serverPlayer);
        if (learned.isEmpty()) {
            serverPlayer.displayClientMessage(
                    Component.translatable("item.pinrecipe.potion_scroll.all_known").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.6F, 1.2F);
        serverPlayer.sendSystemMessage(Component.translatable("item.pinrecipe.potion_scroll.learned",
                learned.get().describe().copy().withStyle(ChatFormatting.LIGHT_PURPLE)));
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.pinrecipe.potion_scroll.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
