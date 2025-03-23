~setupGrainDelay = {
    // ~grainEnvBuf ?? {
    // 	fork {
    // 		s.sync;
    // 		~grainEnvBuf = Buffer.loadCollection(s, Signal.hanningWindow(1/4 * Server.default.sampleRate).asArray);
    // 	};
    // };
    
    SynthDef(\grainDelay2Writer, {
        var pos, in, buffer, inLast, inChanged, inLag;
        
        // in = \in.kr(0);
        // in = in.fadeChange(
        //     {
        //         |in|
        //         In.ar(in, 1)
        //     },
        //     \fadeTime.kr(1/16)
        // );
        in = \in.ar(0);
        
        buffer = \buffer.kr(0);
        pos = Phasor.ar(0, BufRateScale.kr(buffer), 0, BufFrames.kr(buffer));
        
        BufWr.ar(in, buffer, pos, 1);
        
        OffsetOut.ar(\prOut.ir, pos);
    }).addReplace;
};

Pdef(\grainDelay2Writer, Pbind.mono(
    \instrument, \grainDelay2Writer,
    \callback, Prout({
        |e|
        e[\callback] = e[\callback].addFunc {
            "clearing buffer".postln;
            ~buf.clear;
        };
        e = e.yield;
    })
));

~freeGrainDelay = {
    // ~grainEnvBuf = nil;
};

~feedProcessDefault = {
    |feedSig, in, feedDist, feedAmp, filtHi, filtLo|
    feedSig	= blend(
        feedSig,
        SoftClipAmp4.ar(
            feedSig,
            feedDist,
            feedDist.linlin(0, 0.5, 2, 1)
        ),
        feedDist.linlin(0, 0.5, 0, 1)
    );
    
    feedSig = feedAmp * feedSig;
    // feedSig = BHiPass.ar(feedSig, filtLo, 6);
    // feedSig = BLowPass.ar(feedSig, filtHi, 6);
    feedSig = LPF.ar(RLPF.ar(feedSig, filtHi, 0.707), filtHi);
    feedSig = HPF.ar(RHPF.ar(feedSig, filtLo, 0.707), filtLo);
    feedSig	= LeakDC.ar(feedSig);
    
    feedSig = SoftKneeCompressor.peak(
        feedSig,
        (in + feedSig).mean,
        feedAmp * -22.dbamp,
        1/200,
        9,
        0.02,
        0.5
    );
    feedSig = Compander.ar(feedSig, (in + feedSig).mean, -3.dbamp, 1, 1/1000);
};

ServerBoot.add(~setupGrainDelay);
ServerQuit.add(~freeGrainDelay);
if (Server.default.serverRunning) { ~setupGrainDelay.() };

// ~grainDelayBuf = nil;
// ~grainDelayBuf.plot;

