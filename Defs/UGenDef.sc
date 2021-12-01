UDef : Singleton {
	var <>arFunc, <>krFunc, <>irFunc;
	var <path;

	*new {
		arg name, ar, kr, ir;
		^super.new(name, ar, kr, ir)
	}

	*pseudoclasses {
		|introspection|

		^this.all.asArray.collect({
			|singleton|
			var instanceIntrospection = introspection.copy;
			instanceIntrospection[0] = "%(%)".format(
				"\\" ++ singleton.class.name,
				singleton.name
			);
			instanceIntrospection[1] = singleton.definitionFilename ?? { "" };
			instanceIntrospection[2] = 0;
			instanceIntrospection[5] = instanceIntrospection[5].collect {
				|method|
				method[4] = switch(method[1],
					\ar, {
						singleton.arFunc !? {
							|arFunc|
							[
								arFunc.def.argNames.as(Array),
								arFunc.def.prototypeFrame.collect { |val|
									val !? {
										if (val.class === Float) { val.asString } { val.cs }
									}
								};
							].lace [2..]
						} ?? method[4]
					},
					\kr, {
						singleton.krFunc !? {
							|krFunc|
							[
								krFunc.def.argNames.as(Array),
								krFunc.def.prototypeFrame.collect { |val|
									val !? {
										if (val.class === Float) { val.asString } { val.cs }
									}
								};
							].lace [2..]
						}
					} ?? method[4],
					\ir, {
						singleton.irFunc !? {
							|irFunc|
							[
								irFunc.def.argNames.as(Array),
								irFunc.def.prototypeFrame.collect { |val|
									val !? {
										if (val.class === Float) { val.asString } { val.cs }
									}
								};
							].lace [2..]
						}
					} ?? method[4],
					method[4]
				)
			}
		}).do(_.postln);
	}

	arArgs {
		^(arFunc !? {
			[arFunc.def.argNames, arFunc.def.prototypeFrame].flop.flatten.asEvent
		})
	}

	krArgs {
		^(krFunc !? {
			[krFunc.def.argNames, krFunc.def.prototypeFrame].flop.flatten.asEvent
		})
	}

	irArgs {
		^(irFunc !? {
			[irFunc.def.argNames, irFunc.def.prototypeFrame].flop.flatten.asEvent
		})
	}

	set {
		|ar, kr, ir|

		path = thisProcess.nowExecutingPath ?? path;

		if (ar.notNil) {
			this.arFunc_(ar)
		};

		if (kr.notNil) {
			this.krFunc_(kr)
		};

		if (ir.notNil) {
			this.irFunc_(ir)
		};
	}

	wrap {
		|wrapName, ar, kr, ir|
		var newName;

		newName = "%.%".format(name, wrapName).asSymbol;
		ar = ar !? {
			arFunc !? {
				ar.partialApplication(
					\func, arFunc
				)

			}
		};
		kr = kr !? {
			krFunc !? {
				kr.partialApplication(
					\func, krFunc
				)

			}
		};
		ir = ir !? {
			irFunc !? {
				ir.partialApplication(
					\func, irFunc
				)
			}
		};

		^UDef(newName, ar:ar, kr:kr, ir:ir)
	}

	ar_{ |func| this.arFunc = func }
	kr_{ |func| this.krFunc = func }
	ir_{ |func| this.irFunc = func }

	ar {
		|...args|
		if ((args.size == 0) && (arFunc.def.argNames.size > 0)) {
			^arFunc
		} {
			^arFunc.value(*args)
		}
	}

	kr {
		|...args|
		if ((args.size == 0) && (krFunc.def.argNames.size > 0)) {
			^krFunc
		} {
			^krFunc.value(*args)
		}
	}

	ir {
		|...args|
		if ((args.size == 0) && (irFunc.def.argNames.size > 0)) {
			^irFunc
		} {
			^irFunc.value(*args)
		}
	}

	open {
		if (path.notNil) {
			Document.open(path.asString);
		}
	}
}
