ScChannelMonitor : View {
	var <meter, bus, recordButton, writingToMenu, toolbar;
	var armedIcon, unarmedIcon, scopeIcon;
	var <armed;

	*qtClass {^'QcDefaultWidget'}

	*new {
		|parent, bounds, bus, label|
		^super.new(parent, bounds).init(bus, label)
	}

	init {
		|inBus, label|

		armed = false;

		if (inBus.notNil) {
			meter = ScMeter.newFrom(inBus)
		} {
			meter = ScMeter();
		};

		armedIcon = Material('fiber_manual_record', color:Color.red);
		unarmedIcon = Material('fiber_manual_record', color:Color.red(0.5));
		scopeIcon = Material('graphic_eq', color:Color.white);

		recordButton = Button()
						.fixedSize_(32@20)
						.states_([[nil, Color.clear, Color.grey.alpha_(0.2)]])
						.canFocus_(false)
						.icon_(this.armedIcon)
						.iconSize_(16);

		recordButton.signal(\value).connectTo(this.methodSlot("toggleArmed()"));

		this.layout_(HLayout(
			[meter, stretch:1],
			recordButton
		).spacing_(5).margins_(0));

		meter.labelWidth = 120;
		meter.start();

		if (label.notNil) { meter.label = label }
	}

	label_{
		|label|
		meter.label = label;
	}

	armedIcon {
		^armed.if(armedIcon, unarmedIcon)
	}

	toggleArmed {
		armed = armed.not;
		recordButton.icon = this.armedIcon;
		this.changed(\armed, armed)
	}

	bus_{
		|inBus|
		if (inBus != bus) {
			bus = inBus;
		}
	}
}