UDef(\grainDelay2Base, ar: {
    |buf, in, inAmp=1, dryAmp=0, dryDelay=0, preDelay=0, pos,
        amp=1, trig=0, ttrig=0, grainDur=0.25,
        grainRate=1, grainRateV=0,
        delay=0, delayV=0, delayCurve=0,
        decay=999, rise,
        pan=0, panV=0,
        delayRound=0, delayRoundOffset=0,
        env=0.5,
        feedAmp=1, filtLo=20, filtHi=10000, feedDist=0,
        initialWriteProxy=0, feedOut,
        gate=1, envBuf=(-1),
        feedProcess=({|s|s}),
        lagMult=1, lagAdd=0, prePost=0,
        freeze=(DC.ar(0)),
        channels=2
    |
    
    var sig, dryIn, dryPan, feedSig, feedInSig, bufSize, scaledPos, dur, grainAmp, halfGrain, usingExternalBuffer, delayLow, delayHigh, delayRemainder,
        calcDelay, delayMin, delayMax;
    
    // Basic smoothing
    dryAmp = dryAmp.lag(1/64);
    dryPan = pan.lag(1/8);
    dryIn = in;
    
    // SETUP /////////////////////////////////////////////////////////////////////////
    if (pos.isNil && buf.isNil) {
        usingExternalBuffer = false;
        
        // In this case, pos is nil, so we use an internal buffer
        if (buf.notNil) { "\\pos is nil but external buffer was specified".warn };
        if (dryIn.isNil) { "No \\in specified".warn };
        
        // Setup LocalBuf
        buf = [LocalBuf(SampleRate.ir * 10)];
        bufSize = BufSamples.kr(buf[0]);
        
        rise !? {
            buf = buf.add(LocalBuf(SampleRate.ir * 17));
        };
        
        buf.do(ClearBuf(_));
        
        // Write our input into the delay line
        
        // ramp in input to avoid clicks in our delay history
        in = in * inAmp.lag(1/32) * Env([0, 1], [1/32], curve:\sine).kr(gate:1);
        
        pos = Phasor.ar(0, 1 - freeze, 0, bufSize);
        // pos = DelTapWr.ar(buf, in);
        // [Phasor.ar(0, 1, 0, bufSize), pos].poll;
        
        initialWriteProxy = buf.collect({
            |b|
            BufWr.ar(DelayN.ar(in, 4, preDelay), b, pos, 1)
        });
        initialWriteProxy = initialWriteProxy * DC.ar(0);
    } {
        usingExternalBuffer = true;
        
        // In this case, pos is non-nil, so assume we're using an extenal buffer
        if (buf.isNil) { "\\pos is specified, but \\buf is nil.".warn };
        // if (in.notNil) { "\\in is specified, but using an external buffer".warn };
        
        bufSize = BufSamples.kr(buf);
        in = BufRd.ar(1, buf, pos, 1, 1);
        buf = [buf];
        initialWriteProxy = 0;
    };
    
    // PARAMETERS /////////////////////////////////////////////////////////////////////////
    trig    = trig + ttrig;
    dur     = grainDur;
    
    decay = [decay];
    rise !? {
        decay = decay.add(rise)
    };
    
    delayMin = ((delay * 2.pow(delayV.neg)) - delay);
    delayMax = ((delay * 2.pow(delayV)) - delay);
    calcDelay = TRand.ar(delayMin, delayMax, trig);
    // delay = delay + (
    //     delayV
    //         * Select.ar(
    //             calcDelay > 0,
    //             [
    //                 calcDelay
    //                     .lincurve(delayMin, 0, 0, 1, delayCurve)
    //                     .linlin(0, 1, 0, delayMin),
    //                 calcDelay
    //                     .lincurve(0, delayMax, 0, 1, delayCurve)
    //                     * delayMax
    //             ]
    //         )
    // );
    delay = delay + (delayV * calcDelay);
    
    
    delayRoundOffset = delayRoundOffset * delayRound;
    delayRemainder = (delay - delayRoundOffset) / delayRound.max(0.0000001);
    delayLow = delayRemainder.floor;
    delayHigh = delayRemainder.ceil;
    delayRemainder = delayRemainder - delayLow;
    
    delay = (delayRound > 0).if(
        (
            TWChoose.ar(trig, [delayLow, delayHigh], [1 - delayRemainder, delayRemainder], 0) * delayRound
        ) + delayRoundOffset,
        delay
    );
    delay = delay.abs;
    
    grainRate = grainRate * 2.pow(TRand.ar(grainRateV.neg, grainRateV, trig));
    grainRate.assertChannels(1);
    
    pan		= pan + (panV * TRand.ar(-1, 1, trig));
    
    scaledPos = TGrains2.calcDelay(pos, delay, dur, grainRate, env * dur, (1 - env) * dur);
    
    // delayRemainder = (scaledPos - delayRoundOffset) / delayRound.max(0.0000001);
    // delayLow = scaledPos.floor;
    // delayHigh = scaledPos.ceil;
    // delayRemainder = delayRemainder - delayLow;
    
    // scaledPos = (delayRound > 0).if(
    //     (
    //         TWChoose.ar(trig, [delayLow, delayHigh], [1 - delayRemainder, delayRemainder], 0) * delayRound
    //     ) + delayRoundOffset,
    //     scaledPos
    // );
    
    grainAmp = exp(-60.dbamp.log/(decay/max(0.001, delay)));
    
    if (channels > 2) {
        pan = pan * (channels - 1 / channels);
        pan = (pan + 0.75).wrap(-1, 1);
    };
    
    sig = buf.collect {
        |b, i|
        TGrains2.ar(
            channels,
            trigger: 	trig,
            dur: 		dur,
            rate: 		grainRate,
            bufnum: 	b,
            centerPos:	scaledPos,
            amp: 		(trig * grainAmp[i]),
            pan:        pan,
            att: 		(env * dur),
            dec: 		(1 - env) * dur,
            // window:  ~grainDelayBuf,
            interp: 	4,
            maxGrains:  16000
        )
    };
    
    sig.assertChannels(buf.size, channels);
    sig = LeakDC.ar(sig);
    
    buf.do {
        |b, i|
        var feedSig = sig[i];
        feedSig = feedProcess.(feedSig, in);   
        
        sig[i] = blend(
            sig[i],
            feedSig,
            prePost  
        );
        
        if (channels == 2) {
            feedSig = LinXFade2.ar(*feedSig);
        } {
            feedSig = feedSig.mean;
        };
        feedSig = in + feedSig;
        BufWr.ar(feedSig * (freeze < 1), b, pos, 1);
    };
    
    // sig = Compander.ar(sig, sig.mean, -3.dbamp, 1.2, 1/10);
    dryIn = DelayN.ar(dryIn, 8, dryDelay);
    
    // sig = sig.collect({ |s, i| s * [1, 1][i] }).sum;
    sig = sig.sum; //.trace("grainout");
    sig.assertChannels(channels);
    
    if (channels == 2) {
        sig = (amp * sig) + Pan2.ar(dryIn, dryPan, dryAmp);
    } {
        sig = (amp * sig) + PanAz.ar(
            numChans: channels, 
            in: dryIn, 
            pos: dryPan.linlin(-1, 1, 0, 2), 
            level: dryAmp,
        );
    };
    
    sig.assertChannels(channels);
});

