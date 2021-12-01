// When `advance` yields `false`, yield a value from `emptyPat`.
// When `advance` yields `true`, yield all the previously collected values as a tuple.
PBTuple : Pattern {
	var <>sourcePat, <>advance, <>emptyPat, <>dedup, <>keys;

	*new { arg sourcePat, advance=true, emptyPat, dedup=false, keys=1;
		emptyPat = emptyPat ?? { Pseq([\rest], inf) };
		^super.newCopyArgs(sourcePat, advance, emptyPat, dedup, keys)
	}

	embedInStream {  arg inval;
		var sourceStream = sourcePat.asStream;
		var advanceStream = advance.asStream;
		var emptyStream = emptyPat.asStream;
		var value;
		var values = [] ! keys;

		loop {
			var adv = advanceStream.next(inval);

			value = sourceStream.next(inval);

			if (value.notNil) {
				if (keys > 1) {
					keys.do {
						|i|
						values[i] = values[i].addAll(value.wrapAt(i).asArray);
					};
				} {
					values[0] = values[0].addAll(value.asArray);
				}
			} {
				nil.yield;
			};

			if (adv) {
				if (dedup) {
					keys.do {
						|i|
						values[i] = values[i].asSet.asArray;
					}
				};

				if (keys == 1) {
					values = values[0];
				};

				inval = values.yield;
				values = [] ! keys;
			} {
				inval = emptyStream.next(inval).yield;
			}
		};

		^inval;
	}
}

// Yield `emptyPat` values until we have collected values amounting to the next `countPat` value,
// then yield as a tuple.
PBTupleN : Pattern {
	var <>sourcePat, <>countPat, <>emptyPat, <>dedup, <>keys;

	*new { arg sourcePat, countPat, emptyPat, dedup=false, keys=1;
		emptyPat = emptyPat ?? { Pseq([\rest], inf) };
		^super.newCopyArgs(sourcePat, countPat, emptyPat, dedup, keys)
	}

	prToArray {
		|obj|
		if (obj.isKindOf(Array)) {
			^obj
		} {
			^[obj]
		}
	}

	embedInStream {  arg inval;
		var sourceStream = sourcePat.asStream;
		var countStream = countPat.asStream;
		var emptyStream = emptyPat.asStream;

		var foundNil, value, values;
		var count = countStream.next(inval);

		values = [] ! keys;

		while {count.notNil} {
			value = sourceStream.next(inval);

			if (value.notNil) {
				if (keys > 1) {
					keys.do {
						|i|
						values[i] = values[i].addAll(this.prToArray(value.wrapAt(i)));
					};
				} {
					values[0] = values[0].addAll(this.prToArray(value));
				}
			} {
				nil.yield;
			};

			if (values[0].size >= count) {
				if (dedup) {
					keys.do {
						|i|
						values[i] = values[i].asSet.asArray;
					}
				};

				if (keys == 1) {
					values = values[0];
				};


				inval = values.yield;
				values = [] ! keys;
				count = countStream.next(inval);
			} {
				inval = emptyStream.next(inval).yield;
			}
		};

		^inval;
	}
}