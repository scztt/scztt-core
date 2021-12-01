Blocks {
	var <device;
	var <touches;
	var <deviceConnections;
	var onForwarder, offForwarder;

	*new {
		|device=\default|
		if (device.isKindOf(Symbol)) {
			device = BlocksDevice(device);
		};

		^super.new.init.connect(device ?? { BlocksDevice() });
	}

	init {
		touches = BlocksDevice.numTouches.collect {
			BlocksTouch();
		};
	}

	connect {
		|inDevice|
		if (inDevice != device) {
			if (device.notNil) {
				this.disconnect();
			};

			device = inDevice;
			if (device.notNil) {
				device.changed(\modelConnected, this);
				"Connecting BlocksDevice(%) to Blocks(%)".format(device.identityHash, this.identityHash).postln;

				deviceConnections = ConnectionList[
					device.signal(\deviceConnected).connectTo(this.methodSlot(\onDeviceConnected)),
					device.signal(\modelConnected).connectTo(this.methodSlot("onModelConnected(value)")),
				];

				touches.do({
					|touch, i|
					touch.connect(device.touches[i]);
					deviceConnections.addAll([
						touch.signal(\on).connectTo(this.methodSlot("changed(\\on, %, object, *args)".format(i))),
						touch.signal(\off).connectTo(this.methodSlot("changed(\\off, %, object, *args)".format(i))),
					])
				});

				ServerTree.add(this, Server.default);
				this.doOnServerTree(Server.default);
			}
		}
	}

	disconnect {
		if (device.notNil) {
			"Disconnecting BlocksDevice(%) from Blocks(%)".format(device.identityHash, this.identityHash).postln;
			device = nil;
			touches.do(_.disconnect());
		};

		deviceConnections.free.clear;

		ServerTree.remove(this);
		this.disconnectAmpDisplay();
	}

	doOnServerTree {
		this.connectAmpDisplay();
	}

	ampDefName { ^("blocks_amp_" ++ this.identityHash).asSymbol }
	ampRespName { ^"/blocks/amp".asSymbol }

	connectAmpDisplay {
		var ampDefName = this.ampDefName;
		var ampRespName = this.ampRespName;

		Ndef(ampDefName, {
			var amp = ArrayMax.kr(PeakFollower.kr(InFeedback.ar(0, Server.default.options.numOutputBusChannels)));
			SendReply.kr(Impulse.kr(10), ampRespName, amp);
		});

		OSCdef(ampDefName, {
			|msg|
			if (device.notNil) {
				device.setAmplitude(msg[3]);
			}
		}, ampRespName);
	}


	disconnectAmpDisplay {
		Ndef(this.ampDefName).free;
		OSCdef(this.ampDefName).free;
	}

	onDeviceConnected {
		/// nothing to do
	}

	onModelConnected {
		|twisterObj|
		if (twisterObj != this) {
			this.disconnect();
		}
	}
}

BlocksTouch {
	var <>onOffCV, <>xCV, <>yCV, <>zCV;
	var xConnection, yConnection, xConnection;
	var device, deviceConnections;

	var <events;

	*new {
		|device|
		^super.new.init().connect(device)
	}

	init {
		events = ();
	}

	connect {
		|inDevice|
		if (inDevice != device) {
			if (device.notNil) {
				this.disconnect();
			};

			device = inDevice;
			if (inDevice.notNil) {
				// "Connecting device(%) to knob(%)".format(device.identityHash, this.identityHash).postln;
				device = inDevice;

				deviceConnections = ConnectionList [
					device.signal(\on).connectTo(	this.forwardSlot),
					device.signal(\on).connectTo(	this.methodSlot("on(*args)")),

					device.signal(\off).connectTo(	this.forwardSlot),
					device.signal(\off).connectTo(	this.methodSlot("off(*args)")),

					device.signal(\xyz).connectTo(	this.forwardSlot),
					device.signal(\xyz).connectTo(	this.methodSlot("onXYZ(*args)")),
				];
			}
		}
	}

	disconnect {
		deviceConnections.free.clear;
	}

	on {
		|vel, x, y, z|

		xCV !? { xCV.input = x };
		yCV !? { yCV.input = y };
		zCV !? { zCV.input = z };
		onOffCV !? { onOffCV.value = \on };
	}

	off {
		|vel, x, y, z|

		xCV !? { xCV.input = x };
		yCV !? { yCV.input = y };
		zCV !? { zCV.input = z };
		onOffCV !? { onOffCV.value = \off };
	}

	onXYZ {
		|x, y, z|
		xCV !? { xCV.input = x };
		yCV !? { yCV.input = y };
		zCV !? { zCV.input = z };
	}
}

