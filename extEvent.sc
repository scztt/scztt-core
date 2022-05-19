+Event {
	*initClass {
		Class.initClassTree(Require);
		Class.initClassTree(Server);
		Class.initClassTree(TempoClock);
		this.makeParentEvents;

		StartUp.add {
			Event.makeDefaultSynthDef;
		};

		Event.partialEvents.playerEvent[\play] = Require("~/Desktop/Scztt-Core/Event/playFunc");
		Event.parentEvents.default[\play] = Require("~/Desktop/Scztt-Core/Event/playFunc");
		Event.parentEvents.do(_.put(\tempo, { ~bpm !? { ~bpm.value / 120.0 } ?? { 1 } }));
	}

	deltaOrDur_{
		|delta|
		if (this[\delta].notNil) {
			this[\delta] = delta;
		} {
			this[\dur] = delta / (this[\stretch] ? 1)
		}
	}
}

