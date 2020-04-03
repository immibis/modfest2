package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import compuglobalhypermeganet.CaptchalogueMod;
import compuglobalhypermeganet.captchalogue.FetchModusState;
import compuglobalhypermeganet.captchalogue.FetchModusType;
import compuglobalhypermeganet.captchalogue.InventoryUtils;
import compuglobalhypermeganet.captchalogue.InventoryWrapper;
import compuglobalhypermeganet.captchalogue.ModusRegistry;
import compuglobalhypermeganet.captchalogue.mixin_support.AfterInventoryChangedRunnable;
import compuglobalhypermeganet.captchalogue.mixin_support.IContainerMixin;
import compuglobalhypermeganet.captchalogue.mixin_support.IPlayerInventoryMixin;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin implements IPlayerInventoryMixin {
	
	@Shadow public @Final PlayerEntity player;
	@Shadow public int selectedSlot;
	
	@Unique public FetchModusState fetchModusState = FetchModusState.NULL_STATE;
	
	// TODO: For Tree modus it would be good, and probably more stable, to have the tree stored in the PlayerInventory instead of recreating it on demand.
	// This means we should create an instance of something per inventory, and tell it when setInvStack happens, and use it for insert.
	// Probably also switch the other moduses over to this system too.
	
	@Override
	public FetchModusType getFetchModus() {
		return ModusRegistry.getModus((PlayerInventory)(Object)this);
	}
	
	@Inject(at = @At("HEAD"), method="setInvStack(ILnet/minecraft/item/ItemStack;)V")
	public void notifyStackChange(int slot, ItemStack stack, CallbackInfo info) {
		if(player.container != null && slot >= 0 && slot < 36)
			((IContainerMixin)player.container).captchalogue_onPlayerInventoryStackChanging(slot, stack);
	}
	
	@Unique public long captchalogue_changedSlots;
	
	@Inject(at = @At("RETURN"), method="setInvStack(ILnet/minecraft/item/ItemStack;)V")
	public void afterStackChange(int slot, ItemStack stack, CallbackInfo info) {
		if(slot >= 0 && slot < 36) { // including modus slot
			if(captchalogue_changedSlots == 0)
				CaptchalogueMod.executeLater(player.world, new AfterInventoryChangedRunnable(this));
			captchalogue_changedSlots |= 1L << slot;
		}
	}
	
	@Override
	public void captchalogue_afterInventoryChanged() {
		if((captchalogue_changedSlots & (1L << CaptchalogueMod.MODUS_SLOT)) != 0) {
			// Fetch modus has changed!
			//fetchModusState.deinitialize();
			//fetchModusState = getFetchModus().createFetchModusState(new InventoryWrapper.PlayerInventorySkippingModusSlot((PlayerInventory)(Object)this));
			//fetchModusState.initialize();
		}
		captchalogue_changedSlots = 0; // Any inventory changes made in this function do NOT re-trigger it.
	}
	
	@Unique
	private boolean captchalogue_acceptAsModus(ItemStack stack) {
		PlayerInventory this_ = (PlayerInventory)(Object)this;
		if (this_.getInvStack(CaptchalogueMod.MODUS_SLOT).isEmpty() && ModusRegistry.isModus(stack)) {
			this_.setInvStack(CaptchalogueMod.MODUS_SLOT, stack.copy());
			stack.setCount(0);
			return true;
		}
		return false;
	}
	
	@Inject(at = @At("HEAD"), method="insertStack(ILnet/minecraft/item/ItemStack;)Z", cancellable=true)
	public void overrideInsertStack(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> info) {
		
		if (captchalogue_acceptAsModus(stack)) {
			stack.setCount(0);
			info.setReturnValue(Boolean.TRUE);
			return;
		}
		
		FetchModusType modus = getFetchModus();
		if (modus.hasCustomInsert()) {
			// This returns true if any items were successfully inserted.
			// modus.insert updates the stack's count.
			// Slot number is ignored!
			int previousCount = stack.getCount();
			modus.insert(new InventoryWrapper.PlayerInventorySkippingModusSlot((PlayerInventory)(Object)this), stack);
			info.setReturnValue(stack.getCount() < previousCount);
		}
	}
	@Inject(at = @At("HEAD"), method="offerOrDrop(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;)V", cancellable=true)
	public void overrideOfferOrDrop(World world, ItemStack stack, CallbackInfo info) {
		if (!world.isClient()) {
			
			if(stack.isEmpty())
				return;
			
			if (captchalogue_acceptAsModus(stack)) {
				stack.setCount(0);
				info.cancel();
				return;
			}
			
			FetchModusType modus = getFetchModus();
			if (modus.hasCustomInsert()) {
				modus.insert(new InventoryWrapper.PlayerInventorySkippingModusSlot((PlayerInventory)(Object)this), stack);
				if (stack.getCount() == 0) {
					info.cancel();
				}
				// Proceed to the standard implementation; this may 
			}
		}
	}
	
	@Inject(at = @At("HEAD"), method="isUsingEffectiveTool(Lnet/minecraft/block/BlockState;)Z", cancellable=true)
	public void enforceModus_isUsingEffectiveTool(CallbackInfoReturnable<Boolean> info) {
		// If the modus won't allow it, pretend we are not using the item in this slot. 
		if (!getFetchModus().canTakeFromSlot(new InventoryWrapper.PlayerInventorySkippingModusSlot((PlayerInventory)(Object)this), selectedSlot))
			info.setReturnValue(Boolean.FALSE);
	}
	
	@Inject(at = @At("HEAD"), method="getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F", cancellable=true)
	public void enforceModus_getBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> info) {
		// If the modus won't allow it, pretend we are not using the item in this slot. 
		if (!getFetchModus().canTakeFromSlot(new InventoryWrapper.PlayerInventorySkippingModusSlot((PlayerInventory)(Object)this), selectedSlot))
			info.setReturnValue(Float.valueOf(ItemStack.EMPTY.getMiningSpeed(block)));
	}
	
	
	
	public int lastSelectedSlot;
	
	@Inject(at = @At("RETURN"), method="<init>*")
	public void afterConstruct(CallbackInfo info) {
		lastSelectedSlot = -1;
	}
	
	// Last resort to detect wrong hotbar slot changes.
	@Inject(at = @At("HEAD"), method="updateItems()V")
	public void beforeUpdateItems(CallbackInfo info) {
		FetchModusType modus = getFetchModus();
		InventoryUtils.ensureSelectedSlotIsUnblocked(modus, (PlayerInventory)(Object)this, lastSelectedSlot, false);
		lastSelectedSlot = selectedSlot;
	}
	
	
	
	@Inject(at = @At("RETURN"), method="<init>(Lnet/minecraft/entity/player/PlayerEntity;)V")
	public void initDefaultModus(PlayerEntity player, CallbackInfo info) {
		if (!player.world.isClient()) {
			PlayerInventory inv = (PlayerInventory)(Object)this;
			Item modusType = CaptchalogueMod.DEFAULT_MODUSES.get(player.world.random.nextInt(CaptchalogueMod.DEFAULT_MODUSES.size()));
			if(inv.getInvStack(CaptchalogueMod.MODUS_SLOT).isEmpty()) {
				inv.setInvStack(CaptchalogueMod.MODUS_SLOT, new ItemStack(modusType));
			}
		}
	}
}
