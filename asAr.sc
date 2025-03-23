+Object {
    asRate {
        |rate|
        if ((rate == \ar) or: { rate == \audio }) {
            ^this.asAr
        };
        if ((rate == \kr) or: { rate == \control }) {
            ^this.asKr
        };
    }
    
    asAr {
        if (this.rate != \audio) {
            ^K2A.ar(this)
        }
    }
    
    asKr {
        if (this.rate == \audio) {
            A2K.ar(this)
        }
    }
}

+Collection {
    asAr {
        ^this.collect(_.asAr)
    }
    
    asKr {
        ^this.collect(_.asKr)
    }
}
