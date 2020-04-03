package compuglobalhypermeganet.captchalogue.mixin;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import compuglobalhypermeganet.captchalogue.FetchModusType;
import compuglobalhypermeganet.captchalogue.mixin_support.IClickWindowC2SPacketMixin;
import net.minecraft.network.packet.c2s.play.ClickWindowC2SPacket;
import net.minecraft.util.PacketByteBuf;

@Mixin(ClickWindowC2SPacket.class)
public class ClickWindowC2SPacketMixin implements IClickWindowC2SPacketMixin {
	/** Used as random seed for Memory modus */
	public int fetchModusState;
	
	@Inject(at=@At("RETURN"), method="<init>*")
	public void onInit(CallbackInfo info) {
		fetchModusState = FetchModusType.currentPacketFetchModusState.get();
	}
	
	@Override
	public int captchalogue_getFetchModusState() {
		return fetchModusState;
	}
	
	@Inject(at=@At("RETURN"), method="read(Lnet/minecraft/util/PacketByteBuf;)V")
	public void additionalRead(PacketByteBuf buf, CallbackInfo info) throws IOException {
		this.fetchModusState = buf.readInt();
	}
	
	@Inject(at=@At("RETURN"), method="write(Lnet/minecraft/util/PacketByteBuf;)V")
	public void additionalWrite(PacketByteBuf buf, CallbackInfo info) throws IOException {
		buf.writeInt(fetchModusState);
	}
}
