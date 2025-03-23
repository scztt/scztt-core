
UDef(\grainSampler, ar: {
    |
        buf = -1, in, inAmp=(0), feedInBus=(-1), feedOutBus=(-1), amp=0, trig=0, ttrig=0, grainDur=0.25,
        grainRate=1, grainRateV=0,
        pos=0, posV=0, posRound=0, posRoundOffset=0, posMod=0, posDither,
        pan=0, panV=0, env=0.5,
        feedAmp=(-20), filtHi=4000, filtLo=20, feedDist=2,
        interpolate=4,
        numChannels=2
    |
    
    var sig, feedIn, bufSize, scaledPos, dur, posLow, posHigh, posRemainder;
    var time = Sweep.ar(1);
    
    // PARAMETERS
    trig    = trig + ttrig;
    
    dur     = grainDur;
    
    bufSize = BufSamples.kr(buf);
    
    amp		= amp;
    feedAmp = feedAmp.dbamp;
    
    pos	= pos + posMod + TRand.ar(-1, 1, trig).linlin(-1, 1, posV.neg, posV);
    
    // RANDOM WEIGHT
    // posRemainder = (pos - posRoundOffset) / posRound.max(0.0000001);
    // posLow = posRemainder.floor;
    // posHigh = posRemainder.ceil;
    // posRemainder = posRemainder - posLow;
    
    // pos = (posRound > 0).if(
    //     (
    //         TWChoose.ar(trig, [posLow, posHigh], [1 - posRemainder, posRemainder], 0)
    //             * posRound
    //             + posRoundOffset
    //     ),
    //     pos
    // );
    
    // DITHERED WEIGHT
    posRoundOffset = posRoundOffset * posRound;
    posRemainder = (pos - posRoundOffset) / posRound.max(0.0000001);
    posLow = posRemainder.floor;
    posHigh = posRemainder.ceil;
    posDither = WrapSummer.ar(trig, min: -0.5, max: 0.5, step: posRemainder.mod(1));
    
    // ((posRemainder + posDither).round * posRound).poll(trig, label:"final pos");
    
    pos = (posRound > 0).if(
        (posRemainder + posDither).round * posRound.max(0.0000001) + posRoundOffset,
        pos
    );
    
    pos	= max(pos, 0).asArray;
    
    grainRate = TRand.ar(-1.0, 1.0, trig).lincurve(-1.0, 1.0,
        grainRate * 2.pow(grainRateV.neg),
        grainRate * 2.pow(grainRateV),
        0
    );
    grainRate = grainRate.asArray;
    
    pan		= TRand.ar(pan - panV, pan + panV, trig);
    pan     = pan.asArray;
    
    if (numChannels > 2) {
        pan = pan.linlin(-1, 1, -1 / numChannels, 2 - (3 / numChannels))
    };
    
    sig = trig.asArray.collect {
        |t, i|
        t;
        TGrains2.ar(
            numChannels,
            dur: 		dur.min(100),
            trigger: 	trig,
            rate: 		grainRate[i],
            bufnum: 	buf,
            centerPos:	pos[i],
            amp: 		amp,
            pan:        pan[i],
            att: 		env * dur,
            dec: 		(1 - env) * dur,
            interp: 	interpolate,
            maxGrains:  1200
        ).asArray
    };
    sig = sig.sum;    
    sig.assertChannels(numChannels);
    
    sig = BLowPass.ar(BHiPass.ar(sig, filtLo), filtHi);
    
    sig = (1 * sig);
});

