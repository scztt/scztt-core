MultiRecorder : Singleton {
	var buses;
	var <>defaultBus=0, <>defaultChannels=2;
	var recorders;

	init {
		buses = (
			server: Bus(\audio, defaultBus, defaultChannels, Server.default);
		);
		recorders = (
			server: Server.default.recorder
		);
	}

	addTarget {
		|name, bus|
		buses[name] = bus
	}

	removeTarget {
		|bus|
		var name = buses.findKeyForValue(bus);
		name !? {
			buses[name] = nil;
			recorders[name] !? _.stopRecording;
			recorders[name] = nil;
		}
	}

	record {
		|path, duration|

		Log(\MultiRecorder).info("Recording % buses", buses.size);

		fork {
			this.prepareForRecord(path);

			Server.default.sync;

			buses.keysValuesDo {
				|name, bus|
				Log(\MultiRecorder).info(".record(%, %, %)", this.prFixPath(path), bus.index, bus.numChannels);
				recorders[name].record(this.prFixPath(path, name), bus.index, bus.numChannels);
			}
		}
	}

	prFixPath {
		|path, name|
		var adjustedPath = path;

		if (name != \server) {
			adjustedPath = PathName(adjustedPath);
			adjustedPath.extension = "%.".format(name) ++ adjustedPath.extension;
			adjustedPath = adjustedPath.absolutePath;
		};

		^adjustedPath
	}

	isRecording {
		^recorders.any(_.isRecording)
	}

	pauseRecording {
		recorders.do(_.pauseRecording)
	}

	stopRecording {
		recorders.do(_.stopRecording)
	}

	prepareForRecord {
		|path|
		var recorder;

		buses.keysValuesDo {
			|name, bus|
			recorder = recorders.atDefault(name, {
				Recorder(Server.default);
			});

			if (recorder.isPrepared.not) {
				Log(\MultiRecorder).info(".prepareForRecord(%, %)", this.prFixPath(path), bus.numChannels);
				recorder.prepareForRecord(this.prFixPath(path, name), bus.numChannels)
			}
		}
	}
}

+Recorder {
	isPrepared {
		^recordBuf.notNil
	}
}