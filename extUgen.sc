+UGen {
	fadeChange {
		|func, fadeTime=1, fadeClass=(XFade2)|
		^[this].fadeChange(func, fadeTime, fadeClass)
	}
	fadeTrig {
		|func, fadeTime=1, fadeClass=(XFade2)|
		^[this].fadeTrig(func, fadeTime, fadeClass)
	}
	fadeSteps {
		|func, stepSize=1, fadeClass=(LinXFade2), warp=0|
		^[this].fadeSteps(func, stepSize, fadeClass, warp)
	}
}


+ArrayedCollection {
	fadeChange {
		|func, fadeTime=1, fadeClass=(XFade2)|
		var trig, a, b, sig;
		var meth = this.detect({ |s| s.rate == \audio }).notNil.if(\ar, \kr);

		trig = Trig.perform(meth, this.sum, fadeTime);
		trig = ToggleFF.perform(meth, trig);

		a = Latch.perform(meth, this, 1 - trig);
		b = Latch.perform(meth, this, trig);
		a = func.value(*a);
		b = func.value(*b);

		^fadeClass.perform(a.rate.switch(\audio, \ar, \kr),
			a,
			b,
			Delay1.perform(meth, Slew.perform(meth, trig, 1 / fadeTime, 1 / fadeTime)).linlin(0, 1, -1, 1)
		);
	}

	fadeTrig {
		|func, fadeTime=1, fadeClass=(XFade2), values|
		var trig, a, b, sig;
		var meth = this.detect({ |s| s.rate == \audio }).notNil.if(\ar, \kr);

		trig = Trig.perform(meth, this.sum, fadeTime);
		trig = ToggleFF.perform(meth, trig);

		a = Latch.perform(meth, this, 1 - trig);
		b = Latch.perform(meth, this, trig);
		a = func.value(*a);
		b = func.value(*b);

		^fadeClass.perform(a.rate.switch(\audio, \ar, \kr),
			a,
			b,
			Delay1.perform(meth, Slew.perform(meth, trig, 1 / fadeTime, 1 / fadeTime)).linlin(0, 1, -1, 1)
		);
	}

	fadeSteps {
		|func, stepSize=1, fadeClass=(LinXFade2), warp=0|
		var changed, a, b, sig, input, inputA, inputB, fade, isEvenStep;

		input = this / stepSize;
		inputB = input.ceil;
		inputA = input.floor;

		isEvenStep = inputA % 2;
		#inputA, inputB = Select.kr(isEvenStep, [
			[inputB, inputA],
			[inputA, inputB]
		]);

		fade = (input - inputA).abs;
		fade = ControlSpec(0, 1, warp).map(fade);

		a = func.(stepSize * inputA);
		b = func.(stepSize * inputB);

		^fadeClass.perform(a.rate.switch(\audio, \ar, \kr),
			a,
			b,
			(fade * 2 - 1)
		);
	}
}

