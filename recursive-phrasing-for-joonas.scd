// random synth......
(
SynthDef(\rez, {
	var makeEnv, ampEnv, lpf, sig, baseFreq, freq, freqDetune, delay, done;
	var amp, distAmt, distSig, shift;

	makeEnv = {
		|skew=0|
		Env.adsr(
			(\attackTime.kr(0.01) + skew).clip(0, inf),
			(\decayTime.kr(0.7) + skew).clip(0, inf),
			\sustainLevel.kr(-12.dbamp),
			(\releaseTime.kr(1) - (skew * 2)).clip(0, inf)
		).kr(
			gate: \gate.kr(1) + \retrig.tr(0),
		)
	};

	lpf 		= \lpf.kr(300, spec:[10, 10000]);
	baseFreq 	= \freq.kr;
	amp 		= \amp.kr(1);
	distAmt 	= \dist.kr(0.1);
	distAmt = 1;
	shift 		= \shift.kr(4);

	ampEnv = makeEnv.value(0);
	FreeSelfWhenDone.kr(ampEnv);

	freqDetune = [0, 0.13, -0.24] * amp * makeEnv.value(0.04);

	freq = baseFreq;

	freq = freq * [-12, 0, 12.02, 24.03].midiratio;
	freq = freq *.x freqDetune.midiratio;
	freq = freq.flatten + (ampEnv * LocalIn.ar(1).linlin(-1, 1, freq/2.neg, freq/2));

	sig = LFCub.ar(freq) * (ampEnv + LFPar.ar(freq, Rand(0.0, 0.1)));
	sig = sig + LFSaw.ar(freq, 0, -10.dbamp);

	sig = sig.sum;
	sig = LeakDC.ar(sig) ! 2;

	distSig = sig * distAmt.linlin(0, 1, -10, 60).dbamp;
	distSig = ampEnv * -36.dbamp * distSig.tanh;

	sig = -50.dbamp * sig;
	sig = sig.blend(distSig, distAmt);

	delay = FreqShift.ar(sig, makeEnv.value(0.1).linlin(0, 1, -0.1, shift.neg ));
	delay = delay - CombC.ar(delay, 1, 1/[14001, 14000], 0.4);
	delay = Splay.ar(delay);

	sig = LeakDC.ar(sig - delay);


	sig = amp * ampEnv * sig;
	sig = BLowPass4.ar(
		sig,
		lpf,
		0.6
	);
	LocalOut.ar(sig[0]);


	Out.ar(\out.kr(0), sig)
}).add
)







// 1. A basic pattern with this synth....
(
Pdef(\pattern,
	Pbind(
		\instrument, \rez,
		\tempo, 1.4,

		\amp, -0.dbamp,
		\dur, Pbjorklund2(3, 8) / 8,

		\legato, 1,

		\attackTime, 	0.01,
		\decayTime, 	Pexprand(0.5, 1),
		\releaseTime, 	Pexprand(0.1, 1),
		\sustainLevel,  -50.dbamp,
		\dist, 			0.1,
		\shift,         1,

		\lpf, 			Pseg([200, 12000], [4], \exp).repeat,

		\scale, 		Scale.chromatic,
		\octave, 		3,
		\degree, 		Pseq([0, 4, 8, -3, 5], inf) + Pxrand([ [0, 5, 20] ], inf),
	)
).play
)









// 2. Split in two patterns. One is my original \pattern. The other is \phrase,
//    which I'll give: (\dur, 2, \legato 1)
//    Because it's a phrase pointing to my original \pattern name, it will
//    effectively play that pattern as if it were an event itself: \pattern
//    gets re-triggered every 2 beats, and it's duration is limited by \legato
//    or \sustain. Experiment w/ changing the \dur and \legato, it should be obvious
//    what's happening. Phrases will actually CLIP the pattern - meaning (\dur, 2, \legato, 1)
//    will only play 2 beats worth of \pattern before stopping. With \legato > 1,
//    one \pattern will start to overlap with the next event, so e.g. \legato, 2 would
//    mean you'll have 2 \pattern's overlapping at any given point. Also, if the inner
//    pattern runs out of event (e.g. you have a pattern without inf repeats), it will
//    of course stop as well.
//
//    No need to play \pattern, \phrase is the one driving now so play that....
(
Pdef(\pattern,
	Pbind(
		\instrument, \rez,
		\tempo, 1.4,

		\amp, -0.dbamp,
		\dur, Pbjorklund2(3, 8) / 8,

		\legato, 1,

		\attackTime, 	0.01,
		\decayTime, 	Pexprand(0.5, 1),
		\releaseTime, 	Pexprand(0.1, 1),
		\sustainLevel,  -50.dbamp,
		\dist, 			0.1,
		\shift,         1,

		\lpf, 			Pseg([200, 12000], [2], \exp).repeat,

		\scale, 		Scale.chromatic,
		\octave, 		3,
		\degree, 		Pseq([0, 4, 8, -3, 5], inf) + Pxrand([ [0, 5, 20] ], inf),
	)
);

Pdef(\phrase, Pbind(
	\type, \phrase,
	\instrument, \pattern,

	\dur, 2,
	\legato, 1
)).play
)


