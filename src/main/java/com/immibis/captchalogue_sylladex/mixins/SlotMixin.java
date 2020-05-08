package com.immibis.captchalogue_sylladex.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.immibis.captchalogue_sylladex.CaptchalogueMod;
import com.immibis.captchalogue_sylladex.InventoryWrapper;
import com.immibis.captchalogue_sylladex.ModusRegistry;
import com.immibis.captchalogue_sylladex.mixin_support.ICreativeSlotMixin;
import com.immibis.captchalogue_sylladex.mixin_support.IPlayerInventoryMixin;
import com.immibis.captchalogue_sylladex.mixin_support.ISlotMixin;

import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * Captchalogue works by blocking all player slots except for the one we can pull items from.
 * However we still render the items in those slots, by hooking ContainerScreen.drawSlot.
 * 
 * The items are stored in a new field in PlayerInventory(name?)
 */

//@Mixin(ContainerScreen.class)
@Mixin(Slot.class)
public class SlotMixin implements ISlotMixin {
	@Shadow @Final private int invSlot; // don't use directly. use captchalogue_getSlotNum. see comment on captchalogue_getSlotNum.
	@Shadow @Final public Inventory inventory;
	
	@Unique private int originalX;
	@Unique private int originalY;
	
	@Shadow @Mutable public int xPosition;
	@Shadow @Mutable public int yPosition;
	
	@Shadow public ItemStack getStack() {return null;}
	
	@Override public void captchalogue_setPosition(int x, int y) {xPosition = x; yPosition = y;}
	@Override public int captchalogue_getOriginalXPosition() {return originalX;}
	@Override public int captchalogue_getOriginalYPosition() {return originalY;}
	
	@Unique private Container container;
	
	@Override public Container captchalogue_getContainer() {return container;}
	@Override public void captchalogue_setContainer(Container cont) {this.container = cont;}
	
	@Inject(at=@At("RETURN"), method="<init>*")
	public void afterInit(CallbackInfo info) {
		originalX = xPosition;
		originalY = yPosition;
	}
	
	@Override
	public int captchalogue_getSlotNum() {
		// CreativeInventoryScreen.CreativeSlot uses proxy slots. They proxy to the correct slot, but the invSlot field doesn't match the base slot.
		// So we have to find out what the base slot's real index is.
		if(this instanceof ICreativeSlotMixin)
			return ((ISlotMixin)((ICreativeSlotMixin)this).captchalogue_getBaseSlot()).captchalogue_getSlotNum();
		return invSlot;
	}
	
	@Unique
	private boolean captchalogue_isPlayerSlot() {
		return inventory instanceof PlayerInventory && captchalogue_getSlotNum() < ((PlayerInventory)inventory).main.size();
	}
	
	@Inject(at = @At(value="HEAD"), method="canTakeItems(Lnet/minecraft/entity/player/PlayerEntity;)Z", cancellable=true)
	private void blockTakeItemsNonModusSlots(CallbackInfoReturnable<Boolean> info) {
		if(inventory instanceof PlayerInventory) {
			
			// CreativeInventoryScreen won't allow you to click on a slot unless canTakeItems returns true.
			// This prevents the player from inserting anything into a queue or stack modus on the creative inventory screen.
			// Workaround: return true (default) from canTakeItems on creative inventory slots.
			if(this instanceof ICreativeSlotMixin && this.getStack().isEmpty()) {
				return;
			}
			
			int invSlot = captchalogue_getSlotNum();
			if (invSlot == CaptchalogueMod.MODUS_SLOT || invSlot < 0 || invSlot > 35)
				return;
			if (!((IPlayerInventoryMixin)inventory).getFetchModus().canTakeFromSlot(InventoryWrapper.PlayerInventorySkippingModusSlot.fromUnderlyingSlotIndex(invSlot))) {
				info.setReturnValue(false);
			}
		}
	}
	
	@Inject(at = @At(value="HEAD"), method="canInsert(Lnet/minecraft/item/ItemStack;)Z", cancellable=true)
	private void blockInsertIntoNonModusSlots(ItemStack item, CallbackInfoReturnable<Boolean> info) {
		if(inventory instanceof PlayerInventory) {
			int invSlot = captchalogue_getSlotNum();
			if (invSlot == CaptchalogueMod.MODUS_SLOT) {
				if (!ModusRegistry.isModus(item))
					info.setReturnValue(false);
				return;
			}
			if (invSlot < 0 || invSlot > 35)
				return;
			if (!((IPlayerInventoryMixin)inventory).getFetchModus().canInsertToSlot(InventoryWrapper.PlayerInventorySkippingModusSlot.fromUnderlyingSlotIndex(invSlot))) {
				info.setReturnValue(false);
			}
		}
	}
	
	// Commented: This doesn't work, because Container doesn't check whether takeStack returned the requested number of items.
	/*
	// For certain fetch modii, right-click only takes one item. Otherwise it would be impossible to not grab too many items at once.
	@Inject(at = @At(value="HEAD"), method="takeStack(I)Lnet/minecraft/item/ItemStack;", cancellable=true)
	private void overrideTakeStack(int count, CallbackInfoReturnable<ItemStack> info) {
		if(isPlayerSlot()) {
			FetchModus modus = ((IPlayerInventoryMixin)inventory).getFetchModus();
			if (modus.forceRightClickOneItem()) {
				// assume the slot is takeable-from, since the caller already checks for that
				info.setReturnValue(inventory.takeInvStack(invSlot, 1));
			}
		}
	}
	*/
}
