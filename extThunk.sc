+Thunk {
	function { ^function }

	<> {
		|other|

		if (other.isKindOf(Thunk)) {
			other = other.function;
		};

		^Thunk(function <> other)
	}
}