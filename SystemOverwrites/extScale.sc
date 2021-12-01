+Scale {
	octDegreeToMidi {
		|octave, degree|
		var steps = this.tryPerform(\stepsPerOctave) ?? 12;
		var note = degree.degreeToKey(this, steps);
		var midinote = (octave * steps) + note;
		^midinote;
	}

	midiToOctDegree {
		|midinote|
		var steps = this.tryPerform(\stepsPerOctave) ?? 12;
		var root = midinote.trunc(steps);
		var key = midinote % steps;

		var degrees = (steps.neg + this.degrees) ++ this.degrees ++ (steps + this.degrees);
		var tuningArray = (steps.neg + tuning.tuning) ++ tuning.tuning ++ (steps + tuning.tuning);
		var octave = root / steps;
		var tuningIndex, degree;

		tuningIndex = tuningArray.indexInBetween(key);
		tuningIndex = (tuningIndex - tuning.tuning.size) % tuning.tuning.size;

		degree = degrees.indexInBetween(tuningIndex);
		degree = (degree - this.size) % this.size;

		^(octave: octave, degree: degree)
	}
}


+SequenceableCollection {
	octDegreeToMidi {
		|octave, degree|
		var steps = this.tryPerform(\stepsPerOctave) ?? 12;
		var note = degree.degreeToKey(this, steps);
		var midinote = (octave * steps) + note;
		^midinote;
	}

	midiToOctDegree {
		|midinote|
		var steps = this.tryPerform(\stepsPerOctave) ?? 12;
		var root = midinote.trunc(steps);
		var key = midinote % steps;
		var degree = this.indexInBetween(key);
		var octave = root / steps;

		degree = (degree - this.size) % this.size;

		^(octave: octave, degree: degree)
	}
}

+SimpleNumber {
	degreeToKey { |scale, stepsPerOctave = 12|
		var scaleDegree = this.round.asInteger;
		var accidental = (this - scaleDegree);
		^scale.performDegreeToKey(scaleDegree, stepsPerOctave, accidental)
	}
}

+Scale {
	performDegreeToKey { | scaleDegree, stepsPerOctave, accidental = 0 |
		if (accidental != 0) {
			if (accidental.isNegative) {
				scaleDegree = scaleDegree - 1;
				accidental = 1.0 - accidental.abs;
			};
			^this.performDegreeToKey(scaleDegree, stepsPerOctave).blend(
				this.performDegreeToKey(scaleDegree + 1, stepsPerOctave),
				accidental
			)
		} {
			stepsPerOctave = stepsPerOctave ? tuning.stepsPerOctave;
			^(stepsPerOctave * (scaleDegree div: this.size)) + this.blendAt(scaleDegree, \wrapAt);
		}
	}
}