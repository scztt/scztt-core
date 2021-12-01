Pdef(\voiceLimiter, {
	var voices = Array(16);
	var func = {
		|e|

		e[\id].asArray.do {
			|id|
			voices = voices.add(id);
		};

		~count = ~count ?? { ~voices };
		max(0, voices.size - (~count ?? {4})).do {
			|i|
			s.sendMsg(\n_set, voices[i], \gate, -1 - (~gateTime ?? {0.2}));
		};

		e.onFree({
			|node|
			voices.remove(node.nodeID);
		});
	};

	Pbind(
		\callback, Pfunc({
			|e|
			e[\callback].addFunc(func);
		})
	)
});
