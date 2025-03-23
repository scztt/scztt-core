DbFaderWarpUgen : Warp {
    //  useful mapping for amplitude faders
    map { arg value;
        // maps a value from [0..1] to spec range
        var	range = spec.maxval.dbamp - spec.minval.dbamp;
        var which = range > 0;
        ^Select.multiNew(
            which.rate,
            which,
            [
                ((1 - (1-value).squared) * range + spec.minval.dbamp).ampdb,
                (value.squared * range + spec.minval.dbamp).ampdb
            ]
        )
    }
    unmap { arg value;
        // maps a value from spec range to [0..1]
        var which = (spec.range > 0);
        ^Select.multiNew(
            which.rate,
            which,
            [
                1 - sqrt(1 - ((value.dbamp - spec.minval.dbamp) / (spec.maxval.dbamp - spec.minval.dbamp))),
                ((value.dbamp - spec.minval.dbamp) / (spec.maxval.dbamp - spec.minval.dbamp)).sqrt,
            ]
        )
    }
}


