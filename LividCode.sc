// DummyMIDI {
// 	var dummy = 0;
//
// 	*new {
// 		^super.newCopyArgs();
// 	}
//
// 	noteOn {}
// 	noteOff {}
// 	control {}
// 	sysex {}
// 	cc { ^127 }
// 	channel {^16}
// 	latency_{
// 		dummy = 0;
// 	}
// }

LCSysex {
	var <midiOut, state, stateStack, baseSettings;

	*new {
		arg midiOut;
		^super.newCopyArgs(midiOut).init();
	}

	init {
		baseSettings = (
			\localControl: [false, false, false, false, false],
			\encoderRelative: true,
		);
		state = IdentityDictionary.newFrom(baseSettings);
		stateStack = List();
	}

	push {
		var newState = state.copy();
		stateStack.add(state);
		state = newState;
	}

	pop {
		var oldState, newState;
		if (stateStack.isEmpty.not, {
			oldState = state;
			state = stateStack.pop();
			this.resendAll();
		})
	}

	resendAll {
		var tmpState = state;
		state = ();
		tmpState.keysValuesDo({
			arg key, val;
			this.perform((key ++ "Send").asSymbol, *val);
		});
		state = tmpState;
	}

	ledState_{
		| state |
		var msg, stateMsg;
		msg = Int8Array[0xF0, 0x00, 0x01, 0x61, 0x04, 0x1F];
		stateMsg = Int8Array.newClear(64);
		32.do {
			|i|
			var byteA, byteB, knobState = state[i];
			byteA = 0x0
			+ (knobState[0] << 0)
			+ (knobState[1] << 1)
			+ (knobState[2] << 2)
			+ (knobState[3] << 3)
			+ (knobState[4] << 4)
			+ (knobState[5] << 5)
			+ (knobState[6] << 6);
			byteB = 0x0
			+ (knobState[7] << 0)
			+ (knobState[8] << 1)
			+ (knobState[9] << 2)
			+ (knobState[10] << 3)
			+ (knobState[11] << 4)
			+ (knobState[12] << 5);
			stateMsg[i * 2] = byteA;
			stateMsg[i * 2 + 1] = byteB;
		};
		midiOut.sysex(Int8Array.newFrom(msg ++ stateMsg ++ [0xf7]));
	}

	// Ring state
	ringState { ^state[\ringState] }

	ringState_{
		| states |
		if (state[\ringState] != states) {
			state[\ringState] = states;
			this.ringStateSend(*states);
		}
	}

	ringStateSend {
		|...states|
		var msg = Int8Array.newFrom(

			[0xF0, 0x00, 0x01, 0x61, 0x04, 0x32]

			++ Int8Array.newFrom(states.collect {
				|state|
				switch (state,
					\walk, 0x0,
					\fill, 0x1,
					\spread, 0x2,
					\eq, 0x3
				);
			})

			++ [0xf7]
		);
		midiOut.sysex(msg)
	}

	encoderSpeed { ^state[\encoderSpeed] }

	encoderSpeed_{
		arg normal, mod;
		if (state[\encoderSpeed] != [normal, mod]) {
			state[\encoderSpeed] = [normal, mod];
			this.encoderSpeedSend(normal, mod);
		}
	}

	encoderRelative { ^state[\encoderRelative] }

	encoderRelative_{
		arg relative;
		if (state[\encodersRelative] != relative) {
			state[\encodersRelative] = relative;
			this.encoderRelativeSend(relative);
		}
	}

	encoderRelativeSend {
		| value |
		var ccVal;
		if (value) {
			ccVal = 0x7f;
		} {
			ccVal = 0x00;
		};

		midiOut.sysex(Int8Array[
			0xF0, 0x00, 0x01, 0x61, 0x04, 0x11,
			ccVal, ccVal, ccVal, ccVal, ccVal, ccVal, ccVal, ccVal,
			0xF7
		]);
	}


	// Local Control
	// "https://docs.google.com/spreadsheet/ccc?key=0AjLDw8_l-X4rdHZkZzAwSnhFWmVCXzlDVERtWWhoRHc#gid=3"
	localControl { ^state[\localControl] }

	localControl_{
		arg ...args;
		var blob;
		blob = args;

		if (blob != state[\localControl], {
			state[\localControl] = blob;
			this.localControlSend(*blob);
		});
	}

	localControlSend {
		arg analog, relative, absolute, toggle, momentary;
		var ccVal;
		ccVal = 0
		+ (analog.if(1, 0) << 1)
		+ (relative.if(1, 0) << 2)
		+ (absolute.if(1, 0) << 3)
		+ (toggle.if(1, 0) << 4)
		+ (momentary.if(1, 0) << 5)
		+ (1 << 6);

		midiOut.control(15, 122, ccVal);
	}
}



