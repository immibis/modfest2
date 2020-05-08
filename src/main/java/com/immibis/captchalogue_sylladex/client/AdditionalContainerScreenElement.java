package com.immibis.captchalogue_sylladex.client;

import com.immibis.captchalogue_sylladex.FetchModusGuiState;
import com.immibis.captchalogue_sylladex.mixin_support.IContainerMixin;
import com.immibis.captchalogue_sylladex.mixin_support.IContainerScreenMixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.Container;

@Environment(EnvType.CLIENT)
public class AdditionalContainerScreenElement implements Element {
	//private Container cont;
	//private IContainerMixin contAsMixin;
	private ContainerScreen screen;
	private IContainerScreenMixin screenAsMixin;
	public AdditionalContainerScreenElement(ContainerScreen<?> screen) {
		this.screen = screen;
		this.screenAsMixin = (IContainerScreenMixin)screen;
		//cont = screen.getContainer();
		//contAsMixin = (IContainerMixin)cont;
	}
	
	private FetchModusGuiState getGuiHandler() {
		Container cont = screen.getContainer();
		if(cont == null)
			return FetchModusGuiState.NULL_GUI_STATE;
		return ((IContainerMixin)cont).getFetchModusGuiState();
	}
	
	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		getGuiHandler().mouseMoved(mouseX - screenAsMixin.captchalogue_getGuiX(), mouseY - screenAsMixin.captchalogue_getGuiY());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return getGuiHandler().mouseClicked(mouseX - screenAsMixin.captchalogue_getGuiX(), mouseY - screenAsMixin.captchalogue_getGuiY(), button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return getGuiHandler().mouseReleased(mouseX - screenAsMixin.captchalogue_getGuiX(), mouseY - screenAsMixin.captchalogue_getGuiY(), button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return getGuiHandler().mouseDragged(mouseX - screenAsMixin.captchalogue_getGuiX(), mouseY - screenAsMixin.captchalogue_getGuiY(), button, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		return getGuiHandler().mouseScrolled(mouseX - screenAsMixin.captchalogue_getGuiX(), mouseY - screenAsMixin.captchalogue_getGuiY(), amount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		//return getGuiHandler().mouseClicked(mouseX - screenAsMixin.captchalogue_getGuiX(), mouseY - screenAsMixin.captchalogue_getGuiY(), button);
		return false;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		//return getGuiHandler().mouseClicked(mouseX - screenAsMixin.captchalogue_getGuiX(), mouseY - screenAsMixin.captchalogue_getGuiY(), button);
		return false;
	}

	@Override
	public boolean charTyped(char chr, int keyCode) {
		return false;
	}

	@Override
	public boolean changeFocus(boolean lookForwards) {
		return false;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return getGuiHandler().isMouseOver(mouseX, mouseY);
	}
}
