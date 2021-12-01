+Pstep {
	embedInStream { arg inval;
		var stream, val,dur;
		thisThread.endBeat = thisThread.endBeat ? thisThread.beats;
		// endBeat > beats only if Pfindur ended something early
		thisThread.endBeat = thisThread.endBeat min: thisThread.beats;

		repeats.value(inval).do { | i |
			stream = Ptuple([list, durs]).asStream;
			while ({
				while({
					#val, dur = stream.next(inval) ?? { #[nil, nil] };
					if (val.notNil && dur.notNil) {
						thisThread.endBeat = thisThread.endBeat + dur;
						thisThread.endBeat <= thisThread.beats
					} {
						false;
					}
				});

				val.notNil;
			},
			{
				while({ thisThread.beats < thisThread.endBeat }, {
					inval = val.embedInStream(inval)
				})
			});
		};
		^inval;
	}

}