LCDevice {
	classvar <initialized = false, debug = false, allConnected, currentId = 0;
	var <midiInUid, <midiOut, <knobs, <buttons, <settings;
	var <connected = false, <>log;

	*initClass {
		allConnected = IdentitySet();
	}

	*printConnected {
		if (allConnected.notEmpty) {
			Log(this).info("% LCDevices connected:", allConnected.size);

			allConnected.do({
				|lc|
				Log(this).info("\t- %", lc);
			})
		}
	}

	*new {
		arg midiOut, midiIn;

		MIDIClient.initialized.not.if({ MIDIClient.init });
		//try { MIDIClient.init };

		try {
			midiIn = (midiIn ? MIDIIn.findPort("Code", "Controls")).uid;
			midiOut = midiOut ? MIDIOut.newByName("Code", "Controls");
		} {
			midiOut = DummyMIDI();
			midiIn = DummyMIDI();
			"'Livid Control' not found - using dummy midi devices.".warn;
		};
		MIDIIn.connectAll();

		midiOut.latency = 0;
		^super.newCopyArgs(midiIn, midiOut).init();
	}

	init {
		var channel = 0;
		var knobCodes = (1..32);
		var buttonCodes = (33..45);

		log = Log("LCDevice-%".format(currentId).asSymbol);
		currentId = currentId + 1;

		knobs = knobCodes.collect({
			|cc|
			var device = LCDeviceKnob(midiInUid, midiOut, cc, channel);
			LCKnob(midiInUid, device);
		});

		buttons = buttonCodes.collect({
			|cc|
			var device = LCDeviceButton(midiInUid, midiOut, cc, channel);
			LCButton(midiInUid, device);
		});

		settings = LCSysex(midiOut);

		if( initialized.not, {
			initialized = true;
			this.doIntroAnimation();
			this.knobs.do({ |k| k.allOff() });
		});
	}

	doOnCmdPeriod {
		this.free();
	}

	doIntroAnimation {
		var dur = 2, list;
		var stateA, stateB, stateC, stateZ;
		if (midiOut.isNil) { ^this };

		{
			this.settings.push();

			stateA = #[1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1] ! 32;
			stateB = 32.collect { Array.fill(13, { 0.5.rand.round(1).asInt }) };
			stateC = #[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1] ! 32;
			stateZ = (0 ! 13) ! 32;

			Env.sine.asSignal(10).do {
				|i|
				var t;
				this.settings.ledState = [stateA, stateB, stateA, stateB, stateA, stateB, stateA, stateB, stateA, stateB][i % 10];
				stateA = stateA.flop.rotate((i * 10).asInt).flop;
				stateB = stateB.flop.rotate((i * 10).asInt).flop;
				((1 - i) * 0.02).wait;
				this.settings.ledState = stateZ;
				((1 - i) * 0.1).wait;
			};
			this.settings.ledState = stateZ;

			this.settings.pop();
		}.fork;
	}

	connected_{
		| inConnected |
		if (connected != inConnected) {
			if (connected) { this.connect };
			if (connected.not) { this.disconnect };
		}
	}

	connect {
		if (connected.not) {
			{
				connected = true;
				allConnected.add(this);
				this.class.printConnected();

				settings.resendAll();
				this.knobs.do(_.connect());
				this.buttons.do(_.connect());
				CmdPeriod.doOnce(this);
			}.defer(0);
		}
	}

	disconnect {
		if (connected) {
			this.knobs.do(_.disconnect());
			this.buttons.do(_.disconnect());

			connected = false;
			allConnected.remove(this);
			this.class.printConnected();
		}
	}

	free {
		this.disconnect();
		this.knobs.do(_.free());
		this.buttons.do(_.free());
	}

	knobRows {
		^this.knobs.clump(4).flop();
	}

	knobCols {
		^this.knobs.clump(4);
	}

	printOn {
		arg stream;
		stream << "LCDevice(" << this.identityHash << " connected:" << connected << ")";
	}
}

