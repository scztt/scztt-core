// was pembed
Pinject : FilterPattern {
	*new {
		arg pattern;
		^super.new(pattern)
	}
	storeArgs { ^[pattern] }

	embedInStream { arg event;
		var cleanup = EventStreamCleanup.new;
		var val, inEvent, collectedEvent;
		var evtStream = pattern.asStream;

		loop {
			if (event.isNil) { ^nil.yield };
			inEvent = evtStream.next(event);
			if (inEvent.isNil) { ^cleanup.exit(event) };
			if (inEvent.isCollection && inEvent.isKindOf(Event).not) {
				collectedEvent = ();
				inEvent.do {
					|evt|
					evt.keysValuesDo {
						|key, val|
						collectedEvent[key] = collectedEvent[key].add(val);
					}
				};
				collectedEvent.postln;
				inEvent = collectedEvent;
			};
			event.putAll(inEvent);

			cleanup.update(inEvent);
			event = inEvent.yield;
		}
	}
}

Pat : Pattern {
	var	<>key, <>repeats;
	*new { |key, repeats = 1|
		^super.newCopyArgs(key, repeats)
	}
	storeArgs { ^[key, repeats] }
		// avoid creating a routine
	embedInStream {
		arg inval;
		var	keystream = key.asStream;
		repeats.value(inval).do {
			inval = inval[key.next(inval)].embedInStream(inval);
		};
		^inval
	}
}