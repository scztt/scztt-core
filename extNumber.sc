+ Number {
	asEnv {
		|dur=1|
		^Env([this, this], [dur])
	}
}

+ Array {
	asEnv {
		|dur=1|
		^Env(this, 1 ! (this.size - 1)).duration_(dur)
	}
}

+ Env {
	asEnv {
		^this
	}
}