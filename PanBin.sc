PanBin : MultiOutUGen {
    *ar { arg in, maxDelay, delay;
        ^[
            DelayC.ar(in[0], maxDelay, delay.clip(-inf, 0).abs * 0.0006),
            DelayC.ar(in[1], maxDelay, delay.clip(0, inf) * 0.0006),
        ]
    }
}


