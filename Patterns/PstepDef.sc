PstepDef : Singleton {
	var <values, pattern;

	*new {
		|name, size, spec, class|
		^super.new(name, size, spec, class)
	}

	init {
		|name|
	}

	at {
		|i|
		^values[i]
	}

	set {
		|size, spec, class|
		var oldValues, constructor;

		spec = spec.asSpec;
		class = class ? NumericControlValue;
		constructor = class;

		if (constructor.isFunction.not) {
			constructor = { |initialValue, spec| class.new(initialValue:initialValue, spec:spec) };
		};

		oldValues = values.collect(_.value);

		values.do {
			|v|
			v.free;
		};

		values = size.collect {
			|i|
			var cv = constructor.value(
				initialValue: (i < oldValues.size).if({ oldValues[i] }),
				spec: spec
			);
			cv;
		};

		pattern = Pseq(values, inf);
	}

	asStream { ^Routine({ arg inval; this.embedInStream(inval) }) }

	iter { ^this.asStream }

	streamArg { ^this.asStream }

	asEventStreamPlayer { arg protoEvent;
		^EventStreamPlayer(this.asStream, protoEvent);
	}

	embedInStream {
		|inval|
		pattern.embedInStream(inval);
		^inval
	}
}

PstepDefGui : View {
	var rows, controls;

	*qtClass { ^'QcDefaultWidget' }

	*new {
		|parent, bounds|
		^super.new(parent, bounds).init
	}

	init {
		PstepDef.signal(\added).connectTo(this.methodSlot("setup()"));
		PstepDef.signal(\removed).connectTo(this.methodSlot("setup()"));
		this.setup();
	}

	setup {
		var maxSize = 0, rows;

		PstepDef.all.do { |p| maxSize = max(maxSize, p.values.size) };

		this.children.do(_.remove());
		// this.layout = GridLayout.rows(*([nil] ! PstepDef.all.size));

		rows = PstepDef.all.values.asArray.collect({
			|def, i|
			var title;
			title = StaticText().string_(def.name);

			// this.layout.add(title, i, 0, \right);

			[title] ++ def.values.collect({
				|cv, j|
				var knob, level, number, view;
				view = View().layout_(VLayout().margins_(0).spacing_(0));
				view.layout.add(View().layout_(StackLayout(
					knob = Slider().thumbSize_(4).background_(Color.clear),
					View().layout_(HLayout(
						level = LevelIndicator()
									.meterColor_(Color.hsv(0.55, 0.7, 0.8))
									.minSize_(12@12)
									.critical_(1)
									.warning_(1)
					).margins_(3))
				).mode_(\stackAll)));

				view.layout.add(
					number = NumberBox()
				);

				number.decimals = 1;
				number.scroll = 0.05;

				knob.step = (cv.spec.clipHi  - cv.spec.clipLo) / 100;

				knob.value = cv.input;
				number.value = cv.value;
				level.value = cv.input;

				ConnectionList.newFrom([
					knob.signal(\value).connectTo(cv.inputSlot),
					number.signal(\value).connectTo(cv.valueSlot),
					cv.signal(\input).connectTo(knob.valueSlot),
					cv.signal(\value).connectTo(number.valueSlot),
					cv.signal(\input).connectTo(level.valueSlot),
				]).freeAfter(view);

				view
			})
		});

		rows.postln;
		this.layout = GridLayout.rows(*rows);
	}
}