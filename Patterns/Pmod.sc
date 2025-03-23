Pmod : Pattern {
    classvar defHashLRU, <defCache, <defNames, <defNamesFree, defCount=0, maxDefNames=100;
    classvar <synthGroup;
    
    var <>synthName, <>patternPairs, <rate, <>channels, asValues=false, unwrapValues=false,
        shouldExpand=false;
    
    *new {
        |synthName ...pairs, kwpairs|
        ^super.newCopyArgs(synthName, pairs)
    }
    
    *kr {
        |synthName ...pairs, kwpairs|
        ^this.performArgs(\new, synthName, pairs, kwpairs).rate_(\control)
    }
    
    *kr1 {
        |synthName ...pairs, kwpairs|
        ^this.performArgs(\new, synthName, pairs, kwpairs).rate_(\control).channels_(1)
    }
    
    *kr2 {
        |synthName ...pairs, kwpairs|
        ^this.performArgs(\new, synthName, pairs, kwpairs).rate_(\control).channels_(2)
    }
    
    *kr3 {
        |synthName ...pairs, kwpairs|
        ^this.performArgs(\new, synthName, pairs, kwpairs).rate_(\control).channels_(3)
    }
    
    *kr4 {
        |synthName ...pairs, kwpairs|
        ^this.performArgs(\new, synthName, pairs, kwpairs).rate_(\control).channels_(4)
    }
    
    *ar {
        |synthName ...pairs, kwpairs|
        ^this.performArgs(\new, synthName, pairs, kwpairs).rate_(\audio)
    }
    
    *ar1 {
        |synthName ...pairs, kwpairs|
        ^this.performArgs(\new, synthName, pairs, kwpairs).rate_(\audio).channels_(1)
    }
    
    *ar2 {
        |synthName ...pairs, kwpairs|
        ^this.performArgs(\new, synthName, pairs, kwpairs).rate_(\audio).channels_(2)
    }
    
    *ar3 {
        |synthName ...pairs, kwpairs|
        ^this.performArgs(\new, synthName, pairs, kwpairs).rate_(\audio).channels_(3)
    }
    
    *ar4 {
        |synthName ...pairs, kwpairs|
        ^this.performArgs(\new, synthName, pairs, kwpairs).rate_(\audio).channels_(4)
    }
    
    *initClass {
        defCache = ();
        defNames = ();
        defHashLRU = LinkedList();
        defNamesFree = IdentitySet();
        (1..16).do {
            |n|
            [\kr, \ar].do {
                |rate|
                this.wrapSynth(
                    rate: rate,
                    func: { \value.perform(rate, (0 ! n)) },
                    channels: n,
                    defName: "Pmod_constant_%_%".format(n, rate).asSymbol,
                );
            }
        };
        
        this.wrapSynth(
            rate: \ar,
            channels: 1,
            defName: \pmodEnvAr,
            func: {
                var env, curve, mod;
                
                env = \env.kr(Env([0, 1, 0] ++ (0 ! 15), [0.5, 0.5] ++ (0 ! 15)).asArray);
                env = EnvGen.ar(
                    envelope:	env,
                    gate:		1,
                );
                
                mod = \mod.ar(0);
                
                env + mod;
            }
        );
        
        this.wrapSynth(
            rate: \kr,
            channels: 1,
            defName: \pmodEnvKr,
            func: {
                var env, curve, mod;
                
                env = \env.kr(Env([0, 1, 0] ++ (0 ! 15), [0.5, 0.5] ++ (0 ! 15)).asArray);
                env = EnvGen.kr(
                    envelope:	env,
                    gate:		1,
                );
                
                mod = \mod.kr(0);
                
                env + mod;
            }
        )
    }
    
    *env {
        |...args, kwargs|
        ^this.performArgs(\envAr, args, kwargs);
    }
    
    *envAr {
        |...args, kwargs|
        ^Pmod(
            \pmodEnvAr,
            \resend, true,
            *this.prFixEnvArg(args ++ kwargs)
        )
    }
    
    *envKr {
        |...args, kwargs|
        ^Pmod(
            \pmodEnvKr,
            \resend, true,
            *this.prFixEnvArg(args ++ kwargs)
        )
    }
    
    *prFixEnvArg {
        |args|
        // Replace a stream of envs with a stream of functions that optionally STRETCH
        // that env based on ~stretchEnv.
        args.pairsDo {
            |key, value, i|
            if (key == \env) {
                args[i + 1] = Pfunc({
                    |env|
                    env.releaseNode = env.levels.size - 1;
                    
                    {
                        if (~stretchEnv.asBoolean) {
                            env = env.copy;
                            env.duration = ~parentEvent.use { ~dur.value };
                        };
                        env;
                    }
                }) <> value
            }
        };
        ^args
    }
    
    // Wrap a func in fade envelope / provide XOut
    *wrapSynth {
        |defName, func, rate, channels|
        var hash, def, args;
        var added = Deferred();
        
        defName = defName ?? {
            hash = [func, rate, channels].hash;
            defHashLRU.remove(hash);
            defHashLRU.addFirst(hash);
            defNames[hash] ?? {
                defNames[hash] = this.getDefName();
                defNames[hash]
            };
        };
        
        if (defCache[defName].isNil) {
            // "No cache for %, rebuilding".format(defName).postln;
            def = SynthDef(defName, {
                var fadeTime, paramLag, fade, sig;
                
                fadeTime = \fadeTime.kr(0);
                paramLag = \paramLag.ir(0);
                
                fade = Env([1, 1, 0], [0, fadeTime], releaseNode:1).kr(
                    gate:\gate.kr(1),
                    doneAction: 2
                );
                sig = SynthDef.wrap(func, paramLag ! func.def.argNames.size);
                sig = sig.asArray.flatten;
                
                if (channels.isNil) {
                    channels = sig.size;
                };
                
                if (rate.isNil) {
                    rate = sig.rate.switch(\audio, \ar, \control, \kr, \scalar, \kr);
                };
                
                \channels.ir(channels); // Unused, but helpful to see channelization for debugging
                
                sig = sig.collect {
                    |channel|
                    if ((channel.rate == \scalar) && (rate == \ar)) {
                        channel = DC.ar(channel);
                    };
                    
                    if ((channel.rate == \audio) && (rate == \kr)) {
                        channel = A2K.kr(channel);
                        "Pmod output is \audio, \control rate expected".warn;
                    } {
                        if ((channel.rate == \control) && (rate == \ar)) {
                            channel = K2A.ar(channel);
                            "Pmod output is \control, \audio rate expected".warn;
                        }
                    };
                    channel;
                };
                
                if (sig.shape != [channels]) {
                    sig.reshape(channels);
                };
                
                // [NodeID.ir, Line.kr(0, 300, 300) , \out.kr, \gate.kr(1), fade].poll(8, label:["------", "  time", "   out", "  gate", "  fade"]);
                XOut.perform(rate, \out.kr(0), fade, sig);
            }).add(completionMsg: added.completionMsg);
            
            // "Sending %".format(defName).postln;
            
            args = def.asSynthDesc.controlNames.flatten.asArray;
            defCache[defName] = [rate, channels, def, args];
        } {
            // "Reusing cached synth %".format(defName).postln;
            added.value = true;
            #rate, channels, def, args = defCache[defName];
        };
        
        ^(
            instrument: defName, synthDefName: defName,
            args: ([\value, \fadeTime, \paramLag, \out] ++ args).removeDups,
            specialArgs: args.copy.removeAll([\value, \fadeTime, \paramLag, \out, \gate, \channels]),
            pr_rate: rate,
            pr_channels: channels,
            pr_instrumentHash: hash ?? { [func, rate, channels].hash },
            pr_wasAdded: added,
            hasGate: true,
        )
    }
    
    rate_{
        |r|
        rate = (
            control: \kr,
            audio: \ar,
            kr: \kr,
            ar: \kr
        )[r]
    }
    
    makeModGroupFactory {
        |name|
        var access, create, destroy;
        var newModGroup, target, server;
        var fadeTime = 0, holdTime = 0;
        
        create = CallOnce {
            // "creating mod group, synthGroup is: %".format(~synthGroup).postln;
            target = ~synthGroup;
            server = ~server;
            name = "%_mod".format(GroupNames.groupName(target.asControlInput) ?? { name });
            // server.makeBundle(nil) {
            //     newModGroup = Group(target, \addBefore).name_(name);  
            // };
            newModGroup = (
                type: \gdef,
                name: name,
                server: server,
                before: [target],
            ).play;
        };
        
        // access = {
        //     fadeTime = max(fadeTime, ~fadeTime.value ? 0);
        //     holdTime = max(holdTime, ~holdTime.value ? 0);
        //     newModGroup ?? create
        // };
        access = create;
        
        destroy = CallOnce {
            newModGroup !? {
                "not freeing mod group %".format(newModGroup).postln;
                
                // server.makeBundle(server.latency + fadeTime + holdTime + 0.05) {
                //     newModGroup.free;
                // }
            }
        };
        
        ^[access, destroy]
    }
    
    makeSynthGroupFactory {
        var server, target;
        
        ^CallOnce {
            synthGroup ?? {
                server = ~server.value;
                // server.makeBundle(nil) {
                //     synthGroup = Group(server).name_(\PmodSynthGroup);
                //     synthGroup.onFree {
                //         synthGroup = nil;
                //     };
                // };
                synthGroup = (
                    type: \gdef, 
                    server: ~server.value,
                    name: \PmodSynthGroup, 
                    delta: 0,
                );
            }
        }
    }
    
    embedInStream {
        |inEvent|
        var server, synthStream, streamPairs, endVal, cleanup,
            synthGroup, synthGroupFactory, synthGroupDestroy, 
            modGroup, modGroupDestroy, 
            buses, currentArgs, currentBuses, currentEvent, fadeTime,
            nextEvent, nextSynth, streamAsValues, currentChannels, currentRate, cleanupFunc,
            lazyGetBus;
        
        // CAVEAT: Server comes from initial inEvent and cannot be changed later on.
        server = inEvent[\server] ?? { Server.default };
        server = server.value;
        
        streamAsValues = asValues;
        
        // Setup pattern pairs
        streamPairs = patternPairs.copy;
        endVal = streamPairs.size - 1;
        forBy (1, endVal, 2) { |i| streamPairs[i] = streamPairs[i].asStream };
        synthStream = synthName.asStream;
        
        // Prepare busses
        buses = List();
        
        // Cleanup
        cleanupFunc = Thunk({
            currentEvent !? {
                "cleaning up currentEvent: %".format(currentEvent).postln;
                if (currentEvent[\isPlaying].asBoolean && currentEvent[\hasGate]) {
                    "scheduling for holdTime: %".format(currentEvent[\holdTime]).postln;
                    thisThread.clock.sched(currentEvent[\holdTime], {
                        currentEvent[\sendGate] = true;
                        currentEvent.release(/*nextEvent[\fadeTime]*/);
                        currentEvent[\isPlaying] = false;
                    })
                };
                
                this.recycleDefName(currentEvent);
                
                // "cleaning up pmod".postln;
                // newModGroup.debug("newModGroup");
                // newSynthGroup.debug("newSynthGroup");
                {
                    // newModGroup !? _.free;
                    buses.do(_.free);
                }.defer(
                    (currentEvent[\fadeTime] ? 0 )
                    + (currentEvent[\holdTime] ? 0)
                );
                
                // {
                //     newSynthGroup !? _.free;
                // }.defer(
                //     (currentEvent[\fadeTime] ? 0 )
                //     + (currentEvent[\holdTime] ? 0)
                // );
                
                // "cleanup done".postln;
            }
        });
        cleanup = EventStreamCleanup();
        cleanup.addFunction(inEvent, cleanupFunc);
        
        synthGroupFactory = this.makeSynthGroupFactory();
        
        #modGroup, modGroupDestroy = this.makeModGroupFactory(
            name: synthGroup.tryPerform(\name) ?? {
                inEvent[\instrument]
            }
        );
        
        loop {
            // Prepare groups, reusing input group if possible.
            // This is the group that the outer event - the one whose parameters
            // we're modulating - is playing to.
            // 
            // If newSynthGroup.notNil, then we allocated and we must clean up.
            inEvent[\server] = server;
            
            // Prepare modGroup, which is our modulation group and lives before
            // synthGroup.
            // If newModGroup.notNil, then we allocated and we must clean up
            if (inEvent.keys.includes(\modGroup)) {
                modGroup = inEvent[\modGroup];
            } {
                cleanup.addFunction(inEvent, modGroupDestroy);
            };
            
            // modGroupFunc = {
            //     if (~synthGroup.notNil) {
            //         ~server.value.sendBundle(
            //             ~server.value.latency,
            //             modGroup.moveBeforeMsg(~synthGroup.value.asGroup)
            //         );
            //     };
            //     modGroup;
            // };
            
            // We must set group/addAction early, so they are passed to the .next()
            // of child streams.
            nextEvent = ();
            nextEvent[\instrument] 	= nil;
            nextEvent[\synthDefName]= nil;
            nextEvent[\synthDesc] 	= nil;
            nextEvent[\msgFunc] 	= nil;
            nextEvent[\fadeTime] 	= inEvent[\fadeTime] ?? {0};
            nextEvent[\holdTime] 	= inEvent[\holdTime] ?? {0};
            nextEvent[\group] 		= modGroup;
            nextEvent[\addAction] 	= \addToHead;
            nextEvent[\resend] 	    = false;
            
            // Get nexts
            nextSynth = synthStream.next(nextEvent.copy);
            nextSynth = this.prepareSynth(nextSynth);
            
            if (inEvent.isNil || nextEvent.isNil || nextSynth.isNil) {
                ^cleanup.exit(inEvent);
            } {
                cleanup.update(inEvent);
            };
            
            nextEvent.putAll(this.prNext(streamPairs, inEvent.copy));
            nextEvent.putAll(nextSynth);
            
            // SUBTLE: If our inEvent didn't have a group, we set its group here.
            //          We do this late so previous uses of inEvent aren't disrupted.
            // if (inEvent.keys.includes(\group).not) {
            //     inEvent[\group] = synthGroupFactory;
            //     inEvent[\addAction] = \addToTail;
            // };
            
            // 1. We need argument names in order to use (\type, \set).
            // 2. We need size to determine if we need to allocate more busses for e.g.
            //    an event like (freq: [100, 200]).
            currentArgs = nextEvent[\instrument].asArray.collect(_.asSynthDesc).collect(_.controlNames).flatten.asSet.asArray;
            
            currentChannels = nextSynth[\pr_channels];
            currentRate = nextSynth[\pr_rate];
            
            buses.first !? {
                |bus|
                var busRate = switch(bus.rate, \audio, \ar, \control, \kr, bus.rate);
                if (busRate != currentRate) {
                    Error("Cannot use Synths of different rates in a single Pmod (% vs %)".format(
                        bus.rate, currentRate
                    )).throw;
                }
            };
            
            lazyGetBus = {
                |index, grow=true|
                if (grow) {
                    (index - (buses.size - 1)).max(0).do {
                        if (currentRate == \ar) {
                            buses = buses.add(Bus.audio(server, currentChannels))
                        } {
                            buses = buses.add(Bus.control(server, currentChannels))
                        };
                    };
                };
                
                buses.wrapAt(index);
            };
            
            // If we've got a different instrument than last time, send a new one,
            // else just set the parameters of the existing.
            if (nextEvent[\resend]
                or: {nextEvent[\pr_instrumentHash] != currentEvent.tryPerform(\at, \pr_instrumentHash)})
                {
                    nextEvent[\parentType]  = \note;
                    nextEvent[\type] 		= \note;
                    nextEvent[\sustain] 	= nil;
                    nextEvent[\sendGate] 	= false;
                    nextEvent[\fadeTime] 	= fadeTime = nextEvent.use { ~fadeTime } ?? 0;
                    
                    nextEvent[\out] 		= Routine({
                        64.do {
                            |i|
                            lazyGetBus.(i, true).asControlInput.yield
                        }
                    });
                    nextEvent[\group] 		= modGroup;
                    nextEvent[\addAction] 	= \addToHead; // SUBTLE: new synths before old, so OLD synth is responsible for fade-out
                    
                    // Free existing synth
                    currentEvent !? {
                        |e|
                        // Assumption: If \hasGate -> false, then synth will free itself.
                        if (e[\isPlaying].asBoolean && e[\hasGate]) {
                            e[\sendGate] = true;
                            e.release();
                            e[\isPlaying] = false;
                        }
                    };
                } {
                    nextEvent[\parentType]  = \set;
                    nextEvent[\type] 		= \set;
                    nextEvent[\id] 			= currentEvent[\id];
                    nextEvent[\args] 		= currentEvent[\args];
                    nextEvent[\out] 		= currentEvent[\id].size.collect { |i| lazyGetBus.(i) };
                };
            
            nextEvent.parent ?? {
                nextEvent.parent = Event.parentEvents.default
            };
            // nextEvent.proto = inEvent;
            
            // We yield a function, which is evaluated when (and if) it is finally consumed during Event playback.
            // All of the important bits - allocating buses and playing the modulator - are only fired when this
            // function is evaluated.
            inEvent = {
                var busRoutine = Routine({
                    64.do {
                        |i|
                        var bus = lazyGetBus.(i, false);
                        if (shouldExpand) {
                            bus.numChannels.do {
                                |j|
                                bus.subBus(j, 1).yield
                            }
                        } {
                            bus.yield
                        }
                    }
                });
                
                {
                    {
                        |outerEvent|
                        // In this context, ~group refers to the event being modulated,
                        // not the Pmod event.
                        // ~group = ~group.value;
                        // if (~group.notNil and: { ~group != synthGroup }) {
                        //     // modGroup.moveBefore(~group.asGroup)
                        // };
                        nextEvent[\synthGroup] = ~group.value ?? synthGroupFactory;
                        if (nextEvent[\isPlaying].asBoolean.not) {
                            ~addToCleanup = ~addToCleanup.add(cleanupFunc);
                            currentEvent = nextEvent;
                            
                            nextEvent[\isPlaying] = true;
                            nextEvent[\parentEvent] = currentEnvironment;
                            nextEvent[\specialArgs].do {
                                |key|
                                nextEvent[key] ?? {
                                    nextEvent[key] = currentEnvironment[key]
                                }
                            };
                            
                            if (nextEvent.pr_wasAdded.hasValue.not) {
                                nextEvent.pr_wasAdded.then {
                                    |time|
                                    "Pmod def % took % seconds to load, which is too long".format(
                                        nextEvent[\synthDefName],
                                        time
                                    ).warn
                                }.done                                
                            };
                            
                            try { 
                                nextEvent.playAndDelta(cleanup, false) 
                            } { 
                                |e| 
                                e.dumpBackTrace 
                            };
                        };
                        
                        if (streamAsValues) {
                            busRoutine.next.getSynchronous
                        } {
                            busRoutine.next.asMap
                        }
                    }
                }
            }.();
            
            inEvent = inEvent.yield;
        }
        
        ^cleanup.exit(inEvent);
    }
    
    // This roughly follows the logic of Pbind
    prNext {
        |streamPairs, inEvent|
        var event, endVal;
        
        inEvent = this.prScrubEvent(inEvent);
        
        event = ().proto_(inEvent);
        
        endVal = streamPairs.size - 1;
        forBy (0, endVal, 2) { arg i;
            var name = streamPairs[i];
            var stream = streamPairs[i+1];
            var streamout = stream.next(event);
            if (streamout.isNil) { ^inEvent };
            
            if (name.isSequenceableCollection) {
                if (name.size > streamout.size) {
                    ("the pattern is not providing enough values to assign to the key set:" + name).warn;
                    ^inEvent
                };
                name.do { arg key, i;
                    event.put(key, streamout[i]);
                };
            }{
                event.put(name, streamout);
            };
        };
        
        ^event;
    }
    
    recycleDefName {
        |event|
        var hash, name;
        if (defHashLRU.size > maxDefNames) {
            hash = defHashLRU.pop();
            name = defNames[hash];
            defNames[hash] = nil;
            defCache[name] = nil;
            defNamesFree.add(name);
        }
    }
    
    *getDefName {
        if (defNamesFree.notEmpty) {
            ^defNamesFree.pop()
        } {
            defCount = defCount + 1;
            ^"Pmod_unique_%".format(defCount).asSymbol;
        }
    }
    
    // Scrub parent event of Pmod-specific values like group - these will disrupt
    // the way we set up our groups and heirarchy.
    prScrubEvent {
        |event|
        event[\modGroup] = nil;
        event[\instrument] = nil;
        event[\type] = nil;
        event[\parentType] = nil;
        event[\args] = nil;
        event[\msgFunc] = nil;
        
        ^event;
    }
    
    // Convert an item from our instrument stream into a SynthDef name.
    // This can possible add a new SynthDef if supplied with e.g. a function.
    prepareSynth {
        |synthVal|
        var synthDesc, synthOutput;
        
        ^case
            { synthVal.isKindOf(Array) } {
                synthVal.collect(this.prepareSynth(_)).reduce({
                    |a, b|
                    a.merge(b, {
                        |a, b|
                        a.asArray.add(b)
                    })
                }).make {
                    ~pr_channels = ~pr_channels.maxItem;
                    ~pr_rate.reduce({
                        |a, b|
                        if (a != b) {
                            Error("Multichannel expansion with Pmod synths of different rates not supported (%, %)".format(a, b)).throw;
                        };
                        a
                    });
                }
            }
            { synthVal.isKindOf(SimpleNumber) } {
                var constRate = rate ?? { \ar }; // default to \ar, because this works for both ar and kr mappings;
                var constChannels = channels ?? { 1 };
                
                this.class.wrapSynth(
                    defName: "Pmod_constant_%_%".format(constChannels, constRate).asSymbol,
                    func: nil,
                    channels: constChannels,
                    rate: constRate,
                    
                ).putAll((
                    value: synthVal
                ))
            }
            { synthVal.isKindOf(Symbol) } {
                synthDesc = synthVal.asSynthDesc;
                synthOutput = synthDesc.outputs.detect({ |o| o.startingChannel == \out });
                
                if (synthOutput.isNil) {
                    Error("Synth '%' needs at least one output, connected to an \\out synth parameter".format(synthVal)).throw;
                };
                
                (
                    instrument: synthVal,
                    args: synthDesc.controlNames.flatten.asSet.asArray,
                    pr_rate: synthOutput.rate.switch(\audio, \ar, \control, \kr),
                    pr_channels: synthOutput.numberOfChannels,
                    pr_instrumentHash: synthVal.identityHash,
                )
            }
            { synthVal.isKindOf(AbstractFunction) } {
                this.class.wrapSynth(
                    defName: nil,
                    func: synthVal,
                    rate: rate,
                    channels: channels)
            }
            { synthVal.isKindOf(Event) } {
                synthVal.parent = currentEnvironment;
                this.prepareSynth({ 
                    synthVal.use { ~out.value }
                });
            }
            { synthVal.isNil } {
                nil
            }
            {
                synthVal.putAll(this.prepareSynth(synthVal[\instrument]));
            }
    }
    
    asValues {
        |unwrap=false|
        asValues = true;
        unwrapValues = unwrap;
    }
    
    expand {
        shouldExpand = true;
    }
}






