(
var cleck = (
	multEnv: {
		|rate, skew, curve|
		if (skew.isKindOf(Env).not) {
			skew = skew.asArray.collect({
				|s|
				s = s.value;
				if (s.isRest) {
					s
				} {
					s.asEnv
				}
			});
		};

		if (rate.isKindOf(Env).not) {
			rate = rate.asArray.collect({
				|r, i|
				r = r.value;
				if (r.isRest) {
					r
				} {
					(r.asEnv * skew.wrapAt(i)).curves_(curve)
				};
			});
		};
		if (rate.size == 1) { rate = rate[0] };
		rate;
	},
	addEnv: {
		|rate, skew, curve|
		if (skew.isKindOf(Env).not) {
			skew = skew.asArray.collect({
				|s|
				s = s.value;
				if (s.isRest) {
					s
				} {
					s.asEnv
				}
			});
		};

		if (rate.isKindOf(Env).not) {
			rate = rate.asArray.collect({
				|r, i|
				r = r.value;
				if (r.isRest) {
					r
				} {
					(r.asEnv * skew.wrapAt(i)).curves_(curve)
				};
			});
		};
		if (rate.size == 1) { rate = rate[0] };
		rate;
	},

	curve: -4,

	// TRIGGERING
	trOctave: 3,
	trDegree: 0,
	trDetune: 0,
	trDegreeSkew: 0,
	trCurve: \exp,
	trSkew: {
		~trDegreeSkew.value.midiratio
	},
	trDetunedFreq: {
		var e = (
			octave: ~trOctave.value,
			degree: ~trDegree.value,
			scale: ~scale.value,
			detune: ~trDetune.value,
			note: ~trNote.value,
			midinote: ~trMidinote.value,
			freq: ~trFreq.value,
		);
		e.parent = Event.parentEvents.default;
		e.use({ ~detunedFreq.value });
	},
	trigRate: {
		// "trigRate: % -> %".format(~trDetunedFreq.value, ~trDetunedFreq.value + ~trSkew.value).postln;
		~multEnv.value(
			~trDetunedFreq.value,
			~trSkew.value,
			~trCurve.value
		)
	},

	// GRAIN RATE
	grDegree: 0,
	gr: { ~grDegree.value.midiratio },
	grDegreeSkew: 0,
	grSkew: { ~grDegreeSkew.value.midiratio },
	grCurve: { ~curve },
	grainRate: {
		~multEnv.(
			~gr.value,
			~grSkew.value,
			~grCurve.value
		)
	},
	grv: 0,
	grvSkew: 0,
	grvCurve: { ~grCurve },
	grainRateV: {
		~addEnv.(
			~grv.value,
			~grvSkew.value,
			~grvCurve.value
		);
	},

	// POSITION
	posMult: 1,
	posBase: 0,
	posSkew: { ~posMult * ~sustain.value },
	posCurve: 0,
	pos: {
		~addEnv.(
			~posBase.value,
			~posSkew.value,
			~posCurve.value,
			'+'
		);
	},
	posVBase: 0,
	posVSkew: 0,
	posVCurve: { ~curve },
	posV: {
		~addEnv.(
			~posVBase.value,
			~posVSkew.value,
			~posVCurve.value
		);
	},

	panBase: 0,
	panSkew: 0,
	panCurve: 0,
	pan: {
		~addEnv.(
			~panBase.value,
			~panSkew.value,
			~panCurve.value
		);
	},

	attack: 0.5,
	ampCurve: { (~attack > 0.5).if(8, -12) },
	amp: 1,
	ampEnv: {
		Env.perc(
			~attack,
			1 - ~attack,
			1,
			~ampCurve * [1, -1]
		);
	},

	densityMin: 2,
	densityMax: 2,
	densityCurve: { ~ampCurve.value },
	densityAttack: { ~attack.value },
	density: {
		Env(
			[~densityMin, ~densityMax, ~densityMin],
			[~densityAttack.value, 1 - ~densityAttack.value],
			~densityCurve.value
		)
	},

	swing: 0,
	swingScale: 1,
	timingOffset: {
		var time = thisThread.beats;
		if ((time % ~swingScale) >= (~swingScale / 2)) {
			~swingScale = ~swing
		} { 0 }
	}
);

Require("GrainDelay2");
Require("GrainSampler");

Event.addParentType(\cleck, cleck.copy.putAll(Event.parentTypes['grainSampler.env']).putAll((instrument: 'grainSampler.env_2ch')));
Event.addParentType(\cleckDelay, cleck.copy.putAll(Event.parentTypes['grainDelay2.env']).putAll((instrument: 'grainDelay2.env')));
Event.addParentType(\cleckLinked, cleck.copy.putAll(Event.parentTypes['grainDelay2.env']).putAll((instrument: 'grainDelay2.linked')));
Event.addParentType(\cleckEnvLinked, cleck.copy.putAll(Event.parentTypes['grainDelay2.env.linked']).putAll((instrument: 'grainDelay2.env.linked')));
)