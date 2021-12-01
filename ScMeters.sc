ScMeter : View {
	var <monitor, <busTarget;
	var channels=0;
	var levelSpec, <style, <meters, metersLayout, metersView, label, <>lineColor;

	*qtClass {^'QcDefaultWidget'}

	*new {
		|parent, bounds, bus|
		^super.new(parent, bounds).init(bus)
	}

	*newFrom {
		|obj, name|
		var bus, dragObject;

		case
		{ obj.isKindOf(Ndef) } {
			bus = obj.bus;
			name = name ?? "Ndef(%)".format("\\" ++ obj.key.asString);
			dragObject = name;
		}
		{ obj.isKindOf(NodeProxy) } {
			bus = obj.bus;
			name = name ?? { currentEnvironment.findKeyForValue(obj) };
		}
		{ obj.respondsTo(\bus) } {
			bus = obj.bus;
		}
		{ obj.respondsTo(\asBus) && obj.notNil } {
			bus = obj.asBus;
		}
		{
			Error("Cannot turn % into a bus".format(obj)).throw;
		};

		if (name.isNil) {
			name = "% %[%]".format(obj.asString, bus.index, bus.numChannels);
		};

		if (dragObject.isNil) {
			dragObject = bus.asCompileString;
		};

		^this.new(bus:bus).label_(name).dragObject_(dragObject)
	}

	init {
		|bus|
		style = (
			style: 			\led,
			stepWidth: 		1,
			drawsPeak:		true,
			background:		Color.hsv(0.58, 0.2, 0.4),
			lineColor:		Color.hsv(0.58, 0.8, 0.9),
			meterColor:		Color.hsv(0.58, 0.7, 0.7),
			warningColor:	Color.hsv(0.58, 0.5, 0.9),
			criticalColor:	Color.red,
			warning:		0.7,
			critical:		0.9,
			peakLevel:		0.9,
			font:			Font("M+ 1c", 14, true),
			fixedHeight:	16
		);

		meters = List();
		this.layout = HLayout(
			label = DragSource()
				.setBoth_(false)
				.canFocus_(false)
				.stringColor_(Color.grey(1, 0.7))
				.font_(style.font)
				.fixedWidth_(80)
				.align_(\right),
			4,
			[
				metersView = UserView().layout_(
					// VLayout(
						metersLayout = VLayout().spacing_(0).margins_(0),
				// ).margins_(0).spacing_(0)
				),
				stretch: 1
			]
		);
		this.layout.margins_(0).spacing_(0);

		label.palette = label.palette.copy
							.window_(Color.clear)
							.base_(Color.clear);

		levelSpec = ControlSpec(-50, 0, \db);

		metersView.drawFunc = {
			|view|
			Pen.strokeColor = lineColor;
			Pen.strokeRect(view.bounds.moveTo(0, 0).insetBy(0.5, 0.5));
			meters[1..].do {
				|l|
				Pen.line(l.bounds.leftTop, l.bounds.rightTop);
				Pen.stroke();
			}
		};

		this.bus = bus;
		this.onClose = this.onClose.addFunc { this.stop };
		this.label = busTarget !? busTarget.asString;
		this.start();
	}

	labelWidth_{
		|width|
		label.fixedWidth_(width)
	}

	dragObject_{
		|obj|
		label.object = obj;
	}

	label_{
		|string|
		if (string.notNil) {
			label.visible = true;
			label.string = string;
		} {
			label.visible = false;
		}
	}

	bus {
		^busTarget
	}

	bus_{
		|inBus|
		busTarget = inBus;

		if (busTarget.notNil) {
			metersView.attachHoverScope(
				busTarget,
				busTarget.server,
				\addAfter,
				size:300@80,
				align:\left
			);
			this.prSetChannels(busTarget.numChannels);
			monitor !? { monitor.bus = busTarget };
		} {
			this.prSetChannels(0);
		}
	}

	levelSpec_{
		|spec|
		levelSpec = spec;
	}

	setStyle {
		|inStyle|
		style.putAll(inStyle);
		this.prSetStyles();
	}

	hue_{
		|color|
		style[\hue] = color;
		this.prSetStyles();
	}

	prSetStyles {
		var font, styleCopy, hue;
		var old;

		styleCopy = style.copy;

		// Do hue adjustments first
		hue = styleCopy[\hue];
		styleCopy[\hue] = nil;

		if (hue.notNil) {
			hue = hue.asHSV[0];
			[
				\background,
				\meterColor,
				\lineColor
			].do {
				|prop|
				old = styleCopy[prop];
				if (old.notNil) {
					old = old.asHSV;
					old[0] = hue;
					styleCopy[prop] = Color.hsv(*old);
				}
			}
		};

		lineColor = styleCopy[\lineColor] ?? { Color.clear };
		styleCopy[\lineColor] = nil;

		styleCopy.keysValuesDo {
			|key, val|
			meters.do {
				|m|
				m.perform(key.asSetter, val);
			}
		};

		font = styleCopy[\font].copy;
		font.size = min(font.size, styleCopy[\fixedHeight] * 2 - 2);

		label.font = font;

	}

	prSetChannels {
		|num|
		var meter;

		if (num > channels) {
			(num - channels).do {
				meter = LevelIndicator().setProperty(\autoUpdate, false);
				meters.add(meter);
				metersLayout.add(meter);
			};
			this.prSetStyles();
		};

		if (num < channels) {
			(channels - num).do {
				metersLayout.remove(meters.pop());
			};
			this.prSetStyles();
		};

		if (style[\fixedHeight].notNil) {
			metersView.fixedHeight = (meters.size * style[\fixedHeight]);
		};

		channels = num;
	}

	start {
		if (monitor.isNil) {
			monitor = BusStatsUpdater(busTarget, {
				|in, trigger|
				var numRMSSamps = SampleRate.ir / 30;
				var peak;
				(in.rate == \audio).if({
					peak = Peak.ar(in, Delay1.ar(trigger))
				}, {
					peak = Peak.kr(in, Delay1.kr(trigger))
				});

				[
					peak,
					peak.lag(0, 6)
				]
			}, rate:30);
			monitor.signal(\value).connectTo(this.methodSlot("setValues(args[0], args[1])"));
		}
	}

	stop {
		if (monitor.notNil) {
			monitor.free;
			monitor = nil;
		}
	}

	setValues {
		|values, peaks|
		values = levelSpec.map(values).linlin(levelSpec.minval, levelSpec.maxval, 0, 1);
		peaks = levelSpec.map(peaks).linlin(levelSpec.minval, levelSpec.maxval, 0, 1);
		values.do {
			|val, i|
			meters[i].value = val;
		};
		peaks.do {
			|peak, i|
			meters[i].peakLevel = peak;
		};
		metersView.invokeMethod(\update);
	}
}