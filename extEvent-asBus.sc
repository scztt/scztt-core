+Event {
    collectInPlace { 
        |function|
        this.keysValuesDo { |key, value, i| this.put(key, function.value(value, key)) }
    }
    
    asBus {
        ^this.use {
            switch (
                ~type,
                
                \audioBus, {
                    Bus(\audio, ~out, ~numChannels ?? { ~channels } ?? { 1 }, ~server)
                },
                \controlBus, {
                    Bus(\control, ~out, ~numChannels ?? { ~channels } ?? { 1 }, ~server)
                },
                \outputBus, {
                    Bus(\audio, ~out, ~numChannels ?? { ~channels } ?? { 1 }, ~server)
                },
                nil
            )
        }
    }
}

