package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import compuglobalhypermeganet.captchalogue.FetchModus;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
	
	@Shadow
	public ServerPlayerEntity player;
	
	@Inject(at = @At("HEAD"), method="onCreativeInventoryAction(Lnet/minecraft/network/packet/c2s/play/CreativeInventoryActionC2SPacket;)V")
	public void wrapOnCreativeInventoryAction(CallbackInfo info) {
		if(player.getServerWorld().getServer().isOnThread()) { // otherwise the method just delegates to the main thread and doesn't really do anything
			FetchModus.isProcessingPacket.set(Boolean.TRUE);
		}
	}
	
	@Inject(at = @At("RETURN"), method="onCreativeInventoryAction(Lnet/minecraft/network/packet/c2s/play/CreativeInventoryActionC2SPacket;)V")
	public void wrapOnCreativeInventoryAction2(CallbackInfo info) {
		if(player.getServerWorld().getServer().isOnThread()) { // otherwise the method just delegates to the main thread and doesn't really do anything
			FetchModus.isProcessingPacket.set(Boolean.FALSE);
		}
	}
}
