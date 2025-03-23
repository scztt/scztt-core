
+Plot {
	needsPenFill { |pMode|
		^#[\bars, \filledLinear].includes(pMode)
	}

	filledLinear { |x, y|
		var zero = spec.warp.unmap(0);
		zero = plotBounds.bottom - (zero * plotBounds.height); // measures from top left (may be arrays)

		Pen.moveTo(x.first @ zero);
		Pen.lineTo(x.first @ y.first);
		y.size.do { |i|
			Pen.lineTo(x[i] @ y[i]);
		};
		Pen.lineTo(x.last @ zero);
	}
}