PlimitSum : ListPattern {
	var <>limit;

	*new { arg list, limit=16, repeats=inf;
		if (list.isKindOf(Array).not) {
			list = [list]
		};
		^super.new(list, repeats).limit_(limit)
	}

	embedInStream {
		|inval|
		var listStream, value, runningSum, repeatCount;

		listStream = Ppatlace(list, inf).asStream;
		runningSum = 0;
		repeatCount = 0;

		while {
			value = listStream.next(inval);
			value.notNil && (repeatCount < repeats);
		} {
			runningSum = runningSum + value;
			if (runningSum > limit) {
				value = runningSum - limit;
				repeatCount = repeatCount + 1;
				runningSum = 0;
				listStream = Ppatlace(list, inf).asStream;
				value.yield;
			} {
				inval = value.yield
			}
		};
		^inval;
	}
	storeArgs { ^[ list, limit] }
}

+Pattern {
	limitSum {
		|limit=16, tolerance=0.001|
		^Pconst(limit, this, tolerance)
	}
}