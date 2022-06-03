PparStream : Stream {
	var <>initStreamAction, <>endStreamAction;
	var priorityQ, <now;
	var <injectFunc, pausedStreams;

	*forkPatterns {
		|pattern|
		var event, outerEvent, recursionLevel, instrument, embeddingLevel, freq, rest;
		var args, defaults, timingOffset, sustain, gatePattern;
		var size, newPatterns = [];

		if(pattern.notNil) {
			// preserve information from outer pattern, but not delta.
			if(~transparency ? true) {
				outerEvent = currentEnvironment.copy;
				outerEvent.putPairs(
					[
						\pattern, \type,
						\parentType, \addToCleanup,
						\removeFromCleanup, \sustain,
						\legato, \timingOffset, \delta, \gatePattern
					].collect([_, nil]).flatten
				)
			} {
				outerEvent = Event.default.copy;
			};

			if (pattern.isKindOf(Function)) {
				defaults = pattern.def.prototypeFrame;
				args = pattern.def.argNames.collect {
					|name, i|
					currentEnvironment[name].value ?? { defaults[i] }
				};
				pattern = pattern.value(*args);
			};

			sustain = ~sustain.value;
			~timingOffset = timingOffset = (~timingOffset.value ? 0);
			~gatePattern = gatePattern = (~gatePattern.value ? true);

			if (~flop ?? { false }) {
				outerEvent = outerEvent.asPairs.flop.collect(_.asEvent);
				outerEvent.do(_.parent_(currentEnvironment.parent));
			} {
				outerEvent = [outerEvent]
			};

			outerEvent.do {
				|outerEvent, i|
				var innerPattern;

				outerEvent.use {
					innerPattern = pattern;

					if (innerPattern.isKindOf(Symbol)) {
						innerPattern = Pdef(pattern);
					};

					if (innerPattern.isKindOf(Stream) and: { outerEvent.size > 1 }) {
						innerPattern = innerPattern.copy;
					};

					if (innerPattern.isKindOf(Event)) {
						Error("Event patterns must be wrapped in a Ref or a function when passed in as an \instrument argument").throw;
					};

					if (innerPattern.isKindOf(PatternProxy)) {
						innerPattern = innerPattern.pattern; // optimization. outer pattern takes care for replacement
					};

					if (innerPattern.notNil) {
						// not sure why we DON'T need to account for positive timingOffset here,
						// but if we do it breaks....
						if (gatePattern) {
							"limiting to dir=%".format(sustain).postln;
							innerPattern = innerPattern.finDur(sustain - timingOffset.min(0));
						};

						innerPattern = Pevent(innerPattern, outerEvent).asStream;

						if (timingOffset < 0) {
							innerPattern.fastForward(timingOffset.neg, 0, outerEvent);
						};

						newPatterns = newPatterns.add([
							timingOffset, innerPattern
						]);
					}
				}
			}
		};

		^newPatterns;
	}

	*initClass {
		Class.initClassTree(Event);

		Event.addEventType(\fork, {
			this.forkPatterns(~pattern ?? { ~instrument }).do {
				|newPatterns|
				~injectStream.value(
					delta: newPatterns[0],
					stream: newPatterns[1],
					inval: newPatterns[2]
				)
			}
		}, Event.parentEvents.default.copy.putAll((legato:1)));
	}

	*new {
		^super.new.init
	}

	init {
		priorityQ = PriorityQueue();
		pausedStreams = IdentitySet();
		now = 0;
		injectFunc = { |delta, stream, inval| this.injectStream(delta, stream, inval) };
	}

	reset {
		this.init();
	}

	injectStream {
		|delta, stream, inval|
		stream = stream.asStream;
		initStreamAction !? {
			stream = initStreamAction.value(stream, inval).asStream
		};
		priorityQ.put(now + delta, stream);
		^stream;
	}

	pauseStream {
		|stream|
		pausedStreams = pausedStreams.add(stream);
	}

	resumeStream {
		|stream, delta=0|
		pausedStreams = pausedStreams.remove(stream);
	}

	injectEvent {
		|delta, event, inval|
		this.injectStream(
			delta,
			Routine({ event.yield; nil.yield }, 2),
			inval
		)
	}

	// processStreamsToPause {
	// 	streamsToPause.do {
	// 		|stream|
	// 		priorityQ.remove(stream);
	// 		pausedStreams = pausedStreams.add(stream);
	// 	};
	// 	streamsToPause.clear();
	// }

	embedInStream {
		|inval, duration|
		var stream, nextTime, outval, endTime, nextTimeDelta;

		endTime = now + (duration ? inf);

		while { priorityQ.notEmpty and: { now < endTime } } {
			nextTime = min(
				priorityQ.topPriority,
				endTime
			);
			// "now: %        nextTime: %    endTime: %".format(now, nextTime, endTime).postln;

			if (nextTime > now) {
				nextTimeDelta = nextTime - now;

				pausedStreams.do {
					|stream|
					priorityQ.put(nextTimeDelta, stream);
				};

				inval = Event.silent(nextTimeDelta).yield;
				now = nextTime;
			} {
				stream = priorityQ.pop;
				if (pausedStreams.includes(stream).not) {
					outval = stream.next(inval).asEvent;

					if (outval.notNil) {
						// requeue stream
						priorityQ.put(now + outval.delta, stream);
						outval[\delta] = 0;
						outval[\injectStream] = outval[\injectStream] ?? { injectFunc };
						inval = outval.yield;
					} {
						endStreamAction.value(stream, inval);
					}
				};
			}
		};

		if (duration.notNil and: { now < endTime }) {
			inval = Event.silent(endTime - now).yield;
		};

		^inval
	}
}

