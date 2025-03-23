


(
{
    var sig;
    sig = LFDNoise3.kr(2).exprange(100, 2000).fadeSteps({
        |f|
        SinOsc.ar(f);
    }, stepSize:200)
}.play
)


(
{
    var sig, f;
    f = TExpRand.kr(100, 2000, Impulse.kr(4));
    sig = f.fadeChange({
        |f|
        SinOsc.ar(f);
    }, fadeTime: 0.2)
}.play
)


(
{
    var sig, f;
    f = LFDNoise3.kr(2).range(100, 2000);
    f = f * Impulse.kr(4);
    sig = f.fadeTrig({
        |f|
        SinOsc.ar(f);
    }, fadeTime: 0.2)
}.play
    
)


(
{
    var sig, swapTrig, indices;
    
    swapTrig = Impulse.kr(1);
    indices = Dseq([Dshuf((0..19))], inf);
    
    // sig is 20 channels
    sig = sig.size.collect {
        |i|
        Demand.kr(swapTrig, 0, indices).poll
            .fadeChange({
                |index|
                Select.ar(index, sig);
            }, 1)
    };  
    sig = sig[0..2]
}.play;

)

TScramble
        
        (
            {
                var a, freq, trig;
                a = Dseq([Dshuf([1, 3, 2, 7, 8.5], 3)], inf);
                trig = Impulse.kr(MouseX.kr(1, 40, 1));
                freq = Demand.kr(trig, 0, a) * 30 + 340;
                SinOsc.ar(freq) * 0.1
                    
            }.play;
        )

(
delays = [0, 1]
    .resamp1(20)
    .lincurve(0, 1, 0.1, 4, 4);
delays = Dseq([Dshuf(delays)], inf);

sig = delays.size.collect {
    Demand.kr(trig, 0, delays).fadeChange {
        |d|
        DelayC.ar(sig, 10, d);
    }
};
)


(
(
    b = Button().states_([["remove the bugs", Color.black, Color.grey]]);
    w = View(nil, 128@128)
        .minHeight_(128)
        .layout_(
            VLayout(
                HLayout(StaticText().string_("bugs"), b), 
                nil
            )
        )
        .front
        .onResize_{ 
            var d = max(b.bounds.height, b.bounds.width);
            b.resizeTo(d, d);
        }
)

)
