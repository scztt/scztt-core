+Event {
    *composeEventType {
        |type, func, parentEvent, composeType|
        var composeEvent = partialEvents.playerEvent.parentTypes[composeType] ?? {()};
        
        if (parentEvent.notNil) {
            parentEvent = parentEvent.collect {
                |value, key|
                if (value.isKindOf(Function)) {
                    value.value(_, composeEvent[key]);
                } {
                    value
                }
            };
        } {
            parentEvent = parentEvent.copy;
        };
        
        Event.addEventType(
            type,
            {
                |server|
                var eventTypes = ~eventTypes;
                var parentPlayFunc = eventTypes[composeType] ?? { eventTypes[\note] };
                
                func 
                    !? { 
                        func.value(server, {
                            |...args|
                            parentPlayFunc.value(server, *args)
                        }) 
                    } 
                    ?? { 
                        |...args|
                        parentPlayFunc.value(server, *args) 
                    };
            },
            parentEvent
        )
    }
}

