PMeta : PparWrap {
	embedInStream { arg inval;
		var assn;
		var priorityQ = PriorityQueue.new;
		var streamIndex = list.size;

		repeats.value(inval).do({ arg j;
			var outval, stream, nexttime, now = 0.0;
			var newPattern, newStream, timingOffset, sustain;

			this.initStreams(priorityQ, inval);

			// if first event not at time zero
			if (priorityQ.notEmpty and: { (nexttime = priorityQ.topPriority) > 0.0 }) {
				outval = Event.silent(nexttime, inval);
				inval = outval.yield;
				now = nexttime;
			};

			while { priorityQ.notEmpty } {
				stream = priorityQ.pop;
				outval = stream.next(inval).asEvent;

				Log(PMeta).info("Playing at t=% %", now, outval);
				priorityQ.do({ |item, time| Log(PMeta).info("\tpq %: %", time, item)});

				if (outval.isNil) {
					nexttime = priorityQ.topPriority;
					if (nexttime.notNil, {
						// that child stream ended, so rest until next one
						outval = Event.silent(nexttime - now, inval);
						inval = outval.yield;
						now = nexttime;
					},{
						priorityQ.clear;
					});
				} {
					if (outval[\fork] == true) {
						Log(PMeta).info("Forking from outval: %...", outval);

						Log(PMeta).info("Rescheduling current stream to %", now + outval.delta);
						priorityQ.put(now + outval.delta, stream);

						outval = outval.copy;
						[\delta].do(outval.removeAt(_));

						newPattern = outval[\pattern].valueWithEnvir(outval);
						timingOffset = outval[\timingOffset] ?? {0};
						sustain = outval.use { ~sustain.value };

						Log(PMeta).info("NewPattern: %, timingOffset: %, sustain: %",
							newPattern, timingOffset, sustain
						);

						[\fork, \pattern, \timingOffset].do(outval.removeAt(_));

						newPattern = (newPattern <> outval.asStream).finDur(sustain);
						Log(PMeta).info("Scheduling new stream at %", timingOffset);
						newStream = this.initStream(now + timingOffset, priorityQ, outval, newPattern, streamIndex);
						streamIndex = streamIndex + 1;

					} {
						// requeue stream
						Log(PMeta).info("Rescheduling current stream to %", now + outval.delta);

						priorityQ.put(now + outval.delta, stream);
						nexttime = priorityQ.topPriority;
						Log(PMeta).info("updated delta is % - % = %", nexttime, now, nexttime-now);
						outval.put(\delta, nexttime - now);

						inval = outval.yield;
						// inval ?? { this.purgeQueue(priorityQ); ^nil.yield };
						now = nexttime;
					}
				};
			};
		});
		^inval;
	}

}