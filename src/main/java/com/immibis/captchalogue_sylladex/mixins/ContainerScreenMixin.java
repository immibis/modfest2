package com.immibis.captchalogue_sylladex.mixins;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.immibis.captchalogue_sylladex.CaptchalogueMod;
import com.immibis.captchalogue_sylladex.FetchModusGuiState;
import com.immibis.captchalogue_sylladex.FetchModusState;
import com.immibis.captchalogue_sylladex.FetchModusType;
import com.immibis.captchalogue_sylladex.InventoryWrapper;
import com.immibis.captchalogue_sylladex.ModusRegistry;
import com.immibis.captchalogue_sylladex.client.AdditionalContainerScreenElement;
import com.immibis.captchalogue_sylladex.client.DrawerImpl;
import com.immibis.captchalogue_sylladex.mixin_support.IContainerMixin;
import com.immibis.captchalogue_sylladex.mixin_support.IContainerScreenMixin;
import com.immibis.captchalogue_sylladex.mixin_support.IPlayerInventoryMixin;
import com.immibis.captchalogue_sylladex.mixin_support.ISlotMixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

@Mixin(ContainerScreen.class)
@Environment(EnvType.CLIENT)
public abstract class ContainerScreenMixin extends Screen implements IContainerScreenMixin {
	
	// not used, just appeases the compiler
	protected ContainerScreenMixin(Text title) {
		super(title);
	}

	@Shadow
	protected int x;
	@Shadow
	protected int y;

	private boolean unsupportedLayout;
	private Rectangle inventoryRect;
	private Slot[] inventorySlots;
	private PlayerInventory inv;
	
	// updated every frame
	private FetchModusType modus;
	
	private boolean printedUnsupportedMessage;
	
