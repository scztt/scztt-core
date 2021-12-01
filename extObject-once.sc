+Event {
	bind {
		^Pbind(pairs: this.asKeyValuePairs())
	}

	bindOnce {
		^Pbind(pairs: this.asKeyValuePairs() ++ ['Event-bindOnce-dummy', Pseq([0])])
	}
}