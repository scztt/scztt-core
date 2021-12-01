CVal : Singleton {
	var value, spec;
	var controller;

	init {
		|name|
		controller = SimpleController(this);
		spec = ControlSpec();
		value = spec.default ?? 0;
	}

	set {
		|valueOrSpec|
		if (valueOrSpec.isKindOf(Spec)) {
			this.setSpec(valueOrSpec)
		} {
			this.value = valueOrSpec;
		}
	}

	action_ { | function | ^SimpleController(this) .put(\synch, function) }

// reading and writing the CV
	value_ { | val |
		value = spec.constrain(val);
		this.changed(\synch, this);
	}
	input_	{ | in | this.value_(spec.map(in)); }
	input 	{ ^spec.unmap(value) }
	asInput 	{ | val | ^spec.unmap(val) }

// setting the ControlSpec
	spec_ 	{ | s, v |
				spec = s.asSpec;
				this.value_(v ? spec.default);
	}
	sp	{ | default= 0, lo = 0, hi=0, step = 0, warp = 'lin' |
		this.spec = ControlSpec(lo,hi, warp, step, default);
	}

	db	{ | default= 0, lo = -100, hi = 20, step = 1, warp = 'lin' |
		this.spec = ControlSpec(lo,hi, warp, step, default);
	}

// split turns a multi-valued CV into an array of single-valued CV's
	split {
		^value.collect { |v| CV(spec, v) }
	}

// Stream and Pattern support
	next { ^value }
	reset {}
	embedInStream { ^value.yield }


// ConductorGUI support
	draw { |win, name =">"|
		if (value.isKindOf(Array) ) {
			~multicvGUI.value(win, name, this);
		} {
			~cvGUI.value(win, name, this);
		}
	}

	*buildViewDictionary {
		var connectDictionary = (
			numberBox:		CVSyncValue,
			slider:			CVSyncInput,
			rangeSlider:		CVSyncProps(#[lo, hi]),
			slider2D:			CVSyncProps(#[x, y]),
			multiSliderView:	CVSyncMulti,
			popUpMenu:		SVSync,
			listView:			SVSync,
			ezSlider:			CVSyncValue,
			ezNumber:			CVSyncValue,
			knob:			CVSyncInput,
			button:			CVSyncValue,
		);
		CV.viewDictionary = IdentityDictionary.new;

		GUI.schemes.do { | gui|
			var class;
			#[
			numberBox, slider, rangeSlider, slider2D, multiSliderView,
			popUpMenu, listView,
			tabletSlider2D, ezSlider, ezNumber, knob, button].collect { | name |
				if ( (class = gui.perform(name)).notNil) {
					CV.viewDictionary.put(class, connectDictionary.at(name))
				}
			}
		};
	}
	connect { | view |
		CV.viewDictionary[view.class].new(this, view) ;
	}

	asControlInput { ^value.asControlInput }
	asOSCArgEmbeddedArray { | array| ^value.asOSCArgEmbeddedArray(array) }

	indexedBy { | key |
		^Pfunc{ | ev | value.at(ev[key] ) }
	}

	windex { | key |
		^Pfunc{ | ev | value.asArray.normalizeSum.windex  }
	}

	at { | index | ^value.at(index) }
	put { | index, val | value = value.putt(index, val) }
	size { ^value.size }
}