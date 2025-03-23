(
var cleck = (
    curve: -4,
    envBase: { Env([0, 1], [1]) },
    env: 0.5,
    amp: 1,
    interpolate: 4,
    
    // TRIGGERING
    trig: 0,
    trOctave: 3,
    trDegree: 0,
    trDetune: 0,
    trDegreeSkew: 0,
    trEnv: { ~envBase.value.copy.curves_(~trCurve.value) },
    trCurve: \exp,
    trSkew: { ~trDegreeSkew.value.midiratio },
    trFreqMult: 1,
    trDetunedFreq: {
        var e = (
            octave: ~trOctave.value,
            degree: ~trDegree.value,
            scale: ~scale.value,
            detune: ~trDetune.value,
            note: ~trNote.value,
            midinote: ~trMidinote.value,
            freq: ~trFreq.value,
            harmonic: ~trFreqMult.value
        );
        e.parent = Event.parentEvents.default;
        e.use({ ~detunedFreq.value });
    },
    trigRate: {
        var min = ~trDetunedFreq.value;
        var max = min * ~trSkew.value;
        var env = ~trEnv.value.copy;
        env = [min, max, env].flop.collect({
            |v|
            #min, max, env = v;
            (env.copy.levels = ControlSpec(min, max, warp:\exp).map(env.levels));
        });
    },
    trv: 0,
    trvSkew: 0,
    trvCurve: { ~trCurve },
    trvEnv: { ~envBase.value.copy.curves_(~trvCurve.value) },
    trigRateV: {
        var min = ~trv.value;
        var max = min + ~trvSkew.value;
        var env = ~trvEnv.value;
        
        [min, max, env].flop.collect {
            |v|
            #min, max, env = v;
            env.copy.levels = env.levels.linlin(0, 1, min, max)
        }
    },
    
    // GRAIN RATE    
    grScale: Scale.chromatic,
    gr: { 1 },
    grSkew: { 0 },
    grDegree: { ~grScale.performKeyToDegree(~gr.value.ratiomidi) },
    grDegreeSkew: {
        var gr = ~gr.value;
        ~grScale.performKeyToDegree(
            ((gr + ~grSkew.value) / gr).ratiomidi
        )
    },
    grCurve: { ~curve },
    grEnv: { ~envBase.value.copy.curves_(~grCurve.value) },
    grainRate: {
        var min = ~grDegree.value;
        var max = min + ~grDegreeSkew.value;
        var env = ~grEnv.value.copy;
        [min, max, env].flop.collect {
            |v|
            #min, max, env = v;
            env.copy.levels = env.levels.linlin(0, 1, min, max)
                .collect(~scale.performDegreeToKey(_))
                .midiratio
        }
    },
    
    grv: 0,
    grvSkew: 0,
    grvCurve: { ~grCurve },
    grvEnv: { ~envBase.value.copy.curves_(~grvCurve.value) },
    grainRateV: {
        var min = ~grv.value;
        var max = min + ~grvSkew.value;
        var env = ~grvEnv.value;
        
        [min, max, env].flop.collect {
            |v|
            #min, max, env = v;
            env.copy.levels = env.levels.linlin(0, 1, min, max)
        }
    },
    
    // POSITION
    posMult: 1,
    posBase: 0,
    posSkew: { ~posMult * ~sustain.value },
    posEnv: { ~envBase.value.copy.curves_(~posCurve.value) },
    posCurve: 0,
    pos: {
        var min, max, env;
        
        min = ~posBase.value;
        max = min + ~posSkew.value;
        env = ~posEnv.value;
        
        env = [min, max, env].flop.collect {
            |v|
            #min, max, env = v;
            env.copy.levels = env.levels.linlin(0.0, 1.0, min, max).collect(_.value)
        };
    },
    posVBase: 0,
    posVSkew: 0,
    posVCurve: { ~curve },
    posVEnv: { ~envBase.value.copy.curves_(~posVCurve.value) },
    posV: {
        var min = ~posVBase.value;
        var max = min + ~posVSkew.value;
        var curve = ~posVCurve.value;
        var env = ~posVEnv.value;
        
        [min, max, env].flop.collect {
            |v|
            #min, max, env = v;
            env.copy.levels = env.levels.linlin(0.0, 1.0, min, max)
        }
    },
    
    panBase: 0,
    panSkew: 0,
    panCurve: 0,
    panEnv: { ~envBase.value.copy.curves_(~panCurve.value) },
    pan: {
        var min = ~panBase.value;
        var max = min + ~panSkew.value;
        var env = ~panEnv.value;
        
        ~pan = [min, max, env].flop.collect({
            |v|
            #min, max, env = v;
            env.copy.levels = env.levels.linlin(0.0, 1.0, min, max)
        });        
    },
    
    attack: 0.5,
    ampCurve: { 8 },
    ampEnvShape: {
        Env.perc(0.5, 0.5, curve: ~ampCurve.value)
    },
    ampEnv: {
        [~attack.value, ~ampCurve.value, ~ampEnvShape.value].flop.collect {
            |v|
            var attack = v[0].value, ampCurve = v[1].value;
            var env = v[2].copy;
            env.times = [attack] ++ 
                ((1 - attack) * env.times[1..].normalizeSum);
            env
        }
    },
    
    densityBase: 4,
    densitySkew: 0,
    densityCurve: { ~curve },
    densityEnv: { [~ampEnv.value].unbubble(1).collect({
        |e|
        e.copy.curves_(~densityCurve.value)
    }) },
    density: {
        var min = ~densityBase.value;
        var max = min + ~densitySkew.value;
        var env = ~densityEnv.value;
        
        [min, max, env].flop.collect {
            |v|
            #min, max, env = v;
            env.copy.levels = env.levels.linlin(0, 1, min, max)
        };
    },
    
    // swing: 0,
    // swingScale: 1,
    // timingOffset: {
    // 	var time = thisThread.beats;
    // 	if ((time % ~swingScale) >= (~swingScale / 2)) {
    // 		~swingScale = ~swing
    // 	} { 0 }
    // }
);

~cleck = cleck;

Require("GrainDelay2");
Require("GrainSampler");

Event.addParentType(\cleck2, cleck.copy.putAll(Event.parentTypes['grainSampler.env']));
Event.addParentType(\cleck2Delay, cleck.copy.putAll(Event.parentTypes['grainDelay2.env']).putAll((instrument: 'grainDelay2.env')));
Event.addParentType(\cleck2Linked, cleck.copy.putAll(Event.parentTypes['grainDelay2.env']).putAll((instrument: 'grainDelay2.linked')));
Event.addParentType(\cleck2EnvLinked, cleck.copy.putAll(Event.parentTypes['grainDelay2.env.linked']).putAll((instrument: 'grainDelay2.env.linked')));
)
