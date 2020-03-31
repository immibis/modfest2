package compuglobalhypermeganet;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class CaptchalogueMod implements ModInitializer {
	
	public static final int MODUS_SLOT = 8;
	public static final Identifier MODUS_SLOT_BG_IMAGE = new Identifier("compuglobalhypermeganet", "placeholder_fetch_modus");
	public static final Identifier MEMORY_MODUS_QUESTION_MARK_IMAGE = new Identifier("compuglobalhypermeganet", "memory_game_unrevealed_slot");
	public static final Identifier MEMORY_MODUS_CROSS_IMAGE = new Identifier("compuglobalhypermeganet", "memory_game_empty_slot");
	
	
	public static final ItemGroup itemGroupCaptchalogue = FabricItemGroupBuilder.build(
			new Identifier("compuglobalhypermeganet", "captchalogue"),
			() -> new ItemStack(Blocks.COBBLESTONE));
	
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
	public static final Item itemTreeArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueuestackArrayFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	// array of hashtables is functionally equivalent to hashtable of arrays
	
	// hashtables with collision avoidance
	public static final Item itemArrayHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemStackHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueueHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemQueuestackHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	public static final Item itemTreeHashtableFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue).maxCount(1));
	
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queue_fetch_modus"), itemQueueFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "stack_fetch_modus"), itemStackFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "array_fetch_modus"), itemArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queuestack_fetch_modus"), itemQueuestackFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "hashtable_fetch_modus"), itemHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "tree_fetch_modus"), itemMemoryFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queue_array_fetch_modus"), itemQueueArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "stack_array_fetch_modus"), itemStackArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queuestack_array_fetch_modus"), itemQueuestackArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "tree_array_fetch_modus"), itemTreeArrayFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "array_hashtable_fetch_modus"), itemArrayHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queue_hashtable_fetch_modus"), itemQueueHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queuestack_hashtable_fetch_modus"), itemQueuestackHashtableFetchModus);
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "tree_hashtable_fetch_modus"), itemTreeHashtableFetchModus);
	}
}
