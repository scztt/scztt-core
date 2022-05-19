+SynthDef {
	*fx {
		|name, func, channels=2|
		var def = SynthDef(name, {
			var in, out, fadeTime, fadeCurve, inArgs;

			inArgs = func.def.argNames;

			in = inArgs.collect {
				|argName|
				"%Amp".format(argName).asSymbol.kr(1)
				* argName.ar(0 ! channels)
			};

			fadeTime = \fadeTime.kr(1/40);
			fadeCurve = \fadeCurve.kr(-1.77);

			out = func.value(*in);
			out = \amp.kr(1) * Mix([
				\dryAmp.kr(0) * in,
				\wetAmp.kr(1) * out
			]);

			if (UGen.buildSynthDef.controlNames.detect({ |cn| cn.name == 'gate' }).isNil) {
				out = out * Env(
					[0, 1, 0],
					[fadeTime, fadeTime],
					[fadeCurve, fadeCurve.neg],
					releaseNode: 1
				).kr(gate:\gate.kr(1), doneAction:2);
			};

			Out.ar(\out.kr(-1), out)
		});
		def.addReplace;
		^def;
	}
}



