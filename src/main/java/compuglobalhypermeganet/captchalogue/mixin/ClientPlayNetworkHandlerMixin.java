package compuglobalhypermeganet.captchalogue.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import compuglobalhypermeganet.captchalogue.FetchModusType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	
	@Shadow
	private MinecraftClient client;
	
	@Inject(at = @At("HEAD"), method="onContainerSlotUpdate(Lnet/minecraft/network/packet/s2c/play/ContainerSlotUpdateS2CPacket;)V")
	public void wrapOnContainerSlotUpdate(CallbackInfo info) {
		if(client.isOnThread()) { // otherwise the method just delegates to the main thread and doesn't really do anything
			FetchModusType.isProcessingPacket.set(Boolean.TRUE);
		}
	}
	
	@Inject(at = @At("RETURN"), method="onContainerSlotUpdate(Lnet/minecraft/network/packet/s2c/play/ContainerSlotUpdateS2CPacket;)V")
	public void wrapOnContainerSlotUpdate2(CallbackInfo info) {
		if(client.isOnThread()) { // otherwise the method just delegates to the main thread and doesn't really do anything
			FetchModusType.isProcessingPacket.set(Boolean.FALSE);
		}
	}
	
	@Inject(at = @At("HEAD"), method="onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V")
	public void wrapOnInventory(CallbackInfo info) {
		if(client.isOnThread()) { // otherwise the method just delegates to the main thread and doesn't really do anything
			FetchModusType.isProcessingPacket.set(Boolean.TRUE);
		}
	}
	
	@Inject(at = @At("RETURN"), method="onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V")
	public void wrapOnInventory2(CallbackInfo info) {
		if(client.isOnThread()) { // otherwise the method just delegates to the main thread and doesn't really do anything
			FetchModusType.isProcessingPacket.set(Boolean.FALSE);
		}
	}
}
