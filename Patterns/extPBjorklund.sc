// by Juan A. Romero
// Based on the Bjorklund Quark
// gives ratios for durations instead of arrays with binaries

+Pbjorklund  {
	makeSequence { |k, n| ^Bjorklund(k, n) }

	embedInStream {|inval|
		var kStream = k.asStream;
		var nStream = n.asStream;
		var offsetStream = offset.asStream;
		var kVal, nVal, offsetVal;

		length.value(inval).do{
			var outval, bjorklund;

			kVal = kStream.next(inval);
			nVal = nStream.next(inval);
			offsetVal = offsetStream.next(inval);

			if (kVal.notNil and:{nVal.notNil} and:{offsetVal.notNil}) {
				bjorklund = Pseq(this.makeSequence(kVal, nVal), 1, offsetVal).asStream;

				while {outval = bjorklund.next; outval.notNil} {
					inval = outval.yield;
				};
			} {
				inval = nil.yield;
			};
		};
		^inval;
	}
}

+Pbjorklund2 {
	makeSequence { |k, n| ^Bjorklund2(k, n) }
	embedInStream { |inval| ^super.embedInStream(inval) }
}