// UDef(\grainDelay2_feedfx_default, {});

UDef(\grainDelay2, ar: {
    |buf, pos, feedProcess, rise, channels=2|
    var arArgs, krArgs, trArgs, lagArgs, func, argEnv, lagMult, lagAdd;
    var sig, feedSig, feedBuf, feedBufPos, env, trig, seed;
    
    lagArgs = [\lagMult, \lagAdd, \grainDurLag, \grainRateLag, \delayLag, \decayLag, \panLag, \envLag, \feedLag, \filtLag, \ampLag, \densityLag];
    
    arArgs = [\in, \inAmp, \trig, \trigRate, \trigRateV, \feedDist, \density, \grainDur, \grainRate, \grainRateV, \delay, \delayV, \delayCurve, \pan, \panV, \env, \decay, \delayRound, \delayRoundLag, \delayRoundOffset];
    krArgs = lagArgs ++ [\amp, \feedAmp, \filtHi, \filtLo, \dryAmp, \dryDelay, \gate, \envBuf, \preDelay, \prePost, \freeze];
    trArgs = [\ttrig];
    
    rise !? {
        arArgs = arArgs ++ [\rise];
    };
    
    func = UDef(\grainDelay2Base).arFunc;
    
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
    
    argEnv[\channels]   = channels;
    argEnv[\lagMult].spec = ControlSpec(0, 10, default:1);
    argEnv[\lagAdd].spec = ControlSpec(0, 10, default:0);
    argEnv[\amp] 		= argEnv[\amp].lag2(argEnv[\lagMult] * argEnv[\ampLag] + argEnv[\lagAdd]);
    argEnv[\grainDur] 	= argEnv[\grainDur].lag2(argEnv[\lagMult] * argEnv[\grainDurLag] + argEnv[\lagAdd]);
    argEnv[\grainRate] 	= argEnv[\grainRate].lag2(argEnv[\lagMult] * argEnv[\grainRateLag] + argEnv[\lagAdd]);
    argEnv[\grainRateV] = argEnv[\grainRateV].lag2(argEnv[\lagMult] * argEnv[\grainRateLag] + argEnv[\lagAdd]);
    argEnv[\delay] 		= argEnv[\delay].lag2(argEnv[\lagMult] * argEnv[\delayLag] + argEnv[\lagAdd]);
    argEnv[\delayV] 	= argEnv[\delayV].lag2(argEnv[\lagMult] * argEnv[\delayLag] + argEnv[\lagAdd]);
    argEnv[\delayCurve] = argEnv[\delayCurve].lag2(argEnv[\lagMult] * argEnv[\delayLag] + argEnv[\lagAdd]);
    argEnv[\decay] 		= argEnv[\decay].lag2(argEnv[\lagMult] * argEnv[\decayLag] + argEnv[\lagAdd]);
    argEnv[\pan] 		= argEnv[\pan].lag2(argEnv[\lagMult] * argEnv[\panLag] + argEnv[\lagAdd]);
    argEnv[\panV] 		= argEnv[\panV].lag2(argEnv[\lagMult] * argEnv[\panLag] + argEnv[\lagAdd]);
    argEnv[\env] 		= argEnv[\env].lag2(argEnv[\lagMult] * argEnv[\envLag] + argEnv[\lagAdd]);
    argEnv[\feedAmp] 	= argEnv[\feedAmp].lag2(argEnv[\lagMult] * argEnv[\feedLag] + argEnv[\lagAdd]);
    argEnv[\filtHi] 	= argEnv[\filtHi].lag2(argEnv[\lagMult] * argEnv[\filtLag] + argEnv[\lagAdd]);
    argEnv[\filtLo] 	= argEnv[\filtLo].lag2(argEnv[\lagMult] * argEnv[\filtLag] + argEnv[\lagAdd]);
    argEnv[\feedDist] 	= argEnv[\feedDist].lag2(argEnv[\lagMult] * argEnv[\feedLag] + argEnv[\lagAdd]);
    argEnv[\density] 	= argEnv[\density].lag2(argEnv[\lagMult] * argEnv[\densityLag] + argEnv[\lagAdd]);
    argEnv[\delayRound] = argEnv[\delayRound].lag2(argEnv[\lagMult] * argEnv[\delayRoundLag] + argEnv[\lagAdd]);
    
    argEnv[\rise] !? {
        argEnv[\rise] = argEnv[\rise].lag2(argEnv[\lagMult] * argEnv[\decayLag] + argEnv[\lagAdd]);
    };
    
    argEnv[\buf]        = buf;
    argEnv[\pos]        = pos;
    
    env 				= Env([0, 1, 0], [\attack.ir(1/16), \release.ir(1/16)], releaseNode:1).kr(gate:argEnv[\gate], doneAction:2);
    argEnv[\amp] 		= env * argEnv[\amp];
    argEnv[\dryAmp] 	= env * argEnv[\dryAmp];
    argEnv[\feedAmp] 	= env * argEnv[\feedAmp];
    
    trig = TDuty.ar(
        (argEnv[\trigRate] * 2.pow(
            Dseq(Dgauss(argEnv[\trigRateV].neg, argEnv[\trigRateV], 9999999), inf)
        )).reciprocal,
        0,
        1
    );
    argEnv[\trig] = max(argEnv[\trig], trig);
    
    // If density is provided, skip grainDur and use this to determine duration
    argEnv[\grainDur] = (
        (argEnv[\density] > 0).if(
            argEnv[\density] / argEnv[\trigRate].max(1/10),
            argEnv[\grainDur]
        )
    );
    
    argEnv[\feedProcess] = feedProcess !? {
        feedProcess
    } ?? {
        ~feedProcessDefault.value(
            _, _,
            argEnv[\feedDist],
            argEnv[\feedAmp],
            argEnv[\filtHi],
            argEnv[\filtLo],
        );
    };
    
    sig = func.performWithEnvir(\value, argEnv);
    
    sig;
});

