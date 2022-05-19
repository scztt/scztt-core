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

// Only pull a value once per clock time - else, return the previous value
PeventClutch : FilterPattern {
	var <>delta;

	*new {
		|pattern, delta=0.0|
		^super.new(pattern).delta_(delta);
	}

	embedInStream {
		|inEvent|
		var lastEvent, nextTime;
		var stream = pattern.asStream;
		var delta = 0.000001;

		nextTime = thisThread.beats;

		loop {
			var thisTime = thisThread.beats;

			if ((nextTime - thisTime).abs < delta) {
				lastEvent = stream.next(inEvent);
				nextTime = thisTime + lastEvent.delta;
				inEvent = lastEvent.copy.yield;
			} {
				if (thisTime < nextTime) {
					inEvent = lastEvent.copy.yield
				} {
					lastEvent = stream.next(inEvent);
					nextTime = thisTime + lastEvent.delta;
				}
			};
		}
	}
}

+Pattern {
	timeClutch {
		|delta=0.0|
		^PtimeClutch(this, delta)
	}

	eventClutch {
		|delta=0.0|
		^PeventClutch(this, delta)
	}
}