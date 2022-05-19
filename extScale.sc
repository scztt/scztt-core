+Scale {
	at {
		|...index|
		if (index.size == 1) {
			^tuning.at(degrees.wrapAt(index))
		} {
			^index.collect({ |i| tuning.at(degrees.wrapAt(i)) })
		}
	}
}