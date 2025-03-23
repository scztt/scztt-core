Pmonophonic : Pattern {
    *initClass {
        Class.initClassTree(Event);
        Event.composeEventType(
            \monoUpdate,
            composeType: \set,
            parentEvent: (updateGroup:false),
            func: {
                |server, set|
                var bundle;
                
                if (~updateGroup.value) {
                    bundle = [
                        62 /* \n_order */, 
                        Node.actionNumberFor(~addAction),
                        ~group.value,
                    ] ++ ~id;
                    bundle = bundle.asOSCArgArray;
                    ~schedBundleArray.(~lag, ~timingOffset, server, bundle, ~latency);
                };
                
                set.();
            }
        )
    }
    
    embedInStream {
        |inEvent|
        var id, offEvent;
        var event;
        var cleanup = EventStreamCleanup();
        var group, addAction;
        var synthDefHash, newSynthDefHash, monoId, newMonoId;
        
        cleanup.addFunction(event, {
            "mono cleanup".postln;
            offEvent.do(_.play)
        });
        
        while {inEvent.notNil} {
            offEvent !? { offEvent.do(_.play); offEvent = nil };
            
            event = inEvent.copy;
            
            event.putAll((
                gate: 1,
                sendGate: false,
                callback: event[\callback].addFunc({
                    id = ~id.debug("starting mono");
                    group = ~group.value;
                    addAction = ~addAction.value;
                    offEvent = offEvent.add((
                        type:               \off,
                        id:                 ~id, 
                        server:             ~server, 
                        hasGate:            ~hasGate,
                        schedBundleArray:   ~schedBundleArray,
                        schedBundle:        ~schedBundle,
                        callback:           { "mono off: %".format(~id).postln; },
                    ));
                })
            ));  
            
            synthDefHash = event[\instrument].asSynthDesc.identityHash;
            monoId = event.use{ ~monoId.value };
            
            while {
                inEvent = event.yield;
                inEvent.notNil 
                    and: {
                        newSynthDefHash = event[\instrument].asSynthDesc.identityHash;
                        newSynthDefHash == synthDefHash
                    } 
                    and: {
                        newMonoId = event.use{ ~monoId.value };
                        newMonoId == monoId;
                    }
            } {
                synthDefHash = newSynthDefHash;
                monoId = newMonoId;
                
                event = inEvent.copy;
                cleanup.update(event);
                
                event.putAll((
                    type:   \monoUpdate,
                    id:     id,
                    updateGroup: {
                        ~group = ~group.value;
                        ~addAction = ~addAction.value;
                        
                        (group.asGroup != ~group.asGroup) 
                        or: { addAction != ~addAction }
                    },
                    gate:   1,
                    args:   event.keys.includes(\args).if({ event[\args] }, {[]})
                ));                
            };
        };
        
        ^cleanup.exit(inEvent);
    }
}

+Pbind {
    *mono { 
        |...pairs, kwpairs|
        ^Pmonophonic() <> Pbind.performArgs(\new, pairs, kwpairs)
    }
    
    mono {
        ^Pmonophonic() <> this
    }
}

+Env {
    *xfade {
        |time = 1|
        ^Env([0, 1, 0], [time, time], releaseNode: 1)
    }
}








