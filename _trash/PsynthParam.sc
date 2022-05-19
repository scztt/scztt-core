PsynthParam : FilterPattern {
	classvar defCache;
	var <>bufferSize, <>channels, <>rate;

	*new {
		|pattern, channels=1, rate=\control, bufferSize=8|
		^super.newCopyArgs(pattern)
		.bufferSize_(bufferSize)
		.channels_(channels)
		.rate_((control:\kr, audio:\ar, ar:\ar, kr:\kr)[rate]);
	}

	*initClass {
		defCache = ();
		(1..16).do {
			|n|
			[\kr, \ar].do {
				|rate|
				this.wrapSynth("PsynthParam_constant_%_%".format(n, rate).asSymbol, rate, {
					\value.perform(rate, (0 ! n)).poll(1);
				});
			}
		}
	}

	*wrapSynth {
		|defName, rate, func|
		if (defCache[defName].isNil) {
			defCache[defName] = SynthDef(defName, {
				var fadeTime, paramLag, fade, sig;

				fadeTime = \fadeTime.kr(0);
				paramLag = \paramLag.kr(0);

				fade = Env([0, 1, 1], [fadeTime, 99999]).kr(gate:1);
				sig = SynthDef.wrap(func, paramLag ! func.def.argNames.size);

				XOut.perform(rate, \out.kr(-1), fade, sig);
			}).add;
		} {
			defCache[defName].add;
		}
	}

	embedInStream {
		|inval|
		var writeIndex, readIndex, buffer, stream, rebufferFunc;
		var buses, newParamGroup, paramGroup, newSynthGroup, synthGroup, currentEvent, currentArgs, currentSize, fadeTime, nextEvent, cleanup;
		var busEvent;

		cleanup = EventStreamCleanup();

		if (rate == \ar) {
			buses = List.newFrom([Bus.audio(Server.default, channels)])
		} {
			buses = List.newFrom([Bus.control(Server.default, channels)])
		};

		if (inval.keys.includes(\group)) {
			synthGroup = inval[\group];
		} {
			inval[\group] = newSynthGroup = synthGroup = Group(Server.default);
		};

		if (inval.keys.includes(\paramGroup)) {
			paramGroup = inval[\paramGroup];
		} {
			inval[\paramGroup] = newParamGroup = paramGroup = Group(synthGroup, \addBefore);
		};

		cleanup.addFunction(inval, {
			newSynthGroup !? _.free;
			newParamGroup !? _.free;
			buses.do(_.free);
		});

		stream = pattern.asStream;

		loop {
			inval[\group] = synthGroup;

			nextEvent = inval.copy;
			nextEvent[\synthDesc] = nil;
			nextEvent[\msgFunc] = nil;
			nextEvent = stream.next(nextEvent);

			if (nextEvent.isNil) {
				^cleanup.exit
			} {
				nextEvent = this.prepareSynth(nextEvent);

				cleanup.update(inval);

				currentArgs = nextEvent[\instrument].asSynthDesc.controlNames;
				currentSize = nextEvent.atAll(currentArgs).maxValue({ |i| i.asArray.size });
				(currentSize - buses.size).do {
					if (rate == \ar) {
						buses = buses.add(Bus.audio(Server.default, channels))
					} {
						buses = buses.add(Bus.control(Server.default, channels))
					};
				};

				if (nextEvent[\instrument] != currentEvent.tryPerform(\at, \instrument)) {
					nextEvent[\type] 		= \note;
					nextEvent[\sustain] 	= nil;
					nextEvent[\sendGate] 	= false;
					nextEvent[\fadeTime] 	= fadeTime = nextEvent[\fadeTime] ?? inval[\fadeTime] ?? 0;
					nextEvent[\out] 		= buses[0..(currentSize-1)].collect(_.index);
					nextEvent[\group] 		= paramGroup;
					nextEvent[\addAction] 	= \addToTail;

					currentEvent !? {
						|e|
						{
							e.use(_.free)
						}.defer(fadeTime + 0.05)
					};

					currentEvent = nextEvent;
				} {
					nextEvent[\type] 		= \set;
					nextEvent[\id] 			= currentEvent[\id];
					nextEvent[\args] 		= currentArgs;
					nextEvent[\out] 		= buses[0..(currentSize-1)];
				};

				nextEvent.parent ?? { nextEvent.parent = Event.parentEvents.default };
				nextEvent.playAndDelta(cleanup, false);
			};


			inval = {
				buses[0..(currentSize-1)].collect(_.asMap)
			}.yield
		}

		^cleanup.exit;
	}

	prepareSynth {
		|synthVal|
		var defName;

		^case
		{ synthVal.isKindOf(Array) } {
			synthVal.collect(this.prepareSynth(_)).reduce({
				|a, b|
				a.merge(b, {
					|a, b|
					a.asArray.add(b)
				})
			})
		}
		{ synthVal.isKindOf(SimpleNumber) } {
			(
				instrument: "PsynthParam_constant_%_%".format(synthVal.asArray.size, rate).asSymbol,
				value: synthVal
			)
		}
		{ synthVal.isKindOf(Symbol) } {
			(
				instrument: defName
			)
		}
		{ synthVal.isKindOf(AbstractFunction) } {
			defName = "PsynthParam_%".format(synthVal.hash).asSymbol;
			this.class.wrapSynth(defName, rate, synthVal);
			(
				instrument: defName
			)
		}
		{
			synthVal.putAll(this.prepareSynth(synthVal[\instrument]));
			synthVal;
		}
	}

	playSynth {
		|bus, synthVal, event|

		if (synthVal.isKindOf(Symbol)) {
			event[\instrument] = synthVal;
		} {
			event[\instrument] = \PsynthParam_constant;
			event[\value] = synthVal;
		};

		event[\type] = \note;
		event[\sustain] = nil;
		event[\sendGate] = false;
	}
}