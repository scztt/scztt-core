+Object {
	unbox {
		|...classes|
		var obj = this;
		while { classes.detect(obj.isKindOf(_)) } {
			obj = obj.value
		};
		^obj
	}
}
