+EventStreamPlayer {
	prNext { arg inTime;
		var nextTime;
		var outEvent = stream.next(event.copy);
		if (outEvent.isNil) {
			streamHasEnded = stream.notNil;
			this.removedFromScheduler;
			^nil
		}{
			nextTime = outEvent.playAndDelta(cleanup, muteCount > 0);
			if (outEvent[\condition].notNil) {
				// this.pause(); "pausing player".postln;
				fork {
					"waiting for fork".postln;
					outEvent[\condition].wait();
					outEvent[\condition].test = false;
					nextBeat = nil;
					isWaiting = true;
					"going to resume...".postln;
					clock.play({
						thisThread.clock.sched(0, this );
						isWaiting = false;
					});
				};
				^nil
			} {
				if (nextTime.isNil) { this.removedFromScheduler; ^nil };
				nextBeat = inTime + nextTime;	// inval is current logical beat
				^nextTime
			}
		};
	}
}

+Event {
	playAndDelta { | cleanup, mute |
		if (mute) { this.put(\type, \rest) };
		this.play;
		cleanup.update(this);
		^this.delta;
	}
}