LCInput {
	var <midiInUid, <device, cv, cvConnection,
	<connected = false;

	*new {
		arg midiInUid, device, inCV;
		^super.newCopyArgs(midiInUid, device).init(inCV);
	}

	init {
		arg inCV;
		this.cv = inCV;
	}

	cv_{
		arg inCV;

		this.removeCV();
		cv = inCV;

		if (inCV.notNil) {
			cvConnection = inCV.signal(\value).connectTo(this.methodSlot("update(value)"));
			inCV.changed(\value, inCV.value);
		};

		^cv;
	}

	cv {
		if (cv.isNil) {
			this.cv = NumericControlValue();
		};

		^cv;
	}

	removeCV {
		if (cv.notNil, {
			cvConnection.free;
			cv = nil;
		});
	}

	disconnect {
	}

	free {
		this.disconnect();
		this.cv = nil;
	}
}

LCButton : LCInput {
	var noteOnFunc, noteOffFunc,
	<buttonOnActions, <buttonOffActions;
	var <>toggle = false, <toggleState = false, <lit = nil,
	<defaultCV;

	init {
		arg inCV;
		defaultCV = OnOffControlValue();

		buttonOnActions = Set();
		buttonOffActions = Set();

		^super.init(inCV ?? defaultCV);
	}

	default {
		this.cv = defaultCV;
	}

	connect {
		connected = true;

		device.noteOnFunc = {
			arg ...args;
			this.onMidiEvent(true, *args);
		};

		device.noteOffFunc = {
			arg ...args;
			this.onMidiEvent(false, *args);
		};

		cv.changed(\synch);
	}

	disconnect {
		if (cv.notNil) {
			this.lit = false;
		};

		device.noteOnFunc = nil;
		device.noteOffFunc = nil;

		connected = false;
	}

	clear {
		this.cv = nil;
		this.buttonOnActions.clear();
		this.buttonOffActions.clear();
	}

	cv_{
		arg inCV;
		super.cv_(inCV);

		if (connected) {
			device.off();
		}
	}

	lit_{
		arg val, force = false;
		lit = val;
		if (connected && lit.notNil) {
			if (lit) {
				device.on();
			} {
				device.off();
			}
		}
	}

	onMidiEvent {
		arg on;

		if (cv.notNil) {
			if (toggle) {
				if (on) {
					cv.input = toggleState.if(0, 1);
				}
			} {
				if (on) {
					cv.input = 1;
				} {
					cv.input = 0;
				}
			};
		};
	}

	update {
		arg value;

		if (value == \on) {
			this.lit_(true);
			toggleState = true;
			this.doButtonOnActions(this, value);
		} {
			this.lit_(false);
			toggleState = false;
			this.doButtonOffActions(this, value);
		}
	}

	doButtonOnActions {
		arg ...args;
		buttonOnActions.do({
			|action|
			action.(*args)
		})
	}

	doButtonOffActions {
		arg ...args;
		buttonOffActions.do({
			|action|
			action.(*args)
		})
	}
}

