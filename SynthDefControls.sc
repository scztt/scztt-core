SynthDefControls {
	var <buildSynthDef;
	var <controlNames;
	var <controls;

	*new {
		|buildSynthDef|
		^super.newCopyArgs(buildSynthDef).init
	}

	init {
		controlNames = List();
		controls = IdentityDictionary();
	}

	getControl {
		|name, rate, lags, defaultValues, spec|
		var existingControlName, controlName;

		existingControlName = this.controlNameAt(name);
		controlName = ControlName(
			name:			name,
			index:			nil,
			rate:			rate,
			defaultValue:	defaultValues ? (spec ?? { spec.default }),
			argNum:			nil,
			lag:			lags,
			spec:			spec
		);

		if (existingControlName.isNil) {
			this.addControlName(controlName);
		} {
			this.validateEquivalent(controlName, existingControlName);
			controlName = existingControlName;
		};

		^this.controlAt(name);
	}

	controlNameAt {
		|name|
		^controlNames.detect({ |c| c.name == name });
	}

	controlAt {
		|name|
		^controls.at(name)
	}

	addControlName {
		|controlName|
		controlNames.add(controlName);
		controls[controlName.name] = this.createControl(controlName);

		if (controlName.spec.notNil) {
			if (buildSynthDef.specs[controlName.name].notNil) {
				Error("Overwriting a spec that has already been declared as %".format(buildSynthDef.specs[controlName.name])).throw
			};

			buildSynthDef.specs[controlName.name] = controlName.spec;
		}
	}

	createControl {
		|controlName|

	}
}