FEED {
    *new {
        |channels, func, delays=0, maxDelay=5, tempoLocked=false, shapeHint=nil, post=nil|
        var delay, feedIn, feedOut, buffer, phase, bufSize, delayPhase, totalChannels;
        var tempoScale=1, sig;
        
        shapeHint = shapeHint !? { 0 ! channels };
        
        if (tempoLocked) {
            tempoScale = \tempo.ir(1).reciprocal
        };
        
        post = post ?? { {|sig|sig} };
        
        delays = tempoScale * delays.asArray;
        
        maxDelay = delays.collect {
            if (delay.isKindOf(SimpleNumber)) {
                maxDelay = max(delay, ControlDur.ir);
            } {
                maxDelay
            }
        };
        
        bufSize = maxDelay * SampleRate.ir;
        
        buffer = delays.collect { LocalBuf(bufSize.flatten.maxItem, channels) };
        buffer.do(_.clear);
        
        phase = Phasor.ar(1, 1, 0, bufSize.flatten);
        phase.assertChannels(delays.size);
        
        delays = (delays - ControlDur.ir).max(ControlDur.ir);
        delayPhase = phase - (delays * SampleRate.ir);
        
        feedIn = [
            buffer,
            delayPhase
        ].flop.unpackCollect(BufRd.ar(channels, _, _, 1, 4));
        feedIn = feedIn.collect(_.asArray);
        feedIn = feedIn.collect(_.reshapeLike(shapeHint));
        feedIn = feedIn.unbubble(1);
        
        feedOut = feedIn.collect(func);
        feedOut = post.value(feedOut);
        
        [
            feedOut
                .collect(_.asArray)
                .collect(_.flatten)
                .collect(_.leakdc),
            buffer,
            phase
        ].flop.unpackDo(BufWr.ar(_, _, _));
        
        // feedOut.assertChannels(delays.size, channels);
        
        ^feedIn
    }
}

+UGen {
    feed {
        |func, delay=0, preDelay=0, tempoLocked=false, pre=nil, post=nil, max=5|
        ^[this].feed(func, delay, preDelay, tempoLocked, pre, post, max)
    }
}

+Array {
    feed {
        |func, delay=0, preDelay=0, tempoLocked=false, pre=nil, post=nil, max=5|
        var sig, input;
        
        if (tempoLocked) {
            preDelay = preDelay * \tempo.ir(1).reciprocal
        };
        
        preDelay = preDelay.asArray;
        delay = delay.asArray;
        pre = pre ?? { { |in| in } };
        
        input = this ! delay.size;
        input = input.unbubble(1);
        input = pre.(input);
        
        sig = FEED(this.flatten.size, {
            |feed, i|
            input[i] + func.value(feed, i);
        }, delay, max, tempoLocked, shapeHint:this, post:post);
        
        sig = sig.asArray.collect {
            |channel, i|
            var maxPredelay = max,
                thisPredelay = preDelay.wrapAt(i);
            
            if (thisPredelay.isKindOf(SimpleNumber)) {
                maxPredelay = max(thisPredelay, ControlDur.ir);
            };
            
            if (thisPredelay != 0) {
                if (thisPredelay.isNumber || (thisPredelay.rate == \scalar)) {
                    channel = DelayN.ar(channel, maxPredelay, thisPredelay);
                } {
                    channel = DelayC.ar(channel, maxPredelay, thisPredelay);
                }
            };
            
            channel
        };
        
        ^(sig + this)
    }
}