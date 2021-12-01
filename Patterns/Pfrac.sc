// Fractally subdivide `list`, choosing location according to `index`.
Pfrac : Pattern {
	var <>list, <>index, repeats;

	*new { arg list, index, repeats=inf;
		^super.newCopyArgs(list, index ?? { Pseries() }, repeats)
	}

	prIndexToStreamIndex {
		|i|
		var n, r;

		n = list.size-1;

		r = i % 2;
		if (n > 1) {
			(n..2).do {
				|depth|
				r = r + (i % 2.pow(depth).asInteger).min(1);
			};
		};

		^r
	}

	embedInStream { |inval|
		var	streamNum, next, streamList, indexStream, count;

		streamList = list.collect(_.asStream);
		indexStream = index.asStream;

		count = 0;

		while {
			next = nil;
			indexStream.next(inval) !? {
				|i|
				streamNum = this.prIndexToStreamIndex(i);
				next = streamList[streamNum].next(inval)
			};
			(next.notNil && (count < repeats))
		} {
			inval = next.yield;
			count = count + 1;
		}

		^inval;
	}
}

// (
// n = 4;
// ~f = {
// 	|i, depth|
// 	if (depth > 1) {
// 		~f.(i, depth-1) + (i % 2.pow(depth).asInteger).min(1)
// 	} {
// 		i % 2
// 	}
// };
//
// (2.pow(n).asInteger).collect {
// 	|i|
// 	var r;
// 	r
// }
// )
//
