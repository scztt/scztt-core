(
// A dumb pluck synth
SynthDef(\ring, {
	var in, feed, trig, freq, sig, env, trigEnv;

	trig = \trig.tr(0);
	freq = \freq.kr(220);

	env = Env.adsr.kr(gate:\gate.kr(1), doneAction:2);
	trigEnv = Env.perc(0, 0.2, curve:\exp).ar(gate:trig);

	trig = Trig.ar(trig, SampleDur.ir);

	in = LFSaw.ar(freq * 3.015);
	in = in + ClipNoise.ar(0.2);
	in = BLowPass4.ar(
		in,
		\lpf.kr(100),
		0.3
	);

	feed = LocalIn.ar(1);
	feed = PitchShift.ar(feed, 1/8, 0.03.midiratio);
	// feed = DelayC.ar(feed, 1, 1/16);
	feed = 0.dbamp * feed;

	in = in + feed;

	sig = Pluck.ar(
		in, trig,
		0.2,
		freq.lag([0.02, 0.4]).reciprocal,
		8,
		\coef.kr(0.7),
		[0, -6].dbamp
	).sum;
	sig = -24.dbamp * sig;
	sig = sig + (-6.dbamp * feed);

	sig = SoftClipAmp.ar(sig, 1);
	sig = sig - FreqShift.ar(
		sig, freq / (-2.015)
		// sig, freq / trigEnv.linlin(0, 1, -2.015, -1.8)
	);

	LocalOut.ar(sig);

	sig = LeakDC.ar(sig);
	sig = env * sig;
	sig = Pan2.ar(sig, \pan.kr(0), \amp.kr(1));

	Out.ar(\out.kr(0), sig)
}).add;

// Some simple parameter patterns
Pdefn(\octave, Pseq([3, 4, 3, 6, 5], inf));
Pdefn(\degree, Pseq([0]) ++ Prand([-4, 4], inf));
Pdefn(\lpf, Pexprand(580, 12000));

Pdefn(\dur, {
	Pseq([1]) ++ Pgeom(
		2/4,
		rrand(0.81, 0.84),
		[12, 16, 32, 64].choose
	);
	Ptuple([
		Pseq([1/2]) ++ Pseq([1/[3, 4, 2, 4, 6, 12].choose], 16),
		Pswing(1/3, 1)
	])
});

// alt version, more controlled durations
// Pdefn(\dur, {
// 	(
// 		Pseq([1])
// 		++ Pgeom(
// 			1/4,
// 			1 + rrand(-0.001, 0.001),
// 			[12, 16, 32, 64].choose
// 		)
// 	)
// });

// Combine our param patterns and Pclutch them with restart keys
Pdef(\ringMono, Pmono(
	\ring,

	\amp, 		1,
	[\dur, \timingOffset], 	Pdefn(\dur).repeat,

	\scale, 	Scale.partch_o6,
	\scale,		Scale.chromatic,
	\octave, 	Pclutch(Pdefn(\octave), Pkey(\stepOctave)),
	\degree, 	Pclutch(Pdefn(\degree), Pkey(\stepDegree)),
	\lpf, 		Pclutch(Pdefn(\lpf), Pkey(\stepLpf)),
));

// One true and count-1 falses
Pdefn(\stepper, Plazy({
	|count|
	Pseq([true]) ++ Pseq([false], count - 1)
}).repeat);

Pdefn(\tstepper, Plazy({
	|time|
	(Pseq([true]) ++ Pseq([false], inf)).finDur(time)
}).repeat);

// Different reset patterns for each parameter - these will be passed to the Pclutch
// of each of these streams.
Pdef(\step1, Pbind(
	\stepOctave, 	Pdefn(\stepper) <> Prand([24, 16], inf),
	\stepDegree, 	Pdefn(\stepper) <> Prand([3, 6, 9], inf),
	\stepLpf, 		Pdefn(\stepper) <> Pwhite(8, 15),
));

Pdef(\step2, Pbind(
	\stepOctave, 	Pdefn(\stepper) <> 8,
	\stepDegree, 	Pdefn(\stepper) <> 6,
	\stepLpf, 		Prand([true, false, false], inf),
));

Pdef(\stepSlow, Pbind(
	\stepOctave, 	Pdefn(\tstepper) <> 2,
	\stepDegree, 	Pdefn(\tstepper) <> 1.5,
	\stepLpf, 		Pdefn(\tstepper) <> 4,
));

// Compose our patterns above, \ringMono and a step pattern,
// and four streams running in parallel panned.
// Vary some of the parameters (coef and freq) over time.
Pdef(\player,
	Ppar(
		4.collect {
			|i|
			Pbind(
				\pan, i.linlin(0, 3, -1.0, 1.0)
			)
			<> Pdef(\ringMono)
			<> Pdef(\stepSlow) // <-- try different restart patterns
		}
	)
	.finDur(8).repeat // <-- reset everything every 6 beats

	<> Pbind(
		\coef, Pseg([0.9, 0.4, 0.9], [16, 16], \exp).repeat,
		\freq, Pkey(\freq) * Ptuple([
			1,
			Pseg([1, 1.03, 1], [16, 16], \exp).repeat
		])
	)
).play
)


