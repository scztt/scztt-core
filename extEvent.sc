+Event {
	deltaOrDur_{
		|delta|
		if (this[\delta].notNil) {
			this[\delta] = delta;
		} {
			this[\dur] = delta / (this[\stretch] ? 1)
		}
	}
}

