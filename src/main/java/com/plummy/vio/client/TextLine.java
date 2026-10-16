package com.plummy.vio.client;

import net.minecraft.util.FormattedCharSequence;

// A line of text at a position, in text pixels relative to the top-left of the text it belongs to.
public record TextLine(FormattedCharSequence text, int x, int y) {
}
