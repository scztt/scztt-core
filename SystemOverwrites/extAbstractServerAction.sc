+AbstractServerAction {
	*run { arg server;
		var selector = this.functionSelector;
		"".postln;
		"  <%> ".format(this.name).banner();
		this.performFunction(server, { arg obj; obj.perform(selector, server) });
		" </%> ".format(this.name).banner();
		"".postln;
	}
}
