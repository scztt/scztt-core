InstalledBase {
	var func, server;
	var <installed = false;

	*new {
		|func, server|
		server = server ?? Server.default;
		^super.newCopyArgs(func, server).install;
	}

	install {
		if (installed.not) {
			installed = true;

			ServerTree.add(this, server);
			ServerQuit.add(this, server);
			CmdPeriod.add(this);

			if (server.serverRunning) {
				this.ensureRunning();
			};
		}
	}

	free {
		if (installed) {
			installed = false;

			this.ensureStopped();
			ServerTree.remove(this, server);
			ServerQuit.remove(this, server);
			CmdPeriod.remove(this);
		}
	}

	doOnCmdPeriod {}

	doOnServerTree {}

	doOnServerQuit {}

	isRunning {
		^this.subclassResponsibility(\isRunning)
	}

	ensureRunning {
		^this.subclassResponsibility(\ensureRunning)
	}

	ensureStopped {
		^this.subclassResponsibility(\ensureStopped)
	}
}

InstalledBuffer : InstalledBase {
	var buffer;

	doOnServerBoot {
		this.ensureRunning();
	}

	doOnServerQuit {
		buffer = nil;
	}

	isRunning {
		^buffer.notNil
	}

	ensureRunning {
		if (buffer.isNil && server.serverRunning) {
			buffer = func.value(buffer)
		}
	}

	ensureStopped {
		if (buffer.notNil) {
			buffer.free;
			buffer = nil;
		}
	}

	buffer {
		this.ensureRunning();
		^buffer;
	}

	bufnum 			{ ^this.buffer.bufnum }
	asUGenInput 	{ ^this.buffer.index }
	asControlInput 	{ ^this.buffer.asControlInput }
}

InstalledBus : InstalledBase {
	var bus;

	doOnServerBoot {
		this.ensureRunning();
	}

	doOnServerQuit {
		bus = nil;
	}

	isRunning {
		^bus.notNil
	}

	ensureRunning {
		if (bus.isNil && server.serverRunning) {
			bus = func.value(server)
		}
	}

	ensureStopped {
		if (bus.notNil) {
			bus.free;
			bus = nil;
		}
	}

	bus {
		this.ensureRunning();
		^bus;
	}

	asBus 			{ ^this.bus }
	asMap 			{ ^this.bus.asMap }
	asUGenInput 	{ ^this.bus.asUGenInput }
	asControlInput 	{ ^this.bus.asControlInput }
	index 			{ ^this.bus.index }
	ar				{ ^this.bus.ar }
	kr				{ ^this.bus.kr }
}

InstalledNode : InstalledBase {
	var node;

	*new {
		|func, server|
		server = server ?? Server.default;
		^super.newCopyArgs(func, server).install;
	}

	doOnCmdPeriod {
		node = nil;
	}

	doOnServerTree {
		node = nil;
		this.ensureRunning();
	}

	doOnServerQuit {
		node = nil;
	}

	isRunning {
		^(node.notNil and: {node.isRunning})
	}

	func_{
		|inFunc|
		this.free;
		func = inFunc;
		this.install;
	}

	ensureRunning {
		node ?? {
			this.send();
		};
	}

	ensureStopped {
		node !? {
			node.free;
			node = nil;
		};
	}

	send {
		node = func.value(server);
		node.onFree(this.onNodeFree(_));
	}

	onNodeFree {
		|freed|
		if (freed == node) {
			node = nil;
		}
	}

	node {
		this.ensureRunning();
		^node
	}

	asTarget {
		^this.node;
	}

	asGroup {
		^this.node.isKindOf(Group).if(node, nil)
	}

	asControlInput {
		^this.node.asControlInput
	}
}