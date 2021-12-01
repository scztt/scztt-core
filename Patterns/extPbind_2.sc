+Pbind {
	*set {
		|...args|
		^Pbind(
			\type, \set,
			\args, args.clump(2).flop[0].reject(_ == \dur),
			*args
		)
	}
}