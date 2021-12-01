DistCurve : Singleton {
	var <>server, <descriptions, <buffers, <size=2000, <mirror = false, synced = false, log;

	init {
		server = Server.default;
		server.onBootAdd(this);
		server.onQuitAdd(this);
		buffers = List();
		log = Log(\DistCurve);
	}

	set {
		arg ...inDescriptions;
		descriptions = inDescriptions;
		log.info("Setting: %", descriptions);
		log.info("Current buffers: %", buffers);
		this.updateBuffers();
	}

	doOnServerBoot {
		if (buffers.size() > 0) {
			log.warning("Uncleared buffers detected on server boot! These are invalid - why are they here?");
			this.clearBuffers();
		};

		this.updateBuffers();
	}

	doOnServerQuit {
		synced = false;
		this.clearBuffers();
	}

	clearBuffers {
		if (server.serverRunning) {
			buffers.do {
				| buf |
				buf.free;
			}
		};
		buffers = List();
	}

	size_{
		arg inSize;
		size = inSize;
		this.clearBuffers();
		this.updateBuffers();
	}

	mirror_{
		arg inMirror;
		mirror = inMirror;
		this.updateBuffers();
	}

	numBuffers_{
		arg num;
		var sizeDiff = buffers.size() - num;

		if (sizeDiff > 0) {
			log.info("Removing % unused buffers ([%..]).", sizeDiff, num);
			buffers[num..].do(_.free);
			buffers = buffers[0..(num - 1)];
		};

		if (sizeDiff < 0) {
			log.info("Preparing to allocate % new buffers.", sizeDiff.neg);
			sizeDiff.neg.do {
				buffers.add(nil);
			}
		};
	}

	updateBuffers {
		if (server.serverRunning) {
			var buf, unusedBuffers;
			synced = false;

			this.numBuffers = descriptions.size;
			descriptions.do {
				| desc, i |
				if (buffers[i].notNil) {
					buffers[i].sendCollection(this.toWavetable(desc));
					log.info("Refilled buffer % with %", buffers[i]);
				} {
					buffers[i] = Buffer.sendCollection(server, this.toWavetable(desc));
					log.info("Created new buffer %", buffers[i]);
				}
			};

			// Set the flag when the server has finished processing messages.
			fork {
				server.sync();
				synced = true;
			};
		}
	}

	plot {
		this.asArrays.plot();
	}

	asWavetables {
		^descriptions.collect(this.toWavetable(_, size));
	}

	asArrays {
		^descriptions.collect(this.toArray(_, size));
	}

	ar {
		arg in, position, pre, post;
		position = position ?? { DC.kr(0) };

//		if (synced.not) { "Buffers have not yet been synced to the server! You can't call ar yet.".throw };

		if (descriptions.size > 1) {
			^this.xfadeAr(in, position, pre, post);
		} {
			^this.buildShaper(buffers[0], in, pre, post);
		}
	}

	xfadeAr {
		arg in, position, pre, post;
		var sigs = descriptions.collect({
			| desc, i |
			if (desc.isKindOf(Symbol)) {
				this.buildDynamicShaper(desc, in, pre, post);
			} {
				this.buildShaper(buffers[i], in, pre, post);
			}
		});

		^LinSelectX.ar(position * (sigs.size - 1), sigs);
	}

	buildDynamicShaper {
		arg name, in, pre, post;
		var sig;

		sig = in;
		if (pre.notNil) { sig = sig * pre.dbamp };

		switch (name,
			\distort, {
				sig = sig.distort;
			},
			\softclip, {
				sig = sig.softclip;
			},
			\scurve, {
				sig = sig.abs.scurve * sig.sign
			},
			\tanh, {
				sig = sig.tanh;
			},
			{ "Cannot find shape: %".format(name).throw }
		);

		if (post.notNil) { sig = sig * post.dbamp };
		^sig;
	}

	buildShaper {
		arg buffer, in, pre, post;
		var sig;
		sig = in;
		if (pre.notNil) { sig = sig * pre.dbamp };
		sig = Shaper.ar(buffer.bufnum, sig);
		if (post.notNil) { sig = sig * post.dbamp };
		^sig;
	}

	toWavetable {
		arg desc, size;
		^Signal.newFrom(this.toArray(desc, size)).asWavetableNoWrap;
	}

	toArray {
		arg desc;
		var data, targetSize;

		if (mirror) {
			targetSize = (size / 2).asInteger;
		} {
			targetSize = size + 1
		};

		if (desc.class == Symbol)							{ data = this.toWavetableSymbol(desc, targetSize) };
		if (desc.class == Function) 						{ data = this.toWavetableFunction(desc, targetSize) };
		if (desc.class == Env) 								{ data = this.toWavetableEnv(desc, targetSize) };
		if ((desc.class != Symbol)
			&& desc.isKindOf(Collection) && (desc.size >= 2)) 	{ data = this.toWavetableCollection(desc, targetSize) };

		if (mirror) {
			data = (0 - data.reverse) ++ [0] ++ data
		};
		^data;
	}

	toWavetableFunction {
		arg desc, size;
		^size.collect({
			|n|
			desc.value(n / size * 2 - 1);
		});
	}

	toWavetableEnv {
		arg desc, size;
		desc = desc.deepCopy();
		desc.duration = size;
		^size.collect(desc.at(_));
	}

	toWavetableCollection {
		arg desc, size;
		^desc.resamp1(size);
	}

	toWavetableSymbol {
		arg desc, size;
		^size.collect({
			|n|
			n = n / size * 2.0 - 1.0;
			switch(desc,
				\distort, {
					n.distort;
				},
				\softclip, {
					n.softclip;
				},
				\scurve, {
					n.abs.scurve * n.sign
				},
				\tanh, {
					n.tanh;
				},
				{ "Cannot find shape: %".format(name).throw }
			);
		})
	}

	onClear {
		this.clearBuffers();
		descriptions = [];
	}
}