	@Unique
	private void captchalogue_refreshInventoryLayout() {
		ContainerScreen<?> this_ = ((ContainerScreen<?>)(Object)this);
		if(inventorySlots == null)
			inventorySlots = new Slot[36];
		else
			Arrays.fill(inventorySlots, null);
		unsupportedLayout = false;
		this.inv = null;
		for(Slot s : this_.getContainer().slots) {
			if(s.inventory instanceof PlayerInventory) {
				if(this.inv != null && this.inv != s.inventory) {
					if (!printedUnsupportedMessage) {
						System.out.println("Captchalogue: can't understand this GUI. More than one player inventory in the GUI");
						printedUnsupportedMessage = true;
					}
					unsupportedLayout = true;
				}
				this.inv = (PlayerInventory)s.inventory;
				int slotNum = ((ISlotMixin)s).captchalogue_getSlotNum();
				if (slotNum >= 0 && slotNum < 36) {
					if (inventorySlots[slotNum] != null) {
						unsupportedLayout = true;
						if (!printedUnsupportedMessage) {
							System.out.println("Captchalogue: can't understand this GUI. Duplicate player slot "+slotNum);
							printedUnsupportedMessage = true;
						}
					}
					inventorySlots[slotNum] = s;
				}
				
				// if the container switches from supported to unsupported layout (e.g. creative inventory changing tabs), make sure to put all the slots back
				ISlotMixin asSlotMixin = (ISlotMixin)s;
				asSlotMixin.captchalogue_setPosition(asSlotMixin.captchalogue_getOriginalXPosition(), asSlotMixin.captchalogue_getOriginalYPosition());
			}
		}
		if(inv == null) {
			unsupportedLayout = true;
			if (!printedUnsupportedMessage) {
				printedUnsupportedMessage = true;
				System.out.println("Captchalogue: can't understand this GUI. No player inventory slots in the GUI");
			}
		}
		for(Slot s : inventorySlots)
			if(s == null) {
				unsupportedLayout = true;
				if (!printedUnsupportedMessage) {
					printedUnsupportedMessage = true;
					System.out.println("Captchalogue: can't understand this GUI. It doesn't have all the player inventory and hotbar slots.");
				}
			}
		if (unsupportedLayout)
			return; // no inventory rectangle detected - can't override inventory rectangle on this screen
		
		if (inventoryRect == null)
			inventoryRect = new Rectangle();
		
		// Check some ARBITRARY layout constraints to see if they do approximately form a grid.
		for(int y = 0; y < 4; y++)
			for(int x = 0; x < 9; x++) {
				int slotNum = x + (y == 3 ? 0 : (y + 1))*9; // adjust Y coordinate so hotbar is last row instead of first for these checks
				int belowSlotNum = y == 3 ? -1 : (x + (y == 2 ? 0 : y + 2)*9); // adjust Y coordinate so hotbar is last row instead of first for these checks
				int rightSlotNum = x == 8 ? -1 : (slotNum + 1);
				
				ISlotMixin here = (ISlotMixin)inventorySlots[slotNum];
				if (belowSlotNum >= 0) {
					ISlotMixin below = (ISlotMixin)inventorySlots[belowSlotNum];
					if (below.captchalogue_getOriginalYPosition() < here.captchalogue_getOriginalYPosition() + 14 || below.captchalogue_getOriginalYPosition() > here.captchalogue_getOriginalYPosition() + 40)
						unsupportedLayout = true;
					if (below.captchalogue_getOriginalXPosition() < here.captchalogue_getOriginalXPosition() - 4 || below.captchalogue_getOriginalXPosition() > here.captchalogue_getOriginalXPosition() + 4)
						unsupportedLayout = true;
				}
				if (rightSlotNum >= 0) {
					ISlotMixin right = (ISlotMixin)inventorySlots[rightSlotNum];
					if (right.captchalogue_getOriginalXPosition() < here.captchalogue_getOriginalXPosition() + 14 || right.captchalogue_getOriginalXPosition() > here.captchalogue_getOriginalXPosition() + 40)
						unsupportedLayout = true;
					if (right.captchalogue_getOriginalYPosition() < here.captchalogue_getOriginalYPosition() - 4 || right.captchalogue_getOriginalYPosition() > here.captchalogue_getOriginalYPosition() + 4)
						unsupportedLayout = true;
				}
			}
		
		if (unsupportedLayout) {
			if (!printedUnsupportedMessage) {
				printedUnsupportedMessage = true;
				System.out.println("Captchalogue: Can't understand this GUI. The inventory doesn't seem to form a rectangle, or the inventory slots are too far apart or overlapping.");
			}
			return;
		}
		
		int t = Integer.MAX_VALUE, l = Integer.MAX_VALUE, b = Integer.MIN_VALUE, r = Integer.MIN_VALUE;
		for(Slot s : inventorySlots) {
			// include the traditional 1 pixel border
			int st = ((ISlotMixin)s).captchalogue_getOriginalYPosition() - 1, sl = ((ISlotMixin)s).captchalogue_getOriginalXPosition() - 1, sb = st + 18, sr = sl + 18;
			if(t > st) t = st;
			if(l > sl) l = sl;
			if(b < sb) b = sb;
			if(r < sr) r = sr;
		}
		inventoryRect.setBounds(l, t, r-l, b-t);
	}
	
	@Override
	public void captchalogue_invalidateLayout() {
		inventoryLayoutCheckFrames = 0;
	}
	
	@Unique
	private int captchalogue_getSlotAtVisualPosition(int x, int y) {
		if (y < 0 || y > 3 || x < 0 || x > 8)
			return -1;
		// make the hotbar y=3 instead of the slot order y=0
		if(y == 3)
			y = 0;
		else
			y++;
		return x + y*9; // not null, because unsupportedLayout is false
	}
	
	@Unique
	private FetchModusGuiState getFetchModusGuiState() {
		return ((IContainerMixin)((ContainerScreen<?>)(Object)this).getContainer()).getFetchModusGuiState();
	}
	
	@Unique private int inventoryLayoutCheckFrames = 0;
	@Unique private int mouseX, mouseY;
	
	@Inject(at=@At(value="HEAD"), method="render(IIF)V")
	public void onBeforeDraw(int mouseX, int mouseY, float deltaTime, CallbackInfo info) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		
		//System.out.println("onBeforeRender");
		if(inventoryLayoutCheckFrames <= 1) {
			captchalogue_refreshInventoryLayout();
			inventoryLayoutCheckFrames = 120;
		} else {
			inventoryLayoutCheckFrames--;
		}
		modus = ModusRegistry.getModus(inv);
	
