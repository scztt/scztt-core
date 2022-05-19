+Env {
	midicps {
		var copy;
		copy = this.copy;
		copy.levels = copy.levels.midicps;
		^copy
	}

	degreeToKey {
		|...args|
		var copy;
		copy = this.copy;
		copy.levels = copy.levels.degreeToKey(*args);
		^copy
	}

	performBinaryOp { arg aSelector, theOperand, adverb;
		var copy;
		if (theOperand.isKindOf(Env)) {
			^this.performBinaryOpOnEnv(aSelector, theOperand, adverb)
		} {
			copy = this.copy;
			copy.levels = theOperand.performBinaryOpOnSeqColl(aSelector, copy.levels, adverb);
			^copy
		}
	}

	performBinaryOpOnEnv { arg aSelector, theOperand, adverb;
		var levels, times, curves, copy;

		times = [0] ++ this.times.integrate ++ theOperand.times.integrate;
		times = times.asSet.asArray.sort;

		levels = times.collect {
			|t|
			var a, b;
			a = this.at(t);
			b = theOperand.at(t);
			a.perform(aSelector, b, adverb);
		};
		times = times.differentiate[1..];

		if (this.curves.isNumber && theOperand.curves.isNumber) {
			curves = this.curves.blend(theOperand.curves, 0.5)
		};

		^this.copy.levels_(levels).times_(times).curves_(curves ?? this.curves);
	}

	performBinaryOpOnSimpleNumber { arg aSelector, aNumber, adverb;
		var copy = this.copy;
		copy.levels = copy.levels.performBinaryOpOnSimpleNumber(aSelector, aNumber, adverb);
		^copy
	}

	performBinaryOpOnSomething { arg aSelector, theOperand, adverb;
		^this.performBinaryOp(aSelector, theOperand, adverb);
	}

	+ { arg other, adverb; ^this.performBinaryOp('+', other, adverb) }
	- { arg other, adverb; ^this.performBinaryOp('-', other, adverb) }
	* { arg other, adverb; ^this.performBinaryOp('*', other, adverb) }
	/ { arg other, adverb; ^this.performBinaryOp('/', other, adverb) }

	++ {
		|otherEnv|
		^Env(
			this.levels ++ otherEnv.levels,
			this.times ++ [0] ++ otherEnv.times,
			(
				this.curves.asArray.wrapExtend(this.levels.size)
				++ otherEnv.curves.asArray.wrapExtend(this.levels.size)
			),
			otherEnv.releaseNode !? (_ + this.levels.size) ?? nil,
			otherEnv.loopNode !? (_ + this.levels.size) ?? nil,

		)
	}

	*fromArray {
		|array|
		var originalArray, size, expectedSize, releaseNode, loopNode,
		levelArray, timeArray, shapeArray;

		originalArray = array;
		array = array.reverse;

		levelArray = levelArray.add(array.pop());
		size = array.pop();

		expectedSize = ((size + 1) * 4);
		if (expectedSize.isNumber && (originalArray.size != expectedSize)) {
			Error("Array has incorrect size (expected %, actually %)".format(expectedSize, originalArray.size)).throw;
		};

		releaseNode = array.pop();
		if (releaseNode == -99) { releaseNode = nil };

		loopNode = array.pop();
		if (loopNode == -99) { loopNode = nil };

		while { array.notEmpty() } {
			var shapeIndex, curve;

			levelArray = levelArray.add(array.pop());
			timeArray = timeArray.add(array.pop());

			shapeIndex = array.pop();
			curve = array.pop();

			if (shapeIndex == 5) { // hard-coded to be a numeric curve
				shapeArray = shapeArray.add(curve)
			} {
				shapeArray = shapeArray.add(Env.shapeNames.findKeyForValue(shapeIndex) ?? 0)
			};
		};

		^Env(levelArray, timeArray, shapeArray, releaseNode, loopNode);
	}

	padTo {
		|size|
		levels = levels ++ (0 ! (1 + size - levels.size));
		times = times ++ (0 ! (size - times.size));
	}

	reverse {
		levels = levels.reverse;
		times = times.reverse;
		curves.isArray.if {
			curves = curves.reverse
		};

	}

	linlin { arg inMin, inMax, outMin, outMax, clip=\minmax;
		^this.copy.levels_(this.levels.linlin(inMin, inMax, outMin, outMax, clip));
	}
	linexp { arg inMin, inMax, outMin, outMax, clip=\minmax;
		^this.copy.levels_(this.levels.linexp(inMin, inMax, outMin, outMax, clip));
	}
	explin { arg inMin, inMax, outMin, outMax, clip=\minmax;
		^this.copy.levels_(this.levels.explin(inMin, inMax, outMin, outMax, clip));
	}
	expexp { arg inMin, inMax, outMin, outMax, clip=\minmax;
		^this.copy.levels_(this.levels.expexp(inMin, inMax, outMin, outMax, clip));
	}
	lincurve { arg inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip = \minmax;
		^this.copy.levels_(this.levels.lincurve(inMin, inMax, outMin, outMax, curve, clip));
	}
	curvelin { arg inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip = \minmax;
		^this.copy.levels_(this.levels.curvelin(inMin, inMax, outMin, outMax, curve, clip));
	}
	bilin { arg inCenter, inMin, inMax, outCenter, outMin, outMax, clip=\minmax;
		^this.copy.levels_(this.levels.bilin(inCenter, inMin, inMax, outCenter, outMin, outMax, clip));
	}
	biexp { arg inCenter, inMin, inMax, outCenter, outMin, outMax, clip=\minmax;
		^this.copy.levels_(this.levels.biexp(inCenter, inMin, inMax, outCenter, outMin, outMax, clip));
	}
}

+String {
	isValidUGenInput { ^this.asSymbol.isMap }
}

+Symbol {
	isValidUGenInput { ^this.isMap }
}