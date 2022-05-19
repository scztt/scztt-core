#{
	var tempo, server, eventTypes, parentType, type;
	type = ~parentType ?? { ~type };
	parentType = ~parentTypes[type];
	parentType !? { currentEnvironment.parent = parentType };

	server = ~server = ~server ? Server.default;

	~tempo = ~tempo.value(currentEnvironment);

	~finish.value(currentEnvironment);

	if (~scale.isKindOf(Symbol)) {
		~scale = Scale.at(~scale)
	};

	if (~tuning.isKindOf(Symbol)) {
		~tuning = Scale.at(~tuning)
	};

	~ndef !? {
		~ndef = ~ndef.value;

		if (~ndef.isKindOf(Symbol)) {
			~ndef = Ndef(~ndef);
		};

		if (~ndef.numChannels.isNil) {
			var out = ~instrument.asSynthDesc !? _.outputs !? _.detect({
				|out|
				out.type.isKindOf(Meta_Out) or: { out.type.isKindOf(Meta_OffsetOut) }
			});

			~ndef.mold(
				out !? _.numberOfChannels ?? 2;,
				out !? _.rate ?? \audio;
			);
		};

		if (currentEnvironment.keys.includes(\group)) {
			switch (~addAction ?? { \addToTail },
				\addToTail, { ~ndef.group.moveToTail(~group.nodeID.asGroup) },
				1,          { ~ndef.group.moveToTail(~group.nodeID.asGroup) },
				\addToHead, { ~ndef.group.moveToHead(~group.nodeID.asGroup) },
				0, 		    { ~ndef.group.moveToHead(~group.nodeID.asGroup) },
				\addBefore, { ~ndef.group.moveBefore(~group.nodeID.asGroup) },
				\addAfter,  { ~ndef.group.moveAfter(~group.nodeID.asGroup) },
				{ ~ndef.group.moveToTail(~group.nodeID) }
			);
			~ndef.monitor !? _.group !? {
				|g|
				g.moveAfter(~ndef.group)
			}
		} {
			~group = ~ndef.group;
		};

		~addAction = ~addAction ?? { \addToTail };
		~out = ~ndef.bus;
	};

	tempo = ~tempo.value;
	tempo !? { thisThread.clock.tempo = tempo };

	if(currentEnvironment.isRest.not) {
		eventTypes = ~eventTypes ?? { Event.eventTypes };
		(eventTypes[~type] ?? { eventTypes[\note] }).value(server)
	};

	~onFreeAction !? { ~setupOnFree.(currentEnvironment) };
	~callback.value(currentEnvironment);
}