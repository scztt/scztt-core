(
var buses = Set();
var views = ();

v = View(bounds:600@200).layout_(VLayout(nil).spacing_(2).margins_(4));

r = Routine({
	inf.do {
		var added, removed;
		var new = Ndef.all[s.name] !? _.envir !? _.values ?? Set();

		removed	= buses.difference(new);
		added  	= new.difference(buses);

		removed.do {
			|r|
			views[r] !? _.remove;
		};

		added.do {
			|a|
			views[a] = ScChannelMonitor(bus:a, label:a.key);
			views[a].meter.setStyle(( style: \continuous, \fixedHeight: 12 ));
			v.layout.insert(views[a], 1);
		};

		buses 		= new;
		buses.asArray.sort({ |a, b| a.key.asString < b.key.asString }).do {
			|b|
			v.layout.add(views[b]);
		};
		m.do {
			|m|
			v.layout.add(m);
		};
		"added: %   removed: %".format(added, removed);

		1.0.wait;
	}
}).play(AppClock);

{ r.stop }.doAfter(v);

m = [
	// ScChannelMonitor(bus:~b, label:" NOTES"),
	ScChannelMonitor(bus:Bus(\audio, 0, 2, s), label:" MASTER"),
];
m.do({ |m| m.meter.setStyle(( style: \continuous, \fixedHeight: 12 )) });

v.front;
)
