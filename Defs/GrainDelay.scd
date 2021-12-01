UDef(\grainDelay, ar: {
	| buf = -1,
	in, inAmp=(0), feedInBus=(-1), feedOutBus=(-1), amp=0, trig=#[0, 0, 0, 0], ttrig=#[0, 0, 0, 0], grainDur=0.25,
		rate=#[1, 1, 1, 1], rateV=0,
		delay=0, delayV=0,
		pan=0, panV=0, env=0.5,
		feedAmp=(-20), feedHi=20, feedLo=4000, feedDist=2
	|

	var sig, feedIn, bufSize, pos, scaledPos, dur;

	// PARAMETERS
	trig    = trig + ttrig;

	dur     = grainDur;

	bufSize = BufSamples.kr(buf);

	pos		= Phasor.ar(1, 1, 0, bufSize);

	amp		= amp.dbamp;
	feedAmp = feedAmp.dbamp;
	inAmp   = inAmp.dbamp;

	delay	= delay * 2.pow(TRand.ar(delayV.neg, delayV, trig));
	// delay   = TRand.ar(-1.0, 1.0, trig).lincurve(-1.0, 1.0, delay * 2.pow(delayV.neg), delay * 2.pow(delayV), 2.3); // 0.5 <> 2.0, but weighted toward center
	delay	= max(delay, 0);

	// rate	= rate * 2.pow(TRand.ar(rateV.neg, rateV, trig));
	rate   = TRand.ar(-1.0, 1.0, trig).lincurve(-1.0, 1.0,
		rate * 2.pow(rateV.neg),
		rate * 2.pow(rateV),
	0);
	rate.assertChannels(4);

	// SIGNALS
	// feedIn 	= Select.kr((feedInBus > -1), [
	// 	In.ar(feedInBus, 1),
	// 	LocalIn.ar(1)
	// ]);
	// feedIn  = In.ar(feedInBus, 1).poll;
	feedIn  = LocalIn.ar(2);
	feedIn  = LinXFade2.ar(feedIn[0], feedIn[1], pan.lag(0.05));
	feedIn.assertChannels(1);

	// feedIn	= Compander.ar(feedIn, feedIn, feedAmp, 1/1, 1/100);
	// feedIn	= feedIn.blend(
	// 	SoftClipAmp8.ar(feedIn, feedDist),
	// 	feedDist.min(1)
	// );
	// feedIn 	= BLowPass.ar(feedIn, feedLo);
	// feedIn 	= BHiPass.ar(feedIn, feedHi);
	// feedIn	= LeakDC.ar(feedIn);
	feedIn  = feedAmp * feedIn;

	in = in * inAmp;
	in = in + feedIn;
	in = in.tanh;

	// randomize pan AFTER it's used for feedback
	pan		  = TRand.ar(pan - panV, pan + panV, trig);

	pos 	  = BufWr.ar(in, buf, pos) + pos;
	scaledPos = pos;

	scaledPos = scaledPos + (0.5 * dur * rate * SampleRate.ir) + \posOffset.kr(0);
	scaledPos = scaledPos - (dur * max(0, rate - 1) * SampleRate.ir);
	scaledPos = scaledPos - max(0, delay * SampleRate.ir);

	// scaledPos  < pos + (dur * sampleRate * (1 - rate))
	scaledPos = min(scaledPos, pos + (dur * SampleRate.ir * (1 - rate)));

	scaledPos = scaledPos / SampleRate.ir;
	// scaledPos = scaledPos.wrap(0.0, BufDur.kr(buf));

	// Write into input buffer
	[pos + (dur * SampleRate.ir), (scaledPos[0] * SampleRate.ir) + (0.5 * dur * SampleRate.ir)].poll(2);

	sig = trig.collect {
		|t, i|
		TGrains2.ar(
			2,
			trigger: 	t + (0 * in),
			dur: 		dur,
			rate: 		rate[i],
			bufnum: 	buf,
			centerPos:	scaledPos[i],
			amp: 		Latch.ar(t, t),
			pan:        pan[i],
			att: 		env * dur,
			dec: 		(1 - env) * dur,
			interp: 	2,
			maxgrains:  2048
		)
	};
	sig.assertChannels(4, 2);
	sig = sig.sum;
	sig.assertChannels(2);
	sig = LeakDC.ar(sig);

	LocalOut.ar(sig / 4);
	// ReplaceOut.ar(feedOutBus, sig.sum / 8);

	// sig = Compander.ar(sig, sig, -3.dbamp, 1, 1/10);

	sig = (amp * sig);
});

SynthDef(\grainDelay, {
	var arArgs, krArgs, trArgs, lagArgs, func, argEnv, lagMult, lagAdd;
	var sig;

	func = UDef(\grainDelay).arFunc;

	lagArgs = [\lagMult, \lagAdd, \grainDurLag, \rateLag, \delayLag, \panLag, \envLag, \feedLag, \panLag, \ampLag];

	arArgs = [\in, \trig, \grainDur, \rate, \rateV, \delay, \delayV, \pan, \panV, \env];
	krArgs = lagArgs ++ [\buf, \feedInBus, \feedOutBus, \amp, \feedAmp, \feedHi, \feedLo, \feedDist, \inAmp];
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

	argEnv[\lagMult].spec = ControlSpec(0, 10, default:1);
	argEnv[\lagAdd].spec = ControlSpec(0, 10, default:0);
	argEnv[\grainDur] 	= argEnv[\grainDur].lag2(argEnv[\lagMult] * argEnv[\grainDurLag] + argEnv[\lagAdd]);
	argEnv[\rate] 		= argEnv[\rate].lag2(argEnv[\lagMult] * argEnv[\rateLag] + argEnv[\lagAdd]);
	argEnv[\rateV] 		= argEnv[\rateV].lag2(argEnv[\lagMult] * argEnv[\rateLag] + argEnv[\lagAdd]);
	argEnv[\delay] 		= argEnv[\delay].lag2(argEnv[\lagMult] * argEnv[\delayLag] + argEnv[\lagAdd]);
	argEnv[\delayV] 	= argEnv[\delayV].lag2(argEnv[\lagMult] * argEnv[\delayLag] + argEnv[\lagAdd]);
	argEnv[\pan] 		= argEnv[\pan].lag2(argEnv[\lagMult] * argEnv[\panLag] + argEnv[\lagAdd]);
	argEnv[\panV] 		= argEnv[\panV].lag2(argEnv[\lagMult] * argEnv[\panLag] + argEnv[\lagAdd]);
	argEnv[\env] 		= argEnv[\env].lag2(argEnv[\lagMult] * argEnv[\envLag] + argEnv[\lagAdd]);
	argEnv[\amp] 		= argEnv[\amp].lag2(argEnv[\lagMult] * argEnv[\ampLag] + argEnv[\lagAdd]);
	argEnv[\feedAmp] 	= argEnv[\feedAmp].lag2(argEnv[\lagMult] * argEnv[\feedLag] + argEnv[\lagAdd]);
	argEnv[\feedHi] 	= argEnv[\feedHi].lag2(argEnv[\lagMult] * argEnv[\feedLag] + argEnv[\lagAdd]);
	argEnv[\feedLo] 	= argEnv[\feedLo].lag2(argEnv[\lagMult] * argEnv[\feedLag] + argEnv[\lagAdd]);
	argEnv[\feedDist] 	= argEnv[\feedDist].lag2(argEnv[\lagMult] * argEnv[\feedLag] + argEnv[\lagAdd]);
	Out.ar(\out.kr(0),
		func.performWithEnvir(\value, argEnv));
}).addReplace;