BlocksDevice : Singleton {
	classvar <numTouches = 6;

	classvar <endpointDevice="Lightpad Block ZYHF", <endpointName="Bluetooth";
	classvar <deviceChangedConnection;

	var <endpoint;
	var <midiInUid, <midiOut;
	var <touches;

	*initClass {
		Class.initClassTree(MIDIWatcher);

		deviceChangedConnection = ConnectionList [
			MIDIWatcher.deviceSignal(endpointDevice, endpointName).connectTo(
				this.methodSlot("deviceChanged(\\default, changed, value)")
			)
		]
	}

	*deviceChanged {
		|deviceName, changeType, endpoint|
		if (changeType == \sourceAdded) {
			"Added % Lightpad Block [%]".format(deviceName, endpoint.uid).postln;
			BlocksDevice(deviceName, endpoint);
			// { BlocksDevice(deviceName).connectAnimation(); }.defer(0.5)
		};

		if (changeType == \sourceRemoved) {
			"Removed Lightpad Block [%]".format(endpoint.uid).postln;
		}
	}

	init {
		touches = numTouches.collect {
			|i|
			BlocksDeviceTouch(-1, DummyMIDI(), i)
		};
	}

	setAmplitude {
		|val|
		midiOut.control(0, 0, val * 127);
	}

	sendTouch {
		|x, y|
		midiOut.control(0, x * 126.0 + 1, y * 127);
	}

	set {
		|inEndpoint|
		endpoint = inEndpoint;

		if (endpoint.isNil) {
			midiInUid = 0;
			midiOut = DummyMIDI();
		} {
			MIDIIn.connectAll();
			midiInUid = endpoint.uid;

			midiOut = MIDIOut.newByName(endpoint.device, endpoint.name).latency_(0);
			midiInUid = endpoint.uid;

			touches.do {
				|knob|
				knob.setMidiDevices(midiInUid, midiOut);
			};
		};

		this.changed(\deviceConnected);
	}
}

BlocksDeviceTouch {
	var <channel, <midiInUid, <midiOut;

	var lastX, lastY, lastZ,
	<isOn = false, touchOnFunc, touchOffFunc, xFunc, yFunc, zFunc;

	*new {
		arg midiInUid, midiOut, channel;
		var obj = super.newCopyArgs(channel).setMidiDevices(midiInUid, midiOut);
		CmdPeriod.add(obj);

		^obj;
	}

	*from {
		arg otherDevice;
		^this.new(otherDevice.midiInUid, otherDevice.midiOut, otherDevice.channel);
	}

	setMidiDevices {
		|uid, inMidiOut|

		if (midiInUid != uid) {
			midiInUid = uid;
		};

		this.makeMIDIFuncs();

		midiOut = inMidiOut ?? midiOut;
	}

	doOnCmdPeriod {
		this.makeMIDIFuncs();
	}

	makeMIDIFuncs {
		touchOnFunc.free; touchOffFunc.free; xFunc.free; yFunc.free; zFunc.free;

		touchOnFunc = MIDIFunc.cc({
			|vel|
			isOn = true;
			this.changed(\on, vel / 127.0, lastX, lastY, lastZ);
		}, 116, channel, midiInUid);

		touchOffFunc = MIDIFunc.cc({
			|vel|
			isOn = false;
			this.changed(\off, vel / 127.0, lastX, lastY, lastZ);
		}, 117, channel, midiInUid);

		xFunc = MIDIFunc.cc({
			|x|
			lastX = x / 127.0;
		}, 113, channel, midiInUid);

		yFunc = MIDIFunc.cc({
			|y|
			lastY = y / 127.0;
		}, 114, channel, midiInUid);

		zFunc = MIDIFunc.cc({
			|z|
			lastZ = z / 127.0;
			if (isOn) {
				this.changed(\xyz, lastX, lastY, lastZ);
			}
		}, 115, channel, midiInUid);
	}
}
