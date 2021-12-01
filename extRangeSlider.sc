+ RangeSlider {
	center_{
		| center |
		var deviation = (this.hi - this.lo) / 2;
		this.setDeviation(deviation, center)
	}

	deviation_{
		| deviation |
		var center = (this.hi + this.lo) / 2;
		this.setDeviation(deviation, center)
	}
}