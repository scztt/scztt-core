PECapture : EnvironmentRedirect {
	*new { ^super.new(Event()) }

	at { arg key;
		^envir.atDefault(key, {
			PEEventPatternProxy(key)
		})
	}
}

PEEventPatternProxy {
	var <>name;
	var <>keys;
	var <>assignments;

	*new { |name| ^super.newCopyArgs(name) }

	at {
		|key|
		keys = keys ?? {()};
		^keys.atDefault(key, {
			PEEventKeyProxy(this, key)
		})
	}

	put {
		|key, value|
		assignments = assignments ?? {()};
		assignments[key] = value.asInput;
	}
}

PEEventKeyProxy {
	var <>parent, <>key;

	*new { |parent, key, type| ^super.newCopyArgs(parent, key) }

	asInput {
		^PEEventKeyProxyInput(this)
	}
}

PEEventKeyProxyInput {
	var <>from;
	*new { |from| ^super.newCopyArgs(from) }
}
