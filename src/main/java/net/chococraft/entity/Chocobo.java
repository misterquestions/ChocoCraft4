package net.chococraft.entity;

import net.chococraft.Chococraft;
import net.chococraft.config.ChocoConfig;
import net.chococraft.entity.breeding.BreedingHelper;
import net.chococraft.entity.breeding.ChocoboAbilityInfo;
import net.chococraft.entity.goal.ChocoboFollowOwnerGoal;
import net.chococraft.entity.goal.ChocoboHealInPenGoal;
import net.chococraft.entity.properties.ChocoboColor;
import net.chococraft.entity.properties.ModDataSerializers;
import net.chococraft.entity.properties.MovementType;
import net.chococraft.common.init.ModEntities;
import net.chococraft.common.init.ModRegistry;
import net.chococraft.common.init.ModSounds;
import net.chococraft.common.inventory.SaddleBagMenu;
import net.chococraft.common.inventory.SaddleItemStackHandler;
import net.chococraft.item.ChocoDisguiseItem;
import net.chococraft.item.ChocoboSaddleItem;
import net.chococraft.network.PacketManager;
import net.chococraft.network.packets.OpenChocoboGuiMessage;
import net.chococraft.utils.RandomHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class Chocobo extends TamableAnimal {
	private static final String NBTKEY_CHOCOBO_COLOR = "Color";
	private static final String NBTKEY_CHOCOBO_IS_MALE = "Male";
	private static final String NBTKEY_MOVEMENTTYPE = "MovementType";
	private static final String NBTKEY_SADDLE_ITEM = "Saddle";
	private static final String NBTKEY_INVENTORY = "Inventory";
	private static final String NBTKEY_CHOCOBO_GENERATION = "Generation";

	private static final EntityDataAccessor<ChocoboColor> PARAM_COLOR = SynchedEntityData.defineId(Chocobo.class, ModDataSerializers.CHOCOBO_COLOR);
	private static final EntityDataAccessor<Boolean> PARAM_IS_MALE = SynchedEntityData.defineId(Chocobo.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> PARAM_FED_GOLD_GYSAHL = SynchedEntityData.defineId(Chocobo.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<MovementType> PARAM_MOVEMENT_TYPE = SynchedEntityData.defineId(Chocobo.class, ModDataSerializers.MOVEMENT_TYPE);
	private static final EntityDataAccessor<ItemStack> PARAM_SADDLE_ITEM = SynchedEntityData.defineId(Chocobo.class, EntityDataSerializers.ITEM_STACK);

	private final static EntityDataAccessor<Integer> PARAM_GENERATION = SynchedEntityData.defineId(Chocobo.class, EntityDataSerializers.INT);

	private AvoidEntityGoal<Player> chocoboAvoidPlayerGoal;
	private ChocoboHealInPenGoal healInPenGoal;

	public final ItemStackHandler inventory = new ItemStackHandler(45) {
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			if (getSaddle().isEmpty()) {
				return false;
			}
			if (getSaddle().getItem() instanceof ChocoboSaddleItem saddleItem) {
				switch (saddleItem.getInventorySize()) {
					case 18 -> {
						return ((slot > 10 && slot < 16) || (slot > 19 && slot < 25) || (slot > 28 && slot < 34)) && super.isItemValid(slot, stack);
					}
					case 45 -> {
						return super.isItemValid(slot, stack);
					}
					default -> {
						return false;
					}
				}
			}


			return super.isItemValid(slot, stack);
		}
	};
	private final LazyOptional<IItemHandler> inventoryHolder = LazyOptional.of(() -> inventory);

	public final SaddleItemStackHandler saddleItemStackHandler = new SaddleItemStackHandler() {
		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return stack.isEmpty() || stack.getItem() instanceof ChocoboSaddleItem;
		}

		@Override
		protected void onStackChanged() {
			Chocobo.this.setSaddleType(this.itemStack);
		}
	};
	private final LazyOptional<IItemHandler> saddleHolder = LazyOptional.of(() -> saddleItemStackHandler);

	private float wingRotation;
	private float destPos;
	private float wingRotDelta;

	public int timeSinceFeatherChance;

	public Chocobo(EntityType<? extends Chocobo> type, Level world) {
		super(type, world);
		timeSinceFeatherChance = 0;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
		this.goalSelector.addGoal(4, new ChocoboFollowOwnerGoal(this, 1.0D, 5.0F, 5.0F));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 5.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	private final ChocoboFollowOwnerGoal follow = new ChocoboFollowOwnerGoal(this, 2.0D, 3.0F, 10.0F);

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 20 / 100f)
				.add(Attributes.FLYING_SPEED, 0.0F)
				.add(Attributes.MAX_HEALTH, 30);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(PARAM_COLOR, ChocoboColor.YELLOW);
		this.entityData.define(PARAM_IS_MALE, false);
		this.entityData.define(PARAM_FED_GOLD_GYSAHL, false);
		this.entityData.define(PARAM_MOVEMENT_TYPE, MovementType.WANDER);
		this.entityData.define(PARAM_SADDLE_ITEM, ItemStack.EMPTY);
		this.entityData.define(PARAM_GENERATION, 0);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor levelAccessor, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
		if (levelAccessor.getBiome((new BlockPos(blockPosition().below()))).is(BiomeTags.IS_NETHER)) {
			this.setChocoboColor(ChocoboColor.FLAME);
		}
		finalizeChocobo(this);
		return super.finalizeSpawn(levelAccessor, difficultyIn, reason, spawnDataIn, dataTag);
	}

	private void finalizeChocobo(Chocobo chocobo) {
		chocobo.setMale(random.nextBoolean());
		chocobo.getAttribute(Attributes.MAX_HEALTH).setBaseValue(getChocoboColor().getAbilityInfo().getMaxHP());
		chocobo.setHealth(getMaxHealth());
		chocobo.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(getChocoboColor().getAbilityInfo().getLandSpeed() / 100F);
		chocobo.getAttribute(Attributes.FLYING_SPEED).setBaseValue(getChocoboColor().getAbilityInfo().getAirbornSpeed() / 100F);
	}

	@Override
	public boolean canBeControlledByRider() {
		return this.isTame();
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		this.setChocoboColor(ChocoboColor.values()[compound.getByte(NBTKEY_CHOCOBO_COLOR)]);
		this.setMale(compound.getBoolean(NBTKEY_CHOCOBO_IS_MALE));
		this.setMovementType(MovementType.values()[compound.getByte(NBTKEY_MOVEMENTTYPE)]);
		this.saddleItemStackHandler.deserializeNBT(compound.getCompound(NBTKEY_SADDLE_ITEM));
		this.inventory.deserializeNBT(compound.getCompound(NBTKEY_INVENTORY));

		this.setGeneration(compound.getInt(NBTKEY_CHOCOBO_GENERATION));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putByte(NBTKEY_CHOCOBO_COLOR, (byte) this.getChocoboColor().ordinal());
		compound.putBoolean(NBTKEY_CHOCOBO_IS_MALE, this.isMale());
		compound.putByte(NBTKEY_MOVEMENTTYPE, (byte) this.getMovementType().ordinal());
		compound.put(NBTKEY_SADDLE_ITEM, this.saddleItemStackHandler.serializeNBT());
		compound.put(NBTKEY_INVENTORY, this.inventory.serializeNBT());

		compound.putInt(NBTKEY_CHOCOBO_GENERATION, this.getGeneration());
	}

	public ChocoboColor getChocoboColor() {
		return this.entityData.get(PARAM_COLOR);
	}

	public void setChocoboColor(ChocoboColor color) {
		this.entityData.set(PARAM_COLOR, color);
	}

	@Override
	public boolean fireImmune() {
		return getChocoboColor().getAbilityInfo().isImmuneToFire();
	}

	public boolean isMale() {
		return this.entityData.get(PARAM_IS_MALE);
	}

	public void setMale(boolean isMale) {
		this.entityData.set(PARAM_IS_MALE, isMale);
	}

	public boolean isFedGoldGysahl() {
		return this.entityData.get(PARAM_FED_GOLD_GYSAHL);
	}

	public void setFedGoldGysahl(boolean value) {
		this.entityData.set(PARAM_FED_GOLD_GYSAHL, value);
	}

	public MovementType getMovementType() {
		return this.entityData.get(PARAM_MOVEMENT_TYPE);
	}

	public void setMovementType(MovementType type) {
		this.entityData.set(PARAM_MOVEMENT_TYPE, type);
	}

	public boolean isSaddled() {
		return !this.getSaddle().isEmpty();
	}

	public ItemStack getSaddle() {
		return this.entityData.get(PARAM_SADDLE_ITEM);
	}

	private void setSaddleType(ItemStack saddleStack) {
		ItemStack newStack = saddleStack;
		ItemStack oldStack = getSaddle();
		if (oldStack.getItem() != newStack.getItem()) {
			this.entityData.set(PARAM_SADDLE_ITEM, newStack.copy());
			this.reconfigureInventory(oldStack, newStack);
		}
	}

	private int getSaddleCount(ItemStack stack) {
		if (stack.getItem() instanceof ChocoboSaddleItem saddle) {
			return saddle.getInventorySize();
		}
		return 0;
	}

	public int getGeneration() {
		return this.entityData.get(PARAM_GENERATION);
	}

	public void setGeneration(int value) {
		this.entityData.set(PARAM_GENERATION, value);
	}
	//endregion

	@Override
	public double getPassengersRidingOffset() {
		return 1.65D;
	}

	@Override
	public boolean canBeRiddenInWater(Entity rider) {
		return true;
	}

	@Nullable
	public Entity getControllingPassenger() {
		return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
	}

	@Override
	protected boolean updateInWaterStateAndDoFluidPushing() {
		this.fluidHeight.clear();
		this.updateInWaterStateAndDoWaterCurrentPushing();
		double d0 = this.level.dimensionType().ultraWarm() ? 0.007D : 0.0023333333333333335D;
		boolean flag = this.updateFluidHeightAndDoFluidPushing(FluidTags.LAVA, d0);
		return this.isInWater() || flag;
	}

	private void updateInWaterStateAndDoWaterCurrentPushing() {
		if (this.getVehicle() instanceof Boat) {
			this.wasTouchingWater = false;
		} else if (this.updateFluidHeightAndDoFluidPushing(FluidTags.WATER, 0.014D)) {
			if (!this.wasTouchingWater && !this.firstTick) {
				this.doWaterSplashEffect();
			}

			this.resetFallDistance();
			this.wasTouchingWater = true;
			this.clearFire();
		} else {
			this.wasTouchingWater = false;
		}
	}

	@Override
	public void travel(Vec3 travelVector) {
		if (this.isAlive() && this.isVehicle() && this.canBeControlledByRider() && this.isSaddled()) {
			LivingEntity livingentity = (LivingEntity) this.getControllingPassenger();
			this.setYRot(livingentity.getYRot());
			this.yRotO = this.getYRot();
			this.setXRot(livingentity.getXRot() * 0.5F);
			this.setRot(this.getYRot(), this.getXRot());
			this.yBodyRot = this.getYRot();
			this.yHeadRot = this.yBodyRot;
			float strafe = livingentity.xxa * 0.5F;
			float forward = livingentity.zza;
			if (forward <= 0.0F) {
				forward *= 0.25F;
			}

			if (isInWater() && this.getAbilityInfo().canWalkOnWater()) {
				Vec3 delta = getDeltaMovement();
				this.setDeltaMovement(delta.x, 0.4D, delta.y);
				this.moveRelative(getChocoboColor().getAbilityInfo().getWaterSpeed() / 100F, travelVector);
				setJumping(true);
			}

			if (livingentity.jumping && this.getAbilityInfo().getCanFly()) {
				setJumping(true);
				this.jumpFromGround();
				this.hasImpulse = true;
				this.moveRelative(getChocoboColor().getAbilityInfo().getAirbornSpeed() / 100, travelVector);
			} else if (livingentity.jumping && !this.jumping && this.onGround) {
				this.setDeltaMovement(getDeltaMovement().add(0, 0.75D, 0));
				livingentity.setJumping(false);
				this.setJumping(true);
			}

			if (this.isControlledByLocalInstance()) {
				this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
				super.travel(new Vec3((double) strafe, travelVector.y, (double) forward));
			} else if (livingentity instanceof Player) {
				this.setDeltaMovement(Vec3.ZERO);
			}

			this.animationSpeedOld = this.animationSpeed;
			double d1 = this.getX() - this.xo;
			double d0 = this.getZ() - this.zo;
			float f4 = Mth.sqrt((float) (d1 * d1 + d0 * d0)) * 4.0F;

			if (f4 > 1.0F) {
				f4 = 1.0F;
			}

			this.animationSpeed += (f4 - this.animationSpeed) * 0.4F;
			this.animationPosition += this.animationSpeed;

			if (this.onGround) {
				this.setJumping(false);
			}

			this.calculateEntityAnimation(this, false);
			this.tryCheckInsideBlocks();
		} else {
			super.travel(travelVector);
		}
	}

	private ChocoboAbilityInfo getAbilityInfo() {
		return getChocoboColor().getAbilityInfo();
	}

	@Override
	public void positionRider(Entity passenger) {
		super.positionRider(passenger);
		if (passenger instanceof Mob && this.getControllingPassenger() == passenger) {
			this.yBodyRot = ((LivingEntity) passenger).yBodyRot;
		}
	}

	@Nullable
	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		Chocobo babyChocobo = ModEntities.CHOCOBO.get().create(level);
		babyChocobo.setChocoboColor(BreedingHelper.getColor(this, (Chocobo) partner));
		finalizeChocobo(babyChocobo);

		//Reset golden status
		this.setFedGoldGysahl(false);
		((Chocobo) partner).setFedGoldGysahl(false);
		return babyChocobo;
	}

	@Override
	public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
		if (partner instanceof Chocobo target && this.getChocoboColor() == ChocoboColor.PURPLE && target.getChocoboColor() == ChocoboColor.PURPLE) {
			this.spawnAtLocation(new ItemStack(ModRegistry.PURPLE_CHOCOBO_SPAWN_EGG.get()), 0);
			this.setAge(6000);
			target.setAge(6000);
			this.resetLove();
			target.resetLove();
		} else {
			super.spawnChildFromBreeding(level, partner);
		}
	}

	@Override
	public boolean canMate(Animal parent) {
		if (parent == this || !(parent instanceof Chocobo otherChocobo)) return false;
		if (!this.isInLove() || !parent.isInLove()) return false;
		return otherChocobo.isMale() != this.isMale();
	}

	public void dropFeather() {
		if (this.getCommandSenderWorld().isClientSide)
			return;

		if (this.isBaby())
			return;

		this.spawnAtLocation(new ItemStack(ModRegistry.CHOCOBO_FEATHER.get(), 1), 0.0F);
	}

	@Override
	protected boolean canRide(Entity entityIn) {
		return !this.getSaddle().isEmpty() && super.canRide(entityIn);
	}

	@Override
	public float getStepHeight() {
		return getAbilityInfo().getStepHeight(isVehicle());
	}

	@Override
	public void aiStep() {
		super.aiStep();

		this.setRot(this.getYRot(), this.getXRot());

		resetFallDistance();

		if (this.timeSinceFeatherChance == 3000) {
			this.timeSinceFeatherChance = 0;

			if ((float) Math.random() < .25) {
				this.dropFeather();
			}
		} else {
			this.timeSinceFeatherChance++;
		}

		if (this.getCommandSenderWorld().isClientSide) {
			// Wing rotations, control packet, client side
			this.destPos += (double) (this.onGround ? -1 : 4) * 0.3D;
			this.destPos = Mth.clamp(destPos, 0f, 1f);

			if (!this.onGround)
				this.wingRotDelta = Math.min(wingRotation, 1f);
			this.wingRotDelta *= 0.9D;
			this.wingRotation += this.wingRotDelta * 2.0F;

			if (this.onGround) {
				this.animationSpeedOld = this.animationSpeed;
				double d1 = this.getX() - this.xo;
				double d0 = this.getZ() - this.zo;
				float f4 = ((float) Math.sqrt(d1 * d1 + d0 * d0)) * 4.0F;

				if (f4 > 1.0F) {
					f4 = 1.0F;
				}

				this.animationSpeed += (f4 - this.animationSpeed) * 0.4F;
				this.animationPosition += this.animationSpeed;
			} else {
				this.animationPosition = 0;
				this.animationSpeed = 0;
				this.animationSpeedOld = 0;
			}
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (isVehicle() && getFirstPassenger() instanceof LivingEntity passenger) {
			getAbilityInfo().getRiderAbilities().forEach(ability -> {
				passenger.addEffect(ability.get());
				this.addEffect(ability.get());
			});
		}
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(ModRegistry.LOVERLY_GYSAHL_GREEN.get()) || stack.is(ModRegistry.GOLD_GYSAHL.get()) || stack.is(ModRegistry.GYSAHL_CAKE.get());
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack heldItemStack = player.getItemInHand(hand);
		if (this.isFood(heldItemStack)) {
			boolean fedCake = heldItemStack.getItem() == ModRegistry.GYSAHL_CAKE.get();
			//Allow the Chocobo to breed by feeding Loverly or Gold Gysahl
			if (this.isBaby()) {
				if (fedCake) {
					this.usePlayerItem(player, hand, heldItemStack);
					this.setBaby(false);
					this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
					return InteractionResult.sidedSuccess(this.level.isClientSide);
				}
			} else {
				int i = this.getAge();
				if (!this.level.isClientSide && i == 0 && this.canFallInLove() && !fedCake) {
					if (heldItemStack.getItem() == ModRegistry.GOLD_GYSAHL.get()) {
						//If fed a Gold Gysahl set the "fedGoldGysahl" flag to true
						this.setFedGoldGysahl(true);
					}
					this.usePlayerItem(player, hand, heldItemStack);
					this.setInLove(player);
					this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
					return InteractionResult.SUCCESS;
				}
			}

			if (this.level.isClientSide) {
				return InteractionResult.CONSUME;
			}
		}

		if (this.level.isClientSide) {
			return InteractionResult.PASS;
		} else {
			if (this.isTame()) {
				if (this.isSaddled() && player.getMainHandItem().isEmpty() && !player.isShiftKeyDown() && !this.isBaby()) {
					player.startRiding(this);
					return InteractionResult.SUCCESS;
				}

				if (player.isShiftKeyDown() && !this.isBaby() && player.getMainHandItem().isEmpty()) {
					if (player instanceof ServerPlayer) this.displayChocoboInventory((ServerPlayer) player);
					return InteractionResult.SUCCESS;
				}

				//Switch between the Chocobo following, wandering or staying using the Chocobo Whistle
				if (heldItemStack.is(ModRegistry.CHOCOBO_WHISTLE.get()) && !this.isBaby()) {
					if (isOwnedBy(player)) {
						if (getMovementType() == MovementType.STANDSTILL) {
							this.playSound(ModSounds.WHISTLE_SOUND_FOLLOW.get(), 1.0F, 1.0F);
							this.setNoAi(false);
							this.goalSelector.addGoal(0, this.follow);
							this.setMovementType(MovementType.FOLLOW_OWNER);
							player.displayClientMessage(new TranslatableComponent(Chococraft.MODID + ".entity_chocobo.chocobo_followcmd"), true);
						} else if (this.getMovementType() == MovementType.FOLLOW_OWNER) {
							this.playSound(ModSounds.WHISTLE_SOUND_WANDER.get(), 1.0F, 1.0F);
							this.goalSelector.removeGoal(this.follow);
							setMovementType(MovementType.WANDER);
							player.displayClientMessage(new TranslatableComponent(Chococraft.MODID + ".entity_chocobo.chocobo_wandercmd"), true);
						} else if (this.getMovementType() == MovementType.WANDER) {
							this.playSound(ModSounds.WHISTLE_SOUND_STAY.get(), 1.0F, 1.0F);
							this.setNoAi(true);
							this.setMovementType(MovementType.STANDSTILL);
							player.displayClientMessage(new TranslatableComponent(Chococraft.MODID + ".entity_chocobo.chocobo_staycmd"), true);
						}
					} else {
						player.displayClientMessage(new TranslatableComponent(Chococraft.MODID + ".entity_chocobo.not_owner"), true);
					}
					return InteractionResult.SUCCESS;
				}


				//Heal the Chocobo if fed with Gysahl Green after being tamed and not at max health
				if (heldItemStack.is(ModRegistry.GYSAHL_GREEN_ITEM.get())) {
					if (getHealth() != getMaxHealth()) {
						this.usePlayerItem(player, hand, heldItemStack);
						this.heal(5);
						this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
						return InteractionResult.SUCCESS;
					} else {
						player.displayClientMessage(new TranslatableComponent(Chococraft.MODID + ".entity_chocobo.heal_fail"), true);
						return InteractionResult.PASS;
					}
				}

				//Turn Gold Chocobo red or pink depending on the gysahl fed
				if (getChocoboColor() == ChocoboColor.GOLD) {
					if (heldItemStack.getItem() == ModRegistry.RED_GYSAHL.get()) {
						this.usePlayerItem(player, hand, heldItemStack);
						this.setChocoboColor(ChocoboColor.RED);
						return InteractionResult.SUCCESS;
					} else if (heldItemStack.getItem() == ModRegistry.PINK_GYSAHL.get()) {
						this.usePlayerItem(player, hand, heldItemStack);
						this.setChocoboColor(ChocoboColor.PINK);
						return InteractionResult.SUCCESS;
					}
				}

				//Saddle the Chocobo if right-clicked with a saddle
				if (heldItemStack.getItem() instanceof ChocoboSaddleItem && !this.isSaddled() && !this.isBaby()) {
					if (!this.level.isClientSide) {
						this.saddleItemStackHandler.setStackInSlot(0, heldItemStack.copy().split(1));
						this.setSaddleType(heldItemStack);
						this.usePlayerItem(player, hand, heldItemStack);
					}
					return InteractionResult.SUCCESS;
				}
			} else {
				//Chance of taming Chocobo if right-clicked with Gysahl Green
				if (heldItemStack.is(ModRegistry.GYSAHL_GREEN_ITEM.get())) {
					this.usePlayerItem(player, hand, heldItemStack);
					if ((float) Math.random() < ChocoConfig.COMMON.tameChance.get().floatValue()) {
						this.setOwnerUUID(player.getUUID());
						this.setTame(true);
						if (ChocoConfig.COMMON.nameTamedChocobos.get()) {
							if (!hasCustomName()) {
								setCustomName(DefaultNames.getRandomName(random, isMale()));
							}
						}
						player.displayClientMessage(new TranslatableComponent(Chococraft.MODID + ".entity_chocobo.tame_success"), true);
					} else {
						player.displayClientMessage(new TranslatableComponent(Chococraft.MODID + ".entity_chocobo.tame_fail"), true);
					}
					this.gameEvent(GameEvent.MOB_INTERACT, this.eyeBlockPosition());
					return InteractionResult.SUCCESS;
				}
			}
		}
		return InteractionResult.PASS;
	}

	private void displayChocoboInventory(ServerPlayer player) {
		if (player.containerMenu != player.inventoryMenu) {
			player.closeContainer();
		}

		player.nextContainerCounter();
		PacketManager.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenChocoboGuiMessage(this, player.containerCounter));
		player.containerMenu = new SaddleBagMenu(player.containerCounter, player.getInventory(), this);
		player.initMenu(player.containerMenu);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerContainerEvent.Open(player, player.containerMenu));
	}

	private void reconfigureInventory(ItemStack oldSaddle, ItemStack newSaddle) {
		if (!this.getCommandSenderWorld().isClientSide) {
			// TODO: Handle resizing. ItemStackHandler#setSize() clears the internal inventory!
			for (int i = 0; i < this.inventory.getSlots(); i++) {
				if (this.isAlive()) {
					ItemStack stack = this.inventory.extractItem(i, Integer.MAX_VALUE, false);
					Containers.dropItemStack(this.getCommandSenderWorld(), this.getX(), this.getY() + .5, this.getZ(), stack);
				}
			}
		}

		for (Player player : level.players()) {
			if (player.containerMenu instanceof SaddleBagMenu bagContainer) {
				bagContainer.refreshSlots(bagContainer.getChocobo(), player.getInventory());
			}
		}
	}

	@Override
	protected void dropFromLootTable(DamageSource damageSourceIn, boolean attackedRecently) {
		super.dropFromLootTable(damageSourceIn, attackedRecently);

		if (this.inventory != null && this.isSaddled()) {
			for (int i = 0; i < this.inventory.getSlots(); i++) {
				if (!this.inventory.getStackInSlot(i).isEmpty())
					this.spawnAtLocation(this.inventory.getStackInSlot(i), 0.0f);
			}
		}
	}

	protected SoundEvent getAmbientSound() {
		return ModSounds.AMBIENT_SOUND.get();
	}

	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return ModSounds.AMBIENT_SOUND.get();
	}

	protected SoundEvent getDeathSound() {
		return ModSounds.AMBIENT_SOUND.get();
	}

	@Override
	protected float getSoundVolume() {
		return .6f;
	}

	@Override
	public int getAmbientSoundInterval() {
		return (24 * (int) (Math.random() * 100));
	}

	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader levelReader) {
		if (this.level.getBiome((new BlockPos(blockPosition()))).is(BiomeTags.IS_NETHER))
			return 0.0F;

		return super.getWalkTargetValue(pos, levelReader);
	}

	@Override
	protected void reassessTameGoals() {
		super.reassessTameGoals();
		if (chocoboAvoidPlayerGoal == null) {
			chocoboAvoidPlayerGoal = new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.0D, 1.2D, livingEntity -> {
				if (livingEntity instanceof Player player) {
					int chance = 0;
					for (ItemStack stack : player.getInventory().armor) {
						if (stack != null) {
							if (stack.getItem() instanceof ChocoDisguiseItem)
								chance += 25;
						}
					}

					return !RandomHelper.getChanceResult(getRandom(), chance);
				}
				return false;
			});
		}
		if (healInPenGoal == null) {
			healInPenGoal = new ChocoboHealInPenGoal(this);
		}

		goalSelector.removeGoal(chocoboAvoidPlayerGoal);
		goalSelector.removeGoal(healInPenGoal);

		if (isTame()) {
			goalSelector.addGoal(4, healInPenGoal);
			goalSelector.removeGoal(chocoboAvoidPlayerGoal);
		} else {
			goalSelector.addGoal(5, chocoboAvoidPlayerGoal);
			goalSelector.removeGoal(healInPenGoal);
		}
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		if (isBaby()) {
			return super.getDimensions(pose).scale(0.5F);
		}
		return super.getDimensions(pose);
	}

	public static boolean checkChocoboSpawnRules(EntityType<? extends Chocobo> entityType, LevelAccessor levelAccessor,
												 MobSpawnType spawnType, BlockPos pos, Random randomSource) {
		if (levelAccessor.getBiome(new BlockPos(pos)).is(BiomeTags.IS_NETHER)) {
			BlockPos blockpos = pos.below();
			return spawnType == MobSpawnType.SPAWNER || levelAccessor.getBlockState(blockpos).isValidSpawn(levelAccessor, blockpos, entityType);
		}

		return levelAccessor.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && isBrightEnoughToSpawn(levelAccessor, pos);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
		if (this.isAlive() && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			if (facing == Direction.UP) {
				return inventoryHolder.cast();
			} else {
				return saddleHolder.cast();
			}
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		this.inventoryHolder.invalidate();
		this.saddleHolder.invalidate();
	}
}
