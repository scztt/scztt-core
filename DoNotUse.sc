DoNotUse {
	var <obj;
	*new {
		| obj |
		^super.newCopyArgs(obj)
	}

	performBinaryOpOnSomething {
		arg aSelector, thing, adverb;
		this.doesNotUnderstand(aSelector, thing, adverb);
	}

	doesNotUnderstand {
		| selector ... args |
		("DoNotUse:% was called.".format(selector)).warn;
		"Context:".post;
		this.getBackTrace().caller.context.postln;
		^DoNotUse(obj.performList(selector, args));
	}

	printOn {
		| stream |
		stream << "DoNotUse(";
		obj.printOn(stream);
		stream << ")";
	}
}