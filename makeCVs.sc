+Symbol {
	asSynthDesc {
		^SynthDescLib.default[this]
	}
}

+SynthDesc {
	makeCVs {
		var result = ();

		this.metadata !? {
			this.metadata.specs !? {
				this.metadata.specs.keysValuesDo {
					|key, val|
					result[key] = CV(val);
				}
			}
		};

		^result
	}
}