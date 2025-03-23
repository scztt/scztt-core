+UGen {
    fadeChange {
        |func, fadeTime=1, fadeClass=(XFade2)|
        ^[this].fadeChange(func, fadeTime, fadeClass)
    }
    fadeTrig {
        |func, fadeTime=1, fadeClass=(XFade2)|
        ^[this].fadeTrig(func, fadeTime, fadeClass)
    }
    fadeSteps {
        |func, stepSize=1, fadeClass=(LinXFade2), warp=0, debugFunc|
        ^[this].fadeSteps(func, stepSize, fadeClass, warp, debugFunc)
    }
}

+SimpleNumber {
    fadeChange {
        |func, fadeTime=1, fadeClass=(XFade2)|
        ^[this].fadeChange(func, fadeTime, fadeClass)
    }
    fadeTrig {
        |func, fadeTime=1, fadeClass=(XFade2)|
        ^[this].fadeTrig(func, fadeTime, fadeClass)
    }
    fadeSteps {
        |func, stepSize=1, fadeClass=(LinXFade2), warp=0, debugFunc|
        ^[this].fadeSteps(func, stepSize, fadeClass, warp, debugFunc)
    } 
}


+ArrayedCollection {
    fadeChange {
        |func, fadeTime=1, fadeClass=(XFade2)|
        var trig, a, b, sig;
        var meth = this.detect({ |s| s.rate == \audio }).notNil.if(\ar, \kr);
        
        trig = Trig.perform(meth, Changed.perform(meth, this).sum, fadeTime);
        trig = ToggleFF.perform(meth, trig);
        
        a = Latch.perform(meth, this, 1 - trig);
        b = Latch.perform(meth, this, trig);
        a = func.value(*a);
        b = func.value(*b);
        
        ^fadeClass.perform(a.rate.switch(\audio, \ar, \kr),
            a,
            b,
            Delay1.perform(meth, Slew.perform(meth, trig, 1 / fadeTime, 1 / fadeTime)).linlin(0, 1, -1, 1)
        );
    }
    
    fadeTrig {
        |func, fadeTime=1, fadeClass=(XFade2), values|
        var trig, a, b, sig;
        var meth = this.detect({ |s| s.rate == \audio }).notNil.if(\ar, \kr);
        
        trig = Trig.perform(meth, this.sum, fadeTime);
        trig = ToggleFF.perform(meth, trig);
        
        a = Latch.perform(meth, this, 1 - trig);
        b = Latch.perform(meth, this, trig);
        a = func.value(*a);
        b = func.value(*b);
        
        ^fadeClass.perform(a.rate.switch(\audio, \ar, \kr),
            a,
            b,
            Delay1.perform(meth, Slew.perform(meth, trig, 1 / fadeTime, 1 / fadeTime)).linlin(0, 1, -1, 1)
        );
    }
    
    fadeSteps {
        |func, stepSize=1, fadeClass=(LinXFade2), warp=0, debugFunc|
        var changed, a, b, aIndex, bIndex, sig, input, inputOffset, inputBase, inputA, inputB, fade, isEvenStep;
        var meth = this.detect({ |s| s.rate == \audio }).notNil.if(\ar, \kr);
        var inputRate;
        
        inputRate = this.rate.switch(
            \audio, \ar,
            \control, \kr,
            \scalar, \kr
        );
        
        input = this.collect {
            |v|
            if (v.rate == \scalar) {
                DC.kr(v)
            } {
                v
            }
        };
        input = input / stepSize;
        
        inputOffset = (input % 2.0);
        inputBase = input - inputOffset;
        
        aIndex = stepSize * (inputBase + 1.0);
        bIndex = stepSize * (inputBase + 1.0 + (inputOffset - 1.0).sign);
        
        b = func.(aIndex);
        a = func.(bIndex);
        
        fade = inputOffset.fold(0.0, 1.0);
        fade = ControlSpec(-1, 1, warp).map(fade);
        
        fade = Select.perform(
            inputRate,
            0
                + (Changed.perform(inputRate, aIndex.asRate(inputRate)) * 1)
                + (Changed.perform(inputRate, bIndex.asRate(inputRate)) * 2),
            [fade, -1, 1].asRate(inputRate)
        );
        
        debugFunc !? {
            debugFunc.(
                input * stepSize,
                inputBase,
                inputOffset,
                fade,
                stepSize * (inputBase + 1.0 + (inputOffset - 1.0).sign),
                stepSize * (inputBase + 1.0),
            )
        };
        
        if (fadeClass.isKindOf(Function)) {    
            ^fadeClass.value(
                a,
                b,
                fade                
            )
        };
        
        ^fadeClass.perform(
            a.rate.switch(\audio, \ar, \kr),
            a,
            b,
            fade
        );
    }
}

