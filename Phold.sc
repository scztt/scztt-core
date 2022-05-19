Phold : Pattern {
	var pattern, repeats;

	*new {
		|pattern, repeats=inf|
		^super.newCopyArgs(pattern, repeats)
	}

	embedInStream {
		|inval|
		var lastVal;
		var stream = (Pfunc({ |v| lastVal = v }) <> pattern).asStream;

		inval = stream.embedInStream(inval);
		inval = Pseq([lastVal], repeats).asStream.embedInStream(inval);
		^inval
	}
}

+Object {
	hold {
		|repeats=inf|
		^Phold(this, repeats)
	}
}