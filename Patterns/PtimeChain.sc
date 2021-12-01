// Play many patterns according to their own delta's', composing the events from bottom to top.
// Deltas of first pattern are responsible for timing of output events
//
// (
// Pdef(\ptchain).clear;
// Pdef(\ptchain, PTChain(
// 	Pbind(
// 		\dur, Pseg([1/32, 1/2, 1/32], [16, 16], \exponential, inf),
// 		\velocity, Pwhite(40, 127),
// 		\strum, Pkey(\dur) / 2
// 	),
// 	Pbind(
// 		\degree, Ptuple([Pkey(\degreeA), Pkey(\degreeB)])
// 	),
// 	Pbind(
// 		\dur, 1/3,
// 		\degreeA, Ptuple([
// 			Pseq([0, 2, 6, 8, 5], inf),
// 		])
// 	),
// 	Pbind(
// 		\dur, 2,
// 		\degreeB, Ptuple([
// 			12 + Pseq([0, -6], inf),
// 		])
// 	),
// )).play
// )

PTChain : Pattern {
	var <>patterns;

	*new { arg ... patterns;
		^super.newCopyArgs(patterns);
	}

	*durs { arg durs ... patterns;
		^super.newCopyArgs(
			[Pbind(\dur, durs)] ++ patterns
		);
	}

	<< { arg aPattern;
		var list;
		list = patterns.copy.add(aPattern);
		^this.class.new(*list)
	}

	embedInStream { arg inval;
		var structureStream = patterns[0].asStream;
		var startTime = thisThread.beats;
		// Store the value streams, their current time and latest Events
		var valueStreams = patterns[1..].collect{ |p| [p.asStream, (), ThreadStateScope() ] };
		var inevent, cleanup = EventStreamCleanup.new;
		var timeEpsilon = 0.0001;
		loop {
			var structureEvent;
			var cumulativeEvent = inevent = inval.copy;
			// inevent.debug("inevent at start of loop");

			valueStreams.reverseDo { |strData, i|
				var valueStream, nextValueEvent, threadState;
				#valueStream, nextValueEvent, threadState = strData;
				while {
					// "stream: %    current time: %     stream time: %    timeToCheck: %".format(
					// 	valueStream,
					// 	thisThread.beats - startTime,
					// 	threadState.beats - startTime,
					// 	(thisThread.beats - startTime) + timeEpsilon
					// ).debug;
					threadState.beats <= (thisThread.beats + timeEpsilon);
				} {
					var delta;

					threadState.use {
						if (inevent !== cumulativeEvent) {
							inevent.parent_(cumulativeEvent);
						};
						nextValueEvent = valueStream.next(inevent);
						// "\tpulled new value: %".format(nextValueEvent).postln;
					};
					// nextValueEvent.debug("nextValueEvent");
					// Q: Should we exit for value streams that end, or just the structure stream?
					// A: Will have to look at concrete examples, for now: yes, we exit when
					//    any of the streams ends...
					if (nextValueEvent.isNil) { ^cleanup.exit(inval) };
					delta = nextValueEvent.delta.value;

					if (delta.notNil) {
						threadState.beats = threadState.beats + delta;
					} {
						// There is no time information, just use our next value
						// for the next structure Event (as regular Pchain would do)
						threadState.beats = threadState.beats + (timeEpsilon * 2);
					};

					// nextValueTime.debug("nextValueTime updated");
					// inevent feeds from one into the next, gathering/replacing values
					strData[1] = nextValueEvent;
				};

				// Combine the contributions of all the "current" value events
				// that came before the main structure event.
				cumulativeEvent = cumulativeEvent.composeEvents(nextValueEvent);
				// cumulativeEvent.debug("updated cumulativeEvent");
			};

			structureEvent = structureStream.next(cumulativeEvent);
			if (structureEvent.isNil) { ^cleanup.exit(inval) };
			cleanup.update(structureEvent);
			// structureEvent.debug("yielded structureEvent");
			inval = yield(structureEvent);
			// structureTime.debug("structureTime");
		};
	}

	storeOn { arg stream;
		stream << "(";
		patterns.do { |item,i|  if(i != 0) { stream << " <> " }; stream <<< item; };
		stream << ")"
	}
}

+Pattern {

	<< { arg aPattern;
		// time-based pattern key merging
		^PTChain(this, aPattern)
	}

}