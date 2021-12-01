// Reset `pattern` every `durs` beats.
// If pattern is finished, yield `emptyValue` until we are reset.

// (
// Pdef(\ptclip, Pbind(
// 	\scale, Scale.bartok,
// 	\dur, 1/8, \legato, 3,
// 	\octave, 4,
// 	\degree, PTclip(
// 		Prand([0, 1, 3, 6, 8], inf),
// 		Pseq([1, 1.5, 0.75], inf).trace,
// 	)
// )).play
// )

PTclip : FilterPattern {
	var <>durs, <>emptyValue;

	*new { arg pattern, durs, emptyValue;
		^super.new(pattern)
			.durs_(durs)
			.emptyValue_(emptyValue)
			.init
	}
	init {
		if (pattern.isSequenceableCollection) { pattern = Pseq(pattern); };
		if (durs.isNumber) { durs = [durs] };
		if (durs.isSequenceableCollection) { durs = Pseq(durs); };
	}

	storeArgs { ^[pattern, durs, emptyValue] }

	embedInStream { |inval|
		var valueStream, emptyStream, durStream, isRest;
		var dur, value;
		var cleanup = EventStreamCleanup.new;

		thisThread.endBeat = thisThread.endBeat ? thisThread.beats;
		// endBeat > beats only if Pfindur ended something early
		thisThread.endBeat = thisThread.endBeat min: thisThread.beats;

		durStream = durs.asStream;

		while {
			dur = durStream.next(inval);
			dur.notNil;
		} {
			thisThread.endBeat = thisThread.endBeat + dur;
			// Log(PTclip).info("thisThread.endBeat = %, dur = % ", thisThread.endBeat, dur);

			valueStream = pattern.asStream;

			while {
				(thisThread.beats < thisThread.endBeat).if({
					value = valueStream.next(inval) ?? { emptyValue.value(inval) };

					if (value.isKindOf(Event)) {
						if ((thisThread.beats + value.delta) >= thisThread.endBeat) {
							if (value.isRest) {
								value = value.copy.deltaOrDur_(Rest(thisThread.endBeat - thisThread.beats));
							} {
								value = value.copy.deltaOrDur_(thisThread.endBeat - thisThread.beats);
							}
						};
						cleanup.update(value);
					};
					true;
				}, {
					false
				})
			} {
				inval = value.yield(inval);
			}
		};
		^cleanup.exit(inval);
	}
}

+Object {
	finDur {
		|durs, emptyValue|
		^PTclip(this.asPattern, durs, emptyValue)
	}
}

+Pattern {
	finDur {
		|durs, emptyValue|
		^PTclip(this, durs, emptyValue)
	}
}
