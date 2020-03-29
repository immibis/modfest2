package compuglobalhypermeganet.captchalogue;

import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class InventoryUtils {
	// Like PlayerInventory.insertStack, but doesn't delete excess items in
	// creative mode, and takes a start and end slot range.
	// Inserted items are removed from the stack.
	// Also the main hand and offhand slots don't get preferential treatment.
	// Also we don't add crash report data.
	public static void insertStack(PlayerInventory inv, ItemStack stack, int slotStart, int slotEnd) {
		if (!stack.isEmpty()) {
			if (stack.isDamaged()) {
				for(int k = slotStart; k < slotEnd; k++) {
					if(inv.getInvStack(k) == null) {
						inv.setInvStack(k, stack.copy());
						inv.getInvStack(k).setCooldown(5);
						stack.setCount(0);
						inv.markDirty();
						break;
					}
				}
			} else {
				for(int k = slotStart; k < slotEnd && stack.getCount() > 0; k++) {
					ItemStack stackInSlot = inv.getInvStack(k);
					if (stackInSlot.isEmpty()) {
						inv.setInvStack(k, stack.copy());
						stack.setCount(0);
						break;
					}
					if (Container.canStacksCombine(stackInSlot, stack)) {
						int transfer = Math.min(stackInSlot.getMaxCount() - stackInSlot.getCount(), stack.getCount());
						if (transfer > 0) {
							stackInSlot.increment(transfer);
							stack.decrement(transfer);
							inv.markDirty(); // needed?
						}
					}
				}
			}
		}
	}

	public static void ensureSelectedSlotIsUnblocked(FetchModus modus, PlayerInventory inv, int lastSelectedSlot, boolean isScroll) {
		if (modus.blocksAccessToHotbarSlot(inv.selectedSlot)) {
			if (isScroll) {
				if (inv.selectedSlot == (lastSelectedSlot + 8) % 9) {
					// probably scrolled leftwards
					inv.selectedSlot = lastSelectedSlot;
					for(int k = 0; k < 9; k++) {
						inv.selectedSlot = (inv.selectedSlot + 8) % 9;
						if (!modus.blocksAccessToHotbarSlot(inv.selectedSlot))
							return;
					}
					// else slot is left unchanged
					return;
				}
				if (inv.selectedSlot == (lastSelectedSlot + 1) % 9) {
					// probably scrolled rightwards
					inv.selectedSlot = lastSelectedSlot;
					for(int k = 0; k < 9; k++) {
						inv.selectedSlot = (inv.selectedSlot + 1) % 9;
						if (!modus.blocksAccessToHotbarSlot(inv.selectedSlot))
							break;
					}
					// else slot is left unchanged
					return;
				}
			}
			// jumped to a random slot. undo it.
			if (lastSelectedSlot < 0 || lastSelectedSlot > 8)
				lastSelectedSlot = 0; // default slot
			
			if (modus.blocksAccessToHotbarSlot(lastSelectedSlot)) {
				lastSelectedSlot = FetchModus.MODUS_SLOT; // last resort slot (even though it's normally blocked)
				for(int k = 0; k < 9; k++) {
					if (!modus.blocksAccessToHotbarSlot(lastSelectedSlot)) {
						lastSelectedSlot = k;
						break;
					}
				}
			}
			inv.selectedSlot = lastSelectedSlot;
		}
	}
}
