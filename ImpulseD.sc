ImpulseD : UGen {
	*ar {
		|freq, detune=0|
		var clock;
		clock = Phasor.ar(1, freq * SampleDur.ir, 0, inf);
		clock = clock * 2.pow(detune);
		^Changed.ar(clock.ceil);
	}

	*kr {
		|freq, detune=0|
		var clock;
		clock = Phasor.kr(1, freq * ControlDur.ir, 0, inf);
		clock = clock * 2.pow(detune);
		^Changed.kr(clock.ceil);
	}
}