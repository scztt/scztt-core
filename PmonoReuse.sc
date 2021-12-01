PmonoReuseStream : PmonoStream {
	var scheduledNoteOffFunc;

	embedInStream { |inevent|
		var	sustain, rearticulating = true;
		inevent ?? { ^nil.yield };

		this.prInit(inevent);

		loop {
			if(this.prDoStreams) {
				if(rearticulating or: { event[\id].isNil } and: { event.isRest.not }) {
					event[\id] = nil;
					this.prInitNode;
					rearticulating = false;

					Log(this.identityHash).info("rearticulating");

					event.onFree({
						Log(this.identityHash).info("Event ended, so ditching cleanup funcs...");
						inevent[\removeFromCleanup] = inevent[\removeFromCleanup].add(currentCleanupFunc);
						currentCleanupFunc = nil;
						rearticulating = true;
					});
				};
				sustain = event.use { ~sustain.value };

				thisThread.clock.sched(sustain, scheduledNoteOffFunc = {
					if (thisFunction == scheduledNoteOffFunc) {
						Log(this.identityHash).info("Doing scheduled cleanup.");
						currentCleanupFunc.value(true);
						currentCleanupFunc = nil;
						// rearticulating = true;
					}
				});

				cleanup.update(event);
				inevent = event.yield;
				this.prSetNextEvent(inevent);
			} {
				^cleanup.exit(inevent)
			}
		}
	}
}

PmonoReuse : Pmono {
	embedInStream { |inevent|
		^PmonoReuseStream(this).embedInStream(inevent)
	}
}
