(
Event.partialEvents[\nodeObjectEvent] = (
	nodeObject: {
		|self|
		self.use {
			var nodeObj = ~id.asArray.collect {
				|id|
				switch (~type,
					\note, {
						Synth.basicNew(~instrument, ~server, id).isRunning_(true);
					},
					\monoNote, {
						Synth.basicNew(~instrument, ~server, id).isRunning_(true);
					},
					\group, {
						Group.basicNew(~server, id).isRunning_(true)
					},
					{
						Node.basicNew(~server, id).isRunning_(true)
					}
				)
			};

			switch (
				nodeObj.size,
				0, { nil },
				1, { ~nodeObject = nodeObj[0] },
				   { ~nodeObject = nodeObj }
			)
		}
	},

	setupOnFree: {
		|self|
		~nodeObject.value(self) !? {
			|nodeObj|
			~setupOnFree = nil;
			nodeObj.asArray.do ({
				|n|
				n.onFree({
					self.use { ~onFreeAction.(n) }
				})
			})
		};
	},

	onFree: {
		|self, action|
		self.use {
			if (action.notNil) {
				~setupOnFree.(self);
				~onFreeAction = ~onFreeAction.addFunc(action);
			};
		}
	},

	voiceLimit: 4
);

Event.parentEvents.default.putAll(Event.partialEvents.nodeObjectEvent);
Event.parentEvents.synthEvent.putAll(Event.partialEvents.nodeObjectEvent);
Event.parentEvents.groupEvent.putAll(Event.partialEvents.nodeObjectEvent);
)
