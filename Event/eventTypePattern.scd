Event.addEventType(\pattern, {
	var pat, event, outerEvent, recursionLevel, instrument, embeddingLevel, freq, rest;
	var args, defaults, timingOffset;

	~pattern = ~pattern ?? { ~instrument };

	if (~pattern.isKindOf(Function)) {
		defaults = ~pattern.def.prototypeFrame;
		args = ~pattern.def.argNames.collect {
			|name, i|
			currentEnvironment[name].value ?? { defaults[i] }
		};
		pat = ~pattern.value(*args);
	} {
		if (~pattern.isKindOf(Symbol)) {
			~pattern = Pdef(~pattern);
		};

		pat = ~pattern.value;
	};

	if (pat.isKindOf(Event)) {
		Error("Event patterns must be wrapped in a Ref or a function when passed in as an \instrument argument").throw;
	};

	if(pat.notNil) {
		if (pat.isKindOf(PatternProxy)) {
			pat = pat.pattern; // optimization. outer pattern takes care for replacement
		};

		// preserve information from outer pattern, but not delta.
		if(~transparency ? true) {
			outerEvent 						= currentEnvironment.copy;
			outerEvent[\instrument] 		= nil;
			outerEvent[\pattern] 			= nil;
			outerEvent[\type] 				= nil;
			outerEvent[\parentType] 		= nil;
			outerEvent[\timingOffset] 		= nil;
			outerEvent[\addToCleanup] 		= nil;
			outerEvent[\removeFromCleanup]	= nil;
		} {
			outerEvent = Event.default;
		};

		outerEvent.put(\delta, nil); // block delta modification by Ppar
		outerEvent.put(\instrument, ~synthDef);

		timingOffset = ~timingOffset.value ? 0;

		// not sure why we DON'T need to account for positive timingOffset here,
		// but if we do it breaks....
		if (~gatePattern ? true) {
			pat = pat.finDur(~sustain.value - timingOffset.min(0));
		};

		if (timingOffset < 0) {
			pat = pat.asStream;
			pat.fastForward(timingOffset.neg, 0, outerEvent);
			pat = pat.asEventStreamPlayer(outerEvent);
			pat.play(thisThread.clock, quant:0.0);
		} {
			pat.play(thisThread.clock, outerEvent, 0.0)
		}
	}
}, Event.parentEvents.default.copy.putAll((legato:1)));