// 3. In general, the inner phrase pattern events (\pattern here) inherit ALL
//    of the keys of the outer event. So, e.g., I'll remove \octave from \pattern
//    and instead specify it in \phrase. Now, the octave changes every time the
//    phrase is retriggered, and stays the same for the duration of that phrase.
(
Pdef(\pattern,
	Pbind(
		\instrument, \rez,
		\tempo, 1.4,

		\amp, -0.dbamp,
		\dur, Pbjorklund2(3, 8) / 8,

		\legato, 1,

		\attackTime, 	0.01,
		\decayTime, 	Pexprand(0.5, 1),
		\releaseTime, 	Pexprand(0.1, 1),
		\sustainLevel,  -50.dbamp,
		\dist, 			0.1,
		\shift,         1,

		\lpf, 			Pseg([200, 12000], [2], \exp).repeat,

		\scale, 		Scale.chromatic,
		\degree, 		Pseq([0, 4, 8, -3, 5], inf) + Pxrand([ [0, 5, 20] ], inf),
	)
);

Pdef(\phrase, Pbind(
	\type, \phrase,
	\instrument, \pattern,

	\dur, 2,
	\legato, 1,
	\octave, Pseq([3, 4, 5, 4], inf).trace(prefix:"octave:")
)).play
)


// 4. Slightly more interesting \durs on the outer pattern....
(
Pdef(\pattern,
	Pbind(
		\instrument, \rez,
		\tempo, 1.4,

		\amp, -0.dbamp,
		\dur, Pbjorklund2(3, 8) / 8,

		\legato, 1,

		\attackTime, 	0.01,
		\decayTime, 	Pexprand(0.5, 1),
		\releaseTime, 	Pexprand(0.1, 1),
		\sustainLevel,  -50.dbamp,
		\dist, 			0.1,
		\shift,         1,

		\lpf, 			Pseg([200, 12000], [2], \exp).repeat,

		\scale, 		Scale.chromatic,
		\degree, 		Pseq([0, 4, 8, -3, 5], inf) + Pxrand([ [0, 5, 20] ], inf),
	)
);

Pdef(\phrase, Pbind(
	\type, \phrase,
	\instrument, \pattern,

	\dur, Pseq([1, 2, 1, 4], inf),
	\legato, 1,

	\octave, Pseq([3, 4, 5, 4], inf)
)).play
)


// 5. If you wrap your inner \pattern in a function, then you get two things:
//    (1) that pattern is evaluated each time you play it as a new phrase.
//    (2) arguments to that function are pulled automatically from the keys
//        of the pattern triggering the phrase. This is almost identical to
//        using Pkey to refer to values, but you can also e.g. manipulate
//        these values in the function if you want to e.g. create a sequence
//        of notes.

(
Pdef(\pattern, {
	|minLPF, maxLPF|

	var degreeOffset = [0, 4, 8, -3, 5];

	"playing phrase".postln;

	Pbind(
		\instrument, \rez,
		\tempo, 1.4,

		\amp, -0.dbamp,
		\dur, Pbjorklund2(3, 8) / 8,

		\legato, 1,

		\attackTime, 	0.01,
		\decayTime, 	Pexprand(0.5, 1),
		\releaseTime, 	Pexprand(0.1, 1),
		\sustainLevel,  -50.dbamp,
		\dist, 			0.4,
		\shift,         1,

		\lpf, 			Pseg([minLPF, maxLPF], [2], \exp).repeat,

		\scale, 		Scale.chromatic,
		\degree, 		Pseq(degreeOffset, inf) + Pxrand([ [0, 5, 20] ], inf),
	)
});

Pdef(\phrase, Pbind(
	\type, \phrase,
	\instrument, \pattern,

	\dur, Pseq([1, 2, 1, 4], inf),
	\legato, 1,

	\minLPF, Pseq([100, 400, 800], inf),
	\maxLPF, Pseq([1000, 4000, 8000], inf),

	\octave, 3
)).play
)


// 5. A little more interesting: lets control \degreeOffset and
//    the \legato with our outer pattern. Also, just delete our \dist
//    key in the inner pattern so it is now getting it from the outside.

