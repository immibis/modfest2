package com.immibis.captchalogue_sylladex;

import net.minecraft.container.Container;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusArrayOfX extends FetchModusType {

	public static final int modusSlotVisualIndex = InventoryUtils.playerInventoryLogicalToVisualIndex(CaptchalogueMod.MODUS_SLOT);
	public static final int[][] slotsAvailablePerSubModus = new int[8][4];
	public static final int[] unusedSlots;
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
	
	public FetchModusType baseModusType;
	public FetchModusArrayOfX(FetchModusType baseModus) {
		this.baseModusType = baseModus;
	}
	
	public static int getSubModusBasedOnSlotNumber(InventoryWrapper inv, int slot) {
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
	public static int getSubModusRelativeSlot(InventoryWrapper inv, int slot) {
		if(slot >= CaptchalogueMod.MODUS_SLOT)
			slot++;
		return 3 - InventoryUtils.playerInventoryLogicalToVisualIndex(slot) / 9;
	}
	@Override
	public boolean forceRightClickOneItem() {
		return baseModusType.forceRightClickOneItem();
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
	public FetchModusState createFetchModusState(InventoryWrapper inv) {
		return new FetchModusStateArrayOfX(inv, baseModusType);
	}
	
	public static class FetchModusStateArrayOfX extends FetchModusState {
		private FetchModusState[] bases = new FetchModusState[slotsAvailablePerSubModus.length];

		private InventoryWrapper inv;
		public FetchModusStateArrayOfX(InventoryWrapper inv, FetchModusType baseModusType) {
			this.inv = inv;
			
			for(int k = 0; k < bases.length; k++)
				bases[k] = baseModusType.createFetchModusState(new InventoryWrapper.SpecifiedSlots(inv, slotsAvailablePerSubModus[k]));
		}

		@Override
		public void initialize() {
			for(FetchModusState base : bases)
				base.initialize();
			
			// extra slots can't hold items. Insert them anywhere, or else drop them.
			for(int slot : unusedSlots) {
				ItemStack stack = inv.getInvStack(slot);
				if (!stack.isEmpty()) {
					inv.setInvStack(slot, ItemStack.EMPTY);
					insert(stack, true);
					if (!stack.isEmpty())
						CaptchalogueMod.launchExcessItems(inv.getPlayer(), stack);
				}
			}
		}
		
		@Override
		public void deinitialize() {
			for(FetchModusState base : bases)
				base.deinitialize();
		}
		
		@Override
		public boolean canInsertToSlot(int slot) {
			int subModus = getSubModusBasedOnSlotNumber(inv, slot);
			if(subModus < 0)
				return false;
			return bases[subModus].canInsertToSlot(getSubModusRelativeSlot(inv, slot));
		}
		
		@Override
		public boolean canTakeFromSlot(int slot) {
			int subModus = getSubModusBasedOnSlotNumber(inv, slot);
			if(subModus < 0)
				return false;
			return bases[subModus].canTakeFromSlot(getSubModusRelativeSlot(inv, slot));
		}
		
		@Override
		public boolean hasCustomInsert() {
			return bases[0].hasCustomInsert();
		}
		@Override
		public void insert(ItemStack stack, boolean allowViolentExpulsion) {
			for(FetchModusState base : bases) {
				if(stack.isEmpty())
					break;
				// Don't allow violent expulsion when inserting into a base modus. Otherwise the item would always be placed in the first one even if other ones aren't full.
				base.insert(stack, false);
			}
		}
		
		@Override
		protected boolean blocksAccessToHotbarSlot_(int slot) {
			// XXX: this relies on the fact that queue, stack and queuestack all allow access to slot 0.
			return false;
		}
		
		@Override
		public void afterPossibleInventoryChange(long changedSlotMask, boolean serverSync) {
			for(int k = 0; k < slotsAvailablePerSubModus.length; k++) {
				long baseMask = 0;
				for(int i = 0; i < slotsAvailablePerSubModus[k].length; i++) {
					if ((changedSlotMask & (1L << slotsAvailablePerSubModus[k][i])) != 0)
						baseMask |= 1L << i;
				}
				if(baseMask != 0)
					bases[k].afterPossibleInventoryChange(baseMask, serverSync);
			}
		}
		
		@Override
		public boolean overrideInventoryClick(Container cont, PlayerInventory plinv, int slot, SlotActionType actionType, int clickData) {
			int sub = getSubModusBasedOnSlotNumber(inv, slot);
			if(sub < 0)
				return true; // deny click
			int relativeSlot = getSubModusRelativeSlot(inv, slot);
			return bases[sub].overrideInventoryClick(cont, plinv, relativeSlot, actionType, clickData);
		}
	}
}