Pid {
	*new {
		^Pfunc({ |e| e })
	}
}

Pmaybe {
	*new {
		|condition, pattern|
		^Pif(
			Pfunc({ |e| e.use { condition.value(e) } }),
			pattern,
			Pfunc({ |e| Event.silent(e.dur) })
		)
	}
}

PkeyOr : Pkey {
	var	<>key, <>defaults, <>repeats;

	*new { |key, defaults, repeats|
		^super.newCopyArgs(key, defaults, repeats)
	}

	storeArgs { ^[key, defaults, repeats] }

	asStream {
		var	keystream = key.asStream;
		// avoid creating a routine
		var stream = FuncStream({ |inevent| inevent !? { inevent[keystream.next(inevent)] } });
		^if(repeats.isNil) { stream } { stream.fin(repeats) }
	}

	embedInStream { |inval|
		var outval, defaultsStream = defaults.asStream, keystream = key.asStream;
		(repeats.value(inval) ?? { inf }).do {
			outval = inval[keystream.next(inval)];
			if (outval.isNil) { outval = defaults.next(inval) };
			if (outval.isNil) { ^inval };
			inval = outval.yield;
		};
		^inval
	}
}

// +PatternProxy {
// 	defaultEvent {
// 		|...args|
// 		if(envir.isNil) { envir = this.class.event };
// 		if (envir[\functional] === true) {
// 			^args
// 		} {
// 			^if(envir[\independent] === true) { (parent:envir) } { envir }
// 		}
// 	}
//
// 	convertFunction { arg func;
// 		^Prout {
// 			var inval = func.def.prototypeFrame !? { inval = this.defaultEvent(inval) };
// 			func.value( inval ).embedInStream(inval)
// 		}
// 	}
//
// }