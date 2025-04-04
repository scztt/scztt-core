(
SynthDef(\default, {
    var env, sig, freq, velocity;
    
    velocity = \velocity.kr(80).linlin(0, 127, 0, 1);
    
    env = Env.adsr(
        \attackTime.kr(0.05),
        \decayTime.kr(0.2),
        \sustainLevel.kr(-2.dbamp),
        \releaseTime.kr(0.5),
        curve:-2
    ).kr(gate:\gate.kr(1), doneAction:2);
    
    freq = \freq.kr(440);
    freq = freq * [
        0,
        env.linlin(0, 1, 0.1, 0.3)
    ].midiratio;
    
    sig = Saw.ar(freq).sum;
    sig = BLowPass.ar(
        sig,
        env.linexp(
            0, 1,
            velocity.linexp(0, 1, 80, 300),
            velocity.linexp(0, 1, 700, 4200),
        ) * \filt.kr(0.75),
        0.8
    );
    
    sig = Pan2.ar(sig, \pan.kr(0), \amp.kr(1) * env);
    
    Out.ar(\out.kr(0), sig)
}).add
)