Ppar2 : ListPattern {
	var <>initStreamAction, <>endStreamAction;

	embedInStream {
		|inval|
		var parStream = PparStream()
			.initStreamAction_(initStreamAction)
			.endStreamAction_(endStreamAction);

		repeats.value(inval).do({
			list.do(parStream.injectStream(0, _, inval));
			parStream.embedInStream(inval);
			parStream.reset();
		})
	}
}

PwithStream : Stream {
	var <>parStream, <>inputStream, <>condition, <>patternFunction, <>replace, <>gate, <>insert;
	var routine;

	*new {
		|parStream, inputStream, condition, patternFunction, replace, gate, insert|
		^super.newCopyArgs(parStream, inputStream, condition, patternFunction, replace, gate, insert).init
	}

	init {
		routine = Routine({
			|inEvent|
			this.embedInStream(inEvent);
		});
	}

	next {
		|inEvent|
		^routine.next(inEvent);
	}

	embedInStream {
		|inEvent|
		var conditionStream, shouldPlayOriginalEvent, originalEventDelta;
		var inEvents, outputStream;

		conditionStream = condition.asPattern.asStream;

		outputStream = Routine({
			|event|
			var shouldPlayOriginalEvent = true;
			var shouldWaitForForkedStream = false;
			var shouldReplaceAll = false;
			var shouldGatePattern;
			var forkPatterns=[], insertPatterns=[], resultPatterns;

			while {
				event = inputStream.next(event);
				event.notNil
			} {
				if (event.isRest.not and: { conditionStream.next(event) }) {
					shouldPlayOriginalEvent = replace.not;
					shouldWaitForForkedStream = insert;
					shouldReplaceAll = false;

					event.copy.use {
						// SUBTLE: We want to prevent gating if we're doing an insert,
						// but we only know if we're doing an insert once we've run our function.
						shouldGatePattern = ~gatePattern;
						~gatePattern = {
							value(
								shouldGatePattern ?? { (~insert ?? { shouldWaitForForkedStream }) !? _.not }
							)
						};

						resultPatterns = PparStream.forkPatterns(patternFunction);

						shouldPlayOriginalEvent = ~replace !? _.not ?? { shouldPlayOriginalEvent };
						shouldWaitForForkedStream = ~insert ?? { shouldWaitForForkedStream };
						shouldReplaceAll = ~replaceAll ?? { shouldReplaceAll };

						if (shouldReplaceAll) {
							forkPatterns.extend(0);
							insertPatterns.extend(0);
						} {
							if (shouldPlayOriginalEvent) {
								forkPatterns = forkPatterns.add([0, event.fin(1)])
							}
						};

						if (shouldWaitForForkedStream) {
							insertPatterns = insertPatterns.addAll(resultPatterns)
						} {
							forkPatterns = forkPatterns.addAll(resultPatterns)
						}
					}
				};

				// If we had no forks, then yield and move on.
				if ((forkPatterns.size == 0) and: { insertPatterns.size == 0 }) {
					event = event.yield;
				} {
					// Process all input events at the current time BEFORE forking / inserting
					if (event.delta > 0) {
						// If we generated no works, then yield straight through
						// First, inject our forked streams
						forkPatterns !? {
							|toFork|
							toFork = toFork.copy();
							forkPatterns.extend(0);

							toFork.do {
								|p|
								parStream.injectStream(p[0], p[1], currentEnvironment);
							}
						};

						if (insertPatterns.size > 0) {
							insertPatterns !? {
								|toInsert|
								toInsert = toInsert.copy;
								insertPatterns.extend(0);
								event = Ptpar(toInsert.flatten).embedInStream(event);
							};
						}  {
							event = Event.silent(event.delta).yield;
						};
					}
				}
			}
		});

		parStream.injectStream(0, outputStream, inEvent);

		^parStream.embedInStream(inEvent)
	}
}

Pwith : Pattern {
	var <inputPattern, <conditions, <patternFunctions,
	<>replace, <>gate, <>insert;

	*new {
		|inputPattern, condition, pattern ...conditionPatterns|
		conditionPatterns = conditionPatterns.clump(2).flop;

		condition = [condition] ++ conditionPatterns[0];
		pattern = [pattern] ++ conditionPatterns[1];

		^super.newCopyArgs(
			inputPattern,
			condition,
			pattern
		).init
	}

	init {
		replace = true;
		gate = false;
		insert = false;
	}

	fixPattern {
		|p|
		^p.isFunction.if({
			p
		}, {
			{ p }
		})
	}

	asStream {
		var stream;
		var inputStream = inputPattern.asStream;

		[conditions, patternFunctions].flop.flatten.pairsDo {
			|condition, patternFunction|

			inputStream = PwithStream(
				PparStream(),
				inputStream,
				condition,
				this.fixPattern(patternFunction),
				replace,
				gate,
				insert: false
			);
		};

		^inputStream
	}
}

Pfork : Pwith {
	init {
		replace = false;
		gate = true;
		insert = false;
	}
}

Preplace : Pwith {
	init {
		replace = true;
		gate = true;
		insert = false;
	}
}

Pinsert : Pwith {
	init {
		replace = true;
		gate = false;
		insert = false;
	}
}

Pb : Ppar2 {
	*new {
		|...keysValues|
		^super.new([
			Pbind(
				*keysValues
			)
		])
	}
}

+Ppar {
	// *new {
	// 	|list, repeats=1|
	// 	^Ppar2.new(list, repeats);
	// }
}
