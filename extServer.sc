+SynthDef {
	*newPartial { arg name, ugenGraphFunc, rates, prependArgs, variants, metadata;
		^super.newCopyArgs(name.asSymbol).variants_(variants).metadata_(metadata ?? {()}).children_(Array.new(64))
			.partialBuild(ugenGraphFunc, rates, prependArgs)
	}

	partialBuild { arg ugenGraphFunc, rates, prependArgs;
		protect {
			this.initBuild;
			this.buildUgenGraph(ugenGraphFunc, rates, prependArgs);
			//this.finishBuild;
			func = ugenGraphFunc;
		} {
			UGen.buildSynthDef = nil;
		}
	}

	partialFinishBuild {
		this.addCopiesIfNeeded;
		// this.optimizeGraph;
		this.collectConstants;
		this.checkInputs;// will die on error

		// re-sort graph. reindex.
		this.topologicalSort;
		this.indexUGens;
		UGen.buildSynthDef = nil;
	}

}