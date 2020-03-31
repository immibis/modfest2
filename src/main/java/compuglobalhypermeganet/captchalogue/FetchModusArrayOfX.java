package compuglobalhypermeganet.captchalogue;

import compuglobalhypermeganet.CaptchalogueMod;
import net.minecraft.container.Container;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusArrayOfX extends FetchModus {
	
	private static final int modusSlotVisualIndex = InventoryUtils.playerInventoryLogicalToVisualIndex(CaptchalogueMod.MODUS_SLOT);
	private static final int[][] slotsAvailablePerSubModus = new int[8][4];
	private static final int[] unusedSlots;
	static {
		// layout hardcoded for MODUS_SLOT == 8
		if(CaptchalogueMod.MODUS_SLOT != 8) throw new AssertionError();
		for(int x = 0; x < 8; x++) {
			for(int y = 0; y < 4; y++) {
				int slot = InventoryUtils.playerInventoryVisualToLogicalIndex((3-y)*9 + x); // slot 0 (the accessible one) must be at the bottom (in the hotbar)
				if(slot == CaptchalogueMod.MODUS_SLOT) throw new AssertionError();
				if(slot > CaptchalogueMod.MODUS_SLOT)
					slot--;
				slotsAvailablePerSubModus[x][y] = slot;
			}
		}
		unusedSlots = new int[] {16, 25, 34};
	}
	
	public FetchModus baseModus;
	public FetchModusArrayOfX(FetchModus baseModus) {
		this.baseModus = baseModus;
	}
	
	private int getSubModusBasedOnSlotNumber(InventoryWrapper inv, int slot) {
		//int playerIndex = inv.toPlayerInventorySlotIndex(slot);
		int playerIndex = (slot >= CaptchalogueMod.MODUS_SLOT ? slot+1 : slot); // XXX
		int visualIndex = InventoryUtils.playerInventoryLogicalToVisualIndex(playerIndex);
		int col = visualIndex % 9;
		if (col == modusSlotVisualIndex % 9)
			return -1; // skip this column
		if (col >= modusSlotVisualIndex % 9)
			col--;
		return col;
	}
	private int getSubModusRelativeSlot(InventoryWrapper inv, int slot) {
		if(slot >= CaptchalogueMod.MODUS_SLOT)
			slot++;
		return 3 - InventoryUtils.playerInventoryLogicalToVisualIndex(slot) / 9;
	}
	
	@Override
	public boolean canInsertToSlot(InventoryWrapper inv, int slot) {
		int subModus = getSubModusBasedOnSlotNumber(inv, slot);
		if(subModus < 0)
			return false;
		return baseModus.canInsertToSlot(new InventoryWrapper.SpecifiedSlots(inv, slotsAvailablePerSubModus[subModus]), getSubModusRelativeSlot(inv, slot));
	}
	@Override
	public boolean canTakeFromSlot(InventoryWrapper inv, int slot) {
		int subModus = getSubModusBasedOnSlotNumber(inv, slot);
		if(subModus < 0)
			return false;
		return baseModus.canTakeFromSlot(new InventoryWrapper.SpecifiedSlots(inv, slotsAvailablePerSubModus[subModus]), getSubModusRelativeSlot(inv, slot));
	}
	@Override
	public boolean hasCustomInsert() {
		return baseModus.hasCustomInsert();
	}
	@Override
	public void insert(InventoryWrapper inv, ItemStack stack) {
		for(int[] subSlots : slotsAvailablePerSubModus) {
			if(stack.isEmpty())
				break;
			baseModus.insert(new InventoryWrapper.SpecifiedSlots(inv, subSlots), stack);
		}
	}
	@Override
	public boolean forceRightClickOneItem() {
		return baseModus.forceRightClickOneItem();
	}
	
	@Override
	public boolean overridesGuiSlotVisualConnectivity() {
		return true;
	}
	@Override
	public int getBackgroundGroupForSlot(int slot) {
		if(slot == CaptchalogueMod.MODUS_SLOT)
			return BG_GROUP_MODUS;
		// TODO: pass in 0-34 slot indices here instead of 0-35 with MODUS_SLOT
		if(slot > CaptchalogueMod.MODUS_SLOT)
			slot--;
		int id = getSubModusBasedOnSlotNumber(null, slot);
		if(id < 0)
			return BG_GROUP_INVISIBLE;
		return id;
	}
	
	@Override
	public void afterPossibleInventoryChange(Container cont, InventoryWrapper inv) {
		for(int[] subSlots : slotsAvailablePerSubModus) {
			baseModus.afterPossibleInventoryChange(cont, new InventoryWrapper.SpecifiedSlots(inv, subSlots));
		}
	}
	
	@Override
	public boolean overrideInventoryClick(Container cont, PlayerInventory plinv, InventoryWrapper inv, int slot, SlotActionType actionType, int clickData) {
		int sub = getSubModusBasedOnSlotNumber(inv, slot);
		if(sub < 0)
			return true; // deny click
		int relativeSlot = getSubModusRelativeSlot(inv, slot);
		return baseModus.overrideInventoryClick(cont, plinv, new InventoryWrapper.SpecifiedSlots(inv, slotsAvailablePerSubModus[sub]), relativeSlot, actionType, clickData);
	}
	
	@Override
	public void afterInventoryClick(Container cont, PlayerInventory plinv, InventoryWrapper inv, int slot, SlotActionType actionType, int clickData) {
		int sub = getSubModusBasedOnSlotNumber(inv, slot);
		if(sub < 0)
			return;
		int relativeSlot = getSubModusRelativeSlot(inv, slot);
		System.out.println("afterInventoryClick "+slot+"="+sub+"/"+relativeSlot);
		if(slotsAvailablePerSubModus[sub][relativeSlot] != slot) throw new AssertionError("wrong slot mapping; fix me");
		baseModus.afterInventoryClick(cont, plinv, new InventoryWrapper.SpecifiedSlots(inv, slotsAvailablePerSubModus[sub]), relativeSlot, actionType, clickData);
	}
	
	@Override
	protected boolean blocksAccessToHotbarSlot_(int slot) {
		// XXX: this relies on the fact that queue, stack and queuestack all allow access to slot 0.
		return false;
	}
	
	@Override
	public void initialize(InventoryWrapper inv) {
		for(int[] subSlots : slotsAvailablePerSubModus) {
			baseModus.initialize(new InventoryWrapper.SpecifiedSlots(inv, subSlots));
		}
		
		// extra slots can't hold items
		for(int slot : unusedSlots) {
			ItemStack stack = inv.getInvStack(slot);
			if (!stack.isEmpty()) {
				inv.getPlayer().dropItem(stack, false, true);
				inv.setInvStack(slot, ItemStack.EMPTY);
			}
		}
	}
	
	@Override
	public void deinitialize(InventoryWrapper inv) {
		for(int[] subSlots : slotsAvailablePerSubModus) {
			baseModus.deinitialize(new InventoryWrapper.SpecifiedSlots(inv, subSlots));
		}
	}
}