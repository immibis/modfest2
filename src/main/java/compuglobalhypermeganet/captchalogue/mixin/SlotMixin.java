package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import compuglobalhypermeganet.captchalogue.IPlayerInventoryMixin;
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
public class SlotMixin {
	/*@Inject(at = @At(value="INVOKE", shift=At.Shift.AFTER, target="Lnet/minecraft/container/Slot;getStack()Lnet/minecraft/item/ItemStack;"), method = "drawSlot(Lnet/minecraft/container/slot;)V")
	private void init(CallbackInfo info) {
		throw new RuntimeException("called getStack");
	}*/
	
	@Shadow @Final private int invSlot;
	@Shadow @Final public Inventory inventory;
	
	@Inject(at = @At(value="HEAD"), method="canTakeItems(Lnet/minecraft/entity/player/PlayerEntity;)Z", cancellable=true)
	private void blockTakeItemsNonModusSlots(CallbackInfoReturnable<Boolean> info) {
		if(inventory instanceof PlayerInventory) {
			if (!((IPlayerInventoryMixin)inventory).getFetchModus().canTakeFromSlot(invSlot)) {
				info.setReturnValue(false);
			}
		}
	}
	
	@Inject(at = @At(value="HEAD"), method="canInsert(Lnet/minecraft/item/ItemStack;Z)Z", cancellable=true)
	private void blockInsertIntoNonModusSlots(CallbackInfoReturnable<Boolean> info) {
		if(inventory instanceof PlayerInventory) {
			if (!((IPlayerInventoryMixin)inventory).getFetchModus().canInsertToSlot(invSlot)) {
				info.setReturnValue(false);
			}
		}
	}
	
	@Inject(at = @At(value="HEAD"), method="getStack()Lnet/minecraft/item/ItemStack;", cancellable=true)
	private void overrideGetStack(CallbackInfoReturnable<ItemStack> info) {
		if(inventory instanceof PlayerInventory) {
			info.setReturnValue(((IPlayerInventoryMixin)inventory).getFetchModus().getStackInSlot(invSlot));
		}
	}
	
	@Inject(at = @At(value="HEAD"), method="setStack(Lnet/minecraft/item/ItemStack;)V", cancellable=true)
	private void overrideSetStack(ItemStack stack, CallbackInfo info) {
		if(inventory instanceof PlayerInventory) {
			((IPlayerInventoryMixin)inventory).getFetchModus().setStackInSlot(invSlot, stack);
			info.cancel();
		}
	}
	
	@Inject(at = @At(value="HEAD"), method="takeStack(I)Lnet/minecraft/item/ItemStack;", cancellable=true)
	private void overrideTakeStack(int count, CallbackInfoReturnable<ItemStack> info) {
		if(inventory instanceof PlayerInventory) {
			info.setReturnValue(((IPlayerInventoryMixin)inventory).getFetchModus().takeItemsFromSlot(invSlot, count));
		}
	}
}
