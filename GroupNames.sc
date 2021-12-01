GroupNames {
	classvar <>names;

	*initClass {
		names = ();
	}

	*groupName {
		|nodeID|
		^(GroupNames.names[nodeID] ?? { Gdef.nodeMap[nodeID] !? _.name } ?? { nil })
	}
}

+Group {
	name {
		^GroupNames.groupName(nodeID)
	}

	name_{
		|name|
		GroupNames.names[nodeID] = name;
		this.onFree({
			GroupNames.names[nodeID] = nil;
		})
	}
}