package com.immibis.captchalogue_sylladex.mixin_support;

import net.minecraft.container.Container;

public interface ISlotMixin {
	public int captchalogue_getSlotNum();
	public void captchalogue_setPosition(int x, int y);
	public int captchalogue_getOriginalXPosition();
	public int captchalogue_getOriginalYPosition();
	public Container captchalogue_getContainer(); // can be null if not known yet
	public void captchalogue_setContainer(Container cont);
}
