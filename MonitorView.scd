+UGen {
	monitor {
		|name, rate|
		var trig = Impulse.kr(rate);
		SendReply.kr(trig, ("/monitor/" ++ name).asSymbol,
			[this, RunningMin.kr(this, trig), RunningMax.kr(this, trig)]
			, NodeID.ir)
	}
}

MonitorView : Singleton {
	var view, oscFunc;
	var window;

	view {
		if (view.isNil) {
			this.makeView();
		};

		^view;
	}

	makeView {
		window = Window(bounds:400@300);
		window.autoRememberPosition(\MonitorView, name);

		if (oscFunc.notNil) {
			oscFunc = OSCFunc({
				|a, b, c|
				[a, b, c].postln;
			}, '/monitor/*');
		}
	}

}

