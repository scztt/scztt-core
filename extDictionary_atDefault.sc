+Dictionary {
	atDefault {
		|key, defaultFunc|
		^this.at(key) ?? {
			var new = defaultFunc.value(key);
			this.put(key, new);
			new;
		}
	}
}