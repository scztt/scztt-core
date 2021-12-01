ThreadStateScope {
	var beats, endBeat;

	*new {
		^super.newCopyArgs(thisThread.beats, thisThread.endBeat);
	}

	use {
		|function|
		var oldValues;

		if (beats.isNil) { beats = thisThread.beats };
		if (endBeat.isNil) { endBeat = thisThread.endBeat };

		protect {
			oldValues = [thisThread.beats, thisThread.endBeat];
			thisThread.beats = beats;
			thisThread.endBeat = endBeat;

			function.()
		} {
			this.beats = thisThread.beats;
			endBeat = thisThread.endBeat;

			thisThread.beats = oldValues[0];
			thisThread.endBeat = oldValues[1];
		}
	}

	beats_{
		|value|
		if (value.isNil) {
			"setting beats to nil... weird".postln;
		};
		beats = value;
	}

	beats {
		^(beats ?? { 0 })
	}

	endBeat {
		^(endBeat ?? { 0 })
	}

}