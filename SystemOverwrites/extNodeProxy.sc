NodeProxyArgDict {
	classvar all;
	var parentNodeProxy, dict;

	*initClass {
		all = IdentityDictionary();
	}

	*new {
		|np|
		^all[np] ?? {
			this.prNew(np);
		}
	}

	*prNew {
		|np|
		var new;
		new = super.newCopyArgs(np).init;
		all[np] = new;

		^new;
	}

	init {
		dict = IdentityDictionary();
	}

	prMakeName {
		|key|
		^(parentNodeProxy.key.asString ++ "_" ++ key.asString).asSymbol
	}

	prMap {
		|key, np|
		parentNodeProxy.map(key, np);
		// parentNodeProxy <<>.(key) np
	}

	prControlRateCount {
		|key|
		var cnames, cname;
		cnames = parentNodeProxy.objects.collect(_.controlNames).flatten.reject(_.isNil);
		cnames = cnames.select({ |c| c.name == key });
		if (cnames.size > 1) { "Multiple matching control names - using the smallest channel count to prevent overflow".warn };
		cname = cnames.maxItem(_.numChannels);
		^(cname !? [cname.rate, cname.numChannels] ?? [nil, 1]);
	}

	at {
		|key|
		var np, rate, channels;
		^dict[key] ?? {
			#rate, channels = this.prControlRateCount(key);

			np = Ndef(this.prMakeName(key));
			np.mold(channels, rate, \elastic);
			np.fadeTime = parentNodeProxy.fadeTime;

			dict[key] = np;
			rate !? { this.prMap(key, np) };

			np;
		};
	}

	put {
		|key, obj|
		var np, rate, channels;

		if (obj.notNil) {
			if (obj.isKindOf(NodeProxy)) {
				if (dict[key] !? { |o| o.key == this.prMakeName(key) } ?? { false }) { dict[key].clear };
				dict[key] = obj;
				this.prMap(key, obj);
			} {
				np = this.at(key);
				#rate, channels = this.prControlRateCount(key);
				np.source = obj;

				this.prMap(key, np);
			};

			fork {
				parentNodeProxy.server.sync;
				parentNodeProxy.orderNodes(np);
			};
		} {
			dict[key].clear;
			dict[key] = nil;
			this.prMap(key, nil);
		};
	}
}

+NodeProxy {
	args {
		^NodeProxyArgDict(this);
	}
}
