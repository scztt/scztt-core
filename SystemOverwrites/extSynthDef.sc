+SynthDef {
    // buildForProxy {}
    
    *channelized {
        |name, ugenGraphFunc, rates, prependArgs, variants, metadata, channelizations, default|
        var defNames, hasOutputsArg;
        
        channelizations = channelizations ?? {[1,2]};
        // channelizations = channelizations.uniqu
        
        defNames = channelizations.collect({ 
            |ch| 
            if (ch.isKindOf(Number)) {
                "%_%ch".format(name, ch).asSymbol 
            } {
                "%_%ch_%out".format(name, ch[0], ch[1]).asSymbol 
            }
        });
        
        if (ugenGraphFunc.def.argNames[0] != 'numChannels') {
            "First SynthDef func argument should be \numChannels".warn;
        } {
            default = default ?? {
                ugenGraphFunc.def.prototypeFrame !? _[0]
            }
        };
        
        hasOutputsArg = ugenGraphFunc.def.argNames[1] == \numOutputs;
        
        ^defNames.collect {
            |chName, i|
            var def, chans;
            
            if (hasOutputsArg) {
                chans = channelizations[i].asArray.extend(2);
            } {
                chans = channelizations[i].asArray.extend(1);
            };
            
            def = SynthDef(
                chName,
                { SynthDef.wrap(ugenGraphFunc, prependArgs:chans) },
                rates, prependArgs, variants
            ).addReplace;
            
            if (channelizations[i] == default) {
                SynthDef(
                    name.asSymbol,
                    { SynthDef.wrap(ugenGraphFunc, prependArgs:chans) },
                    rates, prependArgs, variants
                ).addReplace;	
            };
            
            def;
        }
    }
    
    numChannels {
        ^(desc !? (_.numChannels))
    }
    
    rate {
        ^(desc !? (_.rate));
    }
}

+SynthDesc {
    prFindProxyOutputs {
        ^outputs.select {
            |o|
            o.startingChannel == \out
        }
    }
    
    numChannels {
        var channels = nil;
        this.prFindProxyOutputs.do {
            |out|
            channels = max(out.numberOfChannels, channels ?? 1);
        };
        ^channels
    }
    
    rate {
        this.prFindProxyOutputs.do {
            |out|
            ^out.rate;
        };
        ^nil
    }
}

+SynthControl {
    build { | proxy, orderIndex |
        var rate, desc, numChannels;
        desc = this.synthDesc;
        
        if(desc.notNil) {
            canFreeSynth = desc.canFreeSynth;
            canReleaseSynth = desc.hasGate && canFreeSynth;
        };
        
        if(proxy.isNeutral) {
            rate = desc.rate;
        };
        numChannels = desc.numChannels;
        
        ^proxy.initBus(rate, numChannels)
    }
}


