package compuglobalhypermeganet.captchalogue.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import compuglobalhypermeganet.CaptchalogueMod;
import compuglobalhypermeganet.captchalogue.FetchModusType;
import compuglobalhypermeganet.captchalogue.FetchModusGuiState;
import compuglobalhypermeganet.captchalogue.InventoryWrapper;
import compuglobalhypermeganet.captchalogue.ModusRegistry;
import compuglobalhypermeganet.captchalogue.mixin_support.IContainerMixin;
import compuglobalhypermeganet.captchalogue.mixin_support.IPlayerInventoryMixin;
import compuglobalhypermeganet.captchalogue.mixin_support.ISlotMixin;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

@Mixin(Container.class)
public abstract class ContainerMixin implements IContainerMixin {
	
	@Shadow public @Final List<Slot> slots;
	
	@Unique private boolean recursive;
	@Unique public long changedSlots;
	
	@Shadow
	protected abstract boolean insertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast);
	
	@Inject(at = @At("HEAD"), method = "insertItem(Lnet/minecraft/item/ItemStack;IIZ)Z", cancellable=true)
	public void overrideInsertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast, CallbackInfoReturnable<Boolean> info) {
		if (recursive)
			return; // allow recursive calls without changing behaviour
		
		int slotIncrement = (fromLast ? -1 : 1);
		if (fromLast) {
			int temp = startIndex;
			startIndex = endIndex - 1;
			endIndex = temp - 1;
		}
		for(int k = startIndex; k != endIndex; k += slotIncrement) {
			Slot slot = slots.get(k);
			if(slot.inventory instanceof PlayerInventory) {
				FetchModusType modus = ((IPlayerInventoryMixin)slot.inventory).getFetchModus();
				if(!modus.hasCustomInsert())
					return; // If the player's fetch modus doesn't have a custom insert function, then don't override anything
				PlayerInventory plinv = (PlayerInventory)slot.inventory;
				int originalCount = stack.getCount();
				modus.insert(new InventoryWrapper.PlayerInventorySkippingModusSlot(plinv), stack);
				
				// Try all remaining slots other than the player's inventory.
				// This assumes only one player inventory is involved. Might not be the case in rare cases, e.g. Chicken Chests.
				// TODO: this code can probably be improved. We should pass consecutive slot ranges to recursive insertItem!
				for (; k != endIndex && !stack.isEmpty(); k += slotIncrement) {
					slot = slots.get(k);
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
		
		if(slotId < 0 || slotId >= slots.size()) {
			return;
		}
		if(actionType == SlotActionType.CLONE)
			return; // don't override this since it doesn't change the inventory.
		
		Slot slot = slots.get(slotId);
		if(!(slot.inventory instanceof PlayerInventory))
			return;
		PlayerInventory inv = (PlayerInventory)slot.inventory;
		int slotIndex = ((ISlotMixin)slot).captchalogue_getSlotNum();
		if(slotIndex < 0 || slotIndex >= 36 || slotIndex == CaptchalogueMod.MODUS_SLOT)
			return;
		
		InventoryWrapper.PlayerInventorySkippingModusSlot wrapper = new InventoryWrapper.PlayerInventorySkippingModusSlot(inv);
		
		FetchModusType modus = ((IPlayerInventoryMixin)inv).getFetchModus();
		if (modus.overrideInventoryClick(this_, inv, wrapper, wrapper.fromUnderlyingSlotIndex(slotIndex), actionType, clickData)) {
			// Return value ItemStack is the one that gets included in the ClickWindowC2SPacket.
			// Its only use is to detect desyncs. If they don't match on the client and server (damage value ignored)
			// then the server re-sends the entire inventory contents to the client.
			// TODO: use this feature
			info.setReturnValue(ItemStack.EMPTY);
			
			changedSlots |= -1L; // don't know which slot changed. TODO: make it a boolean
		}
	}
	
	@Unique
	private PlayerInventory captchalogue_findPlayerInventory(int slotIdHint) {
		if(slotIdHint >= 0 && slotIdHint < slots.size()) {
			Slot slot = slots.get(slotIdHint);
			if (slot.inventory instanceof PlayerInventory)
				return (PlayerInventory)slot.inventory;
		}
		for(Slot slot : slots)
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

			FetchModusType modus = ((IPlayerInventoryMixin)inv).getFetchModus();
			modus.afterPossibleInventoryChange((Container)(Object)this, new InventoryWrapper.PlayerInventorySkippingModusSlot(inv));
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
		
		if(slotId < 0 || slotId >= slots.size())
			return;
		
		Slot slot = slots.get(slotId);
		if(!(slot.inventory instanceof PlayerInventory)) {
			// Don't check for PICKUP_ALL here, because onSlotClick has already called sendContentUpdates
			/*if (actionType == SlotActionType.PICKUP_ALL) {
				// PICKUP_ALL in *other* slots can affect our slots... but we need to find the player inventory first by looking at other slots
				// TODO: instead of detecting click types, maybe it's better to just have a playerInventoryChanged flag set in setInvStack? (that wouldn't catch increment/decrement though)
				for(Slot slot2 : slots) {
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
		FetchModusType modus = ((IPlayerInventoryMixin)inv).getFetchModus();

		if(slotIndex < 0 || slotIndex >= 36 || slotIndex == CaptchalogueMod.MODUS_SLOT) {
			/*
			// PICKUP_ALL in *other* slots can affect our slots. But we have a special hook for that.
			if (actionType == SlotActionType.PICKUP_ALL) {
				modus.afterInventoryPickupAll(this_, inv);
			}
			*/
		} else {
			InventoryWrapper.PlayerInventorySkippingModusSlot wrapper = new InventoryWrapper.PlayerInventorySkippingModusSlot(inv);
			int internalSlotIndex = wrapper.fromUnderlyingSlotIndex(slotIndex);
			modus.afterInventoryClick(this_, inv, wrapper, internalSlotIndex, actionType, clickData);
			
			switch(actionType) {
			case PICKUP:
			case QUICK_MOVE:
			case THROW:
			case QUICK_CRAFT:
				changedSlots |= 1L << internalSlotIndex;
				break;
			case SWAP:
				changedSlots |= -1L; // any slot could change? (actually it's the hover slot + the specified hotbar slot, but we are lazy)
				break;
			case CLONE:
				break; // no changed slots
			case PICKUP_ALL:
				throw new AssertionError("unreachable; PICKUP_ALL ignored at start of function");
			}
		}
	}
	
	@Inject(at = @At("HEAD"), method="close(Lnet/minecraft/entity/player/PlayerEntity;)V")
	public void onClose(PlayerEntity player, CallbackInfo info) {
		// This is mostly relevant for the player's own inventory container, which isn't recreated each time they open it.
		// Memory modus needs this to get a new random seed every time the inventory is opened.
		FetchModusType modus = ((IPlayerInventoryMixin)player.inventory).getFetchModus();
		if(modus.resetStateWhenInventoryClosed()) {
			fetchModusState = null;
			stateForModus = null;
		}
	}
	
	private FetchModusGuiState fetchModusState;
	private FetchModusType stateForModus;
	
	@Unique
	private Slot findPlayerInventorySlot(int slot) {
		for(Slot s : slots) {
			if(((ISlotMixin)s).captchalogue_getSlotNum() == slot && s.inventory instanceof PlayerInventory)
				return s;
		}
		return null;
	}
	
	@Unique
	private void captchalogue_resetSlotPositions() {
		for(Slot s : slots) {
			ISlotMixin m = (ISlotMixin)s;
			m.captchalogue_setPosition(m.captchalogue_getOriginalXPosition(), m.captchalogue_getOriginalYPosition());
		}
	}
	
	@Override
	public FetchModusGuiState getFetchModusGuiState() {
		
		Slot modusSlot = findPlayerInventorySlot(CaptchalogueMod.MODUS_SLOT);
		FetchModusType modus = null;
		if(modusSlot != null)
			modus = ModusRegistry.getModus(modusSlot.getStack());
		
		if (stateForModus != modus) {
			fetchModusState = null;
			stateForModus = modus;
			captchalogue_resetSlotPositions();
		}
		if (fetchModusState == null && modus != null) {
			fetchModusState = modus.createGuiState((Container)(Object)this, (PlayerInventory)modusSlot.inventory);
			if (fetchModusState == null)
				fetchModusState = FetchModusGuiState.NULL_GUI_STATE;
		}
		return fetchModusState;
	}
	
	@Inject(at=@At("RETURN"), method="addSlot(Lnet/minecraft/container/Slot;)Lnet/minecraft/container/Slot;")
	public void afterAddSlot(Slot slot, CallbackInfoReturnable<Slot> info) {
		((ISlotMixin)slot).captchalogue_setContainer((Container)(Object)this);
	}
	
	@Override
	public void captchalogue_onSlotStackChanging(Slot slot, ItemStack stack) {
		if(!(slot.inventory instanceof PlayerInventory))
			return; // we only care about player inventory slots
		int slotIndex = ((ISlotMixin)slot).captchalogue_getSlotNum();
		if(slotIndex < 0 || slotIndex > 35)
			return; // don't care about armour, offhand, etc
		
		if(slotIndex == CaptchalogueMod.MODUS_SLOT) {
			// TODO: notify modus change?
			changedSlots |= -1;
			return;
		}
		
		if(slotIndex >= CaptchalogueMod.MODUS_SLOT)
			slotIndex--;
		
		changedSlots |= 1L << slotIndex;
	}
	
	/*@Shadow
	public void addListener(ContainerListener listener) {}
	*/
	
	/*@Inject(at=@At("RETURN"), method="<init>*")
	public void onAfterInit(CallbackInfo info) {
		//addListener(new )
	}*/
	
	/*@Override
	public int captchalogue_getNextChangedSlot() {
		int nz = Long.numberOfTrailingZeros(changedSlots);
		if(nz > 35)
			return -1;
		changedSlots ^= ~(1L << nz);
		return nz;
	}*/
	// TODO: make changedSlots a boolean if we don't need the above.
	
	@Override
	public boolean captchalogue_haveChanges() {
		if(changedSlots != 0) {
			changedSlots = 0;
			return true;
		}
		return false;
	}
	
	@Override
	public void captchalogue_onPlayerInventoryStackChanging(int slot, ItemStack stack) {
		if(slot >= 0 && slot < 36 && slot != CaptchalogueMod.MODUS_SLOT) {
			if(slot >= CaptchalogueMod.MODUS_SLOT)
				slot--;
			changedSlots |= 1L << slot;
		}
		if(slot == CaptchalogueMod.MODUS_SLOT)
			changedSlots |= -1;
	}
}
