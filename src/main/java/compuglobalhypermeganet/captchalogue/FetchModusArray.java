package compuglobalhypermeganet.captchalogue;

import net.minecraft.item.ItemStack;

public class FetchModusArray extends FetchModusType {
	@Override
	public FetchModusState createFetchModusState(InventoryWrapper inv) {
		return State.instance;
	}
	
	public static class State extends FetchModusState {

		public static State instance = new State();
		
		@Override
		public boolean canInsertToSlot(int slot) {
			return true;
		}

		@Override
		public boolean canTakeFromSlot(int slot) {
			return true;
		}

		@Override
		public boolean hasCustomInsert() {
			return false;
		}

		@Override
		public void insert(ItemStack stack) {
			throw new AssertionError("unreachable");
		}
		
	}
	
	@Override
	public boolean forceRightClickOneItem() {
		return false;
	}
}