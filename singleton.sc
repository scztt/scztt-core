Singleton {
	classvar <>all, <>know=false, creatingNew=false;

	var <>name, logString, <postWindow, <>postActions;

	*initClass {
		all = IdentityDictionary();
	}

	*new {
		arg name = \default ...settings;
		var sing, classAll;

		classAll = all.atFail(this, {
			all[this] = IdentityDictionary();
			all[this];
		});

		sing = classAll.atFail(name, {
			var newSingleton = this.createNew();
			newSingleton.init(name);
			newSingleton.name = name;
			classAll[name] = newSingleton;
			newSingleton;
		});

		if (settings.notNil && settings.notEmpty) { sing.set(*settings) };
		^sing;
	}

	*createNew {
		arg ...args;
		^super.new(*args);
	}

	*doesNotUnderstand { arg selector ... args;
		var item;

		if (know && creatingNew.not) {
			creatingNew = true;		// avoid reentrance
			try {
				if (selector.isSetter) {
					selector = selector.asString;
					selector = selector[0..(selector.size - 2)].asSymbol;
					item = this.new(selector, *args);
				} {
					item = this.new(selector);
				}
			};
			creatingNew = false;
			^item;
		} {
			^this.superPerformList(\doesNotUnderstand, selector, args);
		}
	}

	init {}

	set {
		// Override this to receive 'settings' parameter from Singleton.new(name, settings)
	}

}