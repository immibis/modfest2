package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public abstract class FetchModus {
	
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
}
