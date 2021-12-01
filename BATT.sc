BATT : SAMP {
	classvar <>root, <>extensions;
	*initClass {
		root = "/Users/Shared/Battery 4 Factory Library/Samples/Drums/";
		extensions = ["wav", "aiff", "aif", "flac"];
	}

	*clap { |name="*"|		^BATT("Clap" +/+ name) }
	*combo { |name="*"|		^BATT("Combo" +/+ name) }
	*cymbal { |name="*"|	^BATT("Cymbal" +/+ name) }
	*handDrum { |name="*"|	^BATT("Hand Drum" +/+ name) }
	*hihat { |name="*"|		^BATT("HiHat" +/+ name) }
	*kick { |name="*"|		^BATT("Kick" +/+ name) }
	*mallet { |name="*"|	^BATT("Mallet Drum" +/+ name) }
	*metallic { |name="*"|	^BATT("Metallic" +/+ name) }
	*shaker { |name="*"|	^BATT("Shaker" +/+ name) }
	*snare { |name="*"|		^BATT("Snare" +/+ name) }
	*tom { |name="*"|		^BATT("Tom" +/+ name) }
	*wooden { |name="*"|	^BATT("Wooden" +/+ name) }
}