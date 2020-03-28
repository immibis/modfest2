package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import compuglobalhypermeganet.captchalogue.FetchModus;
import compuglobalhypermeganet.captchalogue.IPlayerInventoryMixin;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

@Mixin(Container.class)
public abstract class ContainerMixin {
	
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
}
