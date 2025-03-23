NI_SAMP : SAMP {
	*clap { |name="*"|		^this.new("Clap" +/+ name) }
	*combo { |name="*"|		^this.new("Combo" +/+ name) }
	*cymbal { |name="*"|	^this.new("Cymbal" +/+ name) }
	*handDrum { |name="*"|	^this.new("Hand Drum" +/+ name) }
	*hihat { |name="*"|		^this.new("HiHat" +/+ name) }
	*kick { |name="*"|		^this.new("Kick" +/+ name) }
	*mallet { |name="*"|	^this.new("Mallet Drum" +/+ name) }
	*metallic { |name="*"|	^this.new("Metallic" +/+ name) }
	*shaker { |name="*"|	^this.new("Shaker" +/+ name) }
	*snare { | name="*"|		^this.new("Snare" +/+ name) }
	*tom { |name="*"|		^this.new("Tom" +/+ name) }
	*wooden { |name="*"|	^this.new("Wooden" +/+ name) }
	*perc { |name="*"|		^this.new("Percussion" +/+ name) }
}

BATT : NI_SAMP {
	classvar <>root, <>extensions;
	*initClass {
		this.root = "/Users/Shared/Battery 4 Factory Library/Samples/Drums/";
		this.extensions = ["wav", "aiff", "aif", "flac"];
	}
}

ARCANE : NI_SAMP {
	classvar <>root, <>extensions;
	*initClass {
		root = "/Users/Shared/Arcane Attic Library/Samples/Drums/";
		extensions = ["wav", "aiff", "aif", "flac"];
	}
}

ASTRAL : NI_SAMP {
	classvar <>root, <>extensions;
	*initClass {
		root = "/Users/Shared/Astral Flutter Library/Samples/Drums/";
		extensions = ["wav", "aiff", "aif", "flac"];
	}
}

CARBON : NI_SAMP {
	classvar <>root, <>extensions;
	*initClass {
		root = "/Users/Shared/Carbon Decay Library/Samples/Drums/";
		extensions = ["wav", "aiff", "aif", "flac"];
	}
}

CAVERN : NI_SAMP {
	classvar <>root, <>extensions;
	*initClass {
		root = "/Users/Shared/Cavern Floor Library/Samples/Drums/";
		extensions = ["wav", "aiff", "aif", "flac"];
	}
}

DECODED : NI_SAMP {
	classvar <>root, <>extensions;
	*initClass {
		root = "/Users/Shared/Decoded Forms Library/Samples/Drums/";
		extensions = ["wav", "aiff", "aif", "flac"];
	}
}

FORGE : NI_SAMP {
	classvar <>root, <>extensions;
	*initClass {
		root = "/Users/Shared/Grey Forge Library/Samples/Drums/";
		extensions = ["wav", "aiff", "aif", "flac"];
	}
}