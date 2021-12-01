Pio : Pattern {
	var inner;

	*new {
		|inner|
		^super.newCopyArgs(inner)
	}

	*in { |name, numChannels=2, rate=\audio|		^Pin(name, numChannels, rate) }
	*out { |name, numChannels=2, rate=\audio|		^Pout(name, numChannels, rate) }

	*make {
		|func, composeWith|
		var result, resultEnvir;

		resultEnvir = DefaultEnvironment().defaultFunc_({ () });

		// Call func, with resultEnvir as currentEnvironment
		// result is considered to be the "final output" Pattern, e.g. it will
		// play to (\out, 0).
		result = resultEnvir.use(func);
		resultEnvir = resultEnvir.envir;

		if (result.isArray.not) { result = [result] };
		result.do {
			|event|
			event[\out] = 0;
		};

		// Fill in \name key, and add Pout with that name
		resultEnvir = resultEnvir.collect {
			|event, key|
			event.putAll((
				name: key,
				out: event[\out] ?? { Pout(key) }
			))
		};

		// Replace references to other objects in event with Pin corresponding to
		// their \out.
		resultEnvir = resultEnvir.collect {
			|event|
			event.value.collect({
				|value, key|
				if (value.isKindOf(Event)) {
					Pin(value[\out].name)
				} {
					value
				}
			})
		};

		// Convert events to event streams
		resultEnvir = resultEnvir.collect {
			|event|
			Pbind(*event.asPairs)
		};

		if (composeWith.notNil) {
			resultEnvir = resultEnvir.merge(composeWith, {
				|pbind, source, key|
				Pchain(pbind, source)
			}, false);
		};

		^resultEnvir
	}

	finishEvent {
		|buses|
		var inputs=[], outputs=[];

		Log(\Pio).debug("finishEvent(%)", buses);

		~out 	= ~out ?? { Pout(~name ?? { \out }) };
		~name 	= ~name ?? { ~out.tryPerform(\name) } ?? ~instrument;

		currentEnvironment.keysValuesChange {
			|key, value|
			var valueFunc, bus, oldBus;

			if (value.isKindOf(PNRe f)) {
				value = value.stream;

				if (value.isInput) {
					inputs = inputs.add(value.name);
				} {
					outputs = outputs.add(value.name);
				};

				buses[value.name] = bus = buses[value.name] ?? {
					(type: \audioBus, channels: value.numChannels)
				};

				if (value.numChannels > bus[\channels]) {
					"Event channels % is greater than allocated bus %".format(value.numChannels, bus[\channels]).postln;

					oldBus = bus;
					buses[value.name] = bus = (type: \audioBus, channels: value.numChannels);
				};

				// We wrap our bus allocation in a function, so it only occurs when this value is actually
				// accessed e.g. by playing to a Synth. A bus will NOT be allocated if this key is later
				// overridden.
				valueFunc = {
					if (oldBus.notNil) {
						"freeing old bus (%) and allocating new one...".format(bus).postln;
						bus[\out] = oldBus[\out];
						oldBus.free;
						bus.play;
					};

					if (bus[\out].isNil) {
						bus.play
					};

					bus
				};

				// Convert bus to an appropriate control input or map argument
				if (value.isOutput) {
					valueFunc = (_.asControlInput) <> valueFunc;
				} {
					valueFunc = {
						|b|
						b.channels.collect {
							|i|
							"a%".format(b.out + i).asSymbol;
						};
					} <> valueFunc
				};

				valueFunc;
			} {
				value
			}
		};

		~group = Gdef(~name, after:inputs).permanent_(false);
	}

	embedInStream {
		|inval|
		var buses = ();
		var innerStream = inner.asStream;
		var cleanup;

		inval = innerStream.next(inval);

		cleanup = EventStreamCleanup();
		cleanup.addFunction(inval, {
			buses.do({ |bus| MultiRecorder().removeTarget(bus.asBus) });
			buses.do(_.free);
			buses.clear();
		});

		while { inval.notNil } {
			// inval[\finish] = inval[\finish].addFunc({
			// 	this.finishEvent(buses)
			// });
			inval = inval.make { this.finishEvent(buses) };
			buses.keysValuesDo({
				|name, bus|
				MultiRecorder().addTarget(name, bus.asBus)
			});
			inval = innerStream.next(inval.yield)
		};

		^inval
	}
}

PNRefStream : Stream {
	var <>name, <>numChannels, <>rate;
	var cleanupActions;
	var <>createFunc, <>freeFunc;
	var <>isInput, <>isOutput;

	create {
		|namespace|
		Log(PNStream).debug("PNRef(%) creating resource", name);
		^createFunc.value(namespace)
	}

	free {
		|resource|
		Log(PNStream).debug("PNRef(%) freeing resource %", name, resource);
		freeFunc.value(resource)
	}

	addCleanup {
		|func|
		Log(PNStream).debug("PNRefStream:addCleanup(%)", func);
		cleanupActions = cleanupActions.addFunc(func);
	}

	embedInStream {
		|inEvent|
		var cleanup = EventStreamCleanup();

		Log(PNStream).debug("Beginning PNRefStream:embedInStream");

		cleanup.addFunction(inEvent, Thunk({
			Log(PNStream).debug("PNRefStream running cleanups");
			cleanupActions.value
		}));

		loop {
			cleanup.update(inEvent);
			inEvent = PNRef(this).yield;
			inEvent ?? {
				^cleanup.exit(inEvent);
			}
		}
	}
}

