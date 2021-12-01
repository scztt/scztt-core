PTindep : Pattern {
	var <>pattern;

	*new { arg pattern;
		^super.newCopyArgs(pattern)
	}

	embedInStream { arg inval;
		var stream, event, delta, startTime, streamTime, currentTime;

		startTime = thisThread.beats;
		currentTime = 0;
		streamTime = 0;
		stream = pattern.asStream;

		loop {
			while { streamTime <= currentTime } {
				event = stream.next(inval);

				if (event.isNil) {
					^inval
				};
				streamTime = streamTime + event.delta
			};

			inval = event.yield;
			currentTime = thisThread.beats - startTime;
		};
	}

	storeArgs {
		^[pattern]
	}
}

+Pattern {
	indep {
		^PTindep(this);
	}
}