// ResourceTree : Environment {
// 	classvar <>currentResourceTree, resourceStack;
// }
//
// Resource : Singleton {
// 	var server;
// 	var createMethod, createArgs, obj;
//
// 	server_{
// 		| newServer |
// 		if (server.notNil) {
// 			ServerQuit.remove(this, server);
// 			ServerTree.remove(this, server);
// 			if (server.serverRunning) {
// 				this.doOnServerQuit(server);
// 			}
// 		};
//
// 		server = newServer;
//
// 		ServerQuit.add(this, server);
// 		ServerTree.add(this, server);
// 		if (server.serverRunning) {
// 			this.doOnServerTree(server);
// 		};
// 	}
//
// 	set {
// 		| newCreateMethod, newCreateArgs |
// 		server.makeBundle(-1, {
// 			if (obj.notNil) {
// 				this.free();  // should nil-ify obj
// 			};
//
//
// 		})
//
// 	}
//
//
// 	resourceInit {
// 		| method, args |
// 		this.set(method, args);
// 	}
//
// 	set {
// 		| method, args |
// 		createMethod = method;
// 		createArgs = args;
// 	}
//
// 	create {
// 		^Error.subclassResponsibility;
// 	}
//
// 	free {
// 		^Error.subclassResponsibility;
// 	}
// }
//
// BufferResource : Resource {
// 	*alloc {
// 		| numFrames, numChannels, completionMessage, bufnum |
// 		^super.new.resourceInit(\alloc,
// 		[numFrames, numChannels, completionMessage, bufnum]);
// 	}
//
// 	*allocConsecutive {
// 		| numBufs, numFrames, numChannels, completionMessage |
// 		^super.new.resourceInit(\allocConsecutive,
// 		[numBufs, numFrames, numChannels, completionMessage]);
// 	}
//
// 	*read {
// 		| path, startFrame, numFrames, action |
// 		^super.new.resourceInit(\read,
// 		[path, startFrame, numFrames, action]);
// 	}
//
// 	*readChannel {
// 		| path, startFrame, numFrames, channels, action |
// 		^super.new.resourceInit(readChannel,
// 		[path, startFrame, numFrames, channels, action]);
// 	}
//
// 	*readNoUpdate {
// 		| path, startFrame, numFrames, bufnum, completionMessage |
// 		^super.new.resourceInit(readChannel,
// 		[path, startFrame, numFrames, bufnum, completionMessage]);
// 	}
//
// 	*loadCollection {
// 		| collection, numChannels, action |
// 		^super.new.resourceInit(readChannel,
// 		[collection, numChannels, action]);
// 	}
//
// 	*sendCollection {
// 		| collection, numChannels, wait, action |
// 		^super.new.resourceInit(readChannel,
// 		[collection, numChannels, wait, action]);
// 	}
//
//
// }
//
// NodeResource {
//
// }
//
// GroupResource {
//
// }
//
// SynthResource {
//
// }