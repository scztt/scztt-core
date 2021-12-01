+Event {
	asBus {
		^this.use {
			switch (
				~type,

				\audioBus, {
					Bus(\audio, ~out, ~numChannels ? 1, ~server)
				},
				\controlBus, {
					Bus(\control, ~out, ~numChannels ? 1, ~server)
				},
				nil
			)
		}
	}
}