LCKnob  : LCInput {
	var <button, <lightWhenActive = true, <ringStyle, <ringFeedback = true, input, <>step=0.01;
	var volume, levelsSynth, levelsSynthName, levelsServer;

	init {
		arg inCV;
		super.init(inCV);

		button = LCButton(midiInUid, LCDeviceButton.from(device));
		this.ringStyle = \fill;
	}

	connect {
		connected = true;

		device.midiFunc = {
			arg ...args;
			this.onMidiEvent(*args);
		};
		button.connect();

		// Send updates to device
		this.updateRing();
		this.updateLightState();
		this.ringStyle = ringStyle;
	}

	disconnect {
		if (cv.notNil) {
			this.allOff();
		};

		connected = false;
		device.midiFunc = nil;
		button.disconnect();
	}

	cv_{
		arg inCV;

		super.cv_(inCV);

		if (inCV.notNil) {
			input = inCV.input;
		};

		this.updateRing();
		this.updateLightState()
	}

	lightWhenActive_{
		arg inActive;
		if (lightWhenActive != inActive) {
			lightWhenActive = inActive;
			if (lightWhenActive) {
				if (button.cv == button.defaultCV) {
					button.cv = nil;
				};
				this.updateLightState();
			} {
				if (button.cv == nil) {
					button.cv = button.defaultCV;
				};
			};
		};
	}

	ringStyle_{
		arg style;
		ringStyle = style;

		if (connected) {
			device.ringStyle(style);
		};
	}

	updateRing {
		if (connected && ringFeedback) {
			if (cv.notNil) {
				device.ring(cv.input);
			} {
				device.ring(0);
			}
		};
	}

	updateLightState {
		if (connected) {
			if (lightWhenActive) {
				button.lit = cv.notNil();
			}
		}
	}

	onMidiEvent {
		|v|
		if (cv.notNil, {
			input = (input - ((v - 64).sign * step)).clip(0, 1);
			cv.input = input;
		});
	}

	update {
		arg value;
		this.updateRing();
	}

	removeCV {
		super.removeCV();

		if (connected) {
			device.off();
		}
	}

	allOff {
		if (connected) {
			device.allOff()
		}
	}

	makeVolumeKnob {
		| bus, cv, lag = 0.5, server |
		var oldVol = 0, msg;
		if (volume.notNil) {
			oldVol = volume.volume;
			volume.free;
		};

		ringFeedback = false;
		levelsServer = server = server ? Server.default;
		ServerTree.add(this);
		ServerQuit.add(this);
		CmdPeriod.add(this);

		if (bus.notNil) {
			volume = Volume(server, bus.index, bus.numChannels, cv.spec.minval, cv.spec.maxval, true);
		} {
			volume = server.volume;
		};

		if (cv.notNil) {
			this.cv = cv;
			volume.setVolumeRange(cv.spec.minval, cv.spec.maxval);
		} {
			this.cv = cv = NumericControlValue(spec:ControlSpec(volume.min, volume.max, \db, step:0.01));
		};
		cv.value = volume.volume;
		cv.action = {
			|cv|
			volume.volume = cv.value;
		};

		levelsSynthName = ("knobLevels" ++ this.identityHash).asSymbol;
		if (server.serverRunning) {
			this.sendLevelsSynth()
		};
		this.connect();
	}

	doOnCmdPeriod {
		this.sendLevelsSynth();
	}

	doOnServerTree {
		this.sendLevelsSynth();
	}

	sendLevelsSynth {
		if (levelsSynth.notNil) { levelsSynth.free };

		OSCFunc({
			|msg|
			device.ring(msg[3].ampdb.min(0).max(-70).linlin(-70, 0, 0, 1));
			if ((msg[4] > 0) && device.isOn.not) {
				device.on();
			};
			if ((msg[4] == 0) && device.isOn){
				device.off();
			};
		}, levelsSynthName).permanent_(true);

		fork {
			SynthDef(levelsSynthName, {
				var in, amp, clip;
				in = InFeedback.ar(volume.slotAt(0), volume.synthNumChans);
				amp = Amplitude.kr(in.sum / in.size, 0.1, 0.2);
				clip = Trig.kr((in.abs) > 1, 0.4).sum;
				SendReply.kr(Impulse.kr(16), levelsSynthName, [amp, clip]);
				Out.ar(0, 0);
			}).add;
			levelsServer.sync;
			levelsSynth = Synth(levelsSynthName, target: levelsServer.defaultGroup, addAction: 'addAfter');
		}
	}

	doOnServerQuit {
		levelsSynth = nil;
	}

}


