/* Rael B. Gimenes Toffolo

<rael dot gimenes at gmail dot com> */

+String {
	nameToMidi {
		var namesMap, split, regexp, note, octave, midinote;
		namesMap = Dictionary[
			"C"->	0,
			"C#"->	1,
			"Db"-> 	1,
			"D"-> 	2,
			"D#"-> 	3,
			"Eb"-> 	3,
			"E"-> 	4,
			"F"-> 	5,
			"F#"-> 	6,
			"Gb"-> 	6,
			"G"-> 	7,
			"G#"-> 	8,
			"Ab"->	8,
			"A"-> 	9,
			"A#"->	10,
			"Bb"-> 	10,
			"B"->	11,
		];
		regexp = "([A-G][#b]?)([0-9]*)";
		split = this.toUpper.findRegexp(regexp);

		if (split.size == 3) {
			note = split[1][1];
			octave = split[2][1];
		};

		if (note.notNil) {
			midinote = namesMap[note];
		};
		if (octave.size > 0) {
			midinote = midinote + (12 * (1 + (octave.asInteger)));
		};

		midinote ?? {
			Error("Not an identifiable note: %".format(this)).throw
		};

		^midinote
	}
}

+Collection {
	midiNames
	{
		var name, octave, testCase, nameOut, stringNotes,arrayNotes;
		arrayNotes = this;
		stringNotes = "";
		arrayNotes.do({ arg z;
			var i;
			i = z.asInteger;
			name = i-(12*(i.div(12)));
			octave = (i.div(12))-1;

			testCase = case
			{name == 0 } { "C" ++ octave ++", " }
			{name == 1 } { "C#" ++ octave ++", " }
			{name == 2 } { "D" ++ octave ++", " }
			{name == 3 } { "D#" ++ octave ++", " }
			{name == 4 } { "E" ++ octave ++", " }
			{name == 5 } { "F" ++ octave ++", " }
			{name == 6 } { "F#" ++ octave ++", " }
			{name == 7 } { "G" ++ octave ++", " }
			{name == 8 } { "G#" ++ octave ++", " }
			{name == 9 } { "A" ++ octave ++", " }
			{name == 10 } { "A#" ++ octave ++", " }
			{name == 11 } { "B" ++ octave ++", " };
			stringNotes = stringNotes ++ testCase;
		});
		stringNotes.removeAt(stringNotes.size-1);
		stringNotes.removeAt(stringNotes.size-1);
		^stringNotes;

	}

	namesToMidi {
		^this.collect(_.nameToMidi);
	}
}
