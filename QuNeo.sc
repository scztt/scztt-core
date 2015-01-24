
QuNeo {
	var midiIn, midiOut,
	<>pads;

	*new {
		| midiOut, midiIn |

		MIDIClient.initialized.not.if({ MIDIClient.init });
		MIDIIn.connectAll();

		try {
			midiIn = midiIn ? MIDIIn.findPort("QUNEO", "QUNEO");
			midiOut = midiOut ? MIDIOut.newByName("QUNEO", "QUNEO");
		} {
			midiOut = DummyMIDI();
			midiIn = DummyMIDI();
			"'QuNeo' not found - using dummy midi devices.".warn;
		};

		midiOut.latency = 0;
		^super.newCopyArgs(midiIn, midiOut).init();
	}

	init {
		pads = 16.collect {
			|i|
			var padDevice, pad;
			padDevice = QuNeoPadDevice(
				midiInUid: midiIn.uid,
				midiOut: midiOut,
				note: i + 36,
				pres: (i * 3) + 23,
				x: (i * 3) + 23 + 1,
				y: (i * 3) + 23 + 2,
				padRed: (i * 2),
				padGreen: (i * 2) + 1,
				channel: 0
			);
			pad = QuNeoPad(padDevice).default();
		};

		// We want rows top-to-bottom, so flop around:
		pads = pads.clump(4).reverse.flatten;
	}

	padRows {
		|n|
		^pads.clump(4);
	}

	padCols {
		|n|
		^pads.clump(4).flop;
	}

	connect {
		pads.do(_.connect);
	}

	disconnect {
		pads.do(_.disconnect);
	}

	free {
		this.disconnect();
		pads.do(_.free());
	}
}

QuNeoInput {
	var <device, <connected = false, <mode=nil;

	*new {
		arg device, mode;
		^super.newCopyArgs(device).init(mode);
	}

	init {}
}

QuNeoPad : QuNeoInput {
	var <lit = false,
	<noteOnActions, <noteOffActions,
	defaultNoteCV, defaultPresCV, defaultXYCV,
	noteCV, xCV, yCV, presCV,
	<>toggle = false, toggleState = false;

	init {
		| mode |
		defaultNoteCV = CV(ControlSpec(0, 1));
		defaultNoteCV.action_({
			arg cv;
			if (cv.value.value > 0) {
				this.lit_(true);
			} {
				this.lit_(false);
			}
		});

		defaultPresCV = CV(ControlSpec(0, 1));
		defaultXYCV = CV(ControlSpec(-1, 1));

		noteOnActions = Set();
		noteOffActions = Set();

		^super.init(mode);
	}

	default {
		this.noteCV = defaultNoteCV;
		this.presCV = defaultPresCV;
		this.xCV = defaultXYCV;
		this.yCV = defaultXYCV;
	}

	mode_{}

	connect {
		connected = true;

		device.noteOnFunc = { |val| this.onNoteEvent(true, val / 127) };
		device.noteOffFunc = { |val| this.onNoteEvent(false, val / 127) };

		device.presFunc = { |val| this.onCCEvent(presCV, val / 127) };
		device.xFunc = { |val| this.onCCEvent(xCV, val / 127) };
		device.yFunc = { |val| this.onCCEvent(yCV, val / 127) };

		noteCV.changed(\synch);
		presCV.changed(\synch);
		xCV.changed(\synch);
		yCV.changed(\synch);
	}

	disconnect {
		this.lit = false;
		device.noteOnFunc = nil;
		device.noteOffFunc = nil;
		device.presFunc = nil;
		device.xFunc = nil;
		device.yFunc = nil;

		connected = false;
	}

	clear {
		noteCV = xCV = yCV = presCV = nil;
		noteOnActions.clear();
		noteOffActions.clear();
	}

	free {
		this.disconnect();
		this.clear();
		device.free();
	}

	onNoteEvent {
		arg on, velocity;

		if (noteCV.notNil) {
			if (toggle) {
				if (on) {
					noteCV.value = toggleState.if(0, velocity);
				}
			} {
				if (on) {
					noteCV.input = velocity;
				} {
					noteCV.input = 0;
				}
			};
		};
	}

	onCCEvent {
		| cv, value |
		if (cv.notNil) {
			cv.input = value;
		}
	}

	lit_{
		arg val;
		lit = val.asBoolean;
		if (connected) {
			if (lit) {
				device.on();
			} {
				device.off();
			}
		}
	}

	noteCV_{
		arg inCV;

		noteCV.removeDependant(this);
		noteCV = inCV;

		if (noteCV.notNil) {
			noteCV.addDependant(this);
			noteCV.changed(\synch);
		};

		^noteCV;
	}

	presCV_{
		arg inCV;

		presCV.removeDependant(this);
		presCV = inCV;

		if (presCV.notNil) {
			presCV.addDependant(this);
			presCV.changed(\synch);
		};

		^presCV;
	}

	xCV_{
		arg inCV;

		xCV.removeDependant(this);
		xCV = inCV;

		if (xCV.notNil) {
			xCV.addDependant(this);
			xCV.changed(\synch);
		};

		^xCV;
	}

	yCV_{
		arg inCV;

		yCV.removeDependant(this);
		yCV = inCV;

		if (yCV.notNil) {
			yCV.addDependant(this);
			yCV.changed(\synch);
		};

		^yCV;
	}

	noteCV { ^(noteCV ?? { noteCV = defaultNoteCV.copy() }) }
	presCV { ^(presCV ?? { presCV = defaultPresCV.copy() }) }
	xCV { ^(xCV ?? { xCV = defaultXYCV.copy() }) }
	yCV { ^(yCV ?? { yCV = defaultXYCV.copy() }) }

	update {
		arg who, what, inCV;

		if ((what == \synch) && (inCV == noteCV)) {
			if (noteCV.input != 0) {
				toggleState = true;
				this.noteOnActions.do { |f| f.value(this, inCV) };
			} {
				toggleState = false;
				this.noteOffActions.do { |f| f.value(this, inCV) };
			}
		}
	}
}

