+Dictionary {
	subset {
		|...keys|
		var new = this.species.new;
		keys.do {
			|k|
			new[k] = this[k]
		};
		^new;
	}

	take {
		|value|
		var key = this.findKeyForValue(value);
		^key !? {
			this.removeAt(key);
			value
		} ?? {
			nil
		}
	}

	takeAt {
		|key|
		var value = this.at(key);
		this.removeAt(key);
		^value
	}
}