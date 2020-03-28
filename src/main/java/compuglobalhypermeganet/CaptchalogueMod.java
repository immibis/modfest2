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
	
	public static final ItemGroup itemGroupCaptchalogue = FabricItemGroupBuilder.build(
			new Identifier("compuglobalhypermeganet", "captchalogue"),
			() -> new ItemStack(Blocks.COBBLESTONE));
	
	public static final Item itemQueueFetchModus = new Item(new Item.Settings().group(itemGroupCaptchalogue));
	
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		System.out.println("Hello Fabric world!");
		
		Registry.register(Registry.ITEM, new Identifier("compuglobalhypermeganet", "queue_fetch_modus"), itemQueueFetchModus);
	}
}
