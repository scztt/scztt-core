PparStream : Stream {
    var <>initStreamAction, <>endStreamAction;
    var priorityQ, <now;
    var <injectFunc, pausedStreams;
    
    *forkPatterns {
        |pattern|
        var event, outerEvent, recursionLevel, instrument, embeddingLevel, freq, rest;
        var args, defaults, timingOffset, sustain, gatePattern;
        var size, newPatterns = [];
        
        if(pattern.notNil) {
            // [1] Preserve information from outer pattern, but not delta.
            if(~transparency ? true) {
                outerEvent = currentEnvironment.copy;
                outerEvent.putPairs(
                    [
                        \pattern, \type,
                        \parentType, \addToCleanup,
                        \removeFromCleanup, \sustain,
                        \legato, \timingOffset, \delta, \gatePattern
                    ].collect([_, nil]).flatten
                )
            } {
                outerEvent = Event.default.copy;
            };
            
            // [2] If a function, fill in it's args from the environment
            if (pattern.isKindOf(Function)) {
                defaults = pattern.def.prototypeFrame;
                args = pattern.def.argNames.collect {
                    |name, i|
                    currentEnvironment[name].value ?? { defaults[i] }
                };
                pattern = pattern.value(*args);
            };
            
            // [3] Grab timing properties
            sustain = ~sustain.value;
            ~timingOffset = timingOffset = (~timingOffset.value ? 0);
            ~gatePattern = gatePattern = (~gatePattern.value ? true);
            
            // [4] Apply flop
            if (~flop ? false) {
                outerEvent = outerEvent.asPairs.flop.collect(_.asEvent);
                outerEvent.do(_.parent_(currentEnvironment.parent));
            } {
                outerEvent = [outerEvent]
            };
            
            // [5] Process inner pattern
            outerEvent.do {
                |outerEvent, i|
                var innerPattern;
                
                outerEvent.use {
                    innerPattern = pattern;
                    
                    if (innerPattern.isKindOf(Symbol)) {
                        innerPattern = Pdef(pattern);
                    };
                    
                    if (innerPattern.isKindOf(Stream) and: { outerEvent.size > 1 }) {
                        innerPattern = innerPattern.copy;
                    };
                    
                    if (innerPattern.isKindOf(Event)) {
                        Error("Event patterns must be wrapped in a Ref or a function when passed in as an \\instrument argument").throw;
                    };
                    
                    if (innerPattern.isKindOf(PatternProxy)) {
                        innerPattern = innerPattern.pattern; // optimization. outer pattern takes care for replacement
                    };
                    
                    if (innerPattern.notNil) {
                        // not sure why we DON'T need to account for positive timingOffset here,
                        // but if we do it breaks....
                        if (gatePattern) {
                            innerPattern = innerPattern.finDur(sustain - timingOffset.min(0) - 0.000000001);
                        };
                        
                        if (not(~filter ? false)) {
                            innerPattern = Pevent(innerPattern, outerEvent).asStream;
                        };
                        
                        if (timingOffset < 0) {
                            innerPattern.fastForward(timingOffset.neg, 0, outerEvent);
                        };
                        
                        newPatterns = newPatterns.add([
                            timingOffset, innerPattern
                        ]);
                    }
                }
            }
        };
        
        ^newPatterns
    }
    
    *initClass {
        Class.initClassTree(Event);
        
        Event.addEventType(\fork, {
            this.forkPatterns(~pattern ?? { ~instrument }).do {
                |newPatterns|
                ~injectStream.value(
                    delta: newPatterns[0],
                    stream: newPatterns[1],
                    inval: newPatterns[2]
                )
            }
        }, Event.parentEvents.default.copy.putAll((legato:1)));
    }
    
    *new {
        ^super.new.init
    }
    
    init {
        priorityQ = PriorityQueue();
        pausedStreams = IdentitySet();
        now = 0;
        injectFunc = { |delta, stream, inval| this.injectStream(delta, stream, inval) };
    }
    
    reset {
        this.init();
    }
    
    injectStream {
        |delta, stream, inval|
        stream = stream.asStream;
        // "injecting stream %".format(stream.identityHash).postln;
        initStreamAction !? {
            stream = initStreamAction.value(stream, inval).asStream
        };
        priorityQ.put(now + delta, stream);
        ^stream;
    }
    
    pauseStream {
        |stream|
        pausedStreams = pausedStreams.add(stream);
    }
    
    resumeStream {
        |stream, delta=0|
        pausedStreams = pausedStreams.remove(stream);
    }
    
    injectEvent {
        |delta, event, inval|
        this.injectStream(
            delta,
            Routine({ event.yield; nil.yield }, 2),
            inval
        )
    }
    
    // processStreamsToPause {
    // 	streamsToPause.do {
    // 		|stream|
    // 		priorityQ.remove(stream);
    // 		pausedStreams = pausedStreams.add(stream);
    // 	};
    // 	streamsToPause.clear();
    // }
    
    embedInStream {
        |inval, duration|
        var stream, nextTime, outval, endTime, nextTimeDelta;
        
        endTime = now + (duration ? inf);
        
        while { priorityQ.notEmpty and: { now < endTime } } {
            nextTime = min(
                priorityQ.topPriority,
                endTime
            );
            // "now: %        nextTime: %    endTime: %".format(now, nextTime, endTime).postln;
            
            if (nextTime > now) {
                nextTimeDelta = nextTime - now;
                
                pausedStreams.do {
                    |stream|
                    priorityQ.put(nextTimeDelta, stream);
                };
                
                inval = Event.silent(nextTimeDelta).yield;
                now = nextTime;
            } {
                stream = priorityQ.pop;
                
                if (pausedStreams.includes(stream).not) {
                    outval = stream.next(inval).asEvent;
                    
                    if (outval.notNil) {
                        // requeue stream
                        priorityQ.put(now + outval.delta, stream);
                        outval[\delta] = 0;
                        outval[\injectStream] = outval[\injectStream] ?? { injectFunc };
                        inval = outval.yield;
                    } {
                        endStreamAction.value(stream, inval);
                    }
                };
            }
        };
        
        if (duration.notNil and: { now < endTime }) {
            inval = Event.silent(endTime - now).put(\debug, "PparStream(%):embedInStream:207".format(this.identityHash)).yield;
        };
        
        ^inval
    }
}

