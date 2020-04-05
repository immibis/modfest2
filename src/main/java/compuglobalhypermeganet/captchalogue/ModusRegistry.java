package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class ModusRegistry {
	public static FetchModusType getModus(PlayerInventory inventory) {
		ItemStack modus = inventory.getInvStack(CaptchalogueMod.MODUS_SLOT);
		return getModus(modus);
	}
	
	public static boolean isModus(ItemStack stack) {
		return getModus(stack) != null;
	}
	
	public static FetchModusType QUEUE = new FetchModusQueue();
	public static FetchModusType STACK = new FetchModusStack();
	public static FetchModusType ARRAY = new FetchModusArray();
	public static FetchModusType NULL = new FetchModusNull();
	public static FetchModusType MEMORY = new FetchModusMemory();
	public static FetchModusType TREE_LEAF = new FetchModusTree(false);
	public static FetchModusType TREE_ROOT = new FetchModusTree(true);
	public static FetchModusType QUEUESTACK = new FetchModusQueuestack();
	public static FetchModusType HASHTABLE = new FetchModusHashtable();
	
	public static FetchModusType ARRAY_OF_QUEUE = new FetchModusArrayOfX(QUEUE);
	public static FetchModusType ARRAY_OF_STACK = new FetchModusArrayOfX(STACK);
	public static FetchModusType ARRAY_OF_QUEUESTACK = new FetchModusArrayOfX(QUEUESTACK);
	
	public static FetchModusType HASHTABLE_OF_QUEUE = new FetchModusHashtableOfX(QUEUE);
	public static FetchModusType HASHTABLE_OF_STACK = new FetchModusHashtableOfX(STACK);
	public static FetchModusType HASHTABLE_OF_QUEUESTACK = new FetchModusHashtableOfX(QUEUESTACK);
	public static FetchModusType HASHTABLE_OF_ARRAY = new FetchModusHashtableOfX(ARRAY);
	
	public static FetchModusType getModus(ItemStack stack) {
		if (stack.getItem() == CaptchalogueMod.itemQueueFetchModus) return QUEUE;
		if (stack.getItem() == CaptchalogueMod.itemStackFetchModus) return STACK;
		if (stack.getItem() == CaptchalogueMod.itemArrayFetchModus) return ARRAY;
		if (stack.getItem() == CaptchalogueMod.itemMemoryFetchModus) return MEMORY;
		if (stack.getItem() == CaptchalogueMod.itemTreeLeafFetchModus) return TREE_LEAF;
		if (stack.getItem() == CaptchalogueMod.itemTreeRootFetchModus) return TREE_ROOT;
		if (stack.getItem() == CaptchalogueMod.itemQueuestackFetchModus) return QUEUESTACK;
		if (stack.getItem() == CaptchalogueMod.itemHashtableFetchModus) return HASHTABLE;
		if (stack.getItem() == CaptchalogueMod.itemQueueArrayFetchModus) return ARRAY_OF_QUEUE;
		if (stack.getItem() == CaptchalogueMod.itemStackArrayFetchModus) return ARRAY_OF_STACK;
		if (stack.getItem() == CaptchalogueMod.itemQueuestackArrayFetchModus) return ARRAY_OF_QUEUESTACK;
		if (stack.getItem() == CaptchalogueMod.itemQueueHashtableFetchModus) return HASHTABLE_OF_QUEUE;
		if (stack.getItem() == CaptchalogueMod.itemStackHashtableFetchModus) return HASHTABLE_OF_STACK;
		if (stack.getItem() == CaptchalogueMod.itemQueuestackHashtableFetchModus) return HASHTABLE_OF_QUEUESTACK;
		if (stack.getItem() == CaptchalogueMod.itemArrayHashtableFetchModus) return HASHTABLE_OF_ARRAY;
		return NULL;
	}
	
	
}
