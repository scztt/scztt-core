+Document {
	restoreCurrentEnvironment {
		if (savedEnvir.notNil) {
			currentEnvironment = savedEnvir;
			savedEnvir = nil;
		}
	}

	saveCurrentEnvironment {
		if (envir.notNil) {
			savedEnvir = currentEnvironment;
			currentEnvironment = envir;
			if (savedEnvir != currentEnvironment) {
				var name;

				if (envir.respondsTo(\name)) {
					name = envir.name;
				} {
					name = envir['name'] ?? "%[%]".format(envir.class, envir.identityHash)
				};

				"*** --> % ***".format(name).postln;
			}
		}
	}
}