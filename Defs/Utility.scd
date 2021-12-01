SynthDef(\trigk, {
	var trig = \trig.tr(0);
	Out.kr(
		\out.kr(0),
		Trig.kr(
			trig,
			\tdur.kr(0)
		)
	)
}).addReplace;

SynthDef(\triga, {
	var trig = \trig.tr([0, 0]);
	OffsetOut.ar(
		\out.kr(0),
		Trig.ar(
			trig,
			\tdur.kr(0)
		)
	)
}).addReplace;

(1..10).do {
	|i|
	var name = "trig%%";
	SynthDef(name.format(i, "k").asSymbol, {
		var trig = \trig.tr(0 ! i);
		Out.kr(
			\out.kr(0),
			Trig.kr(
				trig,
				\tdur.kr(0)
			)
		)
	}).addReplace;
	// name.format(i, "k").asSymbol.asSynthDef.numChannels.postln;

	SynthDef(name.format(i, "a").asSymbol, {
		var trig = \trig.tr(0 ! i);
		OffsetOut.ar(
			\out.kr(0),
			Trig.ar(
				trig,
				\tdur.kr(0)
			)
		)
	}).addReplace;
};

AbstractPlayControl.proxyControlClasses[\trig] = StreamControl;
AbstractPlayControl.buildMethods[\trig] = (
	#{
		| pattern, proxy, channelOffset = 0, index |
		var args = proxy.controlNames.collect(_.name);
		Pchain(
			(
				type: \set,
				id: { proxy.group.nodeID }
			),
			pattern,
			(args: args, trig: 1)
		).buildForProxy( proxy, channelOffset, index )
	}
);

