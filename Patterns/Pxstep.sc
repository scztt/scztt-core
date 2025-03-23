Pxstep : Pattern {
	var <>list, <>durs, <>curves, <>repeats;

	*new { arg levels, durs = 1, curves=0, repeats = 1;
		^super.newCopyArgs(levels, durs, curves, repeats).init
	}
	init {
		if (list.isSequenceableCollection) { list = Pseq(list); };
		if (durs.isSequenceableCollection) { durs = Pseq(durs); };
		if (curves.isSequenceableCollection) { curves = Pseq(curves); };
	}

	embedInStream { arg inval;
		var stream, listStream, val, dur, curve, lastVal;
		thisThread.endBeat = thisThread.endBeat ? thisThread.beats;
		// endBeat > beats only if Pfindur ended something early
		thisThread.endBeat = thisThread.endBeat min: thisThread.beats;

		listStream = list.asStream;
		val = listStream.next(inval);

		repeats.value(inval).do { | i |
			stream = Ptuple([listStream, durs, curves]).asStream;

			while ({
				lastVal = val;
				#val, dur, curve = stream.next(inval) ?? { #[nil, nil] };
				val.notNil && lastVal.notNil;
			},
			{
				thisThread.endBeat = thisThread.endBeat + dur;

				while({ thisThread.beats < thisThread.endBeat }, {
					inval = thisThread.beats.lincurve(
						thisThread.endBeat - dur,
						thisThread.endBeat,
						lastVal, val,
						curve
					).embedInStream(inval);
				});
			});
		};
		^inval;
	}
	storeArgs {
		^[list, durs, repeats]
	}
}

+Object {
	xstep {
		|durs = 1, curves=0, repeats = 1|
		^Pxstep(this, durs, curves, repeats)
	}
}
