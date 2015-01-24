+Spec {
	plot {
		| steps = 256 |
		var plot, points;
		points = steps.collect({
			|i|
			this.map(i / steps);
		});

		plot = Plotter(this.asString(), Rect(300, 300, 400, 300));
		plot.value = points;
		plot.domainSpecs_(ControlSpec(0, 1));
		plot.minval = this.minval;
		plot.maxval = this.maxval;
		^plot;
	}
}