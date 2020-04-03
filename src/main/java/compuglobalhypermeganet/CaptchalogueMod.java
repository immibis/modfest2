package compuglobalhypermeganet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNumberTag;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class CaptchalogueMod implements ModInitializer {
	
	public static final int MODUS_SLOT = 8;
	public static final Identifier MODUS_SLOT_BG_IMAGE = new Identifier("compuglobalhypermeganet", "placeholder_fetch_modus");
	public static final Identifier MEMORY_MODUS_QUESTION_MARK_IMAGE = new Identifier("compuglobalhypermeganet", "memory_game_unrevealed_slot");
	public static final Identifier MEMORY_MODUS_CROSS_IMAGE = new Identifier("compuglobalhypermeganet", "memory_game_empty_slot");
	
	
	public static final ItemGroup itemGroupCaptchalogue = FabricItemGroupBuilder.build(
			new Identifier("compuglobalhypermeganet", "captchalogue"),
			new Supplier<ItemStack>() {
				@Override
				public ItemStack get() {
					return new ItemStack(itemStackFetchModus);
				}
			});
	
	// basic structures (queuestack is still basic)
	public static final Item itemQueueFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemStackFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueuestackFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemTreeFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	
	// minigames
	public static final Item itemMemoryFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	
	// arrays of whatever
	public static final Item itemQueueArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemStackArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueuestackArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemTreeArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	// array of hashtables is functionally equivalent to hashtable of arrays
	
	// hashtables with collision avoidance
	public static final Item itemArrayHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemStackHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueueHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueuestackHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemTreeHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));

	public static final List<Item> DEFAULT_MODUSES = Arrays.asList(
		itemStackFetchModus,
		itemTreeFetchModus,
		itemHashtableFetchModus
	);
	
	@Override
	public void onInitialize() {
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queue_fetch_modus"), itemQueueFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "stack_fetch_modus"), itemStackFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "array_fetch_modus"), itemArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queuestack_fetch_modus"), itemQueuestackFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "hashtable_fetch_modus"), itemHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "memory_fetch_modus"), itemMemoryFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "tree_fetch_modus"), itemTreeFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queue_array_fetch_modus"), itemQueueArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "stack_array_fetch_modus"), itemStackArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queuestack_array_fetch_modus"), itemQueuestackArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "tree_array_fetch_modus"), itemTreeArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "array_hashtable_fetch_modus"), itemArrayHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queue_hashtable_fetch_modus"), itemQueueHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queuestack_hashtable_fetch_modus"), itemQueuestackHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "tree_hashtable_fetch_modus"), itemTreeHashtableFetchModus);
	}
	
	public static int compareItemsForTree(ItemStack a, ItemStack b) {
		int res = a.getName().getString().compareTo(b.getName().getString());
		if(res != 0) return res;
		// As many tie breakers as possible. Compare the entire NBT tag.
		CompoundTag cta = a.getTag();
		CompoundTag ctb = b.getTag();
		return compareNBT(cta, ctb);
	}
	
	private static AtomicBoolean printedCompareNBTUnknownTagWarning = new AtomicBoolean(false);
	private static AtomicBoolean printedCompareNBTEndTagWarning = new AtomicBoolean(false);
	private static int compareNBT(Tag a, Tag b) {
		// null < anything
		if(a == null) {
			if(b == null)
				return 0; // a == b
			return -1; // a < b
		}
		if(b == null)
			return 1; // a > b
		
		int res = Byte.compare(a.getType(), b.getType());
		if(res != 0) return res;
		
		// 0: end
		// 1: byte
		// 2: short
		// 3: int
		// 4: long
		// 5: float
		// 6: double
		// 7: byte array
		// 8: string
		// 9: list
		// 10: compound
		// 11: int array
		// 12: long array
		
		switch(a.getType()) {
		case 0: // end
			if(!printedCompareNBTEndTagWarning.getAndSet(true)) {
				new IllegalArgumentException("NBT end tag shouldn't occur in NBT tree").printStackTrace();
			}
			return 0; // a == b
		case 1: // byte
		case 2: // short
		case 3: // int
			return Integer.compare(((AbstractNumberTag)a).getInt(), ((AbstractNumberTag)b).getInt());
		case 4: // long
			return Long.compare(((LongTag)a).getLong(), ((LongTag)b).getLong());
		case 5: // float
			return Float.compare(((FloatTag)a).getFloat(), ((FloatTag)b).getFloat());
		case 6: // float
			return Double.compare(((DoubleTag)a).getDouble(), ((DoubleTag)b).getDouble());
		case 7: // byte array
			return compareByteArray(((ByteArrayTag)a).getByteArray(), ((ByteArrayTag)b).getByteArray());
		case 8: // string
			return ((StringTag)a).asString().compareTo(((StringTag)b).asString());
		case 9: // list
			{
				ListTag la = (ListTag)a;
				ListTag lb = (ListTag)b;
				if(la.getElementType() != lb.getElementType())
					return Integer.compare(la.getElementType(), lb.getElementType());
				// TODO: since the sort order doesn't need to be particularly meaningful, why don't we just compare the size first before comparing the values?
				for(int k = 0; k < la.size() && k < lb.size(); k++) {
					res = compareNBT(la.get(k), lb.get(k));
					if(res != 0)
						return res;
				}
				return Integer.compare(la.size(), lb.size());
			}
		case 10: // compound
			{
				CompoundTag ca = (CompoundTag)a;
				CompoundTag cb = (CompoundTag)b;
				List<String> keys = new ArrayList<>(ca.getKeys().size() + cb.getKeys().size());
				// TODO: since the sort order doesn't need to be particularly meaningful, why don't we just compare the number of keys, then the keys themselves, before comparing all the child values?
				keys.addAll(ca.getKeys());
				keys.addAll(cb.getKeys());
				Collections.sort(keys);
				for(int k = 0; k < keys.size(); k++) {
					if(k > 0 && keys.get(k).equals(keys.get(k-1)))
						continue;
					Tag ta = ca.get(keys.get(k));
					Tag tb = cb.get(keys.get(k));
					res = compareNBT(ta, tb);
					if(res != 0)
						return res;
				}
				return 0;
			}
		case 11: // int array
			return compareIntArray(((IntArrayTag)a).getIntArray(), ((IntArrayTag)b).getIntArray());
		case 12: // long array
			return compareLongArray(((LongArrayTag)a).getLongArray(), ((LongArrayTag)b).getLongArray());
			
		default:
			if(!printedCompareNBTUnknownTagWarning.getAndSet(true)) {
				new Exception("Encountered unknown NBT tag type "+a.getType()+". NBT tags might not sort correctly in tree moduses.");
			}
			return 0; // a == b
		}
	}
	
	private static int compareByteArray(byte[] a, byte[] b) {
		int max = Math.min(a.length, b.length);
		for(int k = 0; k < max; k++)
			if(a[k] != b[k])
				return Byte.compare(a[k], b[k]);
		return Integer.compare(a.length, b.length);
	}
	
	private static int compareIntArray(int[] a, int[] b) {
		int max = Math.min(a.length, b.length);
		for(int k = 0; k < max; k++)
			if(a[k] != b[k])
				return Integer.compare(a[k], b[k]);
		return Integer.compare(a.length, b.length);
	}
	
	private static int compareLongArray(long[] a, long[] b) {
		int max = Math.min(a.length, b.length);
		for(int k = 0; k < max; k++)
			if(a[k] != b[k])
				return Long.compare(a[k], b[k]);
		return Integer.compare(a.length, b.length);
	}
	
	// Overwritten in client mod initializer
	public static BiConsumer<World, Runnable> executeLaterOnClientWorld = (World w, Runnable r) -> {throw new RuntimeException("client world on a dedicated server?! "+w);};

	public static void executeLater(World world, Runnable runnable) {
		MinecraftServer server = world.getServer();
		// send(), not execute(). execute() will execute the task immediately if possible, but we MUST queue it for later.
		if(server != null)
			server.send(new ServerTask(server.getTicks(), runnable));
		else {
			if(!world.isClient())
				throw new RuntimeException("World isn't server world or client world?! "+world);
			executeLaterOnClientWorld.accept(world, runnable);
		}
	}
}
