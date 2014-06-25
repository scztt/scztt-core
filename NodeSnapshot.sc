NodeSnapshot { var <>nodeId; }

GroupSnapshot : NodeSnapshot {
	var <>numChildren, <children;
	*new {
		^super.new.init()
	}

	init {
		children = List();
	}

	asString {
		arg indent = 0;
		var str, indentString = ("  " ! indent).join("");
		str = indentString ++ "+ Group: %".format(nodeId);
		if (children.notEmpty) {
			str = str + "\n" + children.collect({ |ch| ch.asString(indent + 1) }).join("\n");
		}
		^str
	}
}

SynthSnapshot : NodeSnapshot {
	var <>defName, <controls;

	*new {
		^super.new.init()
	}

	init {
		controls = IdentityDictionary();
	}

	asString {
		arg indent = 0;
		var str, indentString = (("  " ! indent).join(""));
		str = indentString ++ "+ Synth(%): %s".format(nodeId, defName);
		if (controls.notEmpty) {
			str = str ++ "\n" ++ controls.asKeyValuePairs.clump(2).collect({
				|pair|
				indentString ++ "    %: %".format(*pair)
			}).join("\n")
		};
		^str
	}
}

TraceParser {
	var msg, index, hasControls, <rootNode, <nodes;

	*parse {
		|msg|
		^super.newCopyArgs(msg).parse();
	}

	parse {
		//[ /g_queryTree.reply, 1, 0, 2, 1, 0, 1000, -1, volumeAmpControl2, 4, volumeAmp, 0.2786121070385, volumeLag, 0.10000000149012, gate, 1, bus, 0 ]
		index = 1;
		nodes = List();
		hasControls = this.next() == 1;
		rootNode = this.parseNode();
	}

	parseNode {
		var snapshot, nodeId, numChildren;
		nodeId = this.next();
		numChildren = this.next();
		if (numChildren < 0) {
			snapshot = SynthSnapshot().nodeId_(nodeId);
			nodes.add(snapshot);
			this.parseSynth(snapshot);
			^snapshot;
		} {
			snapshot = GroupSnapshot().nodeId_(nodeId).numChildren_(numChildren);
			nodes.add(snapshot);
			this.parseGroup(snapshot);
			^snapshot;
		}
		^GroupSnapshot().nodeId_(nodeId)
	}

	parseGroup {
		arg snapshot;
		snapshot.numChildren.do {
			var child = this.parseNode();
			snapshot.children.add(child);
		}
	}

	parseSynth {
		arg snapshot;
		var controlCount;
		snapshot.defName = this.next();
		if (hasControls) {
			controlCount = this.next();
			controlCount.do {
				var name, val;
				name = this.next(); val = this.next();
				snapshot.controls[name] = val;
			}
		}
	}

	next {
		arg items = 1;
		var nextMsgItem;
		if (items == 1) {
			nextMsgItem = msg[index];

		} {
			nextMsgItem = msg[index..(index + items - 1)];
		};
		index = index + items;
		^nextMsgItem
	}

	@= {

	}
}

TestSnap {
	var server, msg, <nodes, <root;

	*new {
		|a, b|
		^super.newCopyArgs(a, b)
	}

	asString {
		^"asdf"
	}

}

TreeSnapshot {
	var server, msg, <nodes, <root;

	*get {
		arg action, node;
		var server;
		node = node ?? { RootNode(Server.default) };
		server = node.server;

		OSCFunc({
			arg msg;
			var snapshot, parsed;
			snapshot = TreeSnapshot(server, msg);
			action.value(snapshot);
		}, '/g_queryTree.reply').oneShot;

		server.sendMsg("/g_queryTree", node.nodeID, 1);
	}

	*new {
		arg server, traceMsg;
		^super.newCopyArgs(server, traceMsg).parse()
	}

	parse {
		var parsed;
		parsed = TraceParser.parse(msg);
		nodes = parsed.nodes;
		root = parsed.rootNode;
	}

	asString {
		^"TreeSnapshot\n" ++ root.asString(1);
	}

	storeOn {
		| stream, indent=0 |
		stream << "TreeSnapshot: " ++ this.asString();
	}
}