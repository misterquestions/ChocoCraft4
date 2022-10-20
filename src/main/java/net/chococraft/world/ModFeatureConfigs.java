package net.chococraft.world;

import net.chococraft.block.GysahlGreenBlock;
import net.chococraft.config.ChocoConfig;
import net.chococraft.common.init.ModRegistry;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

public class ModFeatureConfigs {
	protected static final BlockState GYSAHL_GREEN = ModRegistry.GYSAHL_GREEN.get().defaultBlockState().setValue(GysahlGreenBlock.AGE, GysahlGreenBlock.MAX_AGE);

	public static final Holder<ConfiguredFeature<RandomPatchConfiguration, ?>> PATCH_GYSAHL_GREEN = FeatureUtils.register("patch_gysahl_green",
			Feature.RANDOM_PATCH,
			FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(GYSAHL_GREEN)),
					List.of(Blocks.GRASS_BLOCK), ChocoConfig.COMMON.gysahlGreenPatchSize.get()));
}
