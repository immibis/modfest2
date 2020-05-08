package com.immibis.captchalogue_sylladex.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.immibis.captchalogue_sylladex.CaptchalogueMod;
import com.immibis.captchalogue_sylladex.FetchModusType;
import com.immibis.captchalogue_sylladex.mixin_support.IClickWindowC2SPacketMixin;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickWindowC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
	
	@Shadow
	public ServerPlayerEntity player;
	
	@Inject(at = @At("HEAD"), method="onCreativeInventoryAction(Lnet/minecraft/network/packet/c2s/play/CreativeInventoryActionC2SPacket;)V")
	public void wrapOnCreativeInventoryAction(CallbackInfo info) {
		if(player.getServerWorld().getServer().isOnThread()) { // otherwise the method just delegates to the main thread and doesn't really do anything
			FetchModusType.isProcessingPacket.set(Boolean.TRUE);
		}
	}
	
	@Inject(at = @At("RETURN"), method="onCreativeInventoryAction(Lnet/minecraft/network/packet/c2s/play/CreativeInventoryActionC2SPacket;)V")
	public void wrapOnCreativeInventoryAction2(CallbackInfo info) {
		if(player.getServerWorld().getServer().isOnThread()) { // otherwise the method just delegates to the main thread and doesn't really do anything
			FetchModusType.isProcessingPacket.set(Boolean.FALSE);
		}
	}
	
	@Inject(at = @At("HEAD"), method="onClickWindow(Lnet/minecraft/network/packet/c2s/play/ClickWindowC2SPacket;)V")
	public void extendOnClickWindow(ClickWindowC2SPacket packet, CallbackInfo info) {
		if(player.getServerWorld().getServer().isOnThread()) { // otherwise the method just delegates to the main thread and doesn't really do anything
			FetchModusType.currentPacketFetchModusState.set(((IClickWindowC2SPacketMixin)packet).captchalogue_getFetchModusState());
		}
	}
	
	@Inject(at = @At("HEAD"), method="onChatMessage(Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;)V")
	public void extendOnChatMessage(ChatMessageC2SPacket packet, CallbackInfo info) {
		if(player.getServerWorld().getServer().isOnThread()) { // otherwise the method just delegates to the main thread and doesn't really do anything
			// TODO: only trigger the modus if the message isn't blocked, etc. Maybe save the message and player reference here, and wait until server.broadcastMessage is called before assuming it actually went through.
			CaptchalogueMod.triggerHashCodesForChatMessage(packet.getChatMessage(), player);
		}
	}
}
