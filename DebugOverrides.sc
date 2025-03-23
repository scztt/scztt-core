// +Pbind {
// 	embedInStream { arg inevent;
// 		var event;
// 		var sawNil = false;
// 		var streampairs = patternpairs.copy;
// 		var endval = streampairs.size - 1;
// 		var benchmarks;
//
// 		forBy (1, endval, 2) { arg i;
// 			streampairs.put(i, streampairs[i].asStream);
// 		};
//
// 		benchmarks = 0 ! (streampairs.size / 2);
//
// 		loop {
// 			if (inevent.isNil) { ^nil.yield };
// 			event = inevent.copy;
// 			forBy (0, endval, 2) { arg i;
// 				var name = streampairs[i];
// 				var stream = streampairs[i+1];
// 				var t = Main.elapsedTime;
// 				var streamout = stream.next(event);
// 				benchmarks[i/2] = Main.elapsedTime - t;
//
// 				if (streamout.isNil) { ^inevent };
//
// 				if (name.isSequenceableCollection) {
// 					if (name.size > streamout.size) {
// 						("the pattern is not providing enough values to assign to the key set:" + name).warn;
// 						^inevent
// 					};
// 					name.do { arg key, i;
// 						event.put(key, streamout[i]);
// 					};
// 				}{
// 					event.put(name, streamout);
// 				};
//
// 			};
// 			event[\keyTiming] = ();
//
// 			forBy (0, endval, 2) {
// 				arg i;
// 				event[\keyTiming][streampairs[i]] = benchmarks[i/2];
// 			};
//
// 			inevent = event.yield;
// 		}
// 	}
// }
//
// +EventStreamCleanup {
// 	update { | event |
// 		if(event.isKindOf(Dictionary)) {
// 			Log(EventStreamCleanup).info("EventStreamCleanup.update(%)", event);
// 			Log(EventStreamCleanup).info("\addToNodeCleanup  = %, event[\addToCleanup] = %", event[\addToNodeCleanup], event[\addToCleanup]);
//
// 			functions.addAll(event[\addToNodeCleanup]);
// 			functions.addAll(event[\addToCleanup]);
// 			functions.removeAll(event[\removeFromCleanup]); // backwards compat.
// 			functions = functions.reject(_.didEvaluate);
//
// 			Log(Pmod).info("EventStreamCleanup[%].functions.size = %", this.identityHash, functions.size);
// 		};
// 		^event
// 	}
// }