Ppar2 : ListPattern {
    var <>initStreamAction, <>endStreamAction;
    
    embedInStream {
        |inval|
        var parStream = PparStream()
            .initStreamAction_(initStreamAction)
            .endStreamAction_(endStreamAction);
        
        repeats.value(inval).do({
            list.do(parStream.injectStream(0, _, inval));
            parStream.embedInStream(inval);
            parStream.reset();
        })
    }
}

PwithStream : Stream {
    var <>parStream, <>inputStream, <>replacementStream;
    var routine;
    
    // *new {
    // 	|parStream, inputStream, condition, patternFunction, replace, gate, insert|
    // 	^super.newCopyArgs(parStream, inputStream, condition, patternFunction, replace, gate, insert).init
    // }
    
    *new {
        |parStream, inputStream, replacementStream|
        ^super.newCopyArgs(parStream, inputStream, replacementStream).init
    }
    
    init {
        routine = Routine({
            |inEvent|
            this.embedInStream(inEvent);
        });
    }
    
    next {
        |inEvent|
        ^routine.next(inEvent);
    }
    
    embedInStream {
        |inEvent|
        var originalEventDelta;
        var inEvents, outputStream, eventFilters;
        
        outputStream = Routine({
            |inEvent|
            var shouldPlayOriginalEvent;
            var shouldWaitForForkedStream;
            var shouldReplaceAll;
            var shouldGatePattern;
            var shouldFilterEvents;
            
            var forkPatterns=[], insertPatterns=[], resultPatterns, inputFilters;
            var outEvent, outDelta, outReplacement, skipRests = false;
            
            while {
                while {
                    outEvent = inputStream.next(inEvent);
                    skipRests and: { outEvent.isRest }
                };
                
                skipRests = false;
                outEvent.notNil
            } {
                shouldPlayOriginalEvent = true;
                shouldWaitForForkedStream = false;
                shouldReplaceAll = false;
                shouldGatePattern = nil;
                shouldFilterEvents = false;
                
                if (outEvent.isRest.not and: {
                    outEvent.use {
                        /////////////////////////////////////////////////////////////////////////////////
                        outReplacement = replacementStream.next(outEvent);
                        /////////////////////////////////////////////////////////////////////////////////
                    };
                    
                    outReplacement.notNil
                }) {
                    shouldPlayOriginalEvent = false;
                    shouldWaitForForkedStream = false;
                    shouldReplaceAll = false;
                    
                    outEvent.copy.use {
                        // SUBTLE: We want to prevent gating if we're doing an insert,
                        // but we only know if we're doing an insert once we've run our function.
                        shouldGatePattern = ~gatePattern ?? { true };
                        
                        ~gatePattern = {
                            if (~insert !? _.value ?? false) {
                                false
                            } {
                                shouldGatePattern.value
                            }
                        };
                        
                        /////////////////////////////////////////////////////////////////////////////////
                        resultPatterns = PparStream.forkPatterns(outReplacement);
                        /////////////////////////////////////////////////////////////////////////////////
                        
                        shouldFilterEvents = ~filter ?? { shouldFilterEvents };
                        shouldWaitForForkedStream = ~insert ?? { shouldWaitForForkedStream };
                        shouldReplaceAll = ~replaceAll ?? { shouldWaitForForkedStream };
                        
                        shouldPlayOriginalEvent = (
                            resultPatterns.isEmpty
                                or: { shouldFilterEvents }
                                // or: { shouldWaitForForkedStream.postln }
                                or: {
                                    ~replace !? _.not ?? { shouldPlayOriginalEvent }
                                }
                        );
                        
                        if (shouldFilterEvents) {
                            resultPatterns.do {
                                |pattern|
                                inputFilters = inputFilters.add(pattern[1].asStream);
                            }
                        };
                        
                        if (resultPatterns.isEmpty.not && shouldReplaceAll) {
                            forkPatterns.extend(0);
                            insertPatterns.extend(0);
                        };
                        
                        if (shouldFilterEvents.not) {
                            if (shouldWaitForForkedStream) {
                                insertPatterns = insertPatterns.addAll(resultPatterns)
                            } {
                                forkPatterns = forkPatterns.addAll(resultPatterns)
                            }
                        }
                    };
                };
                
                //////////////////////////////////////////////////////////////////////////////////////////
                inputFilters.copy.do {
                    |filter|
                    var newEvent = filter.next(outEvent);
                    
                    if (newEvent.isNil) {
                        inputFilters.remove(filter)
                    } {
                        outEvent = newEvent;
                    }
                };
                //////////////////////////////////////////////////////////////////////////////////////////
                
                // Process all input events at the current time BEFORE forking / inserting
                if (outEvent.delta > 0) {
                    // If we generated no works, then yield straight through
                    // First, inject our forked streams
                    forkPatterns !? {
                        |toFork|
                        toFork = toFork.copy();
                        forkPatterns.extend(0);
                        
                        toFork.do {
                            |p|
                            // "parStream.injectStream(%, %, %);".format(p[0], p[1], outEvent).postln;
                            parStream.injectStream(p[0], p[1], outEvent);
                        }
                    };
                    
                    if (insertPatterns.size > 0) {
                        insertPatterns !? {
                            |toInsert|
                            var oldDelta;
                            
                            // TODO: This means the next event occurs RIGHT after this insert ends - do we want this?
                            if (shouldPlayOriginalEvent) {
                                // // @SUBTLE - If we're replacing the original event, then the overall duration of our inserted
                                // // pattern is the "new" duration. 
                                // oldDelta = outEvent.delta;
                                // toInsert = toInsert.add([
                                //     0,
                                //     Event.rest(oldDelta)
                                // ]);
                                "playing original event through insert".postln;
                                inEvent = outEvent.copy.put(\delta, 0).put(\debug, 387).yield;
                            };
                            
                            toInsert = toInsert.flatten;
                            insertPatterns.extend(0);
                            
                            inEvent = Ptpar(toInsert).trace.embedInStream(outEvent);
                            
                            skipRests = true;
                        };
                    } {
                        if (shouldPlayOriginalEvent) {
                            inEvent = outEvent.yield;
                        } {
                            inEvent = Event.silent(outEvent.delta).put(\debug, 406).yield
                        }
                    }
                } {
                    if (shouldPlayOriginalEvent) {
                        inEvent = outEvent.yield;
                    } {
                        inEvent = Event.silent(outEvent.delta).put(\debug, 413).yield
                    }
                }
            }
        });
        
        parStream.injectStream(0, outputStream, inEvent);
        
        ^parStream.embedInStream(inEvent)
    }
}

