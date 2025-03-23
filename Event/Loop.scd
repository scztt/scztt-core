(
SynthDef.channelized(\loop, {
    |numChannels|
    var sig, loopDur, loopStart, offset, loopTrim, buffer, phase, samples;
    var rate, loopSamples;
    
    buffer = \buffer.kr;
    
    loopDur = \loopDur.kr(1);
    loopStart = \loopStart.kr(0);
    offset = \offset.kr(0);
    loopTrim = \loopTrim.kr(-1);
    loopTrim = (loopTrim < 0).if(
        BufDur.kr(buffer),
        loopTrim
    );
    
    samples = loopTrim * BufSampleRate.kr(buffer);
    loopStart = loopStart * BufSampleRate.kr(buffer);
    offset = offset * BufSampleRate.kr(buffer);
    
    rate = loopTrim / loopDur * BufRateScale.kr(buffer);
    
    phase = loopStart + Phasor.ar(1, rate, 0, samples, resetPos: offset);
    sig = BufRd.ar(numChannels, buffer, phase, 0, 4);
    
    sig = \amp.kr(1) * sig * Env([1, 1, 0], [loopDur, 0.01]).kr(gate:\gate.kr(1), doneAction:2);
    
    Out.ar(\out.kr(0), sig);
}, channelizations: (1..4)).do(_.add);

Event.addParentType(\loop, (
    finish: {
        ~numChannels = ~numChannels ?? { ~buffer.tryPerform(\numChannels) } ?? { 1 };
        ~instrument = "loop_%ch".format(~numChannels).asSymbol;
        ~loopDur = ~sustain.value / (~tempo.value ?? {1})
    },
    legato: 1,
    dur: 4
));

SynthDef.channelized(
    \monoLoop, 
    {
        |numChannels, numOutputs|
        var pos, sig;
        var buf, start, end, offset, rate;
        
        start = \start.ar(0);
        end = \end.ar(-1);
        buf = \buffer.kr(-1);
        rate = \rate.ar(1) * BufRateScale.kr(buf);
        offset = \offset.ir(0) * BufRateScale.kr(buf);
        
        end = (end < 0).if(BufDur.kr(buf), end);
        
        #start, end = [start, end] * BufSampleRate.kr(buf);
        
        pos = Phasor.ar(1, rate, start, end, resetPos: offset);
        sig = BufRd.ar(
            numChannels,
            buf,
            pos,
            loop: 1,
            interpolation: 4
        );
        
        sig = BHiPass.ar(sig, \lo.kr(10), 1);
        sig = BLowPass.ar(sig, \hi.kr(20000), 1);
        
        sig = sig.asArray;
        
        if (numOutputs.notNil and: { numOutputs != sig.size }) {
            case 
                { sig.size == 1 } {
                    sig = sig[0] ! numOutputs
                }
                { numOutputs == 1 } {
                    sig = sig.mean;
                }
                { sig.size > numOutputs } {
                    sig = [0.0, 1.0].resamp1(sig.size).collect {
                        |pan, i|
                        PanX.ar(numOutputs, sig[i], pan);
                    };
                    sig = sig.sum;
                } 
                { sig.size < numOutputs } {
                    sig = [0.0, sig.size - 1].resamp1(numOutputs).collect {
                        |which, i|
                        SelectX.ar(which, sig, wrap:0);
                    }
                }
        };
        NamedOut() <= sig;
    }, 
    channelizations: (1..4) ++ [8, 19] ++ [
        [1, 8], 
        [2, 8]
    ]
).do(_.add);

Event.addParentType(\monoLoop, (
    out: 0,
    amp: 1,
    bufferRoot: 261.6255653006,
    rate: {
        ~detunedFreq.value / ~bufferRoot.value
    },
    numChannels: { ~buffer.tryPerform(\numChannels) ?? { 1 } },
    finish: {
        ~rate = ~rate.value;
        ~numChannels = ~numChannels.value ?? { ~buffer.tryPerform(\numChannels) ?? { 1 } };
        ~instrument = "monoLoop_%ch".format(~numChannels).asSymbol;
        ~numOutputs !? {
            ~instrument = "%_%out".format(~instrument, ~numOutputs).asSymbol
        };
    },
    legato: 1,
));

SynthDef.channelized(\soundFilePlayer, {
    |numChannels|
    var sig;
    sig = PlayBuf.ar(
        numChannels, 
        bufnum: \buffer.ir, 
        rate: BufRateScale.kr(\buffer.ir) * \rate.kr(1), 
        loop: \loop.kr(1), 
        doneAction: 2
    );
    Out.ar(\out.kr(0), sig);
}, channelizations: [1, 2]);

Event.addParentType(\soundFile, (
    instrument: \soundFilePlayer,
    out: 0,
    amp: 1,
    rate: 1,
    loop: 0,
    numChannels: { ~buffer.tryPerform(\numChannels) ?? { 1 } },
    legato: 1,
    finish: {
        ~rate = ~rate.value;
        ~numChannels = ~numChannels.value ?? { ~buffer.tryPerform(\numChannels) ?? { 1 } };
        ~instrument = "monoLoop_%ch".format(~numChannels).asSymbol;
        ~numOutputs !? {
            ~instrument = "%_%out".format(~instrument, ~numOutputs).asSymbol
        };
    },
));


)
