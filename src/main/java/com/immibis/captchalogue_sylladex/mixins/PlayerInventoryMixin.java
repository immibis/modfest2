package com.immibis.captchalogue_sylladex.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.immibis.captchalogue_sylladex.CaptchalogueMod;
import com.immibis.captchalogue_sylladex.FetchModusState;
import com.immibis.captchalogue_sylladex.FetchModusType;
import com.immibis.captchalogue_sylladex.InventoryUtils;
import com.immibis.captchalogue_sylladex.InventoryWrapper;
import com.immibis.captchalogue_sylladex.ModusRegistry;
import com.immibis.captchalogue_sylladex.mixin_support.AfterInventoryChangedRunnable;
import com.immibis.captchalogue_sylladex.mixin_support.IPlayerInventoryMixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin implements IPlayerInventoryMixin {
	
	@Shadow public @Final PlayerEntity player;
	@Shadow public int selectedSlot;
	
	@Unique public FetchModusState fetchModus;
	
	@Override
	public FetchModusState getFetchModus() {
		if(fetchModus == null)
			throw new IllegalStateException("object still being constructed - shouldn't get here");
		return fetchModus;
	}
	
	@Unique public long captchalogue_changedSlots;
	private static final long SERVER_CHANGED_MODUS_SLOT_FLAG = 0x8000000000000000L;
	private static final long SERVER_CHANGED_SLOTS_FLAG = 0x4000000000000000L;
	
	@Unique
	private void captchalogue_afterStackChange(int slot) {
		if(slot >= 0 && slot < 36) { // including modus slot

			// all cases set some flag which should lead to captchalogue_afterInventoryChanged being called.
			if(captchalogue_changedSlots == 0)
				CaptchalogueMod.executeLater(player.world, new AfterInventoryChangedRunnable(this));

			// Sync events from the server (or client in creative mode) don't trigger update events as this could cause a desync from the server.
			if(!FetchModusType.isProcessingPacket.get().booleanValue()) {
				captchalogue_changedSlots |= 1L << slot;
			
			} else {
				if (slot == CaptchalogueMod.MODUS_SLOT) {
					// but if the server tells us we have a different modus, then we still need to have a valid modus object
					captchalogue_changedSlots |= SERVER_CHANGED_MODUS_SLOT_FLAG;
				} else {
					// We still need to notify the modus to rebuild any caches; e.g. tree modus might need to update tree layout.
					captchalogue_changedSlots |= SERVER_CHANGED_SLOTS_FLAG;
				}
			}
		}
	}
	
	@Inject(at = @At("RETURN"), method="setInvStack(ILnet/minecraft/item/ItemStack;)V")
	public void afterSetStack(int slot, ItemStack stack, CallbackInfo info) {
		captchalogue_afterStackChange(slot);
	}
	
	@Inject(at = @At("RETURN"), method="takeInvStack(II)Lnet/minecraft/item/ItemStack;")
	public void afterTakeStack(int slot, int amount, CallbackInfoReturnable<ItemStack> info) {
		// when you press Q to drop an item, it calls takeInvStack, and doesn't call setInvStack
		captchalogue_afterStackChange(slot);
	}
		
	
	@Override
	public void captchalogue_afterInventoryChanged() {
		if ((captchalogue_changedSlots & SERVER_CHANGED_MODUS_SLOT_FLAG) != 0) {
			fetchModus = ModusRegistry.getModus((PlayerInventory)(Object)this).createFetchModusState(new InventoryWrapper.PlayerInventorySkippingModusSlot((PlayerInventory)(Object)this));
			captchalogue_changedSlots |= SERVER_CHANGED_SLOTS_FLAG;
		}
		
		if ((captchalogue_changedSlots & SERVER_CHANGED_SLOTS_FLAG) != 0) {
			// TODO: track which slots were changed by the server.
			fetchModus.afterPossibleInventoryChange(-1L, true);
			captchalogue_changedSlots &= ~SERVER_CHANGED_SLOTS_FLAG;
		}
		
		if (captchalogue_changedSlots != 0) {
			if((captchalogue_changedSlots & (1L << CaptchalogueMod.MODUS_SLOT)) != 0) {
				// Fetch modus has changed!
				if(fetchModus != null) // should only be null during construction
					fetchModus.deinitialize();
				fetchModus = ModusRegistry.getModus((PlayerInventory)(Object)this).createFetchModusState(new InventoryWrapper.PlayerInventorySkippingModusSlot((PlayerInventory)(Object)this));
				fetchModus.initialize();
			} else {
				if(fetchModus != null) { // should never be null
					long changedSlots = captchalogue_changedSlots;
					long belowModusMask = (1L << CaptchalogueMod.MODUS_SLOT) - 1;
					long aboveModusMask = (~belowModusMask) << 1;
					changedSlots = (changedSlots & belowModusMask) | ((changedSlots & aboveModusMask) >> 1);
					fetchModus.afterPossibleInventoryChange(changedSlots, false);
				}
			}
			captchalogue_changedSlots = 0; // Any inventory changes made within this function do NOT re-trigger it.
		}
	}
	
	@Override
	public boolean captchalogue_acceptAsModus(ItemStack stack) {
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
		
		FetchModusState modus = getFetchModus();
		if (modus.hasCustomInsert()) {
			// This returns true if any items were successfully inserted.
			// modus.insert updates the stack's count.
			// Slot number is ignored!
			int previousCount = stack.getCount();
			modus.insert(stack, true);
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
			
			FetchModusState modus = getFetchModus();
			if (modus.hasCustomInsert()) {
				modus.insert(stack, true);
				if (stack.getCount() == 0) {
					info.cancel();
				}
				// Proceed to the standard implementation; this will drop excess items that don't fit (it won't CaptchalogueMod.launchExcessItems them)
				// Custom insert() may call CaptchalogueMod.launchExcessItems in which case the stack is now empty.
			}
		}
	}
	
	/*
	// TODO: redundant now that we can't select blocked slots?
	@Inject(at = @At("HEAD"), method="isUsingEffectiveTool(Lnet/minecraft/block/BlockState;)Z", cancellable=true)
	public void enforceModus_isUsingEffectiveTool(CallbackInfoReturnable<Boolean> info) {
		// If the modus won't allow it, pretend we are not using the item in this slot.
		// TODO: if modus slot was any other hotbar slot, we need to remap the slot number here
		if (selectedSlot != CaptchalogueMod.MODUS_SLOT && fetchModus != null && !fetchModus.canTakeFromSlot(selectedSlot))
			info.setReturnValue(Boolean.FALSE);
	}
	
	// TODO: redundant now that we can't select blocked slots?
	@Inject(at = @At("HEAD"), method="getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F", cancellable=true)
	public void enforceModus_getBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> info) {
		// If the modus won't allow it, pretend we are not using the item in this slot.
		// TODO: if modus slot was any other hotbar slot, we need to remap the slot number here
		if (selectedSlot != CaptchalogueMod.MODUS_SLOT && fetchModus != null && !fetchModus.canTakeFromSlot(selectedSlot))
			info.setReturnValue(Float.valueOf(ItemStack.EMPTY.getMiningSpeed(block)));
	}
	*/
	
	
	
	public int lastSelectedSlot;
	
	@Inject(at = @At("RETURN"), method="<init>*")
	public void afterConstruct(CallbackInfo info) {
		lastSelectedSlot = -1;
	}
	
	// Last resort to detect wrong hotbar slot changes.
	@Inject(at = @At("HEAD"), method="updateItems()V")
	public void beforeUpdateItems(CallbackInfo info) {
		FetchModusState modus = getFetchModus();
		InventoryUtils.ensureSelectedSlotIsUnblocked(modus, (PlayerInventory)(Object)this, lastSelectedSlot, false);
		lastSelectedSlot = selectedSlot;
	}
	
	
	
	@Inject(at = @At("RETURN"), method="<init>(Lnet/minecraft/entity/player/PlayerEntity;)V")
	public void initDefaultModus(PlayerEntity player, CallbackInfo info) {
		fetchModus = ModusRegistry.NULL.createFetchModusState(new InventoryWrapper.PlayerInventorySkippingModusSlot((PlayerInventory)(Object)this));
		if (!player.world.isClient()) {
			PlayerInventory inv = (PlayerInventory)(Object)this;
			Item modusType = CaptchalogueMod.DEFAULT_MODUSES.get(player.world.random.nextInt(CaptchalogueMod.DEFAULT_MODUSES.size()));
			
			if(player.world.getServer().getDefaultGameMode().isCreative())
				modusType = CaptchalogueMod.itemArrayFetchModus;
			
			// another mod might have added something to MODUS_SLOT - move it out of the way.
			ItemStack movedStack = inv.getInvStack(CaptchalogueMod.MODUS_SLOT);
			inv.setInvStack(CaptchalogueMod.MODUS_SLOT, new ItemStack(modusType));
			if (!movedStack.isEmpty()) {
				for(int k = 0; k < 36; k++) {
					if(inv.getInvStack(k).isEmpty()) {
						inv.setInvStack(k, movedStack);
						break;
					}
				}
			}
		}
	}
	
	@Inject(at = @At("RETURN"), method="deserialize(Lnet/minecraft/nbt/ListTag;)V")
	public void afterDeserialize(CallbackInfo info) {
		// don't deinitialize previous modus; inventory contents were overwritten
		fetchModus = ModusRegistry.getModus((PlayerInventory)(Object)this).createFetchModusState(new InventoryWrapper.PlayerInventorySkippingModusSlot((PlayerInventory)(Object)this));
		fetchModus.initialize();
	}
}
