package compuglobalhypermeganet;

import java.util.function.BiConsumer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class CaptchalogueModClient implements ClientModInitializer {
	
	@Override
	public void onInitializeClient() {
		ClientSpriteRegistryCallback.event(SpriteAtlasTexture.BLOCK_ATLAS_TEX).register(new ClientSpriteRegistryCallback() {
			@Override
			public void registerSprites(SpriteAtlasTexture atlasTexture, Registry registry) {
				registry.register(CaptchalogueMod.MODUS_SLOT_BG_IMAGE);
				registry.register(CaptchalogueMod.MEMORY_MODUS_QUESTION_MARK_IMAGE);
				registry.register(CaptchalogueMod.MEMORY_MODUS_CROSS_IMAGE);
			}
		});
		
		CaptchalogueMod.executeLaterOnClientWorld = new BiConsumer<World, Runnable>() {
			@Override
			public void accept(World world, Runnable task) {
				MinecraftClient.getInstance().send(task);
			}
		};
	}
}
