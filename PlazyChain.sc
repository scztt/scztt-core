PlazyChain : Pattern {
	var <>func, <>chained, <>nextCondition;

	*new { arg inFunc, chained, nextCondition;
		var func;
		if (inFunc.isKindOf(Function).not) {
			func = {
				|inval|
				inFunc <> inval;
			}
		} {
			func = inFunc;
		};

		^super.new
			.func_(func)
			.chained_(chained)
			.nextCondition_(nextCondition)
	}

	embedInStream { arg inval;
		var chainInval;
		var chainedStream = chained.asStream;
		var funcStream = Pfunc(func).asStream;
		var nextCondition = nextCondition !? _.asPattern;

		loop {
			chainInval = chainedStream.next(inval);
			if (chainInval.isNil) {
				^nil.yield
			};

			inval = funcStream.next(chainInval).postln !? {
				|v|
				if (nextCondition.notNil) {
					Pwhile(nextCondition.asStream, v.asStream).embedInStream(inval)
				} {
					v.embedInStream(inval)
				}
			} ?? { ^nil.yield };
		}
	}

	storeArgs { ^[func, chained] }
}

+Function {
	<*> { arg aPattern, adverb;
		^PlazyChain(
			adverb !? {
				{
					|inval|
					this.value(inval).pwhile(adverb)
				}
			} ?? {
				this
			},
			aPattern
		)
	}
}

+Pattern {
	<*> { arg aPattern, adverb ...args;
		^PlazyChain(
			adverb !? {
				{
					|inval|
					this.pwhile(adverb)
				}
			} ?? {
				this
			},
			aPattern
		)
	}
}

+Stream {
	<*> { arg aPattern, adverb ...args;
		^PlazyChain(
			adverb !? {
				{
					|inval|
					this.pwhile(adverb)
				}
			} ?? {
				this
			},
			aPattern
		)
	}
}

+Object {
	pwhile {
		|condition|
		^this.asPattern.pwhile(condition)
	}
}

+Pattern {
	pwhile {
		|condition|

		"condition is: %".format(condition).postln;
		"condition returns: %".format(condition.value(())).postln;
		"condition.asPattern returns: %".format(condition.asPattern.asStream.value(())).postln;

		^switch (condition,
			_.isSymbol, {
				Pif(Pkey(condition), this)
			},
			{
				// Pwhile((_.asBoolean <> condition).asPattern, this)
				Pif(condition.asPattern, this)
			}
		)
	}
}

+Stream {
	pwhile {
		|condition|

		^switch (condition,
			_.isSymbol, {
				Pif(Pkey(condition), this)
			},
			_.isFunction, {
				Pif(Pfunc(condition), this)
			},
			{
				Pif(condition, this)
			}
		)
	}
}
