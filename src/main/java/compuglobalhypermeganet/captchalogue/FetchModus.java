package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public abstract class FetchModus {
	
	/*
	 * TODO for moduses:
	 * Make sure players can't use items in inaccessible hotbar slots
	 * Indicators for which slots are accessible
	 * Take over rendering (e.g. don't draw gaps between inaccessible slots in queue and stack)
	 * Take over onSlotClicked for our custom slots
	 * Better merging of consecutive identical items
	 * Shift-click out of a queue/stack shouldn't move several consecutive slots
	 * Fix creative pick-block
	 * Fix item duplication when you use up your hotbar slot by using the item (queue modus, test others too)
	 * 
	 * All conceivable modus combinations (including different orders)
	 * Game-based moduses (take over the slot click handler, probably)
	 * Recipes for moduses (except for array)
	 * Loot for array modus (not rare)
	 * Give players a modus when they spawn
	 */
	
	public static final int MODUS_SLOT = 8;
	public static final Identifier MODUS_SLOT_BG_IMAGE = new Identifier("compuglobalhypermeganet", "placeholder_fetch_modus");
	
	public static ThreadLocal<Boolean> isProcessingPacket = ThreadLocal.withInitial(() -> Boolean.FALSE);
	
	static void compactItemsToLowerIndices(PlayerInventory inventory, int start) {
		int to = start;
		int from = start;
		while(from < inventory.main.size()) {
			if(from != MODUS_SLOT && !inventory.main.get(from).isEmpty()) {
				if (from != to)
					inventory.main.set(to, inventory.main.get(from));
				to++;
				if(to == MODUS_SLOT)
					to++;
			}
			from++;
		}
		while(to < inventory.main.size()) {
			inventory.main.set(to, ItemStack.EMPTY);
			to++;
			if(to == MODUS_SLOT)
				to++;
		}
	}
	
	public abstract boolean canTakeFromSlot(PlayerInventory inv, int slot);
	public abstract boolean canInsertToSlot(PlayerInventory inv, int slot);
	public abstract boolean setStackInSlot(PlayerInventory inventory, int slot, ItemStack stack); // return true to override
	public abstract void initialize(PlayerInventory inventory);
	
	public static FetchModus getModus(PlayerInventory inventory) {
		ItemStack modus = inventory.getInvStack(MODUS_SLOT);
		return getFlyweightModus(modus);
	}
	
	public static boolean isModus(ItemStack stack) {
		if (stack.getItem() == CaptchalogueMod.itemQueueFetchModus) return true;
		if (stack.getItem() == CaptchalogueMod.itemStackFetchModus) return true;
		if (stack.getItem() == CaptchalogueMod.itemArrayFetchModus) return true;
		return false;
	}
	
	public static FetchModus QUEUE = new FetchModusQueue();
	public static FetchModus STACK = new FetchModusStack();
	public static FetchModus ARRAY = new FetchModusArray();
	public static FetchModus NULL = new FetchModusNull();
	public static FetchModus getFlyweightModus(ItemStack stack) {
		if (stack.getItem() == CaptchalogueMod.itemQueueFetchModus) return QUEUE;
		if (stack.getItem() == CaptchalogueMod.itemStackFetchModus) return STACK;
		if (stack.getItem() == CaptchalogueMod.itemArrayFetchModus) return ARRAY;
		return NULL;
	}
	public abstract boolean hasCustomInsert();
	public abstract void insert(PlayerInventory inv, ItemStack stack);
	public abstract boolean forceRightClickOneItem();
	
	// For Stack and Queue moduses, we visually connect most of the slots in the GUI (except the one the player can pull items out of, and the modus slot).
	// If overridesGuiSlotVisualConnectivity is true, slots with the same getBackgroundGroupForSlot are connected.
	// Return BG_GROUP_INVISIBLE to hide the slot entirely.
	// Should return BG_GROUP_MODUS for MODUS_SLOT.
	public static final int BG_GROUP_INVISIBLE = -2;
	public static final int BG_GROUP_MODUS = -3;
	public boolean overridesGuiSlotVisualConnectivity() {return false;}
	public int getBackgroundGroupForSlot(int slot) {return -1;} // should be very fast
}
