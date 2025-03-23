Pfinish : Pfunc {
    *new {
        |func|
        ^super.new({
            | e |
            e[\finish].addFunc({
                ~pluckFreq = ~detunedFreq.value.clump(2)
            })  
        });
    }
}

