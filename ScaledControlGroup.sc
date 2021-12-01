ScaledControlValueGroup {
	var scaleFunc, <curve, connections, updating=false;

	*new {
		|cvs, curve=(-6)|
		var obj = super.new.init.curve_(curve);
		cvs.do(obj.add(_));
		^obj;
	}

	init {
		connections = ();
	}

	curve_{
		|inCurve|
		curve = inCurve;
		scaleFunc = {
			|input, others|
			var otherSum, otherScalar, pow, invPow;

			pow = curve.neg.dbamp;
			invPow = curve.dbamp;

			otherSum = others.collect(_.input).pow(pow).sum.pow(invPow);

			if (otherSum > 0) {

				otherScalar = (1.0 - input.pow(pow)).pow(invPow) / otherSum;
				others.do({
					|o|
					o.input = o.input * otherScalar;
				});
			}

		};
	}

	add {
		|cv|
		var others;

		connections[cv] = cv.signal(\input).connectToUnique(\ScaledControlValueGroup, {
			|obj, what, input|
			if (updating.not) {
				protect {
					updating = true;

					others = connections.keys.selectAs({ |other| other != cv }, Array);
					scaleFunc.value(input, others)
				} {
					updating = false;
				}
			}
		});
	}

	remove {
		|cv|
		connections[cv] !? (_.free);
	}

	free {
		connections.keys.do(this.remove(_));
	}
}
