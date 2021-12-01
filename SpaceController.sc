SpaceController : Singleton {
	var hid, <values;

	*new {
		|name = \USB_046d_c626_14600000 ...args|
		^super.new(name, *args);
	}

	init {
		|name|
		var device;
		var usages = ['X', 'Y', 'Z', 'Rx', 'Ry', 'Rz'];
		var buttons = ['b1', 'b2'];

		HID.findAvailable();

		device = hid = HID.findBy(path: name.asString).asArray.first;

		if (hid.isNil) {
			this.clear();
			Error("Could not find HID device at path: %".format(name)).throw;
		} {
			hid = hid.open();

			values = ();

			usages.do {
				|usage|
				var cv = NumericControlValue(0, spec:[-1, 1]);

				hid.elements.detect({
					|element|
					element.usageName.asSymbol == usage;
				}).action = {
					|value|
					cv.value = value;
				};

				values[usage] = cv;
			};

			buttons.do {
				|usage|
				var cv = OnOffControlValue(\off);

				hid.elements.detect({
					|element|
					element.usageName.asSymbol == usage;
				}).action = {
					|value|
					cv.input = value;
				};

				values[usage] = cv;
			}
		}
	}

	xyz {
		^[values[\X].value, values[\Y].value, values[\Z].value]
	}

	rxyz {
		^[values[\Rx].value, values[\Ry].value, values[\Rz].value]
	}

	clear {
		super.clear();
		hid !? _.close();
		hid = nil;
	}
}