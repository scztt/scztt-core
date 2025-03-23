OSCMessageReversePatternDispatcher : OSCMessageDispatcher {
    classvar <instance;
    
    *initClass {
        instance = OSCMessageReversePatternDispatcher();
    }
    
    value {|msg, time, addr, recvPort|
        var pattern;
        pattern = msg[0];
        active.keysValuesDo({|key, func|
            if(pattern.matchOSCAddressPattern(key), {func.value(msg, time, addr, recvPort);});
        })
    }
    
    typeKey { ^('OSC matched').asSymbol }
    
}