UDef('grainDelay2.env', ar: {
    |buf, pos|
    var arArgs, krArgs, trArgs, lagArgs, envArgs, argEnv, lagMult, lagAdd;
    var sig, sustain;
    var baseEnv, tailDur;
    var func;
    
    func = UDef('grainDelay2Base').arFunc;
    
    baseEnv = { |v| Env([v,v,v,v,v,v], [1,0,0,0,0].normalizeSum) };
    // baseEnv = { |v| Env.newClear(6) };
    sustain = \sustain.kr(1);
    
    ~grainDelay2EnvArgs = envArgs = [
        \grainDur,
        \trigRate, \trigRateV,
        \grainRate, \grainRateV,
        \delay, \delayV,
        \decay,
        \density,
        \pan, \panV,
        \env,
        \feedAmp, \feedDist,
        \filtLo, \filtHi, \delayRound, \delayRoundOffset,
        \ttrig
    ];
    arArgs = [\in];
    krArgs = [\dryAmp];
    
    argEnv = ();
    
    envArgs.do {
        |name|
        var default = func.def.argNames.indexOf(name);
        default = default !? { func.def.prototypeFrame[default] } ?? 0;
        argEnv[name] = EnvGen.ar(name.ir(baseEnv.(default).asArray), timeScale: sustain);
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
    
    argEnv[\trig] = Impulse.ar(0) + TDuty.ar(
        (argEnv[\trigRate] * 2.pow(
            Dseq(Dgauss(argEnv[\trigRateV].neg, argEnv[\trigRateV], 9999999), inf)
        )).reciprocal,
        0,
        1
    );
    
    // If density is provided, skip grainDur and use this to determine duration
    argEnv[\grainDur] = (
        (argEnv[\density] > 0).if(
            argEnv[\density] / argEnv[\trigRate],
            argEnv[\grainDur]
        )
    );
    
    argEnv[\buf]    = buf;
    argEnv[\pos]    = pos;
    argEnv[\ttrig] 	= 0;
    argEnv[\amp] 	= \amp.kr(1) * EnvGen.ar(\ampEnv.ir(baseEnv.(1).asArray), timeScale: sustain, doneAction:2);
    
    func.performWithEnvir(\value, argEnv);
});

UDef(\grainDelay2).wrap(\linked, ar: {
    |func|
    func.value(\buf.kr, \pos.ar(0));
});

UDef('grainDelay2.env').wrap(\linked, ar: {
    |func|
    func.value(\buf.kr, \pos.ar(0));
});

SynthDef('grainDelay2', {
    |out|
    OffsetOut.ar(out, UDef('grainDelay2').ar(nil, nil));
}).addReplace;

SynthDef('grainDelay8', {
    |out|
    OffsetOut.ar(out, UDef('grainDelay2').ar(nil, nil, nil, nil, 8));
}).addReplace;

SynthDef('grainDelay18', {
    |out|
    OffsetOut.ar(out, UDef('grainDelay2').ar(nil, nil, nil, nil, 18));
}).addReplace;

SynthDef('grainDelay12', {
    |out|
    OffsetOut.ar(out, UDef('grainDelay2').ar(nil, nil, nil, nil, 12));
}).addReplace;

SynthDef('grainDelay2.env', {
    |out|
    OffsetOut.ar(out, UDef('grainDelay2.env').ar(nil, nil));
}).addReplace;

SynthDef('grainDelay2.linked', {
    |out|
    OffsetOut.ar(out, UDef('grainDelay2.linked').ar());
}).addReplace;

SynthDef('grainDelay2.env.linked', {
    |out|
    OffsetOut.ar(out, UDef('grainDelay2.env.linked').ar());
}).addReplace;

SynthDef('grainDelay2.env.linked', {
    |out|
    OffsetOut.ar(out, UDef('grainDelay2.env.linked').ar());
}).addReplace;

Pdef('grainDelay2', Pbind(
    \parentType, \grainDelay2,
)
);

Pdef('grainDelay2.env', Pbind(
    \parentType, 'grainDelay2.env',
)
);

b = nil;
Fdef('grainDelay2.linked', {
    |pattern, group, addAction, in|
    Pproto(
        {
            try {
                var dur = ~bufferDur ?? { 10 };
                
                ~in = in.asArray[0];
                ~buf = (
                    type: \buffer,
                    numFrames: dur * Server.default.sampleRate,
                    numChannels: 1
                ).yield;
                b = ~buf;
                // b = ~buf = Buffer.alloc(s, dur * Server.default.sampleRate, 1);
                ~group = (type: \gdef, name: \grailDelayLinked).yield;
                ~writerGroup = (type: \gdef, before: [\grailDelayLinked]).yield;
                // (type: \group, group: , addAction: \addToHead).yield;
                ~addAction = \addToTail;
                ~pos = (type: \audioBus).yield;
                ~writer = (
                    instrument: \grainDelay2Writer,
                    prOut: ~pos.out,
                    group: ~writerGroup,
                    buffer: ~buf,
                    in: ~in ?? 0,
                    fadeTime: 0.1,
                    addAction: \addToHead,
                ).play;
                ~pos = "a%".format(~pos.out).asSymbol;
                ~finish = ~finish.addFunc({
                    ~inIndex = ~in.value.asArray;
                    ~inIndex = ~inIndex.collect {
                        |in|
                        if (in.isSymbol) {
                            in.asString[1..].asInteger
                        } {
                            in
                        }
                    };
                    ~writer.set(\in, ~inIndex.asArray[0]);
                });
                
                nil.yield;
            } { |e| e.reportError() }
        },
        // Pfunc({
        // 	|e|
        // 	e[\in] !? {
        // 		|in|
        // 		in = in.value.unbubble(levels:99);
        // 		if (in.isSymbol and: { in.isMap }) {
        // 			in = in.asString[1..].asInteger;
        // 			in.postln;
        // 		};
        // 		in = in.value.postln;
        // 		if (in != e[\writer].in) {
        // 			Server.default.makeBundle(Server.default.latency, {
        // 				Node.basicNew(Server.default, e[\writer].nodeID).set(\in, in)
        // 			})
        // 		}
        // 	};
        // 	e;
        // }) <>
        pattern,
        {
            "freeeing %".format(~writer);
            ~writer.free;
        }
    )
});

Pdef('grainDelay2.linked').source = {
    |group, addAction, in|
    Fdef('grainDelay2.linked').value(Pid(), group, addAction, in);
};

Pdef('grainDelay2.env.linked').source = Pdef('grainDelay2.linked').source.copy;

Event.addParentType(\grainDelay2, (
    instrument: \grainDelay2,
    args: 		{ ~instrument.asSynthDesc.controlNames },
    trig:       0,
    // envBuf:     ~grainEnvBuf
).putAll(UDef(\grainDelay2).arArgs));

Event.addParentType('grainDelay2.linked', (
    instrument: 'grainDelay2.linked',
    args: 		'grainDelay2.linked'.asSynthDesc.controlNames,
    trig:       0,
    // envBuf:     ~grainEnvBuf
).putAll(UDef(\grainDelay2).arArgs));

Event.addParentType('grainDelay2.env', (
    instrument: 'grainDelay2.env',
    args: 		'grainDelay2.env'.asSynthDesc.controlNames,
    envArgs:    ~grainDelay2EnvArgs,
    trig:       0,
    // envBuf:     ~grainEnvBuf,
    // buf: 	~grainDelayBuf,
    // bufPos: 	Ndef(\grainDelay2Writer).asMap,
    finish:     {
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
                        if (v.notNil) {
                            v.duration = 1;
                        }
                    })
                };
                currentEnvironment[name] = val;
            }
        };
    }
));

Event.addParentType('grainDelay2.env.linked', Event.parentTypes['grainDelay2.env'].copy.putAll((
    instrument: 'grainDelay2.env.linked',
    args: 		'grainDelay2.env.linked'.asSynthDesc.controlNames,
    // envBuf:     ~grainEnvBuf
)));


