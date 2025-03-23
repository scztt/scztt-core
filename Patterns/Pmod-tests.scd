(
SynthDef(\saw, {
    var env, sig;
    
    sig = Saw.ar(
        \freq.kr(440) * (\fmod.ar(0).midiratio * [-1, 1]),
        \amp.kr(1)
    );
    env = Env.adsr(releaseTime: \release.kr(1));
    env = env.kr(
        gate: \gate.kr(1),
        timeScale: \sustain.kr,
        doneAction: 2
    );
    
    sig = \lpf.kr([100, 3000]).collect {
        |lpf, i|
        RLPF.ar(
            sig[i],
            lpf,
            0.6
        )
    };
    sig = LeakDC.ar(sig);
    sig = Rotate2.ar(sig[0], sig[1], pi + (SinOsc.kr(1/2) * 0.2));
    sig = env * sig;
    
    sig = Balance2.ar(
        sig[0], sig[1],
        \pan.kr(0)
    );
    
    Out.ar(\out.ir, sig);
}).add;

Pdef(\base, Pbind(
    \instrument, \saw,
    \octave, [4, 4],
    \dur, 1/6,
    \release, Pwhite(1, 2.9),
    \legato, Pwhite(0.7, 1.9),
    \scale, Scale.harmonicMinor,
    \degree, Ptuple([
        0,
        Pseq([
            Pseq([-2, 3, 7, 3, -4], 8),
            Pseq([-2, 3, 7, 3, -4] + [-3, 0], 8),
        ], inf)
    ], inf),
));
)

// Basic kr
(
Pdef(\basicMod, Pbind(
    \lpf, Pmod({
        [1300, 1300] * Env.perc(0.01, 0.2).kr(gate:\trig.tr(1))
    }, \trig, 1),
) <> Pdef(\base)).play;
)


// Explicit rate
(
Pdef(\basicMod, Pbind(
    \amp, [0.5, 0.5],
	\dur, Pxrand([1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6] / 12, inf),
    \lpf, Pmod({
        [8300, 8300] * Env.perc(0.02, \env.kr).kr(gate:\trig.tr(1))
    }, \trig, 1, \env, Pseg([0.1, 3, 0.1], [16, 16], \exp, inf)),
    \freq, Pmod(
        Pxrand([
            { \mult.kr.midiratio * SinOsc.kr(12, Rand(0, 1) ! 2).exprange(400, 5000) },
            { \mult.kr.midiratio * SinOsc.kr(8, Rand(0, 1) ! 2).exprange(50, 1000) },
            { \mult.kr.midiratio * SinOsc.kr(6, Rand(0, 1) ! 2).exprange(10, 400) },
            { \mult.kr.midiratio * SinOsc.kr(4, Rand(0, 1) ! 2).exprange(10, 400) },
        ], inf).durStep(Prand([1, 2, 3, 4, 6] / 8, inf)),
        \fadeTime, 0.001,
        \mult, Pseq([0, -3], inf).durStep(8)
    ).expand,
) <> Pdef(\base)).play;
)
// Explicit rate
(
Pdef(\basicMod, Pbind(
    \lpf, Pmod.kr({
        SinOsc.kr(1/8, [0, 0.3]).exprange(420, 5000)
    }),
) <> Pdef(\base)).play;
)

// Explicit rate and channels, with coercion
(
Pdef(\basicMod, Pbind(
    \lpf, Pmod.kr2({
        SinOsc.ar(1/8, 0).exprange(420, 5000)
    }),
) <> Pdef(\base)).play;
)

// Single values with lag
(
Pdef(\basicMod, Pbind(
    \lpf, Pmod(
        Pseq([300, 800, 4000], inf).stutter(3),
        \paramLag, 0.1
    ),
) <> Pdef(\base)).play;
)



// Basic ar
(
Pdef(\basicMod, Pbind(
    \fmod, Pmod({
        SinOsc.ar(1/8) * SinOsc.ar(200).range(-0.5, 0.5)
    }),
) <> Pdef(\base)).play;
)

// Pattern kr
(
Pdef(\basicMod, Pbind(
    \lpf, Pmod(
        { \f.kr(100).lag3([0.01, 1]) },
        \f, Pexprand(120, 8000).stutter(3)
    )
) <> Pdef(\base)).play;
)

// Pattern kr with two filters
(
Pdef(\basicMod, Pbind(
    \lpf, Pmod(
        { \f.kr(100).lag3([0.01, 1]) },
        \f, Ptuple([
            Pexprand(120, 8000).stutter(4),
            Pexprand(120, 8000).stutter(6)
        ], inf)
    )
) <> Pdef(\base)).play;
)

// Pattern kr as fixed values
(
Pdef(\basicMod, Pbind(
    \lpf, Pmod(
        { \f.kr(100).lag3([0.01, 1]) },
        \f, Ptuple([
            Pexprand(120, 8000).stutter(4),
            Pexprand(120, 8000).stutter(6)
        ], inf)
    ).asValues
) <> Pdef(\base)).play;
)

Pdef(\basicMod).clear;

// Pmod modulating Pmod
(
Pdef(\basicMod, Pbind(
    \lpf, Pmod(
        { \f.kr(100).lag3([0.01, 1]) },
        \f, Pmod({
            LFDNoise3.kr(1).exprange(80, 12000)
        })
    )
) <> Pdef(\base)).play;
)

// 2 Pmods modulating Pmod
// Note: expand converts a 2-channel modulator into an arrayed value.
//       This causes channel expansion when event processing - it is
//       equivalent to eg. \freq, [100, 200] spawning two events,
//       rather than \freq, [[100, 200]] passing an array value to a
//       single synth arg.
(
Pdef(\basicMod, Pbind(
    \lpf, Pmod(
        { \f.kr(100).lag3([0.01, 1]) },
        \f, Pmod.kr({
            LFDNoise3.kr([1, 6]).exprange(80, 12000)
        }).expand
    )
) <> Pdef(\base)).play;
)



// Pmono
(
Pdef(\basicMod, Pmono(
    \saw,
    // \degree, Pfunc({ |e| e.degree +.x [0, 0.02] }),
    // \fmod, Pmod({
    // 	Decay.kr(\trig.tr, 0.2) * SinOsc.ar(200).range(-15, 15)
    // }, \trig, Pseq([1, 0, 0, 1, 0, 0, 0], inf)),
    \lpf, Pmod(
        { \f.kr(100).lag3([0.01, 1]) },
        \f, Pmod({
            LFDNoise3.kr([1, 6]).exprange(80, 12000)
        })
    )
).trace <> Pdef(\base)).play;
)


// \type, \set
// requires a previous Pmod to be defined
(
Ndef(\basicMod, \saw).play;

Pdef(\set, Pbind(
    \type, \set,
    \args, [\pan, \freq],
    \id, Ndef(\basicMod).group,
    \group, Ndef(\basicMod).group,
    \pan, Pmod({
        LFPulse.kr(1/3).range(-0.5, 0.5).lag(0.1)
    })
) <> Pdef(\base)).play
)