SynthDef.channelized(\grainSampler, {
    |numChannels|
    var arArgs, krArgs, trArgs, lagArgs, func, argEnv, lagMult, lagAdd;
    var sig;
    var baseEnv, ampEnv, sustain, gate, envScale, maxEnvSize=8;
    
    func = UDef(\grainSampler).arFunc;
    
    baseEnv = { |v| Env(v ! maxEnvSize, ((1 ! (maxEnvSize - 1).normalizeSum))) };
    
    sustain = \sustain.kr(1);
    gate = \gate.kr(1);
    envScale = \envScale.kr(0);
    envScale = (envScale > 0).if(envScale, sustain);
    
    lagArgs = [\lagMult, \lagAdd, \grainDurLag, \rateLag, \posLag, \panLag, \envLag, \feedLag, \filtLag, \panLag, \ampLag];
    
    arArgs = [\amp, \in, \trig, \trigRate, \trigRateV, \grainDur, \grainRate, \grainRateV, \pos, \posV, \posMod, \pan, \panV, \env, \density, \posRound, \posRoundOffset];
    krArgs = lagArgs ++ [\buf, \feedInBus, \feedOutBus, \feedAmp, \filtHi, \filtLo, \feedDist, \inAmp, \interpolate];
    trArgs = [\ttrig];
    
    argEnv = ();
    
    arArgs.do {
        |name|
        var default = func.def.argNames.indexOf(name);
        default = default !? { func.def.prototypeFrame[default] } ?? 0;
        argEnv[name] = name.ar(default);
    };
    
    krArgs.do {
        |name|
        var default = func.def.argNames.indexOf(name);
        default = default !? { func.def.prototypeFrame[default] } ?? 0;
        argEnv[name] = name.kr(default);
    };
    
    trArgs.do {
        |name|
        var default = func.def.argNames.indexOf(name);
        default = default !? { func.def.prototypeFrame[default] } ?? 0;
        argEnv[name] = name.tr(default);
    };
    
    argEnv[\grainDur] 	= argEnv[\grainDur].lag2(argEnv[\lagMult] * argEnv[\grainDurLag] + argEnv[\lagAdd]);
    argEnv[\grainRate] 	= argEnv[\grainRate].lag2(argEnv[\lagMult] * argEnv[\rateLag] + argEnv[\lagAdd]);
    argEnv[\grainRateV] = argEnv[\grainRateV].lag2(argEnv[\lagMult] * argEnv[\rateLag] + argEnv[\lagAdd]);
    argEnv[\pos] 		= argEnv[\pos].lag2(argEnv[\lagMult] * argEnv[\posLag] + argEnv[\lagAdd]);
    argEnv[\posV] 	    = argEnv[\posV].lag2(argEnv[\lagMult] * argEnv[\posLag] + argEnv[\lagAdd]);
    argEnv[\pan] 		= argEnv[\pan].lag2(argEnv[\lagMult] * argEnv[\panLag] + argEnv[\lagAdd]);
    argEnv[\panV] 		= argEnv[\panV].lag2(argEnv[\lagMult] * argEnv[\panLag] + argEnv[\lagAdd]);
    argEnv[\env] 		= argEnv[\env].lag2(argEnv[\lagMult] * argEnv[\envLag] + argEnv[\lagAdd]);
    argEnv[\amp] 		= argEnv[\amp].lag2(argEnv[\lagMult] * argEnv[\ampLag] + argEnv[\lagAdd]);
    argEnv[\feedAmp] 	= argEnv[\feedAmp].lag2(argEnv[\lagMult] * argEnv[\feedLag] + argEnv[\lagAdd]);
    argEnv[\filtHi] 	= argEnv[\filtHi].lag2(argEnv[\lagMult] * argEnv[\filtLag] + argEnv[\lagAdd]);
    argEnv[\filtLo] 	= argEnv[\filtLo].lag2(argEnv[\lagMult] * argEnv[\filtLag] + argEnv[\lagAdd]);
    argEnv[\feedDist] 	= argEnv[\feedDist].lag2(argEnv[\lagMult] * argEnv[\feedLag] + argEnv[\lagAdd]);
    argEnv[\numChannels]= numChannels;
    
    argEnv[\trigRate] 	= argEnv[\trigRate].max(1/10);
    
    argEnv[\trig] = (
        argEnv[\ttrig]
            + argEnv[\trig]
            + Impulse.ar(0)
            + TDuty.ar(
                (argEnv[\trigRate] * 2.pow(
                    Dseq(Dgauss(argEnv[\trigRateV].neg, argEnv[\trigRateV], 9999999), 9999999)
                )).reciprocal,
                0,
                1
            )
    );
    
    // If density is provided, skip grainDur and use this to determine duration
    argEnv[\grainDur] = (
        (argEnv[\density] > 0).if(
            argEnv[\density] / argEnv[\trigRate],
            argEnv[\grainDur]
        )
    );
    
    ////////////////////////////////////////////////////////////////////////////////////////
    sig = func.performWithEnvir(\value, argEnv);
    ////////////////////////////////////////////////////////////////////////////////////////    
    
    ////////////////////////////////////////////////////////////////////////////////////////
    // AMP ENVELOPE
    baseEnv = Env(
        [0, 1] ++ (0 ! (maxEnvSize - 2)),
        [0] ++ (0 ! (maxEnvSize - 2)),
        0,
        releaseNode:1
    );
    
    sustain = \sustain.kr(1);
    gate = \gate.kr(1);
    envScale = \envScale.kr(0);
    envScale = (envScale > 0).if(envScale, 1);
    
    ampEnv = \ampEnv.ir(baseEnv);    
    ampEnv = EnvGen.ar(ampEnv, gate: gate, timeScale: envScale, doneAction:2);
    
    sig = sig * ampEnv;
    
    ////////////////////////////////////////////////////////////////////////////////////////
    
    OffsetOut.ar(\out.kr(0), sig);
}, channelizations:[2, 4, 8, 32]);

