e = ();
f = (
	\test: {
		|...args|
		args.postln;
	}
);

c = [
	UpdateBroadcaster(\hello).addConnection(f.methodUpdater(\test)),
	UpdateChannel(\hello).addConnection(f.methodUpdater(\test)),
	UpdateBroadcaster(\goodbye).addConnection(f.methodUpdater(\test))
	UpdateBroadcaster(\goodbye).addConnection(f)
];

u = UpdateFilter(e, \test).addDependant(UpdateBroadcaster(\goodbye));
u = UpdateFilter(e, \asdf).addDependant(UpdateChannel(\hello));

e.changed(\asdf, 2345, 2345);
e.changed(\test, 2, 4);

c[1].disconnectWith({
	e.changed(\asdf, 2345, 2345);
	e.changed(\test, 2, 4);
});


UpdateChannel(\hello).changed(\test, 1);




TwisterDevice().addConnection({ |...args| args.postln });
k = TwisterDevice().knobs.collect(TwisterKnob(_));
k[0..3].do(_.ledColor_(Color.green));
k.do(_.enable);
k.do(_.value_(0.0));
~connections = List();
~busses = List();

View().layout_(GridLayout.rows(
	*(~uiKnobs = k.collect({
		|knob|
		var uiKnob = Knob();
		~connections.add(
			UpdateFilter(knob, \value).deferredUpdater.addConnection(UpdateMethod(uiKnob, \value_, 0))
		);

		~busses.add(b = Bus.control(s, 1));
		u = BusUpdater(b, freq:0.05);
		~connections.add(
			u.addConnection(UpdateMethod(knob, \value_, 0));
		);

		uiKnob.action = { |v| knob.value = knob.spec.map(v.value) };
		uiKnob
	}).clump(4))
)).front.onClose_({ ~connections.do(_.disconnect())});


{
	var sigs = 16.collect {
		var sig = Dust.ar(4.0.rand);
		sig = Decay.ar(sig, 1.0.rand.pow(4) * 4);
		sig = sig * WhiteNoise.ar();
		sig = LPF.ar(sig, 2000);
	};
	Out.kr(0, Amplitude.kr(sigs, 0.1, 0.4) * 4);
	Out.ar(0, Splay.ar(sigs));
}.play

(
k = TwisterDevice().knobs.collect(TwisterKnob(_));
fork {
	var dur = 1.5;
	(0,0.02..dur).do {
		|seconds|
		k.do {
			|knob, i|
			var hue, brightness;
			i = (i / 3).floor * 3;
			hue = (seconds / dur).pow(1.1 + (i / k.size)) *  1.5;
			brightness = (seconds / dur).pow(2.4 - (i / k.size * 2.2));
			knob.device.ledColor = Color.hsv(hue % 1, 1, brightness % 1);
		};
		0.02.wait;
	};
	0.1.wait;
	(0,0.02..0.5).do {
		|n|
		k.do({|k| k.device.ledBrightness = (0.5 - n) * 2 });
		0.02.wait;
	}
}
)


k[2].value=0.5;
k[2].knobAction(0.2);


b = Bus.control(s, 1);
b.set(0)
u = BusUpdater(b);
u.addConnection({ |obj, what, val| "bus: %".format(val).postln })

u.freq = 0.1