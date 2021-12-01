+ Pdef {
	phrase {
		|...args|
		^Pbind(
			\type, \phrase,
			\instrument, key,
			*args
		)
	}
}