Arc : Singleton {
	classvar allMonitor;
	var <>knobs, device, <monitor;
	var <flip=true;

	initClass {
		{
			SkipJack({
				if (SerialOSCClient.devices.isEmpty) {
					SerialOSCClient.init;
				};

				if (device != SerialOSCEnc.default) {
					this.device = SerialOSCEnc.default;
				}
			}, 3);
		}.defer(1);

	}

	set {
		SerialOSCClient.init;
	}

	init {
		SerialOSCClient.init;

		monitor = SkipJack({
			if (SerialOSCClient.devices.isEmpty) {
				SerialOSCClient.init;
			};

			if (device != SerialOSCEnc.default) {
				this.device = SerialOSCEnc.default;
			}
		}, 3);

		knobs = 4.collect {
			|i|
			ArcKnob(i)
				.behavior_(ArcBehavior())
				.flip_(flip);
		};

		monitor.start();
	}

	clear {
		monitor.stop;
		knobs.do(_.clear);
		^super.clear;
	}

	device_{
		|inDevice|
		device = inDevice;

		if (device.isNil) {
			"Arc disconnected".postln;
		} {
			"Arc connected".postln;
		};

		knobs.do(_.device_(device))
	}

	flip_{
		|inFlip|
		flip = inFlip;
		knobs.do(_.flip_(flip));
	}

	behaviors {
		^knobs.collect(_.behavior)
	}
}

ArcKnob {
	var index, <device,
	conn, deltaFunc, address, values,
	flip=false,
	<cv, <behavior;

	*new {
		|index|
		^super.newCopyArgs(index).init
	}

	device_{
		|inDevice|

		device = inDevice;

		if (device.notNil) {
			conn = NetAddr("127.0.0.1", device.port);

			deltaFunc.free;
			deltaFunc = EncDeltaFunc({
				|n, delta|
				behavior.delta(delta);
			}, index, device:device);
			deltaFunc.permanent = true;

			cv.updater.pull();
		} {
			conn = nil;

			deltaFunc.free;
			deltaFunc = nil;
		}
	}

	init {
		address 	= SerialOSC.getPrefixedAddress('/ring/map');
		values	 	= Int32Array.fill(64, 0);

		cv 			= SynthControlValue(spec:[0, 1]);
	}

	clear {
		cv.free.clear;
	}

	spec 			{ 			^cv.spec }
	spec_			{ |spec| 	cv.spec = spec }

	flip_{
		|inFlip|
		flip = inFlip;
	}

	values_{
		|inValues|
		values = inValues;

		if (conn.notNil) {
			if (flip) {
				conn.sendMsg(address, index, *values.rotate(32));
			} {
				conn.sendMsg(address, index, *values);
			}
		}
	}

	behavior_{
		|newBehavior|
		if (newBehavior != behavior) {
			behavior.stop();
			behavior = newBehavior.knob_(this).start();
		}
	}
}

ArcBehavior {
	var <>knob,
	<values,
	<backgroundFunc, <foregroundFunc, <deltaFunc, <inputFunc,
	defaultForegroundFunc, defaultBackgroundFunc, defaultDeltaFunc, defaultInputFunc,
	connections,
	connected = false,
	<>majorTicks = #[0],
	<>minorTicks = #[16, 32, 48],
	<>majorTickLevel = 4,
	<>minorTickLevel = 2,
	ledSize = 64;

	*new {
		^super.new.init
	}

	start {
		connected = true;
		connections.free;
		connections = ConnectionList [
			knob.cv.signal(\value).connectTo(
				this.methodSlot("update(args[1])")
			)
		];

		this.inputFunc = inputFunc;
	}

	stop {
		connected = false;
		connections.free;
		connections = nil;
	}

	delta {
		|delta|
		deltaFunc.(delta, knob.cv);
	}

	update {
		|value|
		backgroundFunc.(values);
		foregroundFunc.(values, value);
		knob.values = values;
	}

	init {
		values	 	= 0 ! ledSize;

		defaultBackgroundFunc = {
			|array|
			array.fill(0);
			minorTicks.do({ |i| array[i.round % ledSize] = minorTickLevel });
			majorTicks.do({ |i| array[i.round % ledSize] = majorTickLevel });
		};

		defaultForegroundFunc = {
			|array, value|
			var led, ledRem;

			led 	= value * 63.99;
			ledRem	= led - led.floor;
			led		= led.floor;

			array[led % ledSize] 	= max(array[led % ledSize], (1 - ledRem) * 15);
			array[led + 1 % ledSize]	= max(array[led + 1 % ledSize], (ledRem) * 15);
		};

		defaultDeltaFunc = {
			|delta, cv|
			cv.input = cv.preInput + (delta / 1024.0);
			cv.synth.set(\t_delta, delta);
		};

		defaultInputFunc = {
			|val, t_delta=0|
			var delta, lastDelta, actualVal, actualDelta;
			var lastVal, lastActualVal, sign, deltaSign, smallDelta;

			#lastActualVal, lastDelta = LocalIn.kr(2);

			lastVal 		= DelayN.kr(val, ControlDur.ir, ControlDur.ir);

			// delta	 		= val - lastVal;
			delta			= t_delta / 1024.0;
			lastDelta		= DelayN.kr(delta, ControlDur.ir, ControlDur.ir);
			smallDelta		= ((delta + lastDelta).abs < (2.1 / 1024.0)) * delta;

			sign 			= Gate.kr(delta.sign, delta.abs > 0);
			deltaSign		= 1 - Changed.kr(sign);

			delta			= delta.abs.lincurve(0, 0.02, 0, 0.007, 3.5) * sign;
			smallDelta		= smallDelta.abs.lincurve(0, 0.02, 0, 0.007, 3.5) * sign;
			actualDelta 	= delta - smallDelta;
			actualDelta 	= actualDelta * deltaSign;
			actualDelta 	= actualDelta.abs.lagud(0.01, 4 * deltaSign) * sign;
			actualDelta		= actualDelta + smallDelta;

			actualVal 		= lastActualVal + actualDelta;

			actualVal		= actualVal.min(1.0).max(0.0);

			LocalOut.kr([actualVal, actualDelta]);

			actualVal
		};

		this.backgroundFunc = nil;
		this.foregroundFunc = nil;
		this.inputFunc = nil;
		this.deltaFunc = nil;
	}

	deltaFunc_{
		|func|
		deltaFunc = func ? defaultDeltaFunc;
	}

	inputFunc_{
		|func|
		inputFunc = func ? defaultInputFunc;
		if (connected) {
			knob.cv.inputFunc = inputFunc;
		}
	}

	backgroundFunc_{
		|func|
		backgroundFunc = func ? defaultBackgroundFunc;
	}

	foregroundFunc_{
		|func|
		foregroundFunc = func ? defaultForegroundFunc;
	}
}