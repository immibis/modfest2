package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public abstract class FetchModusType {
	
	/*
	 * TODO for moduses:
	 * Indicators for which slots are insertable/extractable (based on cursor item)
	 * Better merging of consecutive identical items (queue/stack/queuestack)
	 * Fix creative pick-block
	 * Queue/stack inventory should have extra borders to make the inventory look like a linear sequence. This requires changing the order of the slots.
	 * Queuestack might require a different order still in order to use both the first and last in the hotbar. (How would queuestack array do this?)
	 *
	 * Add an item that turns all textures to linear interpolation when you hold it in your inventory.
	 * 
	 * When insert() is called on the client with a tree modus, the container needs to be updated... how?
	 *
	 * moduses:
	 *  Done: Queue
	 *  Done: Stack
	 *  Done: Array
	 *  Done: Memory
	 *  Done: Queuestack
	 *  Done: Array of stacks/queues/queuestacks
	 *  
	 *  Hashtable
	 *  Hashtable with arrays/stacks/queues/queuestack
	 *  Tree
	 *  Queue/stack/queuestack of arrays?
	 *  
	 *  ? Array/stack/queue of trees
	 *  ? Boggle
	 *  ? Connect Four
	 *  ? Ouija
	 *  ? Operation
	 *  ? Battleship
	 *  ? Fibonacci Heap
	 *  ? Puzzle
	 * 
	 * ? Sburb loading screen
	 * 
	 * Make sure all code safely ignores player inventory slots outside of 0-35 range (no out-of-bounds exceptions)
	 * 
	 * Remove all the low-level hooks like setInvStack when possible.
	 * 
	 * All conceivable modus combinations (including different orders)
	 * Game-based moduses (take over the slot click handler, probably)
	 * Recipes for moduses (except for array)
	 * Loot for array modus (not rare)
	 * Give players a modus when they spawn
	 * Drop / violently eject oldest items when items are added to a full modus
	 * Violently eject items when modus is removed?
	 * 
	 * TODO: Make the tree and hashtable work when the server and client languages don't match.
	 */
	
	public static ThreadLocal<Boolean> isProcessingPacket = ThreadLocal.withInitial(() -> Boolean.FALSE);
	
	static int compactItemsToLowerIndices(InventoryWrapper inv, int start) {
		int to = start;
		int from = start;
		while(from < inv.getNumSlots()) {
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
	
	public boolean setStackInSlot(PlayerInventory inventory, int slot, ItemStack stack) {return false;} // return true to override

	public abstract boolean hasCustomInsert();
	public abstract void insert(InventoryWrapper inv, ItemStack stack);
	public abstract boolean forceRightClickOneItem();
	
	// For Stack and Queue moduses, we visually connect most of the slots in the GUI (except the one the player can pull items out of, and the modus slot).
	// If overridesGuiSlotVisualConnectivity is true, slots with the same getBackgroundGroupForSlot are connected.
	// Return BG_GROUP_INVISIBLE to hide the slot entirely.
	// Should return BG_GROUP_MODUS for MODUS_SLOT.
	public static final int BG_GROUP_INVISIBLE = -2;
	public static final int BG_GROUP_MODUS = -3;
	public boolean overridesGuiSlotVisualConnectivity() {return false;}
	public int getBackgroundGroupForSlot(int slot) {return -1;} // should be very fast

	public boolean overrideInventoryClick(Container cont, PlayerInventory plinv, InventoryWrapper inv, int slot, SlotActionType actionType, int clickData) {
		return false; // return true to override
	}

	public FetchModusGuiState createGuiState(Container cont, PlayerInventory inv) {
		return null;
	}
	public boolean resetStateWhenInventoryClosed() {return true;}
	
	@Environment(EnvType.CLIENT)
	public boolean overrideDrawSlot(ContainerScreen<?> screen, int screenX, int screenY, Slot slot, PlayerInventory inv, int slotIndex, int mouseX, int mouseY) {return false;} // return true if overridden

	// Override which slot is hovered over. This DOES affect the quick-swap keys (swap with hotbar 1-9) but it doesn't affect click actions.
	@Environment(EnvType.CLIENT)
	public Slot overrideFocusedSlot(ContainerScreen<?> screen, PlayerInventory inv, int slot, Slot focusedSlot) {
		return focusedSlot;
	}
	
	public static ThreadLocal<Integer> currentPacketFetchModusState = ThreadLocal.withInitial(() -> 0);

	public boolean affectsHotbarRendering() {
		return false;
	}

	// affectsHotbarRendering must be true to apply this
	public boolean hidesHotbarSlot(int k) {
		return false;
	}

	// This only affects whether the player can select a particular slot in their hotbar. It doesn't stop them using that slot on their inventory screen.
	public static final boolean MODUS_HOTBAR_SLOT_SHOULD_BE_BLOCKED = true; // If true, this would prevent players from throwing their modus for example, without opening their inventory. Or right-clicking things with their modus.
	public final boolean blocksAccessToHotbarSlot(int slot) {return (slot == CaptchalogueMod.MODUS_SLOT ? MODUS_HOTBAR_SLOT_SHOULD_BE_BLOCKED : blocksAccessToHotbarSlot_(slot));}
	protected boolean blocksAccessToHotbarSlot_(int slot) {return false;}

	public void afterInventoryClick(Container this_, PlayerInventory plinv, InventoryWrapper inv, int slotIndex, SlotActionType actionType, int clickData) {}

	// Called after an unknown inventory change might have occurred (generally items were removed; other cases are not expected)
	// E.g. this is called after someone double-clicks to pick up all items of a type and we're not sure whether it picked up any items out of their inventory.
	// It's also called after an item is used because the item might have been used up.
	public void afterPossibleInventoryChange(Container cont, InventoryWrapper inv) {}



	public abstract boolean canTakeFromSlot(InventoryWrapper inv, int slot);
	public abstract boolean canInsertToSlot(InventoryWrapper inv, int slot);

	public void initialize(InventoryWrapper inv) {}
	public void deinitialize(InventoryWrapper inv) {}
	
	public FetchModusState createFetchModusState(InventoryWrapper inv) {
		return FetchModusState.NULL_STATE;
	}
}