Pwith : Pattern {
    // var <inputPattern, <conditions, <patternFunctions,
    var inputPattern, replacements, <>replace, <>gate, <>insert;
    
    *new {
        |inputPattern ...replacements|
        ^super.newCopyArgs(inputPattern, replacements ?? {[]})
    }
    
    *conditions {
        |inputPattern, condition, pattern ...conditionPatterns|
        conditionPatterns = ([condition, pattern] ++ conditionPatterns).clump(2);
        conditionPatterns = conditionPatterns.collect {
            |condPat|
            Pfunc({
                |e|
                if (condPat[0].value(e)) {
                    condPat[1].value(e)
                } {
                    nil
                }
            })
        };
        
        ^this.new(
            inputPattern,
            *conditionPatterns
        ).init
    }
    
    init {
        replace = true;
        gate = false;
        insert = false;
    }
    
    fixPattern {
        |p|
        ^p.isFunction.if({
            Pfunc({ |e| e.use(p) })
        }, {
            p
        })
    }
    
    asStream {
        var stream;
        var inputStream = inputPattern.asStream;
        
        replacements.do {
            |replacement|
            inputStream = PwithStream(
                parStream: PparStream(),
                inputStream: inputStream,
                replacementStream: this.fixPattern(replacement).asPattern.asStream,
            );
        }
        
        // [conditions, patternFunctions].flop.flatten.pairsDo {
        // 	|condition, patternFunction|
        //
        // 	inputStream = PwithStream(
        // 		PparStream(),
        // 		inputStream,
        // 		condition,
        // 		this.fixPattern(patternFunction),
        // 		replace,
        // 		gate,
        // 		insert: false
        // 	);
        // };
        
        ^inputStream
    }
}

+Ppar {
    // *new {
    // 	|list, repeats=1|
    // 	^Ppar2.new(list, repeats);
    // }
}