LCDeviceKnob {
	classvar <>ledRingOffset = 32,
	ringStyles,
	<>settingsChannel = 15
	;

	var <midiInUid, <midiOut, <cc, <channel, <isOn = false,
	<midiFunc;

	*initClass {
		ringStyles = [\walk, \fill, \eq, \spread];
	}

	*new {
		arg midiInUid, midiOut, cc, channel;
		^super.newCopyArgs(midiInUid, midiOut, cc, channel)
	}

	*from {
		arg otherDevice;
		^this.new(otherDevice.midiInUid, otherDevice.midiOut, otherDevice.cc, otherDevice.channel);
	}

	midiFunc_{
		arg func;
		midiFunc.free;
		if (func.isNil) {
			midiFunc = nil;
		} {
			midiFunc = MIDIFunc.cc(func, cc, channel, midiInUid);
		}
	}

	on {
		arg vel = 127;
		midiOut.noteOn(channel, cc, vel);
		isOn = true;
	}

	off {
		midiOut.noteOff(channel, cc, 0);
		isOn = false;
	}

	ring {
		arg position;
		midiOut.control(channel, ledRingOffset + cc, position * 127.0);
		midiOut.control(channel, cc, position * 127.0);
	}

	allOff {
		this.ring(0);
		this.off();
	}

	allOn {
		this.ring(1);
		this.on();
	}

	ringStyle {
		arg style;
		var styleByte;
		if (ringStyles.indexOf(style).notNil) {
			styleByte = 64 + ringStyles.indexOf(style);
			midiOut.control(settingsChannel, ledRingOffset + cc, styleByte);
		} {
			"Incorrect style % (%)".format(style, ringStyles).error;
		}
	}

	ringLocal {
	}
}

LCDeviceButton {
	var <midiInUid, <midiOut, <cc, <channel, <isOn,
	noteOnFunc, noteOffFunc;

	*new {
		arg midiInUid, midiOut, cc, channel;
		^super.newCopyArgs(midiInUid, midiOut, cc, channel);
	}

	*from {
		arg otherDevice;
		^this.new(otherDevice.midiInUid, otherDevice.midiOut, otherDevice.cc, otherDevice.channel);
	}

	noteOnFunc_{
		arg func;

		noteOnFunc.free;
		if (func.isNil) {
			noteOnFunc = nil;
		} {
			noteOnFunc = MIDIFunc.noteOn(func, cc, channel, midiInUid);
		}
	}

	noteOffFunc_{
		arg func;

		noteOffFunc.free;
		if (func.isNil) {
			noteOffFunc = nil;
		} {
			noteOffFunc = MIDIFunc.noteOff(func, cc, channel, midiInUid);
		}
	}

	on {
		midiOut.noteOn(channel, cc, 127);
		isOn = true;
	}

	off {
		midiOut.noteOff(channel, cc, 127);
		isOn = false;
	}
}

CVGroup {
	var <cvList, <controllers, syncing = false;

	*new {
		arg ...args;
		^super.new().init(args);
	}

	init {
		|inList|
		cvList = inList.collect({
			| i |
			if (i.isKindOf(CV)) {
				i;
			} {
				i.respondsTo(\cv).if({
					i.cv
				});
			}
		});

		controllers = cvList.collect({
			|cv|
			SimpleController(cv).put(\synch, {
				|changedCV|

				if (syncing.not) {
					syncing = true;
					{ this.onCVChange(changedCV) }.protect({
						|e|
						e.notNil.if({ e.reportError() });
						syncing = false;
					});
				}
			});
		});
	}

	free {
		controllers.do(_.remove);
	}
}

CVModalGroup : CVGroup {
	var <currentCV, <>allowOff = true, <>offAction;

	onCVChange {
		arg cv;

		if (cv.input > 0) {
			currentCV = cv;
			cvList.do({
				| other |
				if ((other != currentCV) && other.notNil) {
					if (other.input != 0) {
						other.input = 0;
					}
				}
			})
		} {
			if (cv == currentCV) {
				if (allowOff == false) {
					cv.input = 1;
				} {
					offAction.value(cv);
				}
			}
		}
	}
}

CVSyncGroup : CVGroup {
	var <>relative, type = \value, syncing = false;

	onCVChange {
		arg cv;
		switch (type)
		{\value} {
			cvList.do({
				|other|
				if (other != cv) {
					other.value = cv.value;
				}
			})
		}

		{\input} {
			cvList.do({
				|other|
				if (other != cv) {
					other.input = cv.input;
				}
			})
		}
	}
}

