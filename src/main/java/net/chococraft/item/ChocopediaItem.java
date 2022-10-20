package net.chococraft.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

public class ChocopediaItem extends Item {

	public ChocopediaItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player playerIn, InteractionHand handIn) {
		if (ModList.get().isLoaded("patchouli")) {
			if (level.isClientSide) {
				vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(new ResourceLocation("chococraft:chocopedia"));
			}
		} else {
			if (level.isClientSide) {
				net.chococraft.client.gui.ChocoboBookScreen.openScreen();
			}
		}

		return new InteractionResultHolder<>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
	}
}
