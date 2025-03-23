Event.eventTypes[\note] = #{|server|
    var freqs, lag, strum, sustain;
    var bndl, addAction, sendGate, ids;
    var msgFunc, instrumentName, offset, strumOffset;
    
    // var schedBundleArray;
    
    freqs = ~detunedFreq.value;
    
    // msgFunc gets the synth's control values from the Event
    msgFunc = ~getMsgFunc.valueEnvir;
    instrumentName = ~synthDefName.valueEnvir;
    
    // determine how to send those commands
    // sendGate == false turns off releases
    
    sendGate = ~sendGate.value ?? ~hasGate;
    
    // update values in the Event that may be determined by functions
    
    ~freq = freqs;
    ~amp = ~amp.value;
    ~sustain = sustain = ~sustain.value;
    lag = ~lag.value;
    offset = ~timingOffset.value;
    strum = ~strum;
    ~server = server;
    ~isPlaying = true;
    addAction = Node.actionNumberFor(~addAction);
    
    // compute the control values and generate OSC commands
    bndl = msgFunc.valueEnvir;
    bndl = [9 /* \s_new */, instrumentName, ids, addAction, ~group] ++ bndl;
    
    if(strum == 0 and: { (sendGate and: { sustain.isArray })
        or: { offset.isArray } or: { lag.isArray } }) {
            bndl = flopTogether(
                bndl,
                [sustain, lag, offset]
            );
            #sustain, lag, offset = bndl[1].flop;
            bndl = bndl[0];
        } {
            bndl = bndl.flop
        };
    
    // produce a node id for each synth
    
    ~id = ids = Array.fill(bndl.size, { server.nextNodeID });
    bndl = bndl.collect { | msg, i |
        msg[2] = ids[i];
        msg.asOSCArgArray
    };
    
    // schedule when the bundles are sent
    
    if (strum == 0) {
        ~schedBundleArray.(lag, offset, server, bndl, ~latency);
        if (sendGate) {
            ~schedBundleArray.(
                lag,
                sustain + offset,
                server,
                [15 /* \n_set */, ids, \gate, 0].flop,
                ~latency
            );
        }
    } {
        if (strum < 0) {
            bndl = bndl.reverse;
            ids = ids.reverse
        };
        strumOffset = offset + Array.series(bndl.size, 0, strum.abs);
        ~schedBundleArray.(
            lag, strumOffset, server, bndl, ~latency
        );
        if (sendGate) {
            if (~strumEndsTogether) {
                strumOffset = sustain + offset
            } {
                strumOffset = sustain + strumOffset
            };
            ~schedBundleArray.(
                lag, strumOffset, server,
                [15 /* \n_set */, ids, \gate, 0].flop,
                ~latency
            );
        }
    }
};
