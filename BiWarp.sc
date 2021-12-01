BiWarp : Warp {
	var specA, specB, <>curve, <>midpoint, <>inputMidpoint;

	*new {
		|curve=0, midpoint=0, spec=(ControlSpec(-1, 1))|
		^(super.new
			.curve_(curve)
			.midpoint_(midpoint)
			.spec_(spec)
		)
	}

	asWarp {
		|spec|
		^this.class.new(curve, midpoint, spec)
	}

	spec_{
		|inSpec|
		spec = inSpec;
		inputMidpoint = 0.5;

		specA = ControlSpec(spec.minval, midpoint, curve.neg);
		specB = ControlSpec(midpoint, spec.maxval, curve);
	}

	map {
		|input|
		if (input < inputMidpoint) {
			^specA.map(input / inputMidpoint)
		} {
			^specB.map((input - inputMidpoint) / (1 - inputMidpoint))
		}
	}

	unmap {
		|value|
		if (value < midpoint) {
			^specA.unmap(value) * inputMidpoint
		} {
			^(inputMidpoint + (specB.unmap(value) * (1 - inputMidpoint)))
		}
	}
}