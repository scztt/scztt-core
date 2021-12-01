FEED {
	*new {
		|channels, func, delays=0, maxDelay=5|
		var delay, feedIn, feedOut, buffer, phase, bufSize, delayPhase, totalChannels;

		delays = delays.asArray;

		^sum(delays.collect({
			|delay|

			if (delay.isKindOf(SimpleNumber)) {
				maxDelay = max(delay, ControlDur.ir);
			};

			bufSize = maxDelay * Server.default.sampleRate;
			buffer = LocalBuf(bufSize, channels).clear();

			phase = Phasor.ar(1, 1, 0, bufSize);

			delay = (delay - ControlDur.ir).max(ControlDur.ir);
			delayPhase = phase - (delay * SampleRate.ir);

			feedIn = BufRd.ar(channels, buffer, delayPhase, 1, 4);
			feedOut = func.(feedIn);

			// bufSize.poll(label:"bufSize");
			// feedIn.poll(label:"feedIn");
			// phase.poll(label:"phase");
			// delay.poll(label:"delay");
			// delayPhase.poll(label:"delayPhase");

			BufWr.ar(LeakDC.ar(feedOut.flatten), buffer, phase);

			feedOut;
		}));
	}
}

+UGen {
	feed {
		|func, delay=0|
		^[this].feed(func, delay)
	}
}

+Array {
	feed {
		|func, delay=0|
		^FEED(this.flatten.size, {
			|in|
			this + func.value(in);
		}, delay)
	}
}