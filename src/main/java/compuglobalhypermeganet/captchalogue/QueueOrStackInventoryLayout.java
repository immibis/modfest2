package compuglobalhypermeganet.captchalogue;

public class QueueOrStackInventoryLayout {
	public byte[] visualToLogicalSlotMapping = new byte[36];
	public byte[] logicalToVisualSlotMapping = new byte[36];
	public byte[] visualSlotBorderStyle = new byte[36];
	
	public void map(int visualSlot, int logicalSlot, int borderStyle) {
		if(visualSlot == FetchModus.MODUS_SLOT || logicalSlot == FetchModus.MODUS_SLOT)
			throw new IllegalArgumentException();
		if(logicalSlot >= FetchModus.MODUS_SLOT)
			logicalSlot++; // don't include modus slot in mapping definition (but do skip over it in the mapping)
		visualToLogicalSlotMapping[visualSlot] = (byte)logicalSlot;
		logicalToVisualSlotMapping[logicalSlot] = (byte)visualSlot;
		visualSlotBorderStyle[visualSlot] = (byte)borderStyle;
	}
	static int nextLogicalSlot(int slot) {
		return slot == FetchModus.MODUS_SLOT - 1 ? slot+2 : slot+1;
	}
	static int prevLogicalSlot(int slot) {
		return slot == FetchModus.MODUS_SLOT + 1 ? slot-2 : slot-1;
	}
	
	public static final int T_BORDER = 1;
	public static final int B_BORDER = 2;
	public static final int L_BORDER = 4;
	public static final int R_BORDER = 8;
	public static final int RENDER_SELF = 16;
	
	public static final QueueOrStackInventoryLayout QUEUE_OR_STACK_LAYOUT = new QueueOrStackInventoryLayout() {{
		if(FetchModus.MODUS_SLOT == 8) {
			// 8  9  10 11 12 13 14 15 16
			// 25 24 23 22 21 20 19 18 17
			// 26 27 28 29 30 31 32 33 34
			// 0  1  2  3  4  5  6  7  XX
			for(int x = 0; x < 9; x++) {
				map(x, x+8, RENDER_SELF);
				map(x+9, 25-x, RENDER_SELF);
				map(x+18, 26+x, RENDER_SELF);
				if(x != 8)
					map(x+27, x, RENDER_SELF);
			}
			setBordersBasedOnAdjacency();
			
		} else {
			throw new IllegalArgumentException("unknown layout");
		}
	}};
	
	protected void setBordersBasedOnAdjacency() {
		for(int visualSlot = 0; visualSlot < 36; visualSlot++) {
			if ((visualSlotBorderStyle[visualSlot] & RENDER_SELF) != 0)
				break;
			int ls = visualToLogicalSlotMapping[visualSlot];
			int nextLS = nextLogicalSlot(ls), prevLS = prevLogicalSlot(ls);
			int below = visualSlot+9;
			int above = visualSlot-9;
			int left = (visualSlot % 9 == 0 ? -1 : visualSlot-1);
			int right = (visualSlot % 9 == 8 ? -1 : visualSlot+1);
			
			if (below < 0 || below > 35 || (visualToLogicalSlotMapping[below] != nextLS && visualToLogicalSlotMapping[below] != prevLS) || (visualSlotBorderStyle[below] & RENDER_SELF) == 0)
				visualSlotBorderStyle[visualSlot] |= B_BORDER;
			if (above < 0 || above > 35 || (visualToLogicalSlotMapping[above] != nextLS && visualToLogicalSlotMapping[above] != prevLS) || (visualSlotBorderStyle[above] & RENDER_SELF) == 0)
				visualSlotBorderStyle[visualSlot] |= T_BORDER;
			if (left < 0 || left > 35 || (visualToLogicalSlotMapping[left] != nextLS && visualToLogicalSlotMapping[left] != prevLS) || (visualSlotBorderStyle[left] & RENDER_SELF) == 0)
				visualSlotBorderStyle[visualSlot] |= L_BORDER;
			if (right < 0 || right > 35 || (visualToLogicalSlotMapping[right] != nextLS && visualToLogicalSlotMapping[right] != prevLS) || (visualSlotBorderStyle[right] & RENDER_SELF) == 0)
				visualSlotBorderStyle[visualSlot] |= R_BORDER;
		}
	}
}
