package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.minecraft.container.Container;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public abstract class FetchModusState {

	public void initialize() {}
	public void deinitialize() {}

	public abstract boolean canInsertToSlot(int slot);
	public abstract boolean canTakeFromSlot(int slot);

	public abstract boolean hasCustomInsert();
	public abstract void insert(ItemStack stack);

	public boolean affectsHotbarRendering() {
		return false;
	}

	// affectsHotbarRendering must be true to apply this
	public ItemStack modifyHotbarRenderItem(int slot, ItemStack stack) {
		return stack;
	}

	// This only affects whether the player can select a particular slot in their hotbar. It doesn't stop them using that slot on their inventory screen.
	public static final boolean MODUS_HOTBAR_SLOT_SHOULD_BE_BLOCKED = true; // If true, this would prevent players from throwing their modus for example, without opening their inventory. Or right-clicking things with their modus.
	public final boolean blocksAccessToHotbarSlot(int slot) {return (slot == CaptchalogueMod.MODUS_SLOT ? MODUS_HOTBAR_SLOT_SHOULD_BE_BLOCKED : blocksAccessToHotbarSlot_(slot >= CaptchalogueMod.MODUS_SLOT ? slot-1 : slot));}
	protected boolean blocksAccessToHotbarSlot_(int slot) {return false;}

	public boolean resetGuiStateWhenInventoryClosed() {return true;}

	public boolean overrideInventoryClick(Container cont, PlayerInventory plinv, int slot, SlotActionType actionType, int clickData) {
		return false; // return true to override
	}
	
	// Called after the inventory changes, on the next tick.
	// changedSlotMask is a bitmask of slots that might have changed, e.g. bit (1<<5) means slot 5 changed.
	// serverSync is true if these slots were changed by the server and we are on the client. If this is true, you shouldn't change any slots because that will cause a desync.
	public void afterPossibleInventoryChange(long changedSlotMask, boolean serverSync) {}

	public FetchModusGuiState createGuiState(Container cont) {
		return null;
	}
}
