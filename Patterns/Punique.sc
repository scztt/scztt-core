// Do not yield two identical values in a row.
Punique : FilterPattern {
	var <>isEqualFunc;

	*new { arg pattern, isEqualFunc;
		isEqualFunc = isEqualFunc ?? {{ |a, b| a == b }};
		^super.newCopyArgs(pattern, isEqualFunc)
	}

	storeArgs { ^[pattern, isEqualFunc] }

	embedInStream {
		|inval|
		var outVal, isEqual, lastVal, patternStream;

		patternStream = pattern.asStream;
		loop {
			outVal = patternStream.next(inval);
			if (outVal.isNil) {
				^inval
			};

			isEqual = isEqualFunc.value(lastVal, outVal);
			if (isEqual.not) {
				lastVal = outVal;
				inval = outVal.yield;
			}
		}
	}
}

+Pattern {
	dedup {
		|func|
		^Punique(this, func)
	}

	unique {
		|func|
		^Punique(this, func)
	}
}