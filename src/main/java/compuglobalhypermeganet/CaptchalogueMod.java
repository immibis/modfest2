package compuglobalhypermeganet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import compuglobalhypermeganet.captchalogue.FetchModusHashtable;
import compuglobalhypermeganet.captchalogue.FetchModusState;
import compuglobalhypermeganet.captchalogue.mixin_support.IPlayerInventoryMixin;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.loot.v1.FabricLootPoolBuilder;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.UniformLootTableRange;
import net.minecraft.loot.entry.ItemEntry;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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
	public static final Item itemStackFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(64)); // Stack moduses stack, because lame pun.
	public static final Item itemArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueuestackFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemTreeLeafFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemTreeRootFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	
	// minigames
	public static final Item itemMemoryFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	
	// arrays of whatever
	public static final Item itemQueueArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemStackArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueuestackArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	// array of hashtables is functionally equivalent to hashtable of arrays
	
	// hashtables with collision avoidance
	public static final Item itemArrayHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemStackHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueueHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueuestackHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	
	public static final List<Item> DEFAULT_MODUSES = Arrays.asList(
		itemStackFetchModus,
		itemTreeLeafFetchModus,
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
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "tree_leaf_fetch_modus"), itemTreeLeafFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "tree_root_fetch_modus"), itemTreeRootFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queue_array_fetch_modus"), itemQueueArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "stack_array_fetch_modus"), itemStackArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queuestack_array_fetch_modus"), itemQueuestackArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "array_hashtable_fetch_modus"), itemArrayHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queue_hashtable_fetch_modus"), itemQueueHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "stack_hashtable_fetch_modus"), itemStackHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queuestack_hashtable_fetch_modus"), itemQueuestackHashtableFetchModus);
		
		LootTableLoadingCallback.EVENT.register((resourceManager, lootManager, id, supplier, setter) -> {
			if(id.getNamespace().equals("minecraft") && id.getPath().equals("chests/simple_dungeon")) {
				FabricLootPoolBuilder poolBuilder = FabricLootPoolBuilder.builder()
						.withRolls(UniformLootTableRange.between(1.0f, 2.0f));
				
				// 100% total chance
				// 50%: memory
				// 26%: array (main way to get Array!)
				// 6%: {hashtable | tree | stack}
				// 18%: {hashtable of arrays | array of stacks | array of queues}
				poolBuilder.withEntry(ItemEntry.builder(itemMemoryFetchModus).setWeight(50));
				poolBuilder.withEntry(ItemEntry.builder(itemArrayFetchModus).setWeight(26));
				
				poolBuilder.withEntry(ItemEntry.builder(itemHashtableFetchModus).setWeight(2));
				poolBuilder.withEntry(ItemEntry.builder(itemTreeLeafFetchModus).setWeight(2));
				poolBuilder.withEntry(ItemEntry.builder(itemStackFetchModus).setWeight(2));
				
				poolBuilder.withEntry(ItemEntry.builder(itemStackHashtableFetchModus).setWeight(6));
				poolBuilder.withEntry(ItemEntry.builder(itemStackArrayFetchModus).setWeight(6));
				poolBuilder.withEntry(ItemEntry.builder(itemArrayHashtableFetchModus).setWeight(6));
				
				supplier.withPool(poolBuilder);
			}
		});
	}
	
	
	
	public static int hashItem(ItemStack a, FetchModusHashtable.HashMode hashMode) {
		String string = a.getName().getString().toLowerCase(Locale.ROOT);
		int hash = 0;
		for(int k = 0; k < string.length(); k++)
			hash += hashMode.hashCodeFor(string.charAt(k));
		return hash;
	}
	
	public static int compareItemsForTree(ItemStack a, ItemStack b) {
		// case-insensitive name comparison
		int res = a.getName().getString().toLowerCase(Locale.ROOT).compareTo(b.getName().getString().toLowerCase(Locale.ROOT));
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

	public static void launchExcessItems(PlayerEntity player, ItemStack stack) {
		if(!player.world.isClient() && !stack.isEmpty()) {
			final int RANGE = 10;
			Vec3d playerPos = player.getPos().add(0, player.getEyeHeight(player.getPose()), 0);
			Box searchBox = new Box(playerPos.getX()-RANGE, playerPos.getY()-RANGE, playerPos.getZ()-RANGE, playerPos.getX()+RANGE, playerPos.getY()+RANGE, playerPos.getZ()+RANGE);
			
			List<Entity> entsInRange = player.world.getEntities(player, searchBox, (ent) -> {
				return ent.isAttackable() && !ent.isSpectator() && ent.isAlive();
			});
			
			Entity closest = null;
			/*double closestDistSq = RANGE*RANGE;
			for(Entity ent : entsInRange) {
				double distSq = ent.getPos().squaredDistanceTo(playerPos);
				if(distSq < closestDistSq) {
					closestDistSq = distSq;
					closest = ent;
				}
			}*/
			closest = (entsInRange.isEmpty() ? null : entsInRange.get(player.world.random.nextInt(entsInRange.size())));
			
			ItemEntity ent = new ItemEntity(player.world, playerPos.getX(), playerPos.getY(), playerPos.getZ(), stack.copy());
			
			final double SPEED = 0.3;
			if (closest == null) {
				// Pick a random horizontal direction, plus or minus a little bit of vertical.
				double dir = player.world.random.nextDouble()*Math.PI*2;
				ent.setVelocity(Math.sin(dir)*SPEED, 0.2, Math.cos(dir)*SPEED);
			} else {
				Vec3d delta = closest.getPos().add(0, closest.getBodyY(0.5f)-closest.getY(), 0).subtract(playerPos).multiply(SPEED);
				ent.setVelocity(delta);
				
				// Too lazy to make an item projectile entity that hurts on impact and can be picked up. Just hurt the target entity directly and immediately.
				closest.damage(DamageSource.ANVIL, (int)Math.ceil(Math.sqrt(stack.getCount())));
			}
			ent.setPickupDelay(20);
			player.world.spawnEntity(ent);
			
			
		}
		stack.setCount(0); // caller shouldn't use the stack any more, but just in case
	}


	private static boolean checkTriggerHashCodeProbability(Random random, int numExclamationMarks, int numUppercaseLetters, int numLowercaseLetters, int segmentLength) {
		if (numUppercaseLetters == 0 && numLowercaseLetters == 0)
			return false;
		if (segmentLength - numExclamationMarks > 15)
			return false;
		int prob;
		if (numUppercaseLetters > numLowercaseLetters*2) {
			prob = 2;
			if(numExclamationMarks > 0) prob += 2;
			prob += 2*numExclamationMarks;
		} else {
			prob = 0;
			prob += 2*numExclamationMarks;
		}
		return prob >= 10 || random.nextInt(10) < prob;
	}
	
	private static void triggerHashtableChatMessageEject(PlayerEntity player, int hashCode) {
		hashCode %= FetchModusHashtable.NUM_HOTBAR_SLOTS;
		ItemStack stack = player.inventory.getInvStack(hashCode);
		if(!stack.isEmpty()) {
			player.inventory.setInvStack(hashCode, ItemStack.EMPTY);
			launchExcessItems(player, stack);
		}
	}

	public static void triggerHashCodesForChatMessage(String message, PlayerEntity player) {
		FetchModusState modus = ((IPlayerInventoryMixin)player.inventory).getFetchModus();
		if(!(modus instanceof FetchModusHashtable.State))
			return;
		
		// XXX hardcoded
		FetchModusHashtable.HashMode hashMode = FetchModusHashtable.HASH_MODE_VOWELS_CONSONANTS;
		
		// Probability:
		
		// AAAA!!! -> 100%
		// AAAA!! -> 80%
		// AAAA! -> 60%
		// AAAA -> 20%
		// aaaa! -> 10%
		// aaaa!!! -> 30%
		// aaaa!!!!!!!!!! -> 50%
		// aaaa -> 0%
		// !!! -> 0%
		
		// Mixed uppercase and lowercase counts as uppercase if more than 2/3 of characters are uppercase.
		// Lowercase segments: start at 0%, add 10% for each exclamation mark, to a maximum of 50%.
		// Uppercase segments: start at 20%, add 20% if any exclamation marks, add 20% for each exclamation mark, to a maximum of 100%.
		
		// Also, the segments have to be short. Anything over 15 characters, excluding exclamation marks, is disqualified.

		int currentSegmentStart = 0;
		int numUppercaseLetters = 0;
		int numLowercaseLetters = 0;
		int numExclamationMarks = 0;
		int segmentHashCode = 0;
		for(int k = 0; k < message.length(); k++) {
			char c = message.charAt(k);
			if(c == '!')
				numExclamationMarks++;
			else {
				if (numExclamationMarks > 0) {
					if (checkTriggerHashCodeProbability(player.world.random, numExclamationMarks, numUppercaseLetters, numLowercaseLetters, k - currentSegmentStart)) {
						triggerHashtableChatMessageEject(player, segmentHashCode);
					}
					currentSegmentStart = k;
					segmentHashCode = numUppercaseLetters = numLowercaseLetters = numExclamationMarks = 0;
				}
				if(c >= 'a' && c <= 'z')
					numLowercaseLetters++;
				if(c >= 'A' && c <= 'Z') {
					numUppercaseLetters++;
					segmentHashCode += hashMode.hashCodeFor((char)(c + 'a' - 'A'));
				} else
					segmentHashCode += hashMode.hashCodeFor(c);
			}
		}
		if (checkTriggerHashCodeProbability(player.world.random, numExclamationMarks, numUppercaseLetters, numLowercaseLetters, message.length() - currentSegmentStart)) {
			triggerHashtableChatMessageEject(player, segmentHashCode);
		}
	}
}
