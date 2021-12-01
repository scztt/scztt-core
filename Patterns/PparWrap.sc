PparWrap : Ppar {
	var wrap, cachedPatterns, streamIndex=0;

	*new { arg list, wrap, repeats=1;
		wrap = wrap ?? { {|v|v} };
		^super.new(list, repeats).wrap(wrap);
	}

	*chained {
		|list, repeats=1, channels=2, mapIn=true|
		var func;
		mapIn = mapIn.asArray;
		func = {
			|pat, i, parent|

			~group = (
				type:	\group,
				group: 	~group, addAction:\addAfter
			).yield;

			if (mapIn.wrapAt(i) && ~out.notNil) {
				~in = ~out.channels.collect {
					|i|
					"a%".format(~out.out + i).asSymbol;
				};
				~in = [~in];
			} {
				~in = ~out;
			};

			~out = (
				type: 	\audioBus,
				channels: 2
			).yield;

			currentEnvironment.parent[\out] = ~out;
			currentEnvironment.parent[\group] = ~group;

			pat
		};

		^this.new(list, func, repeats);
	}

	initStream {
		|time, priorityQ, inval, pattern, i|
		var protoEvent, outval;
		var parentEnvironment = ();
		var initializedEvent = (type:\nil);
		var proto = Pproto({
			|parent|
			currentEnvironment.parent = parentEnvironment;
			pattern = wrap.value(pattern, i);
			initializedEvent.yield;
		}, Plazy({
			pattern
		}));
		proto = proto.asStream;

		while {
			outval = proto.next(inval);
			outval.notNil and: { outval != initializedEvent }
		}{
			inval = outval.yield;
		};

		"priorityQ.put(%, %);".format(
			time, proto
		).postln;
		priorityQ.put(time, proto);

		^proto
	}

	initStreams {
		|priorityQ, inval|
		EventTypesWithCleanup.cleanupTypes[\nil] = ();
		Event.default[\eventTypes][\nil] = {};
		list.do(this.initStream(0, priorityQ, inval, _, _));
	}

	initEvent {
		|event, now, priorityQ|
		event[\pparNow] = now;
		event[\pparPriorityQ] = priorityQ;
		event[\injectPattern] = {
			|pattern, delta=0|
			~pparNow.postln;
			~pparPriorityQ.postln;
			delta.postln;
			this.initStream(
				~pparNow + delta,
				~pparPriorityQ,
				(),
				pattern
			)
		}
	}

	embedInStream { arg inval;
		var assn;
		var priorityQ = PriorityQueue.new;

		repeats.value(inval).do({ arg j;
			var outval, stream, nexttime, now = 0.0;

			this.initStreams(priorityQ, inval);

			// if first event not at time zero
			if (priorityQ.notEmpty and: { (nexttime = priorityQ.topPriority) > 0.0 }) {
				outval = Event.silent(nexttime, inval);
				inval = outval.yield;
				now = nexttime;
			};

			while { priorityQ.notEmpty } {
				stream = priorityQ.pop;
				"Popping, now=%, stream=%".format(now, stream).postln;
				outval = stream.next(inval).asEvent;
				if (outval.isNil) {
					"outval is nil".postln;
					nexttime = priorityQ.topPriority;
					if (nexttime.notNil, {
						// that child stream ended, so rest until next one
						outval = Event.silent(nexttime - now, inval);
						inval = outval.yield;
						"now -> %".format(nexttime).postln;
						now = nexttime;
					},{
						priorityQ.clear;
					});
				} {
					// requeue stream
					"priorityQ.put(% + %, %);".format(
						now, outval.delta, stream
					).postln;

					priorityQ.put(now + outval.delta, stream);
					nexttime = priorityQ.topPriority;
					outval.put(\delta, nexttime - now);

					this.initEvent(outval, now, priorityQ);
					inval = outval.yield;
					// inval ?? { this.purgeQueue(priorityQ); ^nil.yield };
					nexttime = priorityQ.topPriority;
					"now -> %".format(nexttime).postln;
					now = nexttime;
				};
			};
		});
		^inval;
	}

	wrap {
		|func|
		var existing;
		if (wrap.notNil) {
			existing = wrap;
			wrap = {
				|pattern, i|
				func.value(
					existing.value(pattern, i),
					i
				)
			}
		} {
			wrap = func;
		}
	}
}

// +Pdef {
// 	*struct {
// 		|func|
// 		var envir, lazy, result, protoFuncs;
//
// 		envir = ();
// 		lazy = LazyEnvir(envir);
// 		lazy.proxyClass = Event;
// 		result = lazy.use(func);
//
// 		// build a map of proxy functions
// 		protoFuncs = this.makeProtoFuncs(result, envir);
// 	}
//
// 	*makeProtoFuncs {
// 		|output, envir|
// 		var queue, protoFuncs = ();
//
// 		queue = Array(64);
// 		queue = queue.add(output);
//
// 		while { queue.notEmpty } {
// 			var inputs, proto, itemName, func;
//
// 			item = queue.pop();
// 			itemName = envir.findKeyForValue(item);
//
// 			item.keysValuesDo {
// 				|param, input|
// 				var inputName = envir.findKeyForValue(input);
// 				queue = queue.add(input);
// 				inputs = inputs.add(inputName);
// 			};
//
// 			protoFuncs[itemName] = Thunk({
// 				~group = (
// 					type:	\gdef,
// 					name:	[\pstruct, itemName],
// 					after:	inputs
// 				).yield;
//
// 				~out = (
//
// 				);
//
// 				item.keysValuesDo {
// 					|param, input|
// 					var inputName, mapString;
//
// 					inputName = envir.findKeyForValue(input);
// 					mapString = switch(
// 						input[\rate],
// 						\ar, "a%", \audio, "a%",
// 						\kr, "k%", \control, "k%",
// 						"a%"
// 					);
//
// 					input = protoFuncs[inputName].value[\out];
// 					input = input.channels.collect {
// 						|i|
// 						mapString.format(input.out
// 						};
//
//
// 						currentEnvironment[param] = ;
// 					};
//
// 					if (mapIn.wrapAt(i) && ~out.notNil) {
// 						~in = ~out.channels.collect {
// 							|i|
// 							"a%".format(~out.out + i).asSymbol;
// 						};
// 						~in = [~in];
// 					} {
// 						~in = ~out;
// 					};
//
// 					~out = (
// 						type: 	\audioBus,
// 						channels: 2
// 					).yield;
//
// 					currentEnvironment.parent[\out] = ~out;
// 					currentEnvironment.parent[\group] = ~group;
//
// 					pat
// 				};
//
// 				protoFuncs[itemName] = Thunk({
// 					var e = ();
//
//
// 					e[param] = {
// 						"fetched '%': %".format(inputName, protos[inputName]).postln;
// 						protos[inputName][\out].asMap;
// 					};
// 					e[\name] = itemName;
// 					e[\out] = Bus.alloc(item[\rate] ? \audio, s, item[\numChannels] ? 2);
// 					e[\group] = Gdef([\pmix, proto[\name]], after:inputs).permanent_(false);
//
// 				});
// 				};
//
// 				protos.collect {
// 					|proto|
// 					proto.collect(proto.use(_))
// 				};
// 			}
// 		}