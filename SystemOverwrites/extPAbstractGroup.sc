+PAbstractGroup {
	embedInStream { arg inevent;
		var	server, groupID, event, cleanup;
		var	stream, lag = 0, clock = thisThread.clock,
			groupReleaseTime = inevent[\groupReleaseTime] ? 0.1, cleanupEvent;
		var eventType = this.class.eventType;

		cleanup = EventStreamCleanup.new;
		server = inevent[\server] ?? { Server.default };
		groupID = server.nextNodeID;

		event = inevent.copy;
		event[\addAction] = event[\addAction] ?? { 0 };  // \addToHead
		event[\type] = eventType;
		event[\delta] = 0;
		event[\id] = groupID;

		cleanupEvent = (type: \kill, parent: event);

		cleanup.addFunction(event, { | flag |
			if (flag) { cleanupEvent.lag_(lag - clock.beats + groupReleaseTime).play }
		});
		inevent = event.yield;

		inevent !? { inevent = inevent.copy;
			inevent[\group] = groupID;
		};
		stream = pattern.asStream;
		loop {
			event = stream.next(inevent) ?? { ^cleanup.exit(inevent) };
			lag = max(lag, clock.beats + event.use { ~sustain.value });
			inevent = event.yield;
			inevent.put(\group, groupID);
		}
	}

	*embedLoop { arg inevent, stream, groupID, ingroup, cleanup;
		var event, lag;
		loop {
			event = stream.next(inevent) ?? { ^cleanup.exit(inevent) };
			lag = event[\dur];
			inevent = event.yield;
			inevent.put(\group, groupID);
		}
	}
}
