package net.chococraft.item;

import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class CustomBlockNamedItem extends ItemNameBlockItem {
	private final Supplier<Block> blockSupplier;

	public CustomBlockNamedItem(Supplier<Block> blockSupplier, Properties properties) {
		super(null, properties);
		this.blockSupplier = blockSupplier;
	}

	@Override
	public Block getBlock() {
		return this.blockSupplier.get() == null ? null : this.blockSupplier.get().delegate.get();
	}
}
