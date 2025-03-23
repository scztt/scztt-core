+Event {
    *initClass {
        Class.initClassTree(Require);
        Class.initClassTree(Server);
        Class.initClassTree(TempoClock);
        this.makeParentEvents;
        
        StartUp.add {
            Event.makeDefaultSynthDef;
        };
        
        Require.with("~/Desktop/Scztt-Core/Event/playFunc") {
            |playFunc|
            Event.partialEvents.playerEvent[\play] = playFunc;
            Event.parentEvents.default[\play] = playFunc;
        };
        
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

