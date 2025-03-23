Pio : Pattern {
    var inner, recordNames;
    
    *new {
        |inner|
        ^super.newCopyArgs(inner)
    }
    
    *in { |name, numChannels=2, rate=\audio|		^Pin(name, numChannels, rate) }
    *out { |name, numChannels=2, rate=\audio|		^Pout(name, numChannels, rate) }
    
    *pdefs {
        |func|
        ^Pio(
            Ppar(
                Pio.make(func, Pdef.all),
                inf
            )
        )
    }
    
    *make {
        |func, composeWith|
        var result, resultStream, resultEnvir, resultEnvirStream;
        
        resultEnvir = (
            DefaultEnvironment()
                .defaultFunc_({ () })
                .dispatch_({
                    |key, value|
                    if (value.isKindOf(Event)) {
                        value[\name] = value[\name] ?? { key }
                    }
                })
        );
        
        // Call func, with resultEnvir as currentEnvironment
        // result is considered to be the "final output" Pattern, e.g. it will
        // play to (\out, 0).
        result = resultEnvir.use(func);
        resultEnvir = resultEnvir.envir;
        
        resultStream = result.asStream;
        resultEnvirStream = resultEnvir.keysValuesChange({
            |key, envirItem|
            if (envirItem.isKindOf(Event)) {
                Pbind(*envirItem.collect(_.asStream).asPairs)
            } {
                envirItem.asStream
            }
        });
        
        // Set \out of result events to 0
        resultStream = Pfunc({
            |r|
            if (r.isArray.not) { r = [r] };
            
            r.do {
                |event|
                event[\out] = 0;
            };
            
            r;
        }) <> resultStream;
        
        // Set a default name and output key
        resultEnvirStream = resultEnvirStream.collect {
            |eventStream, key|
            var out = Pout(key);
            (eventStream <> Pbind(
                \name, key,
                \out, out
            ))
        };
        
        // Replace \symbols with an input corresponding to another output bus
        resultEnvirStream = resultEnvirStream.collect {
            |eventStream|
            Pfunc({
                |e|
                var mergeInputName;
                
                e.keysValuesDo {
                    |key, value|
                    if (value.isKindOf(Event) and: { value[\name].notNil }) {
                        e[key] = Pin(value[\name])
                    } {
                        e[key] = value;
                    }
                }
            }) <> eventStream
        };
        
        if (composeWith.notNil) {
            resultEnvirStream = resultEnvirStream.collect {
                |stream, key|
                Prout({
                    |inval|
                    var pinStream, composeStream, nextPin, nextCompose;
                    
                    pinStream = stream.asStream;
                    composeStream = composeWith[key].asStream;
                    
                    while {
                        nextCompose = composeStream.next(inval);
                        nextPin = pinStream.next(());
                        (nextCompose.notNil and: { nextPin.notNil })
                    } {
                        nextCompose.putAll(nextPin);
                        inval = nextCompose.yield;
                    }
                })
                // Pevent(value, ()) <> composeWith[key]
            }
        };
        
        ^resultEnvirStream
    }
    
    record {
        |...names|
        recordNames = names;
    }
    
    finishEvent {
        |buses|
        var inputs=IdentitySet(), outputs=IdentitySet();
        
        ~out 	= ~out ?? {
            Pout(
                ~name ?? { \out },
                2,
                \audio
            );
        };
        ~name 	= ~name ?? { ~out.tryPerform(\name) } ?? ~instrument;
        
        currentEnvironment.keysValuesChange {
            |key, values|
            var needsUnbubble = false;
            
            if (values.isKindOf(SequenceableCollection).not) {
                values = [values];
                needsUnbubble = true;    
            };
            
            values = values.collect {
                |value, i|
                var valueFunc, bus, oldBus;
                
                if (value.isKindOf(PioProxy).not) {
                    value
                } {
                    if (value.isInput) {
                        if (value.isFeedback.not) {
                            inputs = inputs.add(value.name);
                        }
                    } {
                        outputs = outputs.add(value.name);
                    };
                    
                    buses[value.name] = bus = buses[value.name] ?? {
                        (type: \audioBus, channels: value.numChannels)
                    };
                    
                    if (value.numChannels > bus[\channels]) {
                        "Event %, channels % is greater than allocated bus %".format(key, value.numChannels, bus[\channels]);
                        
                        oldBus = bus;
                        buses[value.name] = bus = (type: \audioBus, channels: value.numChannels);
                    };
                    
                    // We wrap our bus allocation in a function, so it only occurs when this value is actually
                    // accessed e.g. by playing to a Synth. A bus will NOT be allocated if this key is later
                    // overridden.
                    valueFunc = {
                        if (oldBus.notNil) {
                            "freeing old bus (%) and allocating new one...".format(bus).postln;
                            bus[\out] = oldBus[\out];
                            oldBus.free;
                            bus.play;
                        };
                        
                        if (bus[\out].isNil) {
                            bus.play
                        };
                        
                        bus
                    };
                    
                    // Allow Pin/Pout to modify the bus
                    valueFunc = value.prepareBus(_) <> valueFunc;
                    
                    // Convert bus to an appropriate control input or map argument
                    if (value.isOutput) {
                        valueFunc = (_.asControlInput) <> valueFunc;
                    } {
                        valueFunc = {
                            |b|
                            b.channels.collect {
                                |i|
                                "a%".format(b.out + i).asSymbol;
                            };
                        } <> valueFunc
                    };
                    
                    valueFunc;
                }
            };
            
            if (needsUnbubble) {
                values = values.unbubble;
            };
            
            values
        };
        
        // ~group = Gdef(~name, after:inputs).permanent_(false);
        inputs = inputs.reject({ |g| g == ~name });
        outputs = outputs.reject({ |g| g == ~name });
        // "gdef % - inputs: %  outputs: %".format(~name, inputs, outputs).postln;
        // ~group = ~group.value;
        
        if (~group.isKindOf(Event) and: { ~group[\type] == \gdef }) {
            ~group.before = outputs.addAll(~group.before);
            ~group.after = inputs.addAll(~group.after);
        } {
            ~group = (name: ~name, type: \gdef, after:inputs, before:outputs, delta:0);
        };
        ~group.yield;
        ~group;
        ~group = ~group[\gdef];
    }
    
    embedInStream {
        |inval|
        var buses = (), recordKeys = IdentitySet();
        var innerStream = inner.asStream;
        var cleanup;
        
        inval = innerStream.next(inval);
        
        cleanup = EventStreamCleanup();
        cleanup.addFunction(inval, {
            buses.do({ |bus| MultiRecorder().removeTarget(bus.asBus) });
            buses.do(_.free);
            buses.clear();
        });
        
        while { inval.notNil } {
            // inval[\finish] = inval[\finish].addFunc({
            // 	this.finishEvent(buses);
            // });
            
            inval.use {
                this.finishEvent(buses);
            };
            
            recordKeys.addAll(buses.keys);
            recordNames !? {
                recordKeys = recordKeys.sect(recordNames)
            };
            
            recordKeys.do({
                |name|
                MultiRecorder().addTarget(name, buses[name].asBus)
            });
            
            inval = innerStream.next(inval.yield);
        };
        
        ^inval
    }
}