		if (unsupportedLayout)
			return;
		
		FetchModusGuiState gs = getFetchModusGuiState();
		if(gs.area == null)
			gs.area = new Rectangle2D.Double();
		gs.area.setRect(this.inventoryRect);
		
		gs.onBeforeDraw(this);
	}
	
	@Inject(at=@At(value="INVOKE", shift=At.Shift.AFTER, target="Lnet/minecraft/client/gui/screen/ingame/ContainerScreen;drawBackground(FII)V"), method="render(IIF)V")
	public void onAfterDrawBackground(CallbackInfo info) {
		if (unsupportedLayout)
			return;

		// 0xC6C6C6 (198,198,198) is the standard Minecraft ContainerScreen background colour
		// Some mod ContainerScreens use different colours. FUTURE: do something dynamic with the background texture. (Maybe use the most prevalent colour; still won't work for non-solid-colour backgrounds)
		
		DrawerImpl d = captchalogue_getDrawerImpl(true);
		
		if(modus.overridesGuiSlotVisualConnectivity()) {
			
			final float COL_INVISIBLE = 198.0f/255.0f;
			final float COL_SLOT = 139.0f/255.0f; // standard colour for backgrounds of clickable slots
			final float COL_RIGHT_BOTTOM = 255.0f/255.0f; // standard colour for right and bottom side border.
			final float COL_LEFT_TOP = 55.0f/255.0f; // standard colour for top and left side border.
			
			d.beginRenderingSolidQuads();
			
			// Hide everything that's already there. We draw our own slots. (inventoryRect includes the 1-pixel edges)
			d.appendSolidQuad(inventoryRect.x, inventoryRect.y, inventoryRect.x+inventoryRect.width, inventoryRect.y+inventoryRect.height, COL_INVISIBLE, COL_INVISIBLE, COL_INVISIBLE, 1.0f);
			
			final int DIR_UP = 1;
			final int DIR_DOWN = 2;
			final int DIR_LEFT = 4;
			final int DIR_RIGHT = 8;
			final int RENDER_SELF = 16;
			byte[] borderLocations = new byte[4*9]; // indexed by visual position
			
			// loop over visual positions
			for(int y = 0, n = 0; y < 4; y++)
				for(int x = 0; x < 9; x++, n++) {
					int myGroup = modus.getBackgroundGroupForSlot(captchalogue_getSlotAtVisualPosition(x, y));
					// Invisible slots are recorded with borders, but the borders aren't rendered
					if(myGroup != FetchModusType.BG_GROUP_INVISIBLE) {
						borderLocations[n] |= RENDER_SELF;
						if (x == 0 || modus.getBackgroundGroupForSlot(captchalogue_getSlotAtVisualPosition(x-1, y)) != myGroup) borderLocations[n] |= DIR_LEFT;
						if (x == 8 || modus.getBackgroundGroupForSlot(captchalogue_getSlotAtVisualPosition(x+1, y)) != myGroup) borderLocations[n] |= DIR_RIGHT;
						if (y == 0 || modus.getBackgroundGroupForSlot(captchalogue_getSlotAtVisualPosition(x, y-1)) != myGroup) borderLocations[n] |= DIR_UP;
						if (y == 3 || modus.getBackgroundGroupForSlot(captchalogue_getSlotAtVisualPosition(x, y+1)) != myGroup) borderLocations[n] |= DIR_DOWN;
					} else {
						borderLocations[n] |= DIR_LEFT | DIR_RIGHT | DIR_UP | DIR_DOWN;
					}
				}
			
			// loop over visual positions
			// Draw slot backgrounds, borders, and horizontal connectors
			for(int y = 0; y < 4; y++) {
				for(int x = 0; x < 9; x++) {
					int slotIndex = captchalogue_getSlotAtVisualPosition(x, y);
					//int g = modus.getBackgroundGroupForSlot(slotIndex);
					Slot slot = inventorySlots[slotIndex];
					int which = borderLocations[x+y*9];
					if((which & RENDER_SELF) == 0)
						continue;
					d.appendSolidQuad(slot.xPosition, slot.yPosition, slot.xPosition+16, slot.yPosition+16, COL_SLOT, COL_SLOT, COL_SLOT, 1.0f);
					
					if((which & DIR_UP) != 0)
						d.appendSolidQuad(slot.xPosition - ((which & DIR_LEFT) != 0 ? 1 : 0), slot.yPosition-1, slot.xPosition+16, slot.yPosition, COL_LEFT_TOP, COL_LEFT_TOP, COL_LEFT_TOP, 1.0f);
					if((which & DIR_LEFT) != 0)
						d.appendSolidQuad(slot.xPosition-1, slot.yPosition, slot.xPosition, slot.yPosition+16, COL_LEFT_TOP, COL_LEFT_TOP, COL_LEFT_TOP, 1.0f);
					if((which & DIR_DOWN) != 0)
						d.appendSolidQuad(slot.xPosition, slot.yPosition+16, slot.xPosition+16 + ((which & DIR_RIGHT) != 0 ? 1 : 0), slot.yPosition+17, COL_RIGHT_BOTTOM, COL_RIGHT_BOTTOM, COL_RIGHT_BOTTOM, 1.0f);
					if((which & DIR_RIGHT) != 0)
						d.appendSolidQuad(slot.xPosition+16, slot.yPosition, slot.xPosition+17, slot.yPosition+16, COL_RIGHT_BOTTOM, COL_RIGHT_BOTTOM, COL_RIGHT_BOTTOM, 1.0f);
					
					// 1-pixel corners
					if ((which & (DIR_DOWN | DIR_LEFT)) == (DIR_DOWN | DIR_LEFT))
						d.appendSolidQuad(slot.xPosition-1, slot.yPosition+16, slot.xPosition, slot.yPosition+17, COL_SLOT, COL_SLOT, COL_SLOT, 1.0f);
					if ((which & (DIR_UP | DIR_RIGHT)) == (DIR_UP | DIR_RIGHT))
						d.appendSolidQuad(slot.xPosition+16, slot.yPosition-1, slot.xPosition+17, slot.yPosition-1, COL_SLOT, COL_SLOT, COL_SLOT, 1.0f);
					
					if(x < 8 && (which & DIR_RIGHT) == 0) {
						// The connector has a border if either of the adjacent slots has a border on that side
						Slot nextSlot = inventorySlots[slotIndex+1];
						int nextWhich = borderLocations[x+1+y*9];
						
						d.appendSolidQuad(slot.xPosition+16, slot.yPosition, nextSlot.xPosition, slot.yPosition+16, COL_SLOT, COL_SLOT, COL_SLOT, 1.0f);
						
						if (((which | nextWhich) & DIR_UP) != 0) {
							d.appendSolidQuad(slot.xPosition+16, slot.yPosition-1, nextSlot.xPosition, slot.yPosition, COL_LEFT_TOP, COL_LEFT_TOP, COL_LEFT_TOP, 1.0f);
						}
						if (((which | nextWhich) & DIR_DOWN) != 0) {
							d.appendSolidQuad(slot.xPosition+16, slot.yPosition+16, nextSlot.xPosition, slot.yPosition+17, COL_RIGHT_BOTTOM, COL_RIGHT_BOTTOM, COL_RIGHT_BOTTOM, 1.0f);
						}
					}
				}
			}
			
			// Draw vertical and diagonal connectors
			for(int y = 0; y < 3; y++) {
				for(int x = 0; x < 9; x++) {
					int slotIndex = captchalogue_getSlotAtVisualPosition(x, y);
					int belowSlotIndex = captchalogue_getSlotAtVisualPosition(x, y+1);
					Slot thisSlot = inventorySlots[slotIndex];
					Slot belowSlot = inventorySlots[belowSlotIndex];
					
					int which = borderLocations[x+y*9];
					int belowWhich = borderLocations[x+y*9+9];
					int rightWhich = (x < 8 ? borderLocations[x+y*9+1] : 0);
					
					if ((which & DIR_DOWN) == 0) {
						d.appendSolidQuad(thisSlot.xPosition, thisSlot.yPosition+16, thisSlot.xPosition+16, belowSlot.yPosition, COL_SLOT, COL_SLOT, COL_SLOT, 1.0f);
						if (((which | belowWhich) & DIR_LEFT) != 0) {
							d.appendSolidQuad(thisSlot.xPosition-1, thisSlot.yPosition+16, thisSlot.xPosition, belowSlot.yPosition, COL_LEFT_TOP, COL_LEFT_TOP, COL_LEFT_TOP, 1.0f);
						}
						if (((which | belowWhich) & DIR_RIGHT) != 0) {
							d.appendSolidQuad(thisSlot.xPosition+16, thisSlot.yPosition+16, thisSlot.xPosition+17, belowSlot.yPosition, COL_RIGHT_BOTTOM, COL_RIGHT_BOTTOM, COL_RIGHT_BOTTOM, 1.0f);
						}
					}
					
					if (x < 8 && ((which | belowWhich) & DIR_RIGHT) == 0 && ((which | rightWhich) & DIR_DOWN) == 0) {
						Slot belowNextSlot = inventorySlots[belowSlotIndex+1];
						d.appendSolidQuad(thisSlot.xPosition+16, thisSlot.yPosition+16, belowNextSlot.xPosition, belowNextSlot.yPosition, COL_SLOT, COL_SLOT, COL_SLOT, 1.0f);
					}
				}
			}
			
			// loop over visual positions
			/*for(int y = 0; y < 4; y++) {
				for(int x = 0; x < 9; x++) {
					int n = getSlotAtVisualPosition(x, y); // slot index
					byte borders = borderLocations[x];
					int g = modus.getBackgroundGroupForSlot(n);
					
					Slot s = inventorySlots[n];
					
					if (g == FetchModus.BG_GROUP_INVISIBLE) {
						// no need for this, since we cleared the entire rectangle first.
						//appendGreyQuad(bb, matrix, s.xPosition-1, s.yPosition-1, s.xPosition+17, s.yPosition+17, COL_INVISIBLE);
						
					} else {
						
						appendGreyQuad(bb, matrix, s.xPosition, s.yPosition, s.xPosition+16, s.yPosition+16, COL_SLOT);
						
						int nr = getSlotAtVisualPosition(x+1, y);
						int nd = getSlotAtVisualPosition(x, y+1);
						Slot sr = nr < 0 ? null : inventorySlots[nr];
						Slot sd = nd < 0 ? null : inventorySlots[nd];
						
						int gd = nd < 0 ? -1 : modus.getBackgroundGroupForSlot(nd);
						int gr = nr < 0 ? -1 : modus.getBackgroundGroupForSlot(nr);
						
						if (nr >= 0 && g == gr) {
							appendGreyQuad(bb, matrix, s.xPosition+16, Math.min(s.yPosition, sr.yPosition), sr.xPosition, Math.max(s.yPosition, sr.yPosition)+16, COL_SLOT);
						} else {
							//appendGreyQuad(bb, matrix, s.xPosition+16, s.yPosition, s.xPosition+17, (nd >= 0 && g == gd ? sd.yPosition : s.xPosition+16), COL_RIGHT_BOTTOM);
						}
						
						if (nd >= 0 && g == gd) {
							appendGreyQuad(bb, matrix, Math.min(s.xPosition, sd.xPosition), s.yPosition+16, Math.max(s.xPosition, sd.xPosition)+16, sd.yPosition, COL_SLOT);
						}
						
						if (nr >= 0 && nd >= 0 && g == gd && g == gr) {
							int ndr = getSlotAtVisualPosition(x+1, y+1);
							int gdr = ndr < 0 ? -1 : modus.getBackgroundGroupForSlot(ndr);
							if (ndr >= 0 && g == gdr) {
								Slot sdr = inventorySlots[ndr];
								
								appendGreyQuad(bb, matrix, s.xPosition+16, s.yPosition+16, sdr.xPosition, sdr.yPosition, COL_SLOT);
							}
						}
					}
				}
			}*/
			
			d.endRenderingSolidQuads();
		}
		
		FetchModusGuiState gs = getFetchModusGuiState();
		gs.drawAdditionalBackground(captchalogue_drawerImpl);
	}
	
	@Unique
	private DrawerImpl captchalogue_drawerImpl;
	
	@Unique
	private DrawerImpl captchalogue_getDrawerImpl(boolean needTranslationMatrix) {
		// in drawSlot the global matrix has the GUI translation already (needTranslationMatrix=false), but in drawBackground it doesn't so we apply it (needTranslationMatrix=true)
		if(captchalogue_drawerImpl == null)
			captchalogue_drawerImpl = new DrawerImpl();
		Matrix4f matrix = Matrix4f.translate(needTranslationMatrix ? this.x : 0, needTranslationMatrix ? this.y : 0, 0.0f);
		captchalogue_drawerImpl.setMatrix(matrix, this.x, this.y);
		return captchalogue_drawerImpl;
	}
	
	@Inject(at=@At("HEAD"), method="drawSlot(Lnet/minecraft/container/Slot;)V", cancellable=true)
	public void drawSlotOverride(Slot slot, CallbackInfo info) {
		if(unsupportedLayout)
			return;
		
		//System.out.println("drawSlotOverride");
		if(slot.inventory instanceof PlayerInventory) {
			PlayerInventory inv = (PlayerInventory)slot.inventory;
			int slotNum = ((ISlotMixin)slot).captchalogue_getSlotNum();
			if(slotNum < 0 || slotNum >= 36)
				return;
			
			FetchModusGuiState gs = getFetchModusGuiState();
			if (gs.overrideDrawSlot((ContainerScreen<?>)(Object)this, x, y, slot, inv, slotNum, mouseX-x, mouseY-y)) {
				info.cancel();
			} else {
				DrawerImpl d = captchalogue_getDrawerImpl(false);
				if(slot.inventory instanceof PlayerInventory)
					gs.beforeDrawSlot(slot, d);
			}
			/*
			if(!modus.canTakeFromSlot(inv, slotNum)) {
				// draw some type of overlay
				int x = slot.xPosition;
				int y = slot.yPosition;
				
				// TODO: temporarily disabled for test
				//DrawableHelper.fill(x, y, x+16, y+16, 0x80FF0000);
				
				//info.cancel();
			}*/
			
			// if(unsupportedLayout) to check whether we are doing the full override
			
		}
	}
	
	@Inject(at=@At("RETURN"),method="drawSlot(Lnet/minecraft/container/Slot;)V")
	public void afterDrawSlot(Slot slot, CallbackInfo info) {
		if(unsupportedLayout)
			return;
		
		FetchModusGuiState gs = getFetchModusGuiState();
		DrawerImpl d = captchalogue_getDrawerImpl(false);
		if(slot.inventory instanceof PlayerInventory) {
			int slotNum = ((ISlotMixin)slot).captchalogue_getSlotNum();
			if(slotNum < 0 || slotNum >= 36)
				return;
			gs.afterDrawSlot(slot, d);
		}
	}
	
	private AdditionalContainerScreenElement extraGuiElement;
	
	@Inject(at=@At("HEAD"), method="init()V")
	public void additionalInit(CallbackInfo info) {
		extraGuiElement = new AdditionalContainerScreenElement((ContainerScreen<?>)(Object)this);
		//children.add(extraGuiElement); // accounts for mouse and keyboard callbacks, but not rendering // actually most callbacks don't go through this so we have to extend them anyway
	}
	
	@Inject(at=@At("HEAD"), method="mouseDragged(DDIDD)Z")
	public void hookMouseDragged(double x, double y, int button, double dx, double dy, CallbackInfoReturnable<Boolean> info) {
		if(unsupportedLayout)
			return;
		// XXX: return value ignored
		extraGuiElement.mouseDragged(x, y, button, dx, dy);
	}
	
	@Inject(at=@At("HEAD"), method="mouseClicked(DDI)Z")
	public void hookMouseClicked(double x, double y, int button, CallbackInfoReturnable<Boolean> info) {
		if(unsupportedLayout)
			return;
		
		// XXX: return value ignored
		extraGuiElement.mouseClicked(x, y, button);
	}
	
	@Shadow
	public abstract boolean isPointOverSlot(Slot slot, double x, double y);
	
	@Inject(at=@At("HEAD"), method="isPointOverSlot(Lnet/minecraft/container/Slot;DD)Z", cancellable=true)
	public void overrideIsPointOverSlot(Slot slot, double x, double y, CallbackInfoReturnable<Boolean> info) {
		if(unsupportedLayout)
			return;
		if(!(slot.inventory instanceof PlayerInventory))
			return;
		int index = ((ISlotMixin)slot).captchalogue_getSlotNum();
		if(index < 0 || index > 35)
			return;
		
		FetchModusGuiState gs = getFetchModusGuiState();
		int val = gs.overridesIsPointOverSlot(slot, x-this.x, y-this.y);
		if(val == 0)
			info.setReturnValue(Boolean.FALSE);
		else if(val == 1)
			info.setReturnValue(Boolean.TRUE);
	}
	
	@Shadow
	protected Slot focusedSlot;
	
	// Don't highlight non-takeable slots when the mouse is over them.
	// We still need to set focusedSlot, to get the tooltip
	@Redirect(at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/screen/ingame/ContainerScreen;isPointOverSlot(Lnet/minecraft/container/Slot;DD)Z"), method="render(IIF)V")
	public boolean slotHoverHighlight_replace_isPointOverSlot(ContainerScreen<?> receiver, Slot slot, double x, double y) {
		// receiver == this
		if (isPointOverSlot(slot, x, y)) {
			if (slot.inventory instanceof PlayerInventory) {
				PlayerInventory inv = (PlayerInventory)slot.inventory;
				int slotIndex = ((ISlotMixin)slot).captchalogue_getSlotNum();
				if(slotIndex == CaptchalogueMod.MODUS_SLOT || slotIndex < 0 || slotIndex >= 36)
					return true; // no override
				
				FetchModusState modus = ((IPlayerInventoryMixin)inv).getFetchModus();
				
				boolean holdingItem = !inv.getCursorStack().isEmpty();
				
				if ((!holdingItem && !modus.canTakeFromSlot(InventoryWrapper.PlayerInventorySkippingModusSlot.fromUnderlyingSlotIndex(slotIndex))) || (holdingItem && !modus.canInsertToSlot(InventoryWrapper.PlayerInventorySkippingModusSlot.fromUnderlyingSlotIndex(slotIndex))) ) {
					// Set focusedSlot (normally done if this function returns true), but return false to skip the highlight rendering
					
					FetchModusGuiState guistate = ((IContainerMixin)receiver.getContainer()).getFetchModusGuiState();
					// ... unless the modus blocks it (like Memory modus for unrevealed slots!)
					focusedSlot = guistate.overrideFocusedSlot((ContainerScreen<?>)(Object)this, inv, slotIndex, slot);
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	@Shadow public abstract Slot getSlotAt(double x, double y);
	@Shadow public abstract void onMouseClick(Slot slot, int invSlot, int button, SlotActionType slotActionType);
	
	@Override public int captchalogue_getGuiX() {return x;}
	@Override public int captchalogue_getGuiY() {return y;}
	@Override
	public Slot captchalogue_getSlotAt(double x, double y) {
		return getSlotAt(x, y);
	}
	@Override
	public void captchalogue_onMouseClick(Slot slot, int invSlot, int button, SlotActionType slotActionType) {
		onMouseClick(slot, invSlot, button, slotActionType);
	}
	
	@Unique
	private FetchModusState captchalogue_findFetchModus() {
		for(Slot s : ((ContainerScreen<?>)(Object)this).getContainer().slots) {
			if(s.inventory instanceof PlayerInventory)
				return ((IPlayerInventoryMixin)inv).getFetchModus();
		}
		return null;
	}
	
	@Override
	public void captchalogue_fiddleWithItemRenderTooltip(List<String> tooltip) {
		// Don't check unsupportedLayout. This is valid even with unsupportedLayout.
		FetchModusState modus = captchalogue_findFetchModus();
		if(modus != null)
			modus.fiddleWithItemRenderTooltip(tooltip);
	}
}
