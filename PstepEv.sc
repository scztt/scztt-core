PstepEv : Pattern {
	var <>eventPattern, <>repeats, <>key;

	*new { arg eventPattern, repeats = 1, key=\value;
		^super.newCopyArgs(eventPattern, repeats, key)
	}

	embedInStream { arg inval;
		var stream, val,dur, event;
		thisThread.endBeat = thisThread.endBeat ? thisThread.beats;
		// endBeat > beats only if Pfindur ended something early
		thisThread.endBeat = thisThread.endBeat min: thisThread.beats;

		repeats.value(inval).do { | i |
			stream = eventPattern.asStream;
			while ({
				while({
					event = stream.next(inval);
					if (event.notNil) {
						val = event.at(key);
						dur = event.delta;
						dur.postln;
						if (val.notNil && dur.notNil) {
							thisThread.endBeat = thisThread.endBeat + dur;
							thisThread.endBeat <= thisThread.beats
						} {
							false;
						}
					} {
						false
					}
				});

				val.notNil && event.notNil;
			},
			{
				while({ thisThread.beats < thisThread.endBeat }, {
					inval = val.embedInStream(inval)
				})
			});
		};
		^inval;
	}
	storeArgs {
		^[eventPattern, repeats, key]
	}

}