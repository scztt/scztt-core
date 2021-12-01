+SynthDef {
	// buildForProxy {}

	*channelized {
		|name, ugenGraphFunc, rates, prependArgs, variants, metadata, channelizations|
		var defNames;

		channelizations = channelizations ?? {[1,2]};
		channelizations = Set.newFrom(channelizations).asArray.sort;

		defNames = channelizations.collect({ |ch| "%_%ch".format(name, ch).asSymbol });

		^defNames.collect {
			|chName, i|
			var def;

			if (ugenGraphFunc.def.argNames[0] != 'numChannels') {
				"First SynthDef func argument should be \numChannels".warn;
			};

			def = SynthDef(
				chName,
				{ SynthDef.wrap(ugenGraphFunc, prependArgs:[channelizations[i]]) },
				rates, prependArgs, variants
			);
			def.addReplace;
			def;
		}
	}

	numChannels {
		^(desc !? (_.numChannels))
	}

	rate {
		^(desc !? (_.rate));
	}
}

+SynthDesc {
	prFindProxyOutputs {
		^outputs.select {
			|o|
			o.startingChannel == \out
		}
	}

	numChannels {
		var channels = nil;
		this.prFindProxyOutputs.do {
			|out|
			channels = max(out.numberOfChannels, channels ?? 1);
		};
		^channels
	}

	rate {
		this.prFindProxyOutputs.do {
			|out|
			^out.rate;
		};
		^nil
	}
}

+SynthControl {
    build { | proxy, orderIndex |
		var rate, desc, numChannels;
		desc = this.synthDesc;

		if(desc.notNil) {
			canFreeSynth = desc.canFreeSynth;
			canReleaseSynth = desc.hasGate && canFreeSynth;
		};

		if(proxy.isNeutral) {
            rate = desc.rate;
        };
        numChannels = desc.numChannels;

		^proxy.initBus(rate, numChannels)
	}
}