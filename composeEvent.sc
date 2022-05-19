+Event {
	*composeEventType {
		|type, func, parentEvent, composeType|
		var composeEvent = Event.parentEvents[composeType] ?? { () };

		if (parentEvent.notNil) {
			parentEvent.keysValuesChange {
				|key, value|
				if (value.isKindOf(Function)) {
					value.value(_, composeEvent[key]);
				} {
					value
				}
			};
		};

		Event.addEventType(
			type,
			{
				|server|
				var parentPlayFunc = parentEvent.use {
					var eventTypes = ~eventTypes;
					eventTypes[~type] ?? { eventTypes[\note] }
				};
				func.value(server, parentPlayFunc);
			},
			parentEvent
		)
	}
}

