BusScopeSynth2 {
	// Encapsulate management of server resources

	var server, buffer, synthDefName, synth;
	var playThread;

	*new { arg server;
		var instance;
		server = server ? Server.default;
		instance = super.newCopyArgs(server);
		ServerQuit.add(instance);
		^instance;
	}

	play { arg bufSize, bus, cycle, inputUgen=In, target=(RootNode(server)), addAction=\addToTail;
		var synthDef;
		var synthArgs;
		var bufIndex;
		var busChannels;

		if(server.serverRunning.not) { ^this };

		this.stop;

		if (buffer.isNil) {
			buffer = ScopeBuffer.alloc(server);
			synthDefName = "stethoscope" ++ buffer.index.asString;
		};

		bufIndex = buffer.index.asInteger;

		if( bus.class === Bus ) {
			busChannels = bus.numChannels.asInteger;
			synthDef = SynthDef(synthDefName, { arg busIndex, rate, cycle;
				var z;
				z = Select.ar(rate, [
					inputUgen.ar(busIndex, busChannels),
					K2A.ar(inputUgen.kr(busIndex, busChannels))]
				);
				ScopeOut2.ar(z, bufIndex, bufSize, cycle );
			});
			synthArgs = [\busIndex, bus.index.asInteger, \rate, if('audio' === bus.rate, 0, 1), \cycle, cycle];
		}{
			synthDef = SynthDef(synthDefName, { arg cycle;
				var z = Array();
				bus.do { |b| z = z ++ b.ar };
				ScopeOut2.ar(z, bufIndex, bufSize, cycle);
			});
			synthArgs =	[\cycle, cycle];
		};

		playThread = fork {
			synthDef.send(server);
			server.sync;
			synth = Synth(synthDef.name, synthArgs, target, addAction);
		}
	}

	stop {
		if (playThread.notNil) { playThread.stop; playThread = nil };
		if (synth.notNil) { // avoid node not found if stoppedby cmd-period already
			server.sendBundle(nil, ['/error', -1], [11, synth.nodeID], ['/error', -2]);
			synth = nil
		};
	}

	isRunning { ^playThread.notNil }

	bufferIndex { ^ buffer !? { buffer.index } }

	setBusIndex { arg index;
		if( synth.notNil ) { synth.set(\busIndex, index) };
	}

	setRate { arg rate; // 0 = audio, 1 = control
		if( synth.notNil ) { synth.set(\rate, rate) };

	}

	setCycle { arg frames;
		if( synth.notNil ) { synth.set(\cycle, frames) };
	}

	free {
		this.stop;
		if (buffer.notNil) {
			buffer.free;
			buffer = nil;
		};
		ServerQuit.remove(this, server);
	}

	doOnServerQuit {
		buffer = nil;
		synth = nil;
	}
}