QuNeoPadDevice {
	var <midiInUid, <midiOut, <noteM, <presM, <xM, <yM, padGreenM, padRedM, <channelM,
	<noteOnFunc, <noteOffFunc, <presFunc, <xFunc, <yFunc
	;

	*new {
		| midiInUid, midiOut, note, pres, x, y, padGreen, padRed, channel=1 |
		^super.newCopyArgs(midiInUid, midiOut, note, pres, x, y, padGreen, padRed, channel).init;
	}

	init {

	}

	free {
		this.off();
		noteOnFunc.free;
		noteOffFunc.free;
		presFunc.free;
		xFunc.free;
		yFunc.free;
		noteOnFunc = noteOffFunc = presFunc = xFunc = yFunc = nil;

	}

	noteOnFunc_{
		| func |
		noteOnFunc.free;
		if (func.isNil) {
			noteOnFunc = nil;
		} {
			noteOnFunc = MIDIFunc.noteOn(func, noteM, channelM, midiInUid);
		}
	}

	noteOffFunc_{
		| func |
		noteOffFunc.free;
		if (func.isNil) {
			noteOffFunc = nil;
		} {
			noteOffFunc = MIDIFunc.noteOff(func, noteM, channelM, midiInUid);
		}
	}

	presFunc_{
		| func |
		presFunc.free;
		if (func.isNil) {
			presFunc = nil;
		} {
			presFunc = MIDIFunc.cc(func, presM, channelM, midiInUid);
		}
	}

	xFunc_{
		| func |
		xFunc.free;
		if (func.isNil) {
			presFunc = nil;
		} {
			presFunc = MIDIFunc.cc(func, xM, channelM, midiInUid);
		}
	}

	yFunc_{
		| func |
		yFunc.free;
		if (func.isNil) {
			presFunc = nil;
		} {
			presFunc = MIDIFunc.cc(func, yM, channelM, midiInUid);
		}
	}

	setRed {
		|val|
		if (val.isCollection) {
			NotYetImplementedError().throw;
		} {
			midiOut.noteOn(0, padRedM, val * 127.0);
		}
	}

	setGreen {
		|val|
		if (val.isCollection) {
			NotYetImplementedError().throw;
		} {
			midiOut.noteOn(0, padGreenM, val * 127.0);
		}
	}

	on {
		this.setRed(1);
	}

	off {
		this.setRed(0);
		this.setGreen(0);
	}

	printOn {
		|stream|
		stream << "%(midiInUid: %, midiOut: %, noteM: %, presM: %, xM: %, yM: %, padGreenM: %, padRedM: %, channelM: %)".format(
			this.class.asString, midiInUid, midiOut, noteM, presM, xM, yM, padGreenM, padRedM, channelM
		)
	}
}
