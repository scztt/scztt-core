Bdef : Singleton {
	var <buffer, <bufnum, server;

	init {
		server = Server.default;
		buffer = Buffer(server, 0, 0);
		bufnum = buffer.bufnum;
	}

	set {
		|pathOrDurOrArray ...args|
		var shape;

		case
		{ pathOrDurOrArray.isKindOf(String) } {
			buffer.allocRead(pathOrDurOrArray);
			buffer.updateInfo();
		}
		{ pathOrDurOrArray.isKindOf(Number) } {
			buffer.sampleRate = server.sampleRate;
			buffer.numSamples = (pathOrDurOrArray * buffer.sampleRate + 0.5).asInteger;
			buffer.numChannels = 1;
		}
		{ pathOrDurOrArray.isKindOf(Array) } {
			server.makeBundle(nil, {
				shape = pathOrDurOrArray.shape;
				buffer.sampleRate = server.sampleRate;
				if (shape.size == 1) {
					buffer.numChannels = 1;
					buffer.numSamples = shape[0];
					buffer.alloc(
						buffer.sendCollection(pathOrDurOrArray, action:{ this.changed(\buffer) })
					);
				} {
					if (shape.size > 2) {
						Error("Array is too deep to be made into array.").throw;
					} {
						buffer.numChannels = shape[0];
						buffer.numSamples = shape[1];
						pathOrDurOrArray.do {
							|array|
							buffer.sendCollection
						}
					}
				};
			});
		}
		{ pathOrDurOrArray.isKindOf(Buffer) } {

		}
		{ pathOrDurOrArray.isKindOf(Function) } {

		}
	}


}