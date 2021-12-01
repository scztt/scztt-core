(
var notesList, samples;

samples = MultiLevelIdentityDictionary();

SAMP('salamander/*.wav').paths.do {
	|path, index|
	var note, parsed, vel, oct, name;
	name = path.split($/).last.split($.).first;
	parsed = name.findRegexp("(.s?)([0-9])v([0-9]+)");
	note = parsed[1][1].asSymbol;
	oct = parsed[2][1].asInteger;
	vel = parsed[3][1].asInteger;
	note = (
		C: 0,
		A: 9,
		Ds: 3,
		Fs: 6
	)[note];
	note = note + ((oct + 1) * 12);
	samples.put(note, vel, index);
};
notesList = samples.dictionary.keys.asArray.sort;

SynthDef(\piano, {
	var env, sig, buf1, buf2, rate;

	buf1 = \buffer1.kr(-1);
	buf2 = \buffer2.kr(-1);

	rate = \freq.kr(440) / \sampleRoot.kr(440);

	env = Env.adsr(
		\attack.kr(0.01),
		\decay.kr(0.9),
		\sustain.kr(0.95),
		\release.kr(1)
	).kr(gate:\gate.kr(1), doneAction:2);

	sig = PlayBuf.ar(2, [buf1, buf2], rate * BufRateScale.kr(buf1), 1);
	sig = sig[0].blend(sig[1], \blend.kr(0));

	sig = \amp.kr(1) * env * sig;

	Out.ar(\out.kr(0), sig);
}).add;

Event.addParentType(\piano, (
	finish: {
		var midinote, notesIndex, noteName, octave, rootFreq;
		var note, velocityValues, velocityIndex, bufferA, bufferB, blend, result;

		~instrument = \piano;
		midinote = ~detunedFreq.value.cpsmidi;
		midinote = midinote.asCollection;

		~buffers = midinote.collect {
			|midinote|
			notesIndex = notesList.indexInBetween(midinote);
			note = samples[notesList[notesIndex.round.asInteger]];
			rootFreq = notesList[notesIndex.round.asInteger].midicps;

			velocityValues = note.keys.asArray.sort;
			velocityIndex = velocityValues.indexInBetween(~amp.value * 15);

			bufferA = SAMP('salamander/*.wav')[
				note[velocityValues[velocityIndex.floor.asInteger]]
			];
			bufferB = SAMP('salamander/*.wav')[
				note[velocityValues[velocityIndex.ceil.asInteger]]
			];
			blend = velocityIndex % 1.0;
			[bufferA, bufferB, blend, rootFreq];
		};
		~buffers = ~buffers.flop;

		~buffer1 = ~buffers[0];
		~buffer2 = ~buffers[1];
		~blend = ~buffers[2];
		~sampleRoot = ~buffers[3];
	}
));
)