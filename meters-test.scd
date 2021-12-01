(
~style = (
	style: \led,
	drawsPeak: true,
	background: Color.hsv(0.3, 0.4, 0.9, 0.3),
	meterColor: Color.hsv(0.3, 0.4, 1, 0.9).alpha_(0.5),
	lineColor: Color.hsv(0.6, 0.5, 0.9, 0.5),
	warning: -3.dbamp,
	critical: -1.dbamp,
	warningColor: Color.hsv(0.08, 0.4, 1).alpha_(0.9),
	criticalColor: Color.red,
	fixedHeight: 16,
	font: Font("M+ 1c", 14, true),
	hue: Color.hsv(0.5, 1, 1);
);

s.waitForBoot({
	n = 6.collect {
		|i|
		Ndef(i.asSymbol).clear;
		Ndef(i.asSymbol, {
			//SinOsc.ar(1/[10, 15, 20]).abs * SinOsc.ar(300)
			//SinOsc.ar(400) * LFDNoise3.kr(1/10)
			DelayN.ar(SoundIn.ar([0, 1]), 1, { 1.0.rand } ! 2);
		});
	};
	v = View(bounds:500@70).layout_(VLayout(
		*(
			(n.collect {
				|b|
				m = ScMeter.newFrom(b)
					.labelWidth_(80)
					.levelSpec_(ControlSpec(-80, 0, \db))
					.setStyle(~style)
					.start;
				m;

			}) ++ [nil]
		)
	).spacing_(7)
	).front;
})

)


1000 / 30
(
m = ScMeter(bounds:60@200).bus_(Ndef(2.asSymbol).bus)
					.label_("foo")
					.labelWidth_(80)
					.levelSpec_(ControlSpec(-80, 0, \db))
					.start.front
)

LevelIndicator(bounds:300@50).value_(0.98).warning_(0.5).critical_(0.9).front;

1000/33