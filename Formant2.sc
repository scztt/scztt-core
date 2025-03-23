Formant2 {
    *ar {
        |freq, formant, width=0, twist=0, phase=0| 
        var a, b;
        var oscPhase, mod;
        
        ^[freq, formant, width, twist, phase].flop.collect {
            |params|
            var freq, formant, width, twist, phase;
            #freq, formant, width, twist, phase = params;
            
            oscPhase = Phasor.ar(1, freq / SampleRate.ir, 0, 1, resetPos: phase);
            
            width = width + 1;
            
            a = SinOsc.ar(
                0,
                2pi * (
                    -0.25 + (oscPhase * width).clip(0, 1)
                ).wrap(0, 1)
            ).range(0, 1);
            
            b = SinOsc.ar(
                twist,
                2pi * (
                    (oscPhase * (formant / freq))
                ).wrap(0, 1)
            );
            
            a*b
        }  
    }
}

Formant3 {
    *ar {
        |freq, formant, width=0, widthPow=1, modFreq=1, modAmt=0, freqNoise=0, noiseAmt=0, noiseFreq=200, twist=0, phase=0| 
        var a, b, sig;
        var oscPhase, bFreq, mod, noise;
        
        ^[freq, formant, width, twist, phase].flop.collect {
            |params|
            var freq, formant, width, twist, phase;
            #freq, formant, width, twist, phase = params;
            
            oscPhase = Phasor.ar(1, freq / SampleRate.ir, 0, 1, resetPos: phase);
            
            width = width + 1;
            
            a = SinOsc.ar(
                0,
                2pi * (
                    -0.25 + (oscPhase * width).clip(0, 1)
                ).wrap(0, 1)
            ).range(0, 1);
            a = a.pow(widthPow);
            
            bFreq = (formant / freq);
            
            mod = modFreq.fadeSteps({
                |modFreq|
                modFreq = modFreq.unbubble;
                modFreq = (modFreq < 0).if(
                    1 / (modFreq.abs + 1),
                    modFreq
                );
                SinOsc.ar(
                    0,
                    2pi * (modFreq * oscPhase * bFreq).wrap(0, 1)
                ).range(0, 1)
            }, fadeClass:XFade2);
            
            noise = PinkNoise.ar(noiseAmt);
            noise = BLowPass4.ar(noise, noiseFreq);
            
            b = SinOsc.ar(
                twist,
                2pi * ((oscPhase * bFreq) + (mod * modAmt)).wrap(0, 1)
            );
            
            sig = a * (b + noise);
        }  
    }
}

Harmonic {
    *ar {
        |freq, harmonic=0, oscillator|
        ^[freq, harmonic, oscillator].flop.unbubble(1).collect {
            |args, i|
            this.arSingle(i, *args)
        }
    }
    
    *arSingle {
        |index, freq, harmonic, oscillator|        
        oscillator = oscillator ?? { { |f| SinOsc.ar(f) } };
        
        ^harmonic.fadeSteps({
            |h|
            h = h.unbubble;
            oscillator.value(
                freq * (h > 0).if(1 + h, (1 + h.abs).reciprocal),
                index
            )
        }, 1);
    }
}
















