(
Event.partialEvents[\velocityEvent] = (
	vel: { 0.7 },
	velocity: { ~vel.value * 127.0 }
);

Event.parentEvents.default.putAll(Event.partialEvents.velocityEvent);
Event.parentEvents.synthEvent.putAll(Event.partialEvents.velocityEvent);
Event.parentEvents.groupEvent.putAll(Event.partialEvents.velocityEvent);
)
