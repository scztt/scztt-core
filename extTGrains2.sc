+TGrains2 {
	*calcDelay {
		|pos, delay, dur, rate, attack, decay|
		var halfGrain, scaledPos = pos, grainPeak;

		grainPeak = (attack + (dur - decay)) / 2;
		grainPeak = grainPeak - (0.5 * dur);
		grainPeak = grainPeak * (1 - rate);
		grainPeak = grainPeak * SampleRate.ir;
		halfGrain = (0.5 * dur * SampleRate.ir) + grainPeak;

		scaledPos = scaledPos - max(0, delay * SampleRate.ir);

		scaledPos = min(scaledPos, pos - (halfGrain * (1 - rate)).abs);
		scaledPos = scaledPos + halfGrain;
		scaledPos = scaledPos / SampleRate.ir;
		^scaledPos;
	}
}

+TGrains3 {
	*calcDelay {
		|...args|
		^TGrains2.calcDelay(*args)
	}
}
