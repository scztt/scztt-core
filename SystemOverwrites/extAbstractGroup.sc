+AbstractGroup {
	/** immediately sends **/
	*new { arg target, addAction=\addToHead, permanent=false;
		var group, server, addActionID;
		target = target.asTarget;
		server = target.server;
		group = this.basicNew(server, permanent.if({ server.nextPermNodeID }));
		addActionID = addActions[addAction];
		group.group = if(addActionID < 2) { target } { target.group };
		server.sendMsg(this.creationCmd, group.nodeID, addActionID, target.nodeID);
		^group
	}
}