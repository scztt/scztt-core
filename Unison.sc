+UGen {
    spread {
        |count, curve=0|
        var resampleFunc, inputs, rate;
        var indexFunc = {
            |i|
            i.lincurve(
                0, count - 1 / 2, 
                0, 0.5,
                curve
            ) 
            + i.lincurve(
                count - 1 / 2, count - 1,
                0, 0.5,
                curve.neg
            )
        };    
        inputs = this.inputs.collect(_.value);
        
        rate = IdentitySet();
        rate.addAll(inputs.collect(_.rate));
        rate.add(curve.rate);
        rate = rate.includes(\audio).if(\audio, {
            rate.includes(\control).if(\control, {
                \scalar
            })
        });
        
        ^count.collect {
            |i|
            this.deepCopy.addToSynth.inputs_(
                inputs.collect {
                    |value, j|
                    if (value.isKindOf(Env)) {
                        value.times = value.times.normalizeSum(1);
                        
                        rate.switch(
                            \audio, {
                                value = IEnvGen.ar(value, indexFunc.value(DC.ar(i)))
                            },
                            \control, {
                                value = IEnvGen.kr(value, indexFunc.value(DC.kr(i)))
                            },
                            \scalar, {
                                value = value.at(indexFunc.value(i));
                            }
                        );
                        
                        // hacky, but fix a non-scalar from our original ugen
                        if (i == 0) {
                            this.inputs[j] = 0;
                        }
                    };
                    value.poll;
                }
            );
        }
    }
}

+Collection {
    splay {
        |channels=2|
        
        if (channels == 1) {
            ^this.mean // @TODO wrong
        };
        
        if (channels == 2) {
            ^Splay.ar(this)
        };
        
        ^channels.collect {
            |i|
            // @TODO multi-rate
            SelectX.ar(
                i.linlin(0, channels - 1, 0, this.size - 1),
                this
            )
        }
    }
    
    splaylin {
        |channels=2|
        
        if (channels == 1) {
            ^this.mean
        };
        
        ^this.resamp1(channels)
    }
    
    spread {
        |count, curve=0|
        var resampleFunc, inputs, rate;
        var indexFunc = {
            |i|
            i.lincurve(
                0, count - 1 / 2, 
                0, 0.5,
                curve
            ) 
            + i.lincurve(
                count - 1 / 2, count - 1,
                0, 0.5,
                curve.neg
            )
        };    
        inputs = this.collect(_.inputs).collect(_.value);
        
        rate = IdentitySet();
        rate.addAll(inputs.collect(_.rate));
        rate.add(curve.rate);
        rate = rate.includes(\audio).if(\audio, {
            rate.includes(\control).if(\control, {
                \scalar
            })
        });
        rate.postln;
        
        ^count.collect {
            |i|
            var index = indexFunc.value(i);
            this[0].deepCopy.addToSynth.inputs_(
                rate.switch(
                    \audio, {
                        LinSelectX.ar((inputs.size - 1) * DC.ar(index), DC.ar(inputs))
                    },
                    \control, {
                        LinSelectX.kr((inputs.size - 1) * index, inputs)
                    },
                    \scalar, {
                        inputs.blendAt((inputs.size - 1) * index)
                    }
                )
            );
        }
    }
}
// if (ugen.isArray) {
// } {
//     inputs = ugen.inputs.collect(_.value);
//     ugen = count.collect {
//         |i|
//         var index = DC.kr(i).lincurve(
//             0, count - 1 / 2, 
//             0, 0.5,
//             curve
//         ) + DC.kr(i).lincurve(
//             count - 1 / 2, count - 1,
//             0, 0.5
//                 curve.neg
//         );
//         ugen.deepCopy.addToSynth.inputs = inputs.collect {
//             |value|
//             if (value.isKindOf(Env)) {
//                 value = value.at(index)
//             };
//             if (value.isKindOf(Collection)) {
//                 value = LinSelectX.ar((inputs.size - 1) * index, value)
//             };
//             value;
//         }
//     };
// };

// ugen;

// }



















