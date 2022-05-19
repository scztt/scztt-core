+BusPlug {
	stop { | fadeTime, reset = false |
		var bundle = MixedBundle.new;
		monitor.stopToBundle(bundle, fadeTime);
		bundle.schedSend(this.homeServer, this.clock ? TempoClock.default, this.quant);
		if(reset) { monitor = nil };
		this.changed(\stop, [fadeTime, reset]);
	}
}