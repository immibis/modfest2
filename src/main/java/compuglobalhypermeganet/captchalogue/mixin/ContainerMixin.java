package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import compuglobalhypermeganet.captchalogue.FetchModus;
import compuglobalhypermeganet.captchalogue.IContainerMixin;
import compuglobalhypermeganet.captchalogue.IPlayerInventoryMixin;
import compuglobalhypermeganet.captchalogue.ISlotMixin;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

@Mixin(Container.class)
public abstract class ContainerMixin implements IContainerMixin {
	
	private boolean recursive;
	
	@Shadow
	protected abstract boolean insertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast);
	
	@Inject(at = @At("HEAD"), method = "insertItem(Lnet/minecraft/item/ItemStack;IIZ)Z", cancellable=true)
	public void overrideInsertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast, CallbackInfoReturnable<Boolean> info) {
		if (recursive)
			return; // allow recursive calls without changing behaviour
		
		Container this_ = (Container)(Object)this;
		int slotIncrement = (fromLast ? -1 : 1);
		if (fromLast) {
			int temp = startIndex;
			startIndex = endIndex - 1;
			endIndex = temp - 1;
		}
		for(int k = startIndex; k != endIndex; k += slotIncrement) {
			Slot slot = this_.slots.get(k);
			if(slot.inventory instanceof PlayerInventory) {
				FetchModus modus = ((IPlayerInventoryMixin)slot.inventory).getFetchModus();
				if(!modus.hasCustomInsert())
					return; // If the player's fetch modus doesn't have a custom insert function, then don't override anything
				PlayerInventory plinv = (PlayerInventory)slot.inventory;
				int originalCount = stack.getCount();
				modus.insert(plinv, stack);
				
				// Try all remaining slots other than the player's inventory.
				// This assumes only one player inventory is involved. Might not be the case in rare cases, e.g. Chicken Chests.
				for (; k != endIndex && !stack.isEmpty(); k += slotIncrement) {
					slot = this_.slots.get(k);
					if (slot.inventory == plinv)
						continue;
					try {
						recursive = true;
						insertItem(stack, k, k+1, false);
					} finally {
						recursive = false;
					}
				}
				
				info.setReturnValue(stack.getCount() < originalCount);
				return;
			}
		}
		// If no player inventories are involved, don't override anything
	}
	
	@Inject(at = @At("HEAD"), method = "onSlotClick(IILnet/minecraft/contanier/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/item/ItemStack;", cancellable=true)
	public void overrideSlotClick(int slotId, int clickData, SlotActionType actionType, PlayerEntity playerEntity, CallbackInfoReturnable<ItemStack> info) {
		Container this_ = (Container)(Object)this;
		
		if(slotId < 0 || slotId >= this_.slots.size())
			return;
		
		Slot slot = this_.slots.get(slotId);
		if(!(slot.inventory instanceof PlayerInventory))
			return;
		PlayerInventory inv = (PlayerInventory)slot.inventory;
		int slotIndex = ((ISlotMixin)slot).captchalogue_getSlotNum();
		if(slotIndex < 0 || slotIndex >= 36 || slotIndex == FetchModus.MODUS_SLOT)
			return;
		
		FetchModus modus = ((IPlayerInventoryMixin)inv).getFetchModus();
		if (modus.overrideInventoryClick(this_, inv, slotIndex, actionType, clickData)) {
			// Return value ItemStack is the one that gets included in the ClickWindowC2SPacket.
			// Its only use is to detect desyncs. If they don't match on the client and server (damage value ignored)
			// then the server re-sends the entire inventory contents to the client.
			// TODO: use this feature
			info.setReturnValue(ItemStack.EMPTY);
		}
	}
	
	@Unique
	private PlayerInventory captchalogue_findPlayerInventory(int slotIdHint) {
		Container this_ = (Container)(Object)this;
		if(slotIdHint >= 0 && slotIdHint < this_.slots.size()) {
			Slot slot = this_.slots.get(slotIdHint);
			if (slot.inventory instanceof PlayerInventory)
				return (PlayerInventory)slot.inventory;
		}
		for(Slot slot : this_.slots)
			if (slot.inventory instanceof PlayerInventory)
				return (PlayerInventory)slot.inventory;
		return null;
	}
	
	@Inject(at = @At(value="INVOKE", target="Lnet/minecraft/container/Container;sendContentUpdates()V"), method = "onSlotClick(IILnet/minecraft/contanier/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/item/ItemStack;")
	public void beforeSendContentUpdateInSlotClick(int slotId, int clickData, SlotActionType actionType, PlayerEntity playerEntity, CallbackInfoReturnable<ItemStack> info) {
		if (actionType == SlotActionType.PICKUP_ALL) {

			// PICKUP_ALL in *other* slots can affect our slots... but we need to find the player inventory first

			PlayerInventory inv = captchalogue_findPlayerInventory(slotId);
			if(inv == null)
				return; // can't find player inventory, so can't execute hook

			FetchModus modus = ((IPlayerInventoryMixin)inv).getFetchModus();
			modus.afterPossibleInventoryChange((Container)(Object)this, inv);
		}
	}
	
	@Inject(at = @At("RETURN"), method = "onSlotClick(IILnet/minecraft/contanier/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/item/ItemStack;")
	public void afterSlotClick(int slotId, int clickData, SlotActionType actionType, PlayerEntity playerEntity, CallbackInfoReturnable<ItemStack> info) {
		
		// not sure whether this gets called if the click is overridden by us. probably not.
		
		if (actionType == SlotActionType.PICKUP_ALL) {
			// In the case of PICKUP_ALL, onSlotClick has already called sendContentUpdates.
			// afterInventoryPickupAll must called before sendContentUpdates so we have a special hook for it.
			return;
		}
		
		Container this_ = (Container)(Object)this;
		
		if(slotId < 0 || slotId >= this_.slots.size())
			return;
		
		Slot slot = this_.slots.get(slotId);
		if(!(slot.inventory instanceof PlayerInventory)) {
			// Don't check for PICKUP_ALL here, because onSlotClick has already called sendContentUpdates
			/*if (actionType == SlotActionType.PICKUP_ALL) {
				// PICKUP_ALL in *other* slots can affect our slots... but we need to find the player inventory first by looking at other slots
				// TODO: instead of detecting click types, maybe it's better to just have a playerInventoryChanged flag set in setInvStack? (that wouldn't catch increment/decrement though)
				for(Slot slot2 : this_.slots) {
					if(slot2.inventory instanceof PlayerInventory) {
						FetchModus modus = ((IPlayerInventoryMixin)slot2.inventory).getFetchModus();
						modus.afterInventoryPickupAll(this_, (PlayerInventory)slot2.inventory);
						return;
					}
				}
			}*/
			return;
		}
		PlayerInventory inv = (PlayerInventory)slot.inventory;
		int slotIndex = ((ISlotMixin)slot).captchalogue_getSlotNum();
		FetchModus modus = ((IPlayerInventoryMixin)inv).getFetchModus();

		if(slotIndex < 0 || slotIndex >= 36 || slotIndex == FetchModus.MODUS_SLOT) {
			/*
			// PICKUP_ALL in *other* slots can affect our slots. But we have a special hook for that.
			if (actionType == SlotActionType.PICKUP_ALL) {
				modus.afterInventoryPickupAll(this_, inv);
			}
			*/
		} else {
			modus.afterInventoryClick(this_, inv, slotIndex, actionType, clickData);
		}
	}
	
	@Inject(at = @At("HEAD"), method="close(Lnet/minecraft/entity/player/PlayerEntity;)V")
	public void onClose(PlayerEntity player, CallbackInfo info) {
		// This is mostly relevant for the player's own inventory container, which isn't recreated each time they open it.
		// Memory modus needs this to get a new random seed every time the inventory is opened.
		FetchModus modus = ((IPlayerInventoryMixin)player.inventory).getFetchModus();
		if(modus.resetStateWhenInventoryClosed()) {
			fetchModusState = null;
			stateForModus = null;
		}
	}
	
	private Object fetchModusState;
	private FetchModus stateForModus;
	
	@Override
	public Object getFetchModusState(FetchModus modus, PlayerInventory inv) {
		if (stateForModus != modus) {
			fetchModusState = null;
			stateForModus = modus;
		}
		if (fetchModusState == null)
			fetchModusState = modus.createContainerState((Container)(Object)this, inv);
		return fetchModusState;
	}
}