UDef(\grainSampler).wrap(\env, ar: {
    |func, numChannels|
    
    var arArgs, krArgs, trArgs, lagArgs, envArgs, argEnv, lagMult, lagAdd;
    var sig, sustain, ampEnv;
    var baseEnv, tailDur, gate, envScale;
    
    var maxEnvSize 
    = 6;
    
    baseEnv = { |v| Env(v ! maxEnvSize, ([1] ++ (0 ! maxEnvSize)).normalizeSum) };
    // baseEnv = { |v| Env.newClear(6) };
    sustain = \sustain.kr(1);
    gate = \gate.kr(1);
    envScale = \envScale.kr(0);
    envScale = (envScale > 0).if(envScale, sustain);
    
    envArgs = [
        \grainDur,
        \trigRate, \trigRateV,
        \density,
        \pos, \posV,
        \grainRate, \grainRateV,
        \pan, \panV,
        \env
    ];
    arArgs = [\posMod, \amp, \trig, \grainRateMod, \posRound, \posRoundOffset];
    krArgs = lagArgs ++ [\buf, \feedInBus, \feedOutBus, \filtLo, \filtHi, \interpolate];
    
    argEnv = ();
    
    envArgs.do {
        |name|
        var default = func.def.argNames.indexOf(name);
        default = default !? { func.def.prototypeFrame[default] } ?? 0;
        argEnv[name] = EnvGen.ar(name.ir(baseEnv.(default).asArray), gate:gate, timeScale: envScale);
    };
    
    arArgs.do {
        |name|
        var default = func.def.argNames.indexOf(name);
        default = default !? { func.def.prototypeFrame[default] } ?? 0;
        argEnv[name] = name.ar(default);
    };
    
    krArgs.do {
        |name|
        var default = func.def.argNames.indexOf(name);
        default = default !? { func.def.prototypeFrame[default] } ?? 0;
        argEnv[name] = name.kr(default);
    };
    
    argEnv[\trig] = 
        argEnv[\trig]
        + Impulse.ar(0) 
        + TDuty.ar(
            (argEnv[\trigRate] * 2.pow(
                Dseq(Dgauss(argEnv[\trigRateV].neg, argEnv[\trigRateV], 9999999), inf)
            )).reciprocal,
            0,
            1
        );    
    
    argEnv[\trigRate] = argEnv[\trigRate].max(1/10);
    // If density is provided, skip grainDur and use this to determine duration
    argEnv[\grainDur] = (
        (argEnv[\density] > 0).if(
            argEnv[\density] / argEnv[\trigRate],
            argEnv[\grainDur]
        )
    );
    
    argEnv[\grainRate] = argEnv[\grainRate] + argEnv[\grainRateMod];
    
    argEnv[\ttrig] = 0;
    argEnv[\pan];
    argEnv[\numChannels] = numChannels;
    
    ampEnv = EnvGen.ar(\ampEnv.ir(baseEnv.(1).asArray), gate:gate, timeScale: envScale, doneAction:2);
    
    sig = func.performWithEnvir(\value, argEnv); 
    
    sig = sig * ampEnv;
    // DetectSilence.ar(sig.sum, doneAction:2); // if we don't doneAction above...
    sig;
});

SynthDef.channelized('grainSampler.env', {
    |numChannels, out|
    Out.ar(out, UDef('grainSampler.env').ar.(numChannels:numChannels));
}, channelizations:[2, 4, 8, 16, 32]);

Event.addParentType('grainSampler.env', (
    baseInstrument: 'grainSampler.env',
    args: 		'grainSampler.env_2ch'.asSynthDesc.controlNames,
    channels: 	2,
    envArgs:    ~grainSamplerEnvArgs,
    finish:     {
        |e|
        ~instrument = "%_%ch".format(
            ~baseInstrument,
            ~channels
        ).asSymbol;
        
        ~envArgs.do {
            |name|
            var val = currentEnvironment[name].value;
            if (val.notNil) {
                if (val.isArray.not) {
                    val = [val]
                };
                
                if (val.isArray) {
                    val = val.collect({
                        |v|
                        if (v.isNumber) {
                            v = Env([v, v], [1]);
                        };
                        v.tryPerform(\duration_, 1)
                    })
                };
                
                currentEnvironment[name] = val;
            }
        };
    }
));





