package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import compuglobalhypermeganet.CaptchalogueMod;
import compuglobalhypermeganet.captchalogue.FetchModus;
import compuglobalhypermeganet.captchalogue.IPlayerInventoryMixin;
import compuglobalhypermeganet.captchalogue.InventoryUtils;
import compuglobalhypermeganet.captchalogue.InventoryWrapper;
import compuglobalhypermeganet.captchalogue.ModusRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin implements IPlayerInventoryMixin {
	
	@Shadow
	public int selectedSlot;
	
	@Override
	public FetchModus getFetchModus() {
		return ModusRegistry.getModus((PlayerInventory)(Object)this);
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
		
		FetchModus modus = getFetchModus();
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
			
			FetchModus modus = getFetchModus();
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
		FetchModus modus = getFetchModus();
		InventoryUtils.ensureSelectedSlotIsUnblocked(modus, (PlayerInventory)(Object)this, lastSelectedSlot, false);
		lastSelectedSlot = selectedSlot;
	}
}
