+Scale {
    at {
        |...index|
        if (index.size == 1) {
            ^tuning.at(degrees.wrapAt(index))
        } {
            ^index.collect({ |i| tuning.at(degrees.wrapAt(i)) })
        }
    }
    
    octDegreeToMidi {
        |octave, degree|
        var steps = this.tryPerform(\stepsPerOctave) ?? 12;
        var note = degree.degreeToKey(this, steps);
        var midinote = (octave * steps) + note;
        ^midinote;
    }
    
    midiToOctDegree {
        |midinote|
        var steps = this.tryPerform(\stepsPerOctave) ?? 12;
        var root = midinote.trunc(steps);
        var key = midinote % steps;
        
        var degrees = (steps.neg + this.degrees) ++ this.degrees ++ (steps + this.degrees);
        var tuningArray = (steps.neg + tuning.tuning) ++ tuning.tuning ++ (steps + tuning.tuning);
        var octave = root / steps;
        var tuningIndex, degree;
        
        tuningIndex = tuningArray.indexInBetween(key);
        tuningIndex = (tuningIndex - tuning.tuning.size) % tuning.tuning.size;
        
        degree = degrees.indexInBetween(tuningIndex);
        degree = (degree - this.size) % this.size;
        
        ^(octave: octave, degree: degree)
    }
}


+SequenceableCollection {
    octDegreeToMidi {
        |octave, degree|
        var steps = this.tryPerform(\stepsPerOctave) ?? 12;
        var note = degree.degreeToKey(this, steps);
        var midinote = (octave * steps) + note;
        ^midinote;
    }
    
    midiToOctDegree {
        |midinote|
        var steps = this.tryPerform(\stepsPerOctave) ?? 12;
        var root = midinote.trunc(steps);
        var key = midinote % steps;
        var degree = this.indexInBetween(key);
        var octave = root / steps;
        
        degree = (degree - this.size) % this.size;
        
        ^(octave: octave, degree: degree)
    }
}

+SimpleNumber {
    degreeToKey { |scale, stepsPerOctave|
        var scaleDegree = this.round.asInteger;
        var accidental = (this - scaleDegree);
        ^scale.performDegreeToKey(scaleDegree, stepsPerOctave, accidental)
    }
}

// +Scale {
//     performDegreeToKey { | scaleDegree, stepsPerOctave, accidental |
//         if (scaleDegree != scaleDegree.floor) {
//             ^blend(
//                 this.performDegreeToKey(scaleDegree.floor, stepsPerOctave, accidental),
//                 this.performDegreeToKey(scaleDegree.ceil, stepsPerOctave, accidental),
//                 scaleDegree - scaleDegree.floor
//             )
//         };

//         if (accidental.notNil) {
//             if (accidental.isNegative) {
//                 scaleDegree = scaleDegree - 1;
//                 accidental = 1.0 - accidental.abs;
//             };
//             ^this.performDegreeToKey(scaleDegree, stepsPerOctave).blend(
//                 this.performDegreeToKey(scaleDegree + 1, stepsPerOctave),
//                 accidental
//             )
//         } {
//             stepsPerOctave = stepsPerOctave ? tuning.stepsPerOctave;
//             ^(stepsPerOctave * (scaleDegree div: this.size)) + this.wrapAt(scaleDegree)
//         }
//     }
// }
