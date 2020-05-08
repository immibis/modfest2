package com.immibis.captchalogue_sylladex;

import net.minecraft.item.ItemStack;

public abstract class FetchModusType {
	
	public static ThreadLocal<Boolean> isProcessingPacket = ThreadLocal.withInitial(() -> Boolean.FALSE);
	
	static int compactItemsToLowerIndices(InventoryWrapper inv, int start, boolean alsoMergeStacks) {
		int to = start;
		int from = start;
		while(from < inv.getNumSlots()) {
			if (to > 0 && alsoMergeStacks)
				InventoryUtils.combineStack(inv.getInvStack(to-1), inv.getInvStack(from));
			if(!inv.getInvStack(from).isEmpty()) {
				if (from != to)
					inv.setInvStack(to, inv.getInvStack(from));
				to++;
			}
			from++;
		}
		int numUsedSlots = to;
		while(to < inv.getNumSlots()) {
			inv.setInvStack(to, ItemStack.EMPTY);
			to++;
		}
		return numUsedSlots;
	}
	
	public abstract boolean forceRightClickOneItem();
	
	// For Stack and Queue moduses, we visually connect most of the slots in the GUI (except the one the player can pull items out of, and the modus slot).
	// If overridesGuiSlotVisualConnectivity is true, slots with the same getBackgroundGroupForSlot are connected.
	// Return BG_GROUP_INVISIBLE to hide the slot entirely.
	// Should return BG_GROUP_MODUS for MODUS_SLOT.
	public static final int BG_GROUP_INVISIBLE = -2;
	public static final int BG_GROUP_MODUS = -3;
	public boolean overridesGuiSlotVisualConnectivity() {return false;}
	public int getBackgroundGroupForSlot(int slot) {return -1;} // should be very fast
	
	
	public static ThreadLocal<Integer> currentPacketFetchModusState = ThreadLocal.withInitial(() -> 0);

	public abstract FetchModusState createFetchModusState(InventoryWrapper inv);
}
