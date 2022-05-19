+Bus {
	asMap {
		^mapSymbol ?? {
			if(index.isNil) { MethodError("bus not allocated.", this).throw };
			mapSymbol = numChannels.collect {
				|i|
				"%%".format(
					if(rate == \control) { "c" } { "a" },
					index+i
				).asSymbol
			};
			if (mapSymbol.size == 1) { mapSymbol = mapSymbol[0] };
			mapSymbol;
		}
	}

	realloc {
		|newChannels, newRate, bundle|
		if ((newChannels != numChannels)
			|| (newRate != rate))
		{
			if(index.notNil) {
				this.free;
				rate = newRate;
				numChannels = newChannels;
				this.alloc;
			} {
				rate = newRate;
				numChannels = newChannels;
			}
		}
	}
}

