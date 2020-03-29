package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
