+Meta_Pmono {
	<> {
		|other|
		^(

			Plazy({
				|e|
				Pmono(e.use({
					~instrument.value ?? { \default }
				}))
			}) <> other
		)
	}
}

+Meta_PmonoArtic {
	<> {
		|other|
		^(

			Plazy({
				|e|
				PmonoArtic(e.use({
					~instrument.value ?? { \default }
				}))
			}) <> other
		)
	}
}