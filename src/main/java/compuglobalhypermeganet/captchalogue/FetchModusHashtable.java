package compuglobalhypermeganet.captchalogue;

import java.util.ArrayList;
import java.util.List;

import compuglobalhypermeganet.CaptchalogueMod;
import compuglobalhypermeganet.captchalogue.mixin_support.ISlotMixin;
import net.minecraft.container.Container;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;

public class FetchModusHashtable extends FetchModusType {
	
	public static abstract class HashMode {
		public abstract int hashCodeFor(char c);
	}
	
	public static final HashMode HASH_MODE_VOWELS_CONSONANTS = new HashMode() {
		@Override
		public int hashCodeFor(char c) {
			// char is already lowercase
			if(c >= 'a' && c <= 'z') {
				if(c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u')
					return 1;
				else
					return 2;
			} else
				return 0;
		}
	};
	
	@Override
	public boolean forceRightClickOneItem() {
		return false;
	}

	public static final int NUM_HOTBAR_SLOTS = (CaptchalogueMod.MODUS_SLOT < 9 ? 8 : 9);
	
	@Override
	public boolean overridesGuiSlotVisualConnectivity() {
		return true;
	}
	@Override
	public int getBackgroundGroupForSlot(int slot) {
		if(slot == CaptchalogueMod.MODUS_SLOT)
			return BG_GROUP_MODUS;
		if(slot >= NUM_HOTBAR_SLOTS)
			return BG_GROUP_INVISIBLE;
		return slot;
	}
	
	@Override
	public FetchModusState createFetchModusState(InventoryWrapper inv) {
		return new State(inv);
	}
	
	public static class State extends FetchModusState {
		private InventoryWrapper inv;
		public State(InventoryWrapper inv) {
			this.inv = inv;
		}
		
		@Override
		protected boolean blocksAccessToHotbarSlot_(int slot) {
			return false; // all hotbar slots available
		}
		
		@Override
		public void initialize() {
			List<ItemStack> stacks = new ArrayList<>();
			
			for(int k = 0; k < inv.getNumSlots(); k++) {
				ItemStack stack = inv.getInvStack(k);
				if(!stack.isEmpty()) {
					int requiredSlot = CaptchalogueMod.hashItem(stack, HASH_MODE_VOWELS_CONSONANTS) % NUM_HOTBAR_SLOTS;
					if(requiredSlot != k) { // leave alone items that are already in their correct slot
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
			return slot < NUM_HOTBAR_SLOTS;
		}
		
		@Override
		public boolean canTakeFromSlot(int slot) {
			return slot < NUM_HOTBAR_SLOTS;
		}

		@Override
		public boolean hasCustomInsert() {
			return true;
		}
		
		@Override
		public void insert(ItemStack stack, boolean allowViolentExpulsion) {
			int slot = CaptchalogueMod.hashItem(stack, HASH_MODE_VOWELS_CONSONANTS) % NUM_HOTBAR_SLOTS;
			ItemStack blockingStack = inv.getInvStack(slot);
			if(!blockingStack.isEmpty()) {
				
				if (Container.canStacksCombine(stack, blockingStack)) {
					int nt = Math.min(stack.getCount(), blockingStack.getMaxCount() - blockingStack.getCount());
					if (nt > 0) {
						stack.decrement(nt);
						blockingStack.increment(nt);
						inv.setInvStack(slot, blockingStack);
					}
					// If the same item is blocking insertion, then don't eject the old items; eject the new items instead.
					// This prevents the silly situation where we drop 64 items and then insert the 2. We should only drop the overflow of the same item type.
					if (!stack.isEmpty() && allowViolentExpulsion) {
						CaptchalogueMod.launchExcessItems(inv.getPlayer(), stack.copy());
						stack.setCount(0);
					}
					return;
				}
				
				if (!allowViolentExpulsion)
					return;
				
				CaptchalogueMod.launchExcessItems(inv.getPlayer(), blockingStack);
				// don't need to clear slot, since we're about to overwrite it.
			}
			inv.setInvStack(slot, stack.copy());
			stack.setCount(0);
		}
		
		@Override
		public void afterPossibleInventoryChange(long changedSlotMask, boolean serverSync) {
			if(!serverSync)
				initialize(); // ensure item are in the correct slots
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
				int cursorStackSlot = CaptchalogueMod.hashItem(plinv.getCursorStack(), HASH_MODE_VOWELS_CONSONANTS) % NUM_HOTBAR_SLOTS;
				if(slot.inventory == plinv && ((ISlotMixin)slot).captchalogue_getSlotNum() == cursorStackSlot) {
					// Draw a highlight over the slot where the object can go.
					d.enableBlend();
					d.beginRenderingSolidQuads();
					d.appendSolidQuad(slot.xPosition, slot.yPosition, slot.xPosition+16, slot.yPosition+16, 1.0f, 1.0f, 1.0f, 0.5f);
					d.endRenderingSolidQuads();
					d.disableBlend();
				}
			}
		}
	}
	
	public static void fiddleWithItemRenderTooltip(List<String> tooltip) {
		if(tooltip.size() > 0) {
			String s = tooltip.get(0);
			StringBuilder sb = new StringBuilder();
			Formatting lastFormatting = Formatting.RESET; // unknown
			int hash = 0;
			for(int k = 0; k < s.length(); k++) {
				char c = s.charAt(k);
				if(c == '§') {
					// Found a formatting code
					if(k+1 < s.length()) {
						k++;
						Formatting codeHere = Formatting.byCode(s.charAt(k)); 
						// Preserve non-colour formatting codes
						if(codeHere != null && codeHere.getColorIndex() < 0) {
							sb.append(codeHere);
						}
					}
					continue;
				}
				// XXX: locale-specific (this whole thing is locale-specific though)
				if(c >= 'A' && c <= 'Z')
					c += 'a' - 'A';
				
				int charHash = HASH_MODE_VOWELS_CONSONANTS.hashCodeFor(c); 
				// Homestuck colouring: vowels(1) = red, consonants(2) = blue (we use aqua to increase contrast with the background)
				Formatting charFormat = (charHash == 0 ? Formatting.WHITE : charHash == 1 ? Formatting.RED : charHash == 2 ? Formatting.AQUA : Formatting.GRAY);
				if(charFormat != lastFormatting && c != ' ') { // colour doesn't matter for spaces so don't insert colour codes for them
					sb.append(charFormat);
					lastFormatting = charFormat;
				}
				sb.append(s.charAt(k)); // original case, not lowercased version
				hash += charHash;
			}
			
			tooltip.set(0, sb.toString());
			// TODO: what if other mods add other stuff into the first line...?
			tooltip.add(1, Formatting.GRAY+"Hash slot: "+(hash % NUM_HOTBAR_SLOTS));
		}
	}
}