package compuglobalhypermeganet.captchalogue.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import compuglobalhypermeganet.captchalogue.mixin_support.IContainerScreenMixin;
import net.minecraft.client.gui.screen.Screen;

@Mixin(Screen.class)
public class ScreenMixin {
	@ModifyArg(at = @At(value="INVOKE", target="Lnet/minecraft/client/gui/screen/Screen;renderTooltip(Ljava/util/List;II)V"), method="renderTooltip(Lnet/minecraft/item/ItemStack;II)V", index=0)
	public List<String> overrideItemRenderTooltip(List<String> itemRenderTooltip) {
		if(this instanceof IContainerScreenMixin)
			((IContainerScreenMixin)this).captchalogue_fiddleWithItemRenderTooltip(itemRenderTooltip);
		return itemRenderTooltip;
	}
}