PN : Pattern {
	embedInStream { |inval| ^PNStream().init.pns_(this).embedInStream(inval) }
}

PNStream : Stream {
	var <>pns;
	var resources, refs, cleanups, order;

	init {
		resources = ();
		refs = ();
		cleanups = ();
		order = LinkedList();
	}

	getResource {
		|pnref|
		Log(PNStream).debug("Fetching resource for %(%)", pnref, pnref.name);

		this.addRef(pnref);

		^resources.atDefault(pnref.name, {
			var resource = pnref.create(this);
			cleanups[pnref.name] = { pnref.free(resource) };
			resource
		});
	}

	getGroup {
	}

	addRef {
		|ref|
		var set = refs.atDefault(ref.name, { IdentitySet() });
		if (set.includes(ref).not) {
			set.add(ref);
			ref.addCleanup({
				this.removeRef(ref)
			});
		};

		Log(PNStream).debug("addRef(%(%)) - now % refs", ref, ref.name, refs[ref.name].size);
	}

	removeRef {
		|ref|
		var set = refs[ref.name];
		set.remove(ref);

		if (set.notNil and: { set.size == 0 }) {
			this.cleanup(ref.name);
		};

		Log(PNStream).debug("removeRef(%(%)) - now % refs", ref, ref.name, refs[ref.name].size);
	}

	cleanup {
		|name|
		cleanups[name].value();
		cleanups[name] = nil;
		resources[name] = nil;
	}

	embedInStream {
		|inevent|
		var cleanup = EventStreamCleanup.new;
		var inputs = [], outputs = [];
		var target;

		Log(\PNStream).debug("Beginning PNStream:embedInStream");

		// inevent = inevent.copy;
		cleanup.addFunction(inevent, {
			|flag|
			if (flag) {
				Log(PNStream).debug("PNStream, running % remaining cleanups", cleanups.size);

				cleanups.do(_.value);

				if (refs.values.collect(_.size).sum > 0) {
					"Some resources are still referenced by streams".warn;
				};

				cleanups.clear(); resources.clear(); refs.clear();
			}
		});

		loop {
			inevent.keysValuesChange {
				|key, value|
				if (value.isKindOf(PNRef)) {
					if (value.stream.isInput) { inputs = inputs.add(value.stream) };
					if (value.stream.isOutput) { outputs = outputs.add(value.stream) };

					value = this.getResource(value.stream);
					Log(PNStream).debug("Replacing % with resource %", key, value);

					value
				} {
					value
				}
			};

			inevent = inevent.yield;
			inevent ?? {
				cleanup.exit(inevent);
			}
		};
	}
}

PNRef {
	var <>stream;

	*new {
		|stream|
		^super.newCopyArgs(stream)
	}
}

PioProxy : Pattern {
	var <>name, <>numChannels, <>rate;

	embedInStream {
		|inval|
		^PNRefStream()
			.name_(name)
			.numChannels_(numChannels)
			.rate_(rate)
			.createFunc_({ this.create(Server.default) })
			.freeFunc_(this.free(_))
			.isInput_(this.isInput)
			.isOutput_(this.isOutput)
			.embedInStream(inval)
	}

	identityHash { ^[name, numChannels, rate].hash }

	asStream {
		^Routine({ |inval| this.embedInStream(inval) })
	}
}

Pout : PioProxy {
	*new {
		|name=\out, numChannels=2, rate=\audio|
		^super.newCopyArgs(name, numChannels, rate)
	}

	type 			{ \input }
	isInput 		{ ^false }
	isOutput 		{ ^true }

	create 			{ |server| Bus.alloc(rate, server, numChannels) }
	free 			{ |bus| bus.free() }
	toControlInput 	{ |bus| ^bus.asControlInput }

	printOn			{ |stream| stream << "Pout(\\" << name << ")" }
}

Pin : PioProxy {
	*new {
		|name, numChannels=2, rate=\audio|
		^super.newCopyArgs(name, numChannels, rate);
	}

	type 			{ \output }
	isInput 		{ ^true }
	isOutput 		{ ^false }

	create 			{ |server| ^Bus.alloc(rate, server, numChannels) }
	free 			{ |bus| bus.free() }
	toControlInput 	{ |bus| ^bus.asMap }

	printOn			{ |stream| stream << "Pin(\\" << name << ")" }
}

+Pdef {
	*chained {
		|func|
		var envir = Pio.make(func, Pdef.all);
		^Pio(Ppar(envir.values));
	}

	chained {
		|func|
		this.source_(Pdef.chained(func));
	}
}