
QuNeo {
	var midiIn, midiOut,
	<>pads, connections;

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
				midiInUid: 	midiIn.uid,
				midiOut: 	midiOut,
				note: 		i + 36,
				pres: 		(i * 3) + 23,
				x: 			(i * 3) + 23 + 1,
				y: 			(i * 3) + 23 + 2,
				padRed: 	(i * 2),
				padGreen: 	(i * 2) + 1,
				channel: 	0
			);
			pad = QuNeoPad(padDevice).default();
		};
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
		connections = ConnectionList();

		pads.do {
			|pad, i|
			pad.connect();
			connections.addAll([
				pad.signal(\xypres).connectTo(
					this.methodSlot("changed(\\xypres, %, object, *args)".format(i))),
				pad.signal(\on).connectTo(
					this.methodSlot("changed(\\on, %, object, *args)".format(i))),
				pad.signal(\off).connectTo(
					this.methodSlot("changed(\\off, %, object, *args)".format(i)))
			]);
		}
	}

	disconnect {
		pads.do(_.disconnect);
		connections.free.clear;
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
	var parent;

	var <lit = false,
	<noteOnActions, <noteOffActions,
	defaultNoteCV, defaultPresCV, defaultXYCV,
	noteCV, xCV, yCV, presCV, noteC,
	<>toggle = false, toggleState = false;

	var <events;

	init {
		| mode |
		defaultNoteCV = NumericControlValue(spec:ControlSpec(0, 1));
		defaultPresCV = NumericControlValue(spec:ControlSpec(0, 1));
		defaultXYCV = NumericControlValue(spec:ControlSpec(-1, 1));

		this.connectOnNoteChanged(defaultNoteCV);

		noteOnActions = Set();
		noteOffActions = Set();

		events = ();

		^super.init(mode);
	}

	default {
		this.noteCV 	= defaultNoteCV.copy;
		this.presCV 	= defaultPresCV.copy;
		this.xCV 		= defaultXYCV.copy;
		this.yCV		= defaultXYCV.copy;
	}

	connectOnNoteChanged {
		|cv|
		noteC !? { noteC.free };
		noteC = cv.signal(\input).connectTo(this.methodSlot("onNoteChanged(value)"))
	}

	onNoteChanged {
		|value|
		if (value > 0) {
			this.lit_(true);
		} {
			this.lit_(false);
		}
	}

	mode_{}

	connect {
		connected = true;

		device.noteOnFunc 	= { |val| this.onNoteEvent(true, val / 127) };
		device.noteOffFunc 	= { |val| this.onNoteEvent(false, val / 127) };

		device.presFunc 	= { |val| this.onCCEvent(\pressure, presCV, val / 127) };
		device.xFunc 		= { |val| this.onCCEvent(\x, xCV, val / 127) };
		device.yFunc 		= { |val| this.onCCEvent(\y, yCV, val / 127) };

		noteCV.value 		= noteCV.value;
		presCV.value 		= presCV.value;
		xCV.value 			= xCV.value;
		yCV.value 			= yCV.value;
	}

	disconnect {
		this.lit 			= false;
		device.noteOnFunc 	= nil;
		device.noteOffFunc 	= nil;
		device.presFunc		= nil;
		device.xFunc 		= nil;
		device.yFunc 		= nil;

		noteC.disconnect();

		connected = false;
	}

	clear {
		noteCV = xCV = yCV = presCV = nil;
		noteOnActions.clear();
		noteOffActions.clear();
		noteC.free();
	}

	free {
		this.disconnect();
		this.clear();
		device.free();
		noteC.free();
	}

	onNoteEvent {
		arg on, velocity, input;

		if (toggle) {
			if (on) {
				input = toggleState.if(0, velocity);
			}
		} {
			if (on) {
				input = velocity;
			} {
				input = 0;
			}
		};

		noteCV !? { noteCV.input = input };

		if (input > 0) {
			this.changed(\on, input, xCV.value, yCV.value, presCV.value);
		} {
			this.changed(\off, input, xCV.value, yCV.value, presCV.value);
		}
	}

	onCCEvent {
		| name, cv, value |

		if (cv.notNil) {
			cv.input = value;
		};

		// Send x,y,pres notification with pressure, since it comes last
		if (cv == presCV) {
			this.changed(\xypres, xCV.value, yCV.value, presCV.value);
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
		if (inCV != noteCV) {
			noteCV = inCV;
			this.connectOnNoteChanged(noteCV);
		};

		^noteCV;
	}

	presCV_{
		arg inCV;

		if (inCV != presCV) {
			presCV = inCV;
		};

		^presCV;
	}

	xCV_{
		arg inCV;

		if (inCV != xCV) {
			xCV = inCV;
		};

		^xCV;
	}

	yCV_{
		arg inCV;

		if (inCV != yCV) {
			yCV = inCV;
		};

		^yCV;
	}

	noteCV 	{ ^(noteCV ?? { noteCV = defaultNoteCV.copy() }) }
	presCV 	{ ^(presCV ?? { presCV = defaultPresCV.copy() }) }
	xCV 	{ ^(xCV ?? { xCV = defaultXYCV.copy() }) }
	yCV 	{ ^(yCV ?? { yCV = defaultXYCV.copy() }) }
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