(
Pdef(\pattern, {
	|minLPF, maxLPF, degreeOffset=0, noteLegato=0.4|

	"playing phrase".postln;

	Pbind(
		\instrument, \rez,

		\amp, -0.dbamp,

		\dur, Pbjorklund2(3, 8) / 8,
		\legato, noteLegato,

		\attackTime, 	0.01,
		\decayTime, 	Pexprand(0.5, 1),
		\releaseTime, 	Pexprand(0.1, 1),
		\sustainLevel,  -50.dbamp,
		\shift,         1,

		\lpf, 			Pseg([minLPF, maxLPF], [2], \exp).repeat,

		\scale, 		Scale.chromatic,
		\degree, 		degreeOffset + Pxrand([ [0, 5, 20] ], inf),
	)
});

Pdef(\phrase, Pbind(
	\type, \phrase,
	\instrument, \pattern,

	\dur, Pseq([1, 2, 1, 4], inf),
	\legato, 1,

	\minLPF, Pseq([100, 400, 800], inf),
	\maxLPF, Pseq([1000, 4000, 8000], inf),

	\degreeOffset, Pseq([0, -2, 0, -6], inf).stutter(4),
	\noteLegato, Pseq([0.1, 0.5, 1, 2], inf),

	\dist, Pseq([0.1, 0.3, 0.4, 0.6], inf),

	\octave, 3
)).play
)




// 5. You can, as you imagine, have phrases that play phrases...
//    The recursive phrasing tutorial goes deep here, and it's really
//    fast to generate wildly complex out of control systems like this
//    but generally I think that even two levels of phrasing is already
//    a TON of musical information and complexity. Here's a simple example...
//    exactly the same as my previous pattern setup, except \pattern is
//    playing \innerPattern, which will just be two notes... so effectively
//    I'll just be doubling my notes. Note that the same rules apply....
//    \innerPattern will get clipped according to \pattern's \legato and \dur,
//    so you won't always hear both notes. If you tweak the \legato and \dur
//    in \pattern it should start to make sense.

(
Pdef(\innerPattern, Pbind(
	\instrument, \rez,

	\dur, Pseq([1/8, 1]), // just two notes, otherwise all other keys come from \pattern
));

Pdef(\pattern, {
	|minLPF, maxLPF, degreeOffset=0, noteLegato=0.4|

	"playing phrase".postln;

	Pbind(
		\type, \phrase,
		\instrument, \innerPattern,

		\amp, -0.dbamp,
		\dur, Pbjorklund2(3, 8) / 8,

		\legato, noteLegato,

		\attackTime, 	0.01,
		\decayTime, 	Pexprand(0.5, 1),
		\releaseTime, 	Pexprand(0.1, 1),
		\sustainLevel,  -50.dbamp,
		\shift,         1,

		\lpf, 			Pseg([minLPF, maxLPF], [2], \exp).repeat,

		\scale, 		Scale.chromatic,
		\degree, 		degreeOffset + Pxrand([ [0, 5, 20] ], inf),
	)
});

Pdef(\phrase, Pbind(
	\type, \phrase,
	\instrument, \pattern,

	\dur, Pseq([1, 2, 1, 4], inf),
	\legato, 1,

	\minLPF, Pseq([1000, 2000, 4000], inf),
	\maxLPF, Pseq([2000, 4000, 12000], inf),

	\degreeOffset, Pseq([0, -2, 0, -6], inf).stutter(4),
	\noteLegato, Pseq([0.1, 0.5, 1], inf),

	\dist, Pseq([0.1, 0.5, 1, 0.7], inf),

	\octave, 3
)).play
)




// 5. Once it's stable, you can generate a ton of variations pretty easily...

(
Pdef(\innerPattern, Pbind(
	\instrument, \rez,

	\dur, Pseq([1/8, 1]),
	\legato, 1/2,

	\degree, Pkey(\degree) + Pseq([ 0, 24 ]),

	\strum, Prand([0, 1/8.2], inf)
));

Pdef(\pattern, {
	|minLPF, maxLPF, degreeOffset=0, noteLegato=0.4|

	"playing phrase".postln;

	Pbind(
		\type, \phrase,
		\instrument, \innerPattern,

		\amp, -0.dbamp,
		\dur, Pbjorklund2(3, 8) / 8,

		\legato, noteLegato,

		\attackTime, 	0.01,
		\decayTime, 	Pexprand(0.5, 1),
		\releaseTime, 	Pexprand(0.1, 1),
		\sustainLevel,  -50.dbamp,

		\shift,         Pseg([1, 24, 1], [2, 2], \exp),

		\lpf, 			Pseg([minLPF, maxLPF], [4], \exp).repeat,

		\scale, 		Scale.chromatic,
		\degree, 		degreeOffset + Pxrand([ [0, 5, 20].scramble ], inf),
	)
});

Pdef(\phrase, Pbind(
	\type, \phrase,
	\instrument, \pattern,

	\dur, 4,
	\legato, 1,

	\minLPF, Pseq([1000, 2000, 4000], inf),
	\maxLPF, Pseq([2000, 4000, 12000], inf),

	// \degreeOffset, Pseq([0, -2, 0, -6], inf).stutter(4),
	\noteLegato, Pseq([0.1, 0.5, 1, 2], inf),

	\dist, Pseq([0.1, 0.5, 1, 0.7], inf),

	\octave, 4
)).play
)







