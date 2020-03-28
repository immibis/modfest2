package compuglobalhypermeganet.captchalogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.container.Container;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public abstract class FetchModus {
	
	public static class Queue extends FetchModus {
		public List<ItemStack> items = new ArrayList<>(0); // ArrayList even though it's a queue, because we do a lot more random access than deletion.
		@Override
		protected void deserialize(List<ItemStack> items, CompoundTag tag) {
			// Queue is used as a last resort when the correct modus can't be loaded, so we may have empty itemstacks in the list being deserialized,
			// even though Queue can't add those. Remove them.
			items.removeIf(ItemStack::isEmpty);
			this.items = items;
		}
		@Override
		protected void serialize(List<ItemStack> items, CompoundTag tag) {
			items.addAll(this.items);
		}
		@Override
		protected String getSerializedType() {
			return "queue";
		}
		
		@Override
		public boolean canTakeFromSlot(int slot) {
			return slot == 0;
		}
		@Override
		public boolean canInsertToSlot(int slot) {
			return slot >= items.size(); // any slot past the end is counted as the end
		}
		@Override
		public ItemStack getStackInSlot(int slot) {
			return slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
		}
		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			if (stack.isEmpty()) {
				// Should only be slot 0, but just in case
				if (slot < items.size())
					items.remove(slot);
				return;
			}
			if (slot == 0 && items.size() > 0)
				items.set(0, stack);
			else {
				if (items.size() > 0) {
					ItemStack existingLast = items.get(items.size() - 1);
					if (Container.canStacksCombine(stack, existingLast)) {
						int ntransfer = Math.max(stack.getMaxCount() - existingLast.getCount(), stack.getCount());
						if (ntransfer > 0) {
							existingLast.increment(ntransfer);
							stack.decrement(ntransfer);
							if (stack.getCount() <= 0)
								return;
						}
					}
				}
				items.add(stack);
			}
		}
		@Override
		public ItemStack takeItemsFromSlot(int slot, int count) {
			if (slot > 0)
				return ItemStack.EMPTY;
			return Inventories.splitStack(items, slot, count);
		}
	}

	protected abstract String getSerializedType();
	protected abstract void serialize(List<ItemStack> items, CompoundTag tag);
	protected abstract void deserialize(List<ItemStack> items, CompoundTag tag);
	public abstract boolean canTakeFromSlot(int slot);
	public abstract boolean canInsertToSlot(int slot);
	public abstract ItemStack getStackInSlot(int slot);
	public abstract void setStackInSlot(int slot, ItemStack stack);
	public abstract ItemStack takeItemsFromSlot(int slot, int count);
	
	public static CompoundTag serialize(FetchModus fetchModus) {
		CompoundTag specificTag = new CompoundTag();
		List<ItemStack> items = new ArrayList<>(0);
		fetchModus.serialize(items, specificTag);
		
		ListTag itemsTag = new ListTag();
		for(int k = 0; k < items.size(); k++) {
			if(!items.get(k).isEmpty()) {
				CompoundTag itemTag = new CompoundTag();
				itemTag.putInt("Slot", k);
				items.get(k).toTag(itemTag);
				itemsTag.add(itemTag);
			}
		}
		
		CompoundTag outerTag = new CompoundTag();
		outerTag.put("Specific", specificTag);
		outerTag.putInt("ItemsCount", items.size());
		outerTag.put("Items", itemsTag);
		return specificTag;
	}

	public static FetchModus deserialize(CompoundTag tag) {
		String type = tag.getString("Type");
		
		List<ItemStack> items = new ArrayList<>(tag.getInt("ItemsCount"));
		ListTag itemsTag = tag.getList("Items", 10 /*compound*/);
		Collections.fill(items, ItemStack.EMPTY);
		for(int k = 0; k < itemsTag.size(); k++) {
			CompoundTag itemTag = itemsTag.getCompound(k);
			int slot = itemTag.getInt("Slot");
			if (slot < 0 || slot >= items.size())
				new Exception("corrupted FetchModus serialization (out-of-range slot "+slot+"/"+items.size()+")").printStackTrace();
			else
				items.set(slot, ItemStack.fromTag(itemTag));
		}
		
		CompoundTag specificData = tag.getCompound("Specific");
		
		FetchModus modus;
		if(type.equals("queue")) {
			modus = new Queue();
		} else {
			new Exception("Unrecognized FetchModus type: "+type+". Resetting to default...");
			modus = new Queue();
			specificData = new CompoundTag(); // don't try to load unknown Specific data as queue. Queue has no Specific data anyway
		}
		
		modus.deserialize(items, specificData);
		return modus;
	}
}
