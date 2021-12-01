+ Function {
	e {
		arg envir;
		^this.inEnvir(envir ?? currentEnvironment);
	}

	partialApplication {
		|...args|
		var factory, argNames;

		args = args.asDict;
		argNames = this.def.argNames;
		argNames = Array.newFrom(argNames);
		argNames = argNames.removeAll(args.keys);

		factory = "{
			|innerFunc, argsDict|
			argsDict = innerFunc.def.makeEnvirFromArgs.putAll(argsDict);
			{
				%
				var finalArgsDict = argsDict.copy.putPairs([%]);
				innerFunc.valueWithEnvir(finalArgsDict);
			}
		}".format(
			argNames.notEmpty.if("|" ++ argNames.join(", ") ++ "|", ""),
			argNames.collect({
				|a|
				"'%', %".format(a, a)
			}).join(", ")
		).interpret;

		^factory.(this, args)
	}
}
