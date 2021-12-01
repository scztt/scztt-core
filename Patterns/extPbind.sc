Psustain : FilterPattern  {
	var <>dur, <>tolerance;

	*new { arg dur, pattern, tolerance = 0.001;
		^super.new(pattern).dur_(dur).tolerance_(tolerance)
	}
	storeArgs { ^[dur,pattern,tolerance] }

	embedInStream { arg inval;
		var startTime, nextTime;
		var stream = pattern.repeat.asStream;
		var cleanup = EventStreamCleanup.new;

		startTime = thisThread.endBeat ? thisThread.beats;
		thisThread.endBeat = startTime + dur.value(inval);

		while {
			thisThread.beats < thisThread.endBeat;
		} {
			inval = stream.next(inval);
			if (inval.isKindOf(Event)) {
				cleanup.update(inval);
				nextTime = thisThread.beats + inval.delta.value;
				if (nextTime.roundUp(tolerance) >= thisThread.endBeat) {
					// must always copy an event before altering it.
					// fix delta time and yield to play the event.
					inval = inval.copy.put(\delta, nextTime - thisThread.endBeat).yield;
					^cleanup.exit(inval);
				};
			};

			inval.yield;
		}
	}
}

+Object {
	sustain {
		|dur|
		^Psustain(dur, this)
	}
}

+Pbind {
	*make {
		|func|
		var environment, order, pbindArgs;
		order = OrderedIdentitySet();
		environment = EnvironmentRedirect();
		environment.envir = ();
		environment.envir.parent = currentEnvironment;
		environment.dispatch = {
			|key, value|
			order.add(key);
		};
		environment.use(func);
		pbindArgs = Array(order.size * 2);
		order.do {
			|key|
			pbindArgs.add(key);
			pbindArgs.add(environment[key]);
		};
		^Pbind(*pbindArgs)
	}
}

+Pdef {
	use {
		|func|
		var parent, envir;
		parent = ();
		source.patternpairs.pairsDo {
			|key, pat|
			if (pat.isKindOf(PatternProxy)) {
				parent[key] = pat.source;
			} {
				parent[key] = pat;
			}
		};
		envir = Event(parent:parent);
		envir.use(func);
		Pbindef(key, *envir.asPairs);
	}
}

+PbindProxy {
	patternpairs {
		^pairs
	}
}

+Symbol {
	in {
		|...args|
		^PatternProxy({
			currentEnvironment.parent[this];
		}.inEnvir).repeat
	}
}

+EnvironmentRedirect {
	parent {
		^envir.parent
	}

	proto {
		^envir.proto
	}
}

