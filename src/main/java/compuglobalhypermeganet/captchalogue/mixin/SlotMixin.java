package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.util.Pair;

import compuglobalhypermeganet.captchalogue.FetchModus;
import compuglobalhypermeganet.captchalogue.IPlayerInventoryMixin;
import compuglobalhypermeganet.captchalogue.ISlotMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Captchalogue works by blocking all player slots except for the one we can pull items from.
 * However we still render the items in those slots, by hooking ContainerScreen.drawSlot.
 * 
 * The items are stored in a new field in PlayerInventory(name?)
 */

//@Mixin(ContainerScreen.class)
@Mixin(Slot.class)
public class SlotMixin implements ISlotMixin {
	/*@Inject(at = @At(value="INVOKE", shift=At.Shift.AFTER, target="Lnet/minecraft/container/Slot;getStack()Lnet/minecraft/item/ItemStack;"), method = "drawSlot(Lnet/minecraft/container/slot;)V")
	private void init(CallbackInfo info) {
		throw new RuntimeException("called getStack");
	}*/
	
	@Shadow @Final private int invSlot;
	@Shadow @Final public Inventory inventory;
	
	@Override
	public int captchalogue_getSlotNum() {
		return invSlot;
	}
	
	@Unique
	private boolean captchalogue_isPlayerSlot() {
		return inventory instanceof PlayerInventory && invSlot < ((PlayerInventory)inventory).main.size();
	}
	
	@Inject(at = @At(value="HEAD"), method="canTakeItems(Lnet/minecraft/entity/player/PlayerEntity;)Z", cancellable=true)
	private void blockTakeItemsNonModusSlots(CallbackInfoReturnable<Boolean> info) {
		if(captchalogue_isPlayerSlot()) {
			if (invSlot == FetchModus.MODUS_SLOT)
				return;
			if (!((IPlayerInventoryMixin)inventory).getFetchModus().canTakeFromSlot((PlayerInventory)inventory, invSlot)) {
				info.setReturnValue(false);
			}
		}
	}
	
	@Inject(at = @At(value="HEAD"), method="canInsert(Lnet/minecraft/item/ItemStack;Z)Z", cancellable=true)
	private void blockInsertIntoNonModusSlots(ItemStack item, CallbackInfoReturnable<Boolean> info) {
		if(captchalogue_isPlayerSlot()) {
			if (invSlot == FetchModus.MODUS_SLOT) {
				if (!FetchModus.isModus(item))
					info.setReturnValue(false);
				return;
			}
			if (!((IPlayerInventoryMixin)inventory).getFetchModus().canInsertToSlot((PlayerInventory)inventory, invSlot)) {
				info.setReturnValue(false);
			}
		}
	}
	
	/*@Inject(at = @At(value="HEAD"), method="getStack()Lnet/minecraft/item/ItemStack;", cancellable=true)
	private void overrideGetStack(CallbackInfoReturnable<ItemStack> info) {
		if(inventory instanceof PlayerInventory) {
			info.setReturnValue(((IPlayerInventoryMixin)inventory).getFetchModus().getStackInSlot(invSlot));
		}
	}*/
	
	@Inject(at = @At(value="HEAD"), method="setStack(Lnet/minecraft/item/ItemStack;)V", cancellable=true)
	private void overrideSetStack(ItemStack stack, CallbackInfo info) {
		if (captchalogue_isPlayerSlot()) {
			if (FetchModus.isProcessingPacket.get())
				return; // When the server is sending us the inventory state, no silly business - just replicate exactly what the server says.

			if (invSlot == FetchModus.MODUS_SLOT) {
				IPlayerInventoryMixin m = (IPlayerInventoryMixin)inventory;
				FetchModus oldModus = m.getFetchModus();
				// TODO: what should happen if the player has no modus or if the wrong item is somehow inserted here? They should get a null modus.
				//if (!stack.isEmpty() && FetchModus.isModus(stack)) {
					//m.setFetchModus(FetchModus.createModus(stack));
				((PlayerInventory)inventory).main.set(FetchModus.MODUS_SLOT, stack);
				oldModus.deinitialize((PlayerInventory)inventory);
				m.getFetchModus().initialize((PlayerInventory)inventory);
				info.cancel();
				//}
				return;
			}
			if (((IPlayerInventoryMixin)inventory).getFetchModus().setStackInSlot((PlayerInventory)inventory, invSlot, stack))
				info.cancel();
		}
	}
	
	// In one case - where you click on a take-able, non-insertable slot and you are holding the same item - Minecraft will
	// directly update the item stack count without calling setStack.
	// markDirty is still called afterwards, so we hook that.
	// TODO: maybe we should wait until markDirty to move ANY items to their fetchModus locations? That might also fix dragging? But other mods don't call markDirty...
	@Inject(at = @At(value="HEAD"), method="markDirty()V")
	private void onMarkDirty(CallbackInfo info) {
		if (captchalogue_isPlayerSlot()) {
			if (invSlot == FetchModus.MODUS_SLOT)
				return;
			if (inventory.getInvStack(invSlot).isEmpty()) {
				// This call might be redundant, or it might not...
				((IPlayerInventoryMixin)inventory).getFetchModus().setStackInSlot((PlayerInventory)inventory, invSlot, ItemStack.EMPTY);
			}
		}
	}
	
	@Inject(at = @At(value="HEAD"), method="getBackgroundSprite()Lcom/mojang/datafixers/util/Pair;", cancellable=true)
	@Environment(EnvType.CLIENT)
	public void getBackgroundSprite(CallbackInfoReturnable<Pair<Identifier, Identifier>> info) {
		if (captchalogue_isPlayerSlot()) {
			if(invSlot == FetchModus.MODUS_SLOT) {
				info.setReturnValue(new Pair<Identifier, Identifier>(SpriteAtlasTexture.BLOCK_ATLAS_TEX, FetchModus.MODUS_SLOT_BG_IMAGE));
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
