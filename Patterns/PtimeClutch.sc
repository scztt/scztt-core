// Only pull a value once per clock time - else, return the previous value
PtimeClutch : FilterPattern {
	var <>delta;

	*new {
		|pattern, delta=0.0|
		^super.new(pattern).delta_(delta);
	}

	embedInStream {
		|input|
		var lastTime, lastVal;
		var stream = pattern.asStream;

		loop {
			var thisTime = thisThread.beats;

			if (lastTime.isNil or: { (thisTime - lastTime) > delta }) {
				lastVal = stream.next(input);
				lastTime = thisTime;
			};

			input = lastVal.copy.yield;
		}
	}
}

+Pattern {
	timeClutch {
		|delta=0.0|
		^PtimeClutch(this, delta)
	}
}