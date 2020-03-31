package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class ModusRegistry {
	public static FetchModus getModus(PlayerInventory inventory) {
		ItemStack modus = inventory.getInvStack(CaptchalogueMod.MODUS_SLOT);
		return getModus(modus);
	}
	
	public static boolean isModus(ItemStack stack) {
		if (stack.getItem() == CaptchalogueMod.itemQueueFetchModus) return true;
		if (stack.getItem() == CaptchalogueMod.itemStackFetchModus) return true;
		if (stack.getItem() == CaptchalogueMod.itemArrayFetchModus) return true;
		if (stack.getItem() == CaptchalogueMod.itemMemoryFetchModus) return true;
		if (stack.getItem() == CaptchalogueMod.itemQueuestackFetchModus) return true;
		return false;
	}
	
	public static FetchModus QUEUE = new FetchModusQueue();
	public static FetchModus STACK = new FetchModusStack();
	public static FetchModus ARRAY = new FetchModusArray();
	public static FetchModus NULL = new FetchModusNull();
	public static FetchModus MEMORY = new FetchModusMemory();
	public static FetchModus QUEUESTACK = new FetchModusQueuestack();
	public static FetchModus getModus(ItemStack stack) {
		if (stack.getItem() == CaptchalogueMod.itemQueueFetchModus) return QUEUE;
		if (stack.getItem() == CaptchalogueMod.itemStackFetchModus) return STACK;
		if (stack.getItem() == CaptchalogueMod.itemArrayFetchModus) return ARRAY;
		if (stack.getItem() == CaptchalogueMod.itemMemoryFetchModus) return MEMORY;
		if (stack.getItem() == CaptchalogueMod.itemQueuestackFetchModus) return QUEUESTACK;
		return NULL;
	}
	
	
}
