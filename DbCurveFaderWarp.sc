MetaCurveWarp {
	var <originalWarp, <curve;

	*new { arg warp, curve=0;
		^super.newCopyArgs(warp, ControlSpec(0, 1, curve))
	}

	*asWarp {
		arg spec;
		^this.new(spec)
	}

	asWarp {
		|spec|
		var new = this.deepCopy;
		new.spec = spec;
		^new
	}

	spec {
		^originalWarp.spec
	}

	spec_{
		|spec|
		originalWarp.spec = spec;
	}

	asSpecifier {

	}

	map {
		arg value;
		^originalWarp.map(curve.map(value));
	}
	unmap {
		arg value;
		value = originalWarp.unmap(value);
		^curve.unmap(value)
	}
}

+ControlSpec {
	curved {
		|curve=4|
		var newSpec = this.deepCopy;
		newSpec.warp = MetaCurveWarp(warp, curve);
		^newSpec
	}
}