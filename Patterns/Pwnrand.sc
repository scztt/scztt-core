Pwnrand : ListPattern {
	var <>weights;

	*new { arg list, weights, repeats=1;
		^super.new(list, repeats).weights_(weights)
	}

	embedInStream {  arg inval;
		var item, weightsVal, repeatStream;
		var weightsStream = Ptuple(weights).asStream;
		repeatStream = repeats.asStream;
		repeatStream.next(inval).do({ arg i;
			weightsVal = weightsStream.next(inval);
			if(weightsVal.isNil) { ^inval };
			weightsVal = weightsVal.extend(list.size, 0);
			weightsVal = weightsVal.normalizeSum;
			item = list.at(weightsVal.windex);
			inval = item.embedInStream(inval);
		});
		^inval
	}
	storeArgs { ^[ list, weights, repeats ] }
}