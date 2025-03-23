+TGrains2 {
    *calcDelay {
        |pos, delay, dur, rate, attack, decay, sampleRate=(SampleRate.ir)|
        var halfGrain, scaledPos, grainPeak, grainStart;
        
        grainPeak = (attack + (dur - decay)) / 2;
        
        grainStart = grainPeak - (0.5 * dur);
        grainStart = grainStart * (1 - rate);
        grainStart = grainStart * sampleRate;
        
        halfGrain = (0.5 * dur * sampleRate) + grainStart;
        
        scaledPos = pos;
        scaledPos = scaledPos - max(BlockSize.ir, delay * sampleRate);
        scaledPos = min(scaledPos, pos - (halfGrain * (1 - rate)).abs);
        scaledPos = scaledPos + halfGrain;
        scaledPos = scaledPos / sampleRate;
        
        scaledPos = scaledPos - (16 / sampleRate);
        
        ^scaledPos;
    }
}

+TGrains3 {
    *calcDelay {
        |...args|
        ^TGrains2.calcDelay(*args)
    }
}
