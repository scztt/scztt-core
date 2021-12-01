// Broadcaster {
// 	classvar broadcasters, anonymousBroadcasters;
// 	classvar uniqueId = 0;
//
// 	var obj;
// 	var connections;
// 	var enabled=true, hasEnumerated=false;
//
// 	*initClass {
// 		broadcasters = IdentityDictionary();
// 		anonymousBroadcasters = IdentitySet();
// 	}
//
// 	*new {
// 		|obj|
// 		var newObj;
//
// 		if (obj.isNil) {
// 			newObj = super.newCopyArgs(obj);
// 			anonymousBroadcasters.add(newObj);
// 			^newObj;
// 		} {
// 			^(broadcasters[obj] ?? {
// 				newObj = super.newCopyArgs(obj);
// 				broadcasters[obj] = newObj;
// 				newObj.init;
// 			})
// 		}
// 	}
//
// 	init {
// 		connections = IdentityDictionary();
// 	}
//
// 	broadcast {
// 		|signal, value|
// 		if (enabled) {
// 			connections[signal].do {
// 				|connection|
// 				connection.notify(this.obj, signal, value);
// 			}
// 		}
// 	}
// }
//
// Connection {
// 	var <broadcaster, <reciever;
// 	var connected=true;
//
// 	*new {
// 		|b, r|
// 		^super.newCopyArgs(b, r).init
// 	}
// }
//
// SimpleConnection : Connection {
// 	notify {
// 		|who, what, value|
// 		reciever.notify(who, what, value);
// 	}
//
// 	disconnect {
// 		broadcaster.removeConnection(this);
// 		reciever.removeConnection(this);
// 	}
//
// 	connect {
//
// 	}
// }
//
// Reciever {
//
// }
