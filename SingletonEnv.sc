SingletonDef {
	var <classType;
	var type, <argsArray;

	*new {
		|type ...args|
		^super.new.init(type, args);
	}

	init {
		|inType, inArgs|
		type = inType;
		argsArray = inArgs;
	}

	type {
		^(type ?? classType)
	}
}

SingletonEnvO : Environment {
	classvar <>typeDict;

	*initClass {
		typeDict = IdentityDictionary();
	}

	*getConstructor {
		| type |
		var class;

		if (type.isKindOf(Class)) {
			class = type;
		} {
			class = typeDict[type];
		};

		if (class.isKindOf(Class)) {
			class = { |...args| class.new(*args) };
		};

		if (class.isNil) {
			Error("No type defined for %. Add to SingletonEnv:typeDict, or use a class.".format(type)).throw
		};

		^class
	}

	put {
		| key, value |
		if (value.isKindOf(Singleton) || value.isKindOf(SingletonDef)) {
			^this.putSingleton(key, value);
		} {
			^super.put(key, value);
		}
	}

	putSingleton {
		| key, value |
		if (value.isKindOf(SingletonDef)) {
			var constructor = this.getConstructor(value.type);
			value = constructor.value(key, value);
		};

		^super.put(key, value);
	}

	putGet {
		| key, value |
		if (value.isKindOf(Singleton) || value.isKindOf(SingletonDef)) {
			^this.putGetSingleton(key, value);
		} {
			^super.putGet(key, value);
		}
	}

	putGetSingleton {

	}
}