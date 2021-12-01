Finish : UGen {
	*initClass {
		SynthDef.fx(\finish, {
			|in|
			Finish.ar(in);
		}).add
	}

	*ar {
		|sig, limit|
		var lowFreq, highFreq, centerFreq, bandwidth;

		// sig = SoftKneeCompressor.ar(
		// 	sig,
		// 	-3.dbamp * sig.sum,
		// 	'cmp_thresh'.kr(	spec:ControlSpec(-60, 0, \db, default:-6)),
		// 	1 / 'cmp_ratio'.kr(	spec:ControlSpec(1, 400, 8, default:3)),
		// 	'cmp_knee'.kr(		spec:ControlSpec(0, 12, default:6)),
		// 	'cmp_attack'.kr(	spec:ControlSpec(0, 1, default:0.001)),
		// 	'cmp_release'.kr(	spec:ControlSpec(0.01, 1.5, \exp, default:0.2)),
		// 	'cmp_makeUp'.kr(	spec:ControlSpec(0, 1, default:0)).dbamp
		// );
		sig = DCompressor.ar(
			sig,
			sig.mean,
			1,
			'cmp_ratio'.kr(4, spec:[1, 100]),
			'cmp_thresh'.kr(-40, spec:[-90, 12]),
			'cmp_attack'.kr(0.05, spec:ControlSpec(0.0001, 10, \exp)),
			'cmp_release'.kr(0.4, spec:ControlSpec(0.0001, 10, \exp)),
			'cmp_makeup'.kr(0, spec:[0, 40]),
			automakeup:0
		);

		lowFreq = 'eq_lowFreq'.kr(spec:ControlSpec(20, 10000, warp:\exp, default:300));
		highFreq = 'eq_hiFreq'.kr(spec:ControlSpec(100, 20000, warp:\exp, default:1600));
		centerFreq = [highFreq, lowFreq].mean;
		bandwidth = (highFreq - lowFreq).abs;


		sig = BLowShelf.ar(
			sig,
			freq: lowFreq,
			rs: 0.95,
			db: 'eq_lowCut'.kr(0, spec:ControlSpec(-60, 12, default:0))
		);

		sig = BPeakEQ.ar(
			sig,
			freq: centerFreq,
			rq:  bandwidth / centerFreq,
			db: 'eq_midCut'.kr(0, spec:ControlSpec(-60, 12, default:0))
		);

		sig = BHiShelf.ar(
			sig,
			freq: highFreq,
			rs: 0.95,
			db: 'eq_hiCut'.kr(0, spec:ControlSpec(-60, 12, default:0))
		);

		if (limit.notNil) {
			if (limit == true) { limit = 0 };
			sig = Limiter.ar(sig, limit)
		};

		^sig;
	}
}
	