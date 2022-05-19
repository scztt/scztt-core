Send {
	*kr {
		|name, signal|
		Out.kr(
			"%Out".format(name).asSymbol.kr(-1),
			signal * "%Amp".format(name).asSymbol.kr(0, spec:ControlSpec(0, 1, default:0))
		)
	}

	*ar {
		|name, signal|
		Out.ar(
			"%Out".format(name).asSymbol.kr(-1),
			signal * "%Amp".format(name).asSymbol.kr(0, spec:ControlSpec(0, 1, default:0))
		)
	}
}