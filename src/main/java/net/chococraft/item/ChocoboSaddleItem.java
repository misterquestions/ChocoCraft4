package net.chococraft.item;

import net.minecraft.world.item.Item;

public class ChocoboSaddleItem extends Item {
	private final int inventorySize;

	public ChocoboSaddleItem(Properties properties, int inventorySize) {
		super(properties.stacksTo(4));
		this.inventorySize = inventorySize;
	}

	public int getInventorySize() {
		return inventorySize;
	}
}
