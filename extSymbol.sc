+String {
	f {
		|...args|
		^this.format(*args)
	}
}

+Symbol {
	format {
		|...args|
		^this.asString.format(*args).asSymbol
	}

	f {
		|...args|
		^this.format(*args)
	}

	asBusIndex {
		if (this.isMap) {
			^this.asString[2..].asInteger
		} {
			^nil
		}
	}
}
