package net.chococraft.common.inventory;

import net.chococraft.entity.Chocobo;
import net.chococraft.item.ChocoboSaddleItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class SaddleBagMenu extends AbstractContainerMenu {
	private Chocobo chocobo;

	public SaddleBagMenu(int id, Inventory player, Chocobo chocobo) {
		super(null, id);
		this.chocobo = chocobo;
		this.refreshSlots(chocobo, player);
	}

	public Chocobo getChocobo() {
		return chocobo;
	}

	public void refreshSlots(Chocobo chocobo, Inventory inventory) {
		this.slots.clear();

		// Saddle slot
		this.addSlot(new SlotChocoboSaddle(chocobo.saddleItemStackHandler, 0, -16, 18));

		//Chocobo inventory
		for (int row = 0; row < 5; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new SlotItemHandler(chocobo.inventory, row * 9 + col, 8 + col * 18, 18 + row * 18) {
					@Override
					public boolean isActive() {
						ItemStack saddleStack = chocobo.getSaddle();
						if (saddleStack.isEmpty()) {
							return false;
						}
						if (!saddleStack.isEmpty() && saddleStack.getItem() instanceof ChocoboSaddleItem saddleItem) {
							switch (saddleItem.getInventorySize()) {
								case 18 -> {
									int index = getSlotIndex();
									return (index > 10 && index < 16) || (index > 19 && index < 25) || (index > 28 && index < 34);
								}
								case 54 -> {
									return super.isActive();
								}
							}
						}

						return super.isActive();
					}
				});
			}
		}

		//Player inventory
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
			}
		}

		for (int i = 0; i < 9; ++i) {
			this.addSlot(new Slot(inventory, i, 8 + i * 18, 180));
		}
	}


	@Override
	public boolean stillValid(Player playerIn) {
		return this.chocobo.isAlive() && this.chocobo.distanceTo(playerIn) < 8.0F;
	}

	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			if (index < this.chocobo.inventory.getSlots()) {
				if (!this.moveItemStackTo(itemstack1, this.chocobo.inventory.getSlots(), this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(itemstack1, 0, this.chocobo.inventory.getSlots(), false)) {
				return ItemStack.EMPTY;
			}

			if (itemstack1.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return itemstack;
	}
}
