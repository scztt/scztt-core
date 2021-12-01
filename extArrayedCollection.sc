+ArrayedCollection {
	playAndDelta {
		|cleanup, mute|
		var maxDelta = 0;

		if (this.isEmpty) {
			^nil
		};

		this.do {
			|item|
			maxDelta = max(0,
				item.playAndDelta(cleanup, mute)
			);
		};

		^maxDelta;
	}
}