PioProxy : Pattern {
    var <>name, <>numChannels, <>rate, <>owner;
    var <>offset=0, <>count=nil;
    
    isInput { ^this.subclassResponsibility }
    isOutput { ^this.subclassResponsibility }
    
    at {
        |index|
        ^this.copy.offset_(offset + index).count_(1)
    }
    
    copyRange {
        |start, end|
        ^this.copy.offset_(start).count_(end - start + 1)
    }
    
    copySeries {
        |first, second, last|
        second !? {
            if ((second - first) => 1) { 
                Error("Cannot make a Pio series with non-contiguous indices").throw;
            }
        };
        ^this.copyRange(first, last);
    }
    
    embedInStream {
        |inval|
        var nameStream = name.asStream;
        var nextName;
        
        while {
            (nextName = nameStream.next(inval)).notNil
        } {
            this.class
                .new(nextName, numChannels, rate)
                .offset_(offset)
                .count_(count)
                .yield;
        }
    }
    
    prepareBus {
        |bus|
        if (offset == 0 and: { count.isNil }) {
            ^bus
        } {
            ^bus.copy.putAll((
                out:            bus[\out] + offset,
                numChannels:    count ?? { numChannels },
                channels:       count ?? { numChannels },
            ));
        }
    }
    
    expand {
        |size=1|
        if (size == 1) {
            ^Array.series(count ? numChannels, offset ? 0).collect {
                |i|
                this[i]
            };
        } {
            ^Array.series(
                size: ((count ? numChannels) / size).asInteger, 
                start: offset ? 0,
                step: size
            ).collect {
                |i|
                this[i..(i + size - 1)]
            };
        }
    }
    
    identityHash { ^[name, numChannels, rate].hash }
    
    asStream {
        ^Routine({ |inval| this.embedInStream(inval) })
    }
    
    printOnSuffix   { 
        |stream| 
        if (offset != 0 or: { count.notNil }) {
            if (count == 1) {
                stream << "[%]".format(offset)
            } {
                stream << "[%..%]".format(offset, offset + count - 1)
            }
        } 
    }
}

Pout : PioProxy {
    *new {
        |name=\out, numChannels=2, rate=\audio|
        if (name.isSequenceableCollection) {
            ^(name.collect(super.newCopyArgs(_, numChannels, rate)))
        } {
            ^super.newCopyArgs(name, numChannels, rate)
        }
    }
    
    isInput 		{ ^false }
    isOutput 		{ ^true }
    
    toControlInput 	{ |bus| ^bus.asControlInput }
    
    printOn			{ |stream| stream << "Pout(\\" << name << ")"; this.printOnSuffix(stream); }
}

Pin : PioProxy {
    *new {
        |name, numChannels=2, rate=\audio|
        if (name.isSequenceableCollection) {
            ^(name.collect(super.newCopyArgs(_, numChannels, rate)))
        } {
            ^super.newCopyArgs(name, numChannels, rate)
        }
    }
    
    isInput 		{ ^true }
    isOutput 		{ ^false }
    isFeedback		{ ^false }
    
    toControlInput 	{ |bus| ^bus.asMap }
    
    printOn			{ |stream| stream << "Pin(\\" << name << ")"; this.printOnSuffix(stream); }
}

PinFeed : Pin {
    *new {
        |name, numChannels=2, rate=\audio|
        ^super.newCopyArgs(name, numChannels, rate);
    }
    
    type 			{ \output }
    isInput 		{ ^true }
    isOutput 		{ ^false }
    isFeedback		{ ^true }
    
    toControlInput 	{ |bus| ^bus.asMap }
    
    printOn			{ |stream| stream << "PinFeed(\\" << name << ")"; this.printOnSuffix(stream); }
}


+Pdef {
    *chained {
        |func|
        var envir = Pio.make(func, Pdef.all);
        ^Pio(Ppar(envir.values));
    }
    
    chained {
        |func|
        this.source_(Pdef.chained(func));
    }   
}




