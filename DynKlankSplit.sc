DynKlankSplit : UGen {

	*ar { arg specificationsArrayRef, input, freqscale = 1.0, freqoffset = 0.0, decayscale = 1.0;
		^this.multiNew(\audio, specificationsArrayRef, input, freqscale, freqoffset, decayscale)
	}

	*kr { arg specificationsArrayRef, input, freqscale = 1.0, freqoffset = 0.0, decayscale = 1.0;
		^this.multiNew(\control, specificationsArrayRef, input, freqscale, freqoffset, decayscale)
	}

	*new1 { arg rate, specificationsArrayRef, input, freqscale = 1.0, freqoffset = 0.0, decayscale = 1.0;
		var spec = specificationsArrayRef.value;
		var selector = this.methodSelectorForRate(rate);
		^Ringz.perform(selector,
				input,
				spec[0] ? #[440.0] * freqscale + freqoffset,
				spec[2] ? #[1.0] * decayscale,
				spec[1] ? #[1.0]
		)
	}
}

