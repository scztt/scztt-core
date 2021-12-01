(
SynthDef.channelized(\loop, {
	|numChannels|
	var sig, loopDur, loopStart, loopTrim, buffer, phase, samples;
	var rate, loopSamples;

	buffer = \buffer.kr;

	loopDur = \loopDur.kr(1);
	loopStart = \loopStart.kr(0);
	loopTrim = \loopTrim.kr(-1);
	loopTrim = (loopTrim < 0).if(
		BufDur.kr(buffer),
		loopTrim
	);

	samples = loopTrim * BufSampleRate.kr(buffer);
	loopStart = loopStart * BufSampleRate.kr(buffer);

	rate = loopTrim / loopDur * BufRateScale.kr(buffer);

	phase = loopStart + Phasor.ar(1, rate, 0, samples);
	sig = BufRd.ar(numChannels, buffer, phase, 0, 4);

	sig = \amp.kr(1) * sig * Env([1, 1, 0], [loopDur, 0.01]).kr(gate:\gate.kr(1), doneAction:2);

	Out.ar(\out.kr(0), sig);
}, channelizations: (1..4)).do(_.add);

Event.addParentType(\loop, (
	finish: {
		~numChannels = ~numChannels ?? { ~buffer.tryPerform(\numChannels) } ?? { 1 };
		~instrument = "loop_%ch".format(~numChannels).asSymbol;
		~loopDur = ~sustain.value / (~tempo.value ?? {1})
	},
	legato: 1,
	dur: 4
));
)