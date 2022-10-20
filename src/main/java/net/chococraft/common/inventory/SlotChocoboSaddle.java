package net.chococraft.common.inventory;

import net.chococraft.item.ChocoboSaddleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class SlotChocoboSaddle extends SlotItemHandler {
	public SlotChocoboSaddle(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
		super(itemHandler, index, xPosition, yPosition);
	}

	@Override
	public boolean mayPlace(@Nonnull ItemStack stack) {
		return stack.isEmpty() || stack.getItem() instanceof ChocoboSaddleItem;
	}

	@Override
	public int getMaxStackSize(@Nonnull ItemStack stack) {
		return 1;
	}
}
