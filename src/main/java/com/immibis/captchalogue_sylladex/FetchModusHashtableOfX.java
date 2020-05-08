package com.immibis.captchalogue_sylladex;

import java.util.ArrayList;
import java.util.List;

import com.immibis.captchalogue_sylladex.mixin_support.ISlotMixin;

import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class FetchModusHashtableOfX extends FetchModusType {
	
	public final FetchModusType baseModusType;
	
	public FetchModusHashtableOfX(FetchModusType baseModusType) {
		this.baseModusType = baseModusType;
	}
	
	@Override
	public boolean forceRightClickOneItem() {
		return baseModusType.forceRightClickOneItem();
	}

	// Very much like the ArrayOfX layout, except certain items can only go in certain bases.
	public static final int NUM_HOTBAR_SLOTS = FetchModusHashtable.NUM_HOTBAR_SLOTS;
	public static final int modusSlotVisualIndex = FetchModusArrayOfX.modusSlotVisualIndex;
	public static final int[][] slotsAvailablePerSubModus = FetchModusArrayOfX.slotsAvailablePerSubModus;
	public static final int[] unusedSlots = FetchModusArrayOfX.unusedSlots;
	
	@Override
	public boolean overridesGuiSlotVisualConnectivity() {
		return true;
	}
	@Override
	public int getBackgroundGroupForSlot(int slot) {
		if(slot == CaptchalogueMod.MODUS_SLOT)
			return BG_GROUP_MODUS;
		int index = FetchModusArrayOfX.getSubModusBasedOnSlotNumber(null, InventoryWrapper.PlayerInventorySkippingModusSlot.fromUnderlyingSlotIndex(slot));
		if(index < 0)
			return BG_GROUP_INVISIBLE;
		else
			return index;
	}
	
	@Override
	public FetchModusState createFetchModusState(InventoryWrapper inv) {
		return new State(inv, baseModusType);
	}
	
	public static class State extends FetchModusState {
		private InventoryWrapper inv;
		private FetchModusState[] bases = new FetchModusState[NUM_HOTBAR_SLOTS];
		
		public State(InventoryWrapper inv, FetchModusType baseType) {
			this.inv = inv;
			for(int k = 0; k < bases.length; k++)
				bases[k] = baseType.createFetchModusState(new InventoryWrapper.SpecifiedSlots(inv, slotsAvailablePerSubModus[k]));
		}
		
		@Override
		protected boolean blocksAccessToHotbarSlot_(int slot) {
			// XXX: this relies on the bases all making slot 0 available.
			return false; // all hotbar slots available
		}
		
		@Override
		public void initialize() {
			List<ItemStack> stacks = new ArrayList<>();
			
			for(int slot : unusedSlots) {
				ItemStack stack = inv.getInvStack(slot);
				if(!stack.isEmpty()) {
					inv.setInvStack(slot, ItemStack.EMPTY);
					stacks.add(stack);
				}
			}
			
			for(int k = 0; k < inv.getNumSlots(); k++) {
				ItemStack stack = inv.getInvStack(k);
				if(!stack.isEmpty()) {
					// slot must not be an unused slot number, or getSubModusBasedOnSlotNumber is unpredictable.
					// So, we empty those slots first in the preceding loop.
					int requiredSlot = CaptchalogueMod.hashItem(stack, FetchModusHashtable.HASH_MODE_VOWELS_CONSONANTS) % NUM_HOTBAR_SLOTS;
					if(requiredSlot != FetchModusArrayOfX.getSubModusBasedOnSlotNumber(null, k)) { // leave alone items that are already in their correct slot
						stacks.add(stack);
						inv.setInvStack(k, ItemStack.EMPTY);
					}
				}
			}
			
			for(ItemStack stack : stacks) {
				insert(stack, true);
				if(!stack.isEmpty())
					throw new AssertionError("unreachable - not empty after insert"); // insert always launches leftover items
			}
		}
		
		@Override
		public boolean canInsertToSlot(int slot) {
			int subModus = FetchModusArrayOfX.getSubModusBasedOnSlotNumber(inv, slot);
			if(subModus < 0)
				return false;
			return bases[subModus].canInsertToSlot(FetchModusArrayOfX.getSubModusRelativeSlot(inv, slot));
		}
		
		@Override
		public boolean canTakeFromSlot(int slot) {
			int subModus = FetchModusArrayOfX.getSubModusBasedOnSlotNumber(inv, slot);
			if(subModus < 0)
				return false;
			return bases[subModus].canTakeFromSlot(FetchModusArrayOfX.getSubModusRelativeSlot(inv, slot));
		}

		@Override
		public boolean hasCustomInsert() {
			return true;
		}
		
		@Override
		public void insert(ItemStack stack, boolean allowViolentExpulsion) {
			int slot = CaptchalogueMod.hashItem(stack, FetchModusHashtable.HASH_MODE_VOWELS_CONSONANTS) % NUM_HOTBAR_SLOTS;
			
			bases[slot].insert(stack, allowViolentExpulsion);
			
			// Array base won't expel anything (because how would it choose which item?), so expel the new item instead.
			if(!stack.isEmpty() && allowViolentExpulsion) {
				CaptchalogueMod.launchExcessItems(inv.getPlayer(), stack.copy());
				stack.setCount(0);
			}
		}
		
		@Override
		public void afterPossibleInventoryChange(long changedSlotMask, boolean serverSync) {
			
			List<ItemStack> stacksInWrongHashSlots = new ArrayList<>();
			
			if (!serverSync) {
				for(int slot : unusedSlots) {
					ItemStack stack = inv.getInvStack(slot);
					if(!stack.isEmpty()) {
						inv.setInvStack(slot, ItemStack.EMPTY);
						stacksInWrongHashSlots.add(stack);
					}
				}
				
				for(int k = 0; k < inv.getNumSlots(); k++) {
					ItemStack stack = inv.getInvStack(k);
					if(!stack.isEmpty()) {
						// slot must not be an unused slot number, or getSubModusBasedOnSlotNumber is unpredictable.
						// So, we empty those slots first in the preceding loop.
						int requiredSlot = CaptchalogueMod.hashItem(stack, FetchModusHashtable.HASH_MODE_VOWELS_CONSONANTS) % NUM_HOTBAR_SLOTS;
						if(requiredSlot != FetchModusArrayOfX.getSubModusBasedOnSlotNumber(null, k)) { // leave alone items that are already in their correct slot
							stacksInWrongHashSlots.add(stack);
							inv.setInvStack(k, ItemStack.EMPTY);
						}
					}
				}
			}
			
			for(int k = 0; k < slotsAvailablePerSubModus.length; k++) {
				long baseMask = 0;
				for(int i = 0; i < slotsAvailablePerSubModus[k].length; i++) {
					if ((changedSlotMask & (1L << slotsAvailablePerSubModus[k][i])) != 0)
						baseMask |= 1L << i;
				}
				if(baseMask != 0)
					bases[k].afterPossibleInventoryChange(baseMask, serverSync);
			}
			
			for(ItemStack stack : stacksInWrongHashSlots)
				insert(stack, true);
		}
		
		@Override
		public boolean overrideInventoryClick(Container cont, PlayerInventory plinv, int slot, SlotActionType actionType, int clickData) {
			int sub = FetchModusArrayOfX.getSubModusBasedOnSlotNumber(inv, slot);
			if(sub < 0)
				return true; // deny click
			int relativeSlot = FetchModusArrayOfX.getSubModusRelativeSlot(inv, slot);
			return bases[sub].overrideInventoryClick(cont, plinv, relativeSlot, actionType, clickData);
		}
		
		@Override
		public void fiddleWithItemRenderTooltip(List<String> tooltip) {
			FetchModusHashtable.fiddleWithItemRenderTooltip(tooltip);
		}
		
		@Override
		public FetchModusGuiState createGuiState(Container cont) {
			return new GuiState(cont, inv.getPlayer().inventory);
		}
	}
	
	public static class GuiState extends FetchModusGuiState {
		private Container cont;
		private PlayerInventory plinv;
		public GuiState(Container cont, PlayerInventory plinv) {
			this.cont = cont;
			this.plinv = plinv;
		}
		
		@Override
		public void afterDrawSlot(Slot slot, Drawer d) {
			if (!plinv.getCursorStack().isEmpty()) {
				int cursorStackSlot = CaptchalogueMod.hashItem(plinv.getCursorStack(), FetchModusHashtable.HASH_MODE_VOWELS_CONSONANTS) % NUM_HOTBAR_SLOTS;
				int underlyingSlotIndex = ((ISlotMixin)slot).captchalogue_getSlotNum();
				if(slot.inventory == plinv && underlyingSlotIndex != CaptchalogueMod.MODUS_SLOT && FetchModusArrayOfX.getSubModusBasedOnSlotNumber(null, InventoryWrapper.PlayerInventorySkippingModusSlot.fromUnderlyingSlotIndex(underlyingSlotIndex)) == cursorStackSlot) {
					// Draw a highlight over the slots where the object can go.
					d.enableBlend();
					d.beginRenderingSolidQuads();
					d.appendSolidQuad(slot.xPosition, slot.yPosition, slot.xPosition+16, slot.yPosition+16, 1.0f, 1.0f, 1.0f, 0.5f);
					d.endRenderingSolidQuads();
					d.disableBlend();
				}
			}
		}
	}
}