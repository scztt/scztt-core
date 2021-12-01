Pswing : Pfunc {
	*new {
		|offset, period=0.25, division=0.5|
		^super.new({
			if ((thisThread.beats % period) >= (period * division)) {
				offset * period
			} {
				0
			}
		})
	}
}