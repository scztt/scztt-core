DereferencedEnvironmentRedirect : EnvironmentRedirect {
	var <dereferencedEnvir;

	at {
		|key|
		^this.dereferenceAt(key);
	}

	dereferenced {
		envir.keysValuesDo {
			|key|
			this.dereferenceAt(key)
		};

		^this.envir.species.newFrom(dereferencedEnvir)
	}

	dereferenceAt {
		|key|
		var value;

		dereferencedEnvir = dereferencedEnvir ?? { Environment() };

		value = dereferencedEnvir[key];

		if (value == \recursionMarker) {
			Error("Recursively referencing key: %".format(key)).throw;
		};

		if (value.isNil) {
			value = envir[key];

			if (value.isKindOf(Function)) {
				dereferencedEnvir[key] = \recursionMarker;

				try {
					if (currentEnvironment != this) {
						this.use {
							value = value.value();
						}
					} {
						value = value.value();
					};
				} {
					|e|
					dereferencedEnvir[key] = nil;
					e.throw;
				};

			};

			dereferencedEnvir[key] = value;
		};

		^value
	}
}

+Environment {
	dereferenced {
		^DereferencedEnvironmentRedirect(this).dereferenced
	}
}