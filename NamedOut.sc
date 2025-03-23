NamedOut {
    var <name, <amp;
    
    *new {
        |name, amp=true|
        ^super.newCopyArgs(name, amp)
    }
    
    <= {
        |ugen|
        var rate, outName, outAmp;
        rate = switch(ugen.rate) { \control } { \kr } { \audio } { \ar };
        
        ^Out.perform(
            rate, 
            this.prOutBusControl,
            this.prAmpControl * ugen
        );
    }
    
    prOutBusControl {
        ^(
            name !? {
                "out_%".format(name)
            } ?? {
                "out"
            }
        ).asSymbol.kr(-1);
    }
    
    prAmpControl {
        if (amp.not) {
            ^1
        };
        
        ^(
            name !? {
                "amp_%".format(name)
            } ?? {
                "amp"
            }
        )
        .asSymbol
        .kr(1.0)
        .lag(ControlDur.ir * 2);
    }
}
