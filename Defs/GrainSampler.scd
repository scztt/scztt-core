
UDef(\grainSampler, ar: {
	|
		buf = -1, in, inAmp=(0), feedInBus=(-1), feedOutBus=(-1), amp=0, trig=0, ttrig=0, grainDur=0.25,
		grainRate=1, grainRateV=0,
		pos=0, posV=0, posRound=0, posRoundOffset=0,
		pan=0, panV=0, env=0.5,
		feedAmp=(-20), filtHi=4000, filtLo=20, feedDist=2,
		interpolate=4,
		numChannels=2
	|

	var sig, feedIn, bufSize, scaledPos, dur, posLow, posHigh, posRemainder;

	// PARAMETERS
	trig    = trig + ttrig;

	dur     = grainDur;

	bufSize = BufSamples.kr(buf);

	amp		= amp;
	feedAmp = feedAmp.dbamp;

	// pos	= pos + TRand.ar(-1, 1, trig).linlin(-1, 1, posV.neg, posV);

	posRemainder = (pos - posRoundOffset) / posRound.max(0.0000001);
	posLow = posRemainder.floor;
	posHigh = posRemainder.ceil;
	posRemainder = posRemainder - posLow;

	pos = (posRound > 0).if(
		(
			TWChoose.ar(trig, [posLow, posHigh], [1 - posRemainder, posRemainder], 0)
			* posRound
			+ posRoundOffset
		),
		pos
	);
	pos	= pos + TRand.ar(-1, 1, trig).linlin(-1, 1, posV.neg, posV);
	// pos = (pos - posRoundOffset).round(posRound) + posRoundOffset;
	// delay   = TRand.ar(-1.0, 1.0, trig).lincurve(-1.0, 1.0, delay * 2.pow(delayV.neg), delay * 2.pow(delayV), 2.3); // 0.5 <> 2.0, but weighted toward center
	pos	= max(pos, 0).asArray;

	// rate	= rate * 2.pow(TRand.ar(grainRateV.neg, grainRateV, trig));
	grainRate = TRand.ar(-1.0, 1.0, trig).lincurve(-1.0, 1.0,
		grainRate * 2.pow(grainRateV.neg),
		grainRate * 2.pow(grainRateV),
		0
	);
	grainRate = grainRate.asArray;

	pan		= TRand.ar(pan - panV, pan + panV, trig);
	pan     = pan.asArray;

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
			amp: 		t,
			pan:        pan[i],
			att: 		env * dur,
			dec: 		(1 - env) * dur,
			interp: 	interpolate,
			// maxgrains:  1200
			maxgrains: 700
		).asArray
	};
	sig.assertChannels(1, numChannels);
	sig = sig.sum;
	sig.assertChannels(numChannels);

	sig = BLowPass.ar(BHiPass.ar(sig, filtLo), filtHi);

	sig = (amp * sig);
});

SynthDef.channelized(\grainSampler, {
	|numChannels|
	var arArgs, krArgs, trArgs, lagArgs, func, argEnv, lagMult, lagAdd;
	var sig;

	func = UDef(\grainSampler).arFunc;

	lagArgs = [\lagMult, \lagAdd, \grainDurLag, \rateLag, \posLag, \panLag, \envLag, \feedLag, \filtLag, \panLag, \ampLag];

	arArgs = [\in, \trig, \trigRate, \trigRateV, \grainDur, \grainRate, \grainRateV, \pos, \posV, \pan, \panV, \env, \density, \posRound, \posRoundOffset];
	krArgs = lagArgs ++ [\buf, \feedInBus, \feedOutBus, \amp, \feedAmp, \filtHi, \filtLo, \feedDist, \inAmp, \interpolate];
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

	OffsetOut.ar(\out.kr(0),
		func.performWithEnvir(\value, argEnv));
}, channelizations:[2, 8]);

UDef(\grainSampler).wrap(\env, ar: {
	|func, numChannels|

	var arArgs, krArgs, trArgs, lagArgs, envArgs, argEnv, lagMult, lagAdd;
	var sig, sustain;
	var baseEnv, tailDur, gate, envScale;

	var maxEnvSize = 6;

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
	krArgs = lagArgs ++ [\buf, \feedInBus, \feedOutBus, \filtLo, \filtHi, \interpolate, \posRound, \posRoundOffset];

	argEnv = ();

	envArgs.do {
		|name|
		var default = func.def.argNames.indexOf(name);
		default = default !? { func.def.prototypeFrame[default] } ?? 0;
		argEnv[name] = EnvGen.ar(name.ir(baseEnv.(default).asArray), gate:gate, timeScale: envScale);
	};

	~grainSamplerEnvArgs = envArgs;

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

	argEnv[\trigRate] = argEnv[\trigRate].max(1/10);
	// If density is provided, skip grainDur and use this to determine duration
	argEnv[\grainDur] = (
		(argEnv[\density] > 0).if(
			argEnv[\density] / argEnv[\trigRate],
			argEnv[\grainDur]
		)
	);

	argEnv[\ttrig] = 0;
	argEnv[\amp] = \amp.kr(1) * EnvGen.ar(\ampEnv.ir(baseEnv.(1).asArray), gate:gate, timeScale: envScale, doneAction:2);
	argEnv[\numChannels] = numChannels;

	func.performWithEnvir(\value, argEnv);
});

SynthDef.channelized('grainSampler.env', {
	|numChannels, out|
	Out.ar(out, UDef('grainSampler.env').ar.(numChannels:numChannels));
}, channelizations:[2, 8, 16]);

Event.addParentType('grainSampler.env', (
	baseInstrument: 'grainSampler.env',
	args: 		'grainSampler.env_2ch'.asSynthDesc.controlNames,
	channels: 	2,
	envArgs:    ~grainSamplerEnvArgs,
	finish:     {
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


