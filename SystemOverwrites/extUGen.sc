+UGen {
	lincurve { arg inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip = \minmax;
		var grow, a, b, scaled, curvedResult;
		curve = (curve.abs < 0.00001).if(0.00001, curve);
		grow = exp(curve);
		a = outMax - outMin / (1.0 - grow);
		b = outMin + a;
		scaled = (this.prune(inMin, inMax, clip) - inMin) / (inMax - inMin);

		curvedResult = b - (a * pow(grow, scaled));
		^curvedResult
	}

	curvelin { arg inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip = \minmax;
		var grow, a, b, scaled, linResult;
		curve = curve.abs.max(0.00001) * curve.sign;
		grow = exp(curve);
		a = inMax - inMin / (1.0 - grow);
		b = inMin + a;

		linResult = log( (b - this.prune(inMin, inMax, clip)) / a ) * (outMax - outMin) / curve + outMin;
		^linResult
	}

	prune { arg min, max, type;
		switch(type,
			\minmax, {
				^this.clip(min, max);
			},
			\min, {
				^this.max(min);
			},
			\max, {
				^this.min(max);
			},
			\none, {
				^this
			}
		);
		^this
	}
}
