OSequence2 {
	var <events;
	var <>extend=false, <>duration = 0;
	var <>overlapsFunc, <>deleteFunc, <>warpFunc;

	*new {
		|... args|
		var obj = super.new.init;
		args.pairsDo {
			|time, value|
			obj.put(time, value)
		};
		^obj
	}

	*fromPbind {
		|pbind, duration, trim=true, protoEvent|
		^this.fromStream(pbind.asStream, duration, trim, protoEvent);
	}

	*fromStream {
		|stream, duration, trim=true, protoEvent|
		var seq = OSequence();
		var startTime = 0;
		var endTime = startTime + (duration ? 1000);
		var time = startTime;
		var oldTime = thisThread.beats;

		thisThread.beats = startTime;

		protect {
			protoEvent = protoEvent ?? { Event.default };
			while {
				var dur, outEvent;

				outEvent = stream.next(protoEvent.copy);

				if (outEvent.notNil) {
					seq.put(time, outEvent);
					dur = outEvent.dur.value();

					if (trim && ((time + dur) > endTime)) {
						dur = endTime - time;
						outEvent.dur = dur;
					};

					if (dur.notNil) {
						thisThread.beats = time = time + dur;
					}
				};

				(dur.notNil && outEvent.notNil && (time < endTime));
			};

			if (duration.notNil) {
				seq.duration = duration;
			} {
				seq.duration = 0;
				seq.events.do {
					|eventList, t|
					eventList.do {
						|e|
						seq.duration = max(seq.duration, t + seq.prEventDur(e))
					}
				}
			};
		} {
			thisThread.beats = oldTime;
		};

		^seq
	}

	init {
		events = Order();

		overlapsFunc = {
			|time, event, range|
			^(range.includes(time) && range.includes(time + this.prGetDur(event)))
		};

		warpFunc = {
			|time, event, warpFunc|
			this.prSetDur(event, warpFunc.value(time + this.prSetDur(event), event));
		};

		<>deleteFunc, <>warpFunc;

	}

	do {
		|func|
		events.do {
			|eventList, t|
			eventList.do {
				|event|
				func.value(event, t);
			}
		}
	}

	at {
		|time|
		^events[time] ?? {
			var newList;
			events[time] = newList = List();
			newList;
		};
	}

	put {
		|time, value|
		if (value.notNil) {
			if (extend) {
				duration = max(
					duration,
					time + this.prGetDur(value)
				);
			};
			this.at(time).add(value);
		}
	}

	putAll {
		|time, valueArray|
		if (valueArray.notNil) {
			if (extend) {
				duration = max(
					duration,
					time + valueArray.maxItem({
						|e|
						this.prEventDur(e)
					})
				)
			};
			this.at(time).addAll(valueArray);
		}
	}

	putSeq {
		|baseTime, seq|
		if (seq.notNil) {
			seq.events.do {
				|events, time|
				this.putAll(baseTime + time, events)
			};
		}
	}

	delete {
		|start=0, end, ripple=false|
		end = end ?? duration;

		events = events.reject {
			|event, t|
			(t >= start) && (t <= end)
		};

		if (ripple) {
			var range = end - start;
			this.warp({
				|t|
				if (t > end) { t - range } { t };
			})
		};
	}

	crop {
		|start=0, end, cropDuration=true|
		var newEvents = Order();
		end = end ?? duration;
		events.do({
			|val, t|
			var newTime;
			if ((t >= start) && (t <= end)) {
				newTime = t - start;
				newEvents[newTime] = val;
				if (cropDuration) {
					this.prEventSetDur(val, min(
						this.prEventDur(val),
						end - t
					))
				}
			}
		});
		events = newEvents;
		duration = end - start;
	}

	envWarp {
		|env, warpDurations|
		env = env.copy.duration_(duration);
		this.warp({
			|time|
			env.at(time);
		}, warpDurations)
	}

	warp {
		|func, warpEvents=true|
		var oldEvents = events;
		events = Order();
		oldEvents.do {
			|eventList, time|
			eventList.do {
				|event|
				var newTime = func.value(time, event);
				this.put(newTime, event);
				this.prEventWarp(newTime, event, func);
			}
		};
	}

	stretch {
		|newDuration, warpEvents|
		var ratio = newDuration / duration;
		this.warp({ |t| t * ratio }, warpEvents);
		duration = newDuration;
	}

	stretchBy {
		|factor=1, warpEvents|
		this.warp({ |t| t * factor }, warpEvents);
		duration = duration * factor;
	}

	reverse {
		this.warp({ |t| duration - t }, false)
	}

	doPutSeq {
		|func|
		events.copy.do {
			|eventList, time|
			eventList.do {
				|event|
				this.putSeq(time, func.value(event, time))
			}
		}
	}

	doReplace {
		|func|
		events = events.collect({
			|eventList, time|
			eventList.collect({
				|event|
				func.value(event, time)
			}).reject(_.isNil)
		})
	}

	doReplaceSeq {
		|func|
		var oldEvents = events;
		events = Order();
		oldEvents.do {
			|eventList, time|
			eventList.do {
				|event|
				this.putSeq(time, func.value(event, time));
			};
		}
	}

	sub {
		|start = 0, end, cropDuration|
		var sub = this.copy;
		sub.crop(start, end, cropDuration);
		^sub;
	}

	overwrite {
		|start, seq|
		var end = start + seq.duration;
		this.delete(start, end);
		this.putSeq(start, seq);
	}

	insert {
		|start, end, seq|
		var insertDur = end - start;
		this.delete(start, end);
		if (insertDur != seq.duration) {
			this.warp({
				|t|
				if (t > end) {
					t = t + duration - insertDur
				} {
					t
				}
			}, false)
		};
		this.putSeq(start, seq);
	}

	replaceSub {
		|start=0, end, func, ripple=false, cropDuration=false|
		var sub;

		if (func.isKindOf(OSequence)) {
			sub = func;
		} {
			sub = this.sub(start, end, cropDuration);
			sub = func.value(sub);
		};
		if (ripple) {
			this.insert(start, end, sub)
		} {
			this.overwrite(start, sub);
		}
	}

	prGetDur {
		|e|
		^e.tryPerform(\dur) ?? 0;
	}

	prSetDur {
		|e, dur|
		^e.tryPerform(\dur_, dur);
	}

	prEventWarp {
		|baseTime, event, warpFunc|
		if (event.isKindOf(Event)) {
			var endTime = baseTime + this.prEventDur(event);
			var newEndTime = warpFunc.value(endTime, event) - baseTime;
			this.prEventSetDur(event, newEndTime);
		}
	}

	prUpdateEventDurs {
		var lastTime = 0;
		var lastEvents, duration;
		events.do {
			|eventList, time|
			"% - %".format(time, lastTime).postln;
			if (lastEvents.notNil) {
				duration = time - lastTime;
				lastEvents.do {
					|e|
					if (e.isKindOf(Event)) {
						this.prEventSetDur(e, duration)
					}
				}
			};
			lastEvents = eventList;
			lastTime = time;
		};
		duration = max(0, this.duration - lastTime);
		lastEvents.do {
			|e|
			if (e.isKindOf(Event)) {
				this.prEventSetDur(e, duration);
			}
		}
	}

	embedInStream {
		|inEvent|
		var firstEvent = true;
		this.prUpdateEventDurs();
		events.do {
			|eventList, time|
			if (firstEvent && (time > 0)) {
				inEvent = Rest(time).yield;
			};

			firstEvent = false;

			eventList = eventList.collect {
				|event|
				event = event.composeEvents(inEvent);
			};
			eventList.yield;
		}
	}

	asStream {
		^Routine({ arg inval; this.embedInStream(inval) })
	}
}