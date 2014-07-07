NodeSnapshot {
	var <>server, <>nodeId;

	asNode {
		^Node.basicNew(server, nodeId);
	}

	== {
		|other|
		^(this.class == other.class) and: {
			nodeId == other.nodeId
		}
	}

}

GroupSnapshot : NodeSnapshot {
	var <>numChildren, <children;
	*new {
		^super.new.init()
	}

	init {
		children = List();
	}

	asGroup {
		^Group.basicNew(server, nodeId);
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
	var <defName, <def, <controls;

	*new {
		^super.new.init()
	}

	init {
		controls = IdentityDictionary();
	}

	asSynth {
		^Synth.basicNew(defName, server, nodeId);
	}

	== {
		| other |
		// the only way we have of matching in cases where a synthdef has been changed from one update to the next is
		// matching via the def itself, not the def name
		^(this.class == other.class) and: {
			(nodeId == other.nodeId) && (def == other.def);
		}
	}

	defName_{
		| inDefName |
		defName = inDefName;
		def = SynthDescLib.match(defName);
	}

	outputs {
		if (def.notNil) {
			^def.outputs.collect({
				|out|
				out = out.copy();
				if (out.startingChannel.isKindOf(Symbol)) {
					out.startingChannel = controls[out.startingChannel];
				};
				out;
			})
		} {
			^[]
		}
	}

	outBusses {
		^this.outputs.collect {
			|output|
			Bus(output.rate, output.startingChannel, output.numberOfChannels, server);
		}
	}

	controlsString {
		|join=" ", dec = 0.01|
		^controls.asKeyValuePairs.clump(2).collect({
			|pair|
			"%: %".format(pair[0], pair[1].round(dec))
		}).join(join);
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
	var server, msg, index, hasControls, <rootNode, <nodes;

	*parse {
		|server, msg|
		^super.newCopyArgs(server, msg).parse();
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
			snapshot = SynthSnapshot().server_(server).nodeId_(nodeId);
			nodes.add(snapshot);
			this.parseSynth(snapshot);
			^snapshot;
		} {
			snapshot = GroupSnapshot().server_(server).nodeId_(nodeId).numChildren_(numChildren);
			nodes.add(snapshot);
			this.parseGroup(snapshot);
			^snapshot;
		}
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
	classvar <>dump=false;
	var server, msg, <nodes, <root, <drawFunc;

	*get {
		arg action, node;
		var server;
		node = node ?? { RootNode(Server.default) };
		server = node.server;

		OSCFunc({
			arg msg;
			var snapshot, parsed;
			if (dump) { msg.postln };
			snapshot = TreeSnapshot(server, msg);
			{ action.value(snapshot) }.defer;
		}, '/g_queryTree.reply').oneShot;

		server.sendMsg("/g_queryTree", node.nodeID, 1);
	}

	*new {
		arg server, traceMsg;
		^super.newCopyArgs(server, traceMsg).parse()
	}

	nodeIDs {
		^nodes.collect(_.nodeId);
	}

	parse {
		var parsed;
		parsed = TraceParser.parse(server, msg);
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

TreeSnapshotView : Singleton {
	var <view, <viewMap, <viewsInUse, currentSnapshot, collapse=false,
	groupColor, groupOutline, autoUpdateRoutine
	;

	init {
		viewMap = IdentityDictionary();
		viewsInUse = IdentitySet();
		groupColor = Color.hsv(0.35, 0.6, 0.5, 0.5);
		groupOutline = Color.grey(1, 0.3);
	}

	front {
		if (view.isNil) {
			this.makeNew();
		} {
			view.front;
		};
	}

	autoUpdate {
		|up=true|
		if (up && autoUpdateRoutine.isNil) {
			autoUpdateRoutine = fork({
				inf.do {
					TreeSnapshot.get({
						|sn|
						this.update(sn);
					});
					0.5.wait;
				}
			});
			this.front();
		} {
			if (up.not && autoUpdateRoutine.notNil) {
				autoUpdateRoutine.stop.reset();
				autoUpdateRoutine = nil;
			}
		}
	}

	update {
		|newSn|
		var oldViews;

		if (view.notNil) {
			oldViews = viewsInUse;
			viewsInUse = IdentitySet();
			currentSnapshot = newSn;
			this.makeViewNode(currentSnapshot.root);
			oldViews.difference(viewsInUse).do {
				|toRemove|
				toRemove.view.remove();
				if (viewMap[toRemove.snapshot.nodeId] == toRemove) {
					viewMap[toRemove.snapshot.nodeId] = nil;
				}
			}
		}
	}

	makeNew {
		TreeSnapshot.get({
			|sn|
			{
				currentSnapshot = sn;
				view = ScrollView(bounds:Rect(200, 200, 500, 600));
				view.canvas = View().layout_(VLayout(
					this.makeViewNode(sn.root),
					nil
				));
				view.canvas.background = QtGUI.palette.window;
				view.onClose = {
					this.autoUpdate(false);
					view = nil;
					viewsInUse.clear();
					viewMap.clear();
				};

				view.front;
			}.defer
		})
	}

	makeViewNode {
		| node ...args |
		var viewObj = viewMap[node.nodeId];
		if (viewObj.notNil) {
			if (viewObj.snapshot == node) {
				viewObj.snapshot = node;
			} {
				viewObj = nil;
			}
		};

		case
		{ node.isKindOf(GroupSnapshot) } {
			viewObj = viewObj ?? { this.makeViewGroup(node, *args) };
			this.populateViewGroup(viewObj);
		}
		{ node.isKindOf(SynthSnapshot) } {
			viewObj = viewObj ?? { this.makeViewSynth(node, *args) };
		};

		// { node.isKindOf(Collection) } {
		// 	if ((node.size() > 1) && collapse) {
		// 		^this.makeViewCollapsedSynth(node, *args);
		// 	} {
		// 		^this.makeViewSynth(node, *args);
		// 	}
		// }
		viewObj.set(node);
		viewsInUse.add(viewObj);
		viewMap[node.nodeId] = viewObj;
		^viewObj.view;
	}

	drawBorder {
		|v, color|
		Pen.addRect(v.bounds.moveTo(0, 0).insetBy(0.5, 0.5));
		Pen.strokeColor = color;
		Pen.strokeRect();
	}

	separateSynths {

	}

	makeViewGroup {
		| group |
		var gsv = GroupSnapshotView(group);
		gsv.view = (UserView()
			.background_(groupColor)
			.drawFunc_(this.drawBorder(_, groupOutline))
		);
		gsv.view.layout = VLayout().spacing_(3).margins_(5);
		gsv.view.layout.add(
			StaticText().font_(Font("M+ 1c", 10, true))
			.string_("[%] group".format(group.nodeId))
			.mouseUpAction_({
				|v|
				gsv.folded = gsv.folded.not;
				gsv.view.children.do({
					|c|
					if (c.isKindOf(StaticText).not) { c.visible = gsv.folded };
				})
			})
		);

		// if (collapse) {
		// 	(group.children
		// 		.separate(this.separateSynthDefs(_))
		// 		.collect(this.makeViewNode(_))
		// 	)
		// } {
		// 	group.children.collect(this.makeViewNode(_)).do {
		// 		|v|
		// 		gsv.view.layout.add(v);
		// 	};
		// };

		^gsv
	}

	populateViewGroup {
		| gsv |
		if (collapse) {
			(gsv.snapshot.children
				.separate(this.separateSynthDefs(_))
				.collect(this.makeViewNode(_))
			)
		} {
			gsv.snapshot.children.collect(this.makeViewNode(_)).do {
				|v|
				gsv.view.layout.add(v);
				v.visible = gsv.folded.not;
			};
		};
	}

	makeViewSynth {
		|synth|
		var sv = SynthSnapshotView(synth);
		sv.view = UserView().layout_(
			VLayout().spacing_(0).margins_(2)
		);

		sv.view.layout.add(this.makeSynthHeader(sv));
		sv.view.layout.add(2);
		sv.view.layout.add(this.makeSynthBody(sv));

		(sv.view.background_(Color.grey(0.2, 0.8))
			.minHeight_(20)
			.drawFunc_(~drawBorder.(Color.grey(1, 0.3), _))
		);

		^sv
	}

	makeSynthHeader {
		|sv|
		^HLayout(
			(StaticText()
				.string_("[%]".format(sv.synth.nodeId))
				.stringColor_(QtGUI.palette.highlightText)
				.font_(Font("M+ 1c", 10, false))
			),
			10,
			(StaticText()
				.string_("\\" ++ sv.synth.defName)
				.stringColor_(QtGUI.palette.highlightText)
				.font_(Font("M+ 1c", 10, true))
			),
			nil,
			[StaticText()
				.string_("✕")
				.align_(\right)
				//				.stringColor_(QtGUI.palette.highlightText)
				.font_(Font("M+ 1c", 8, true))
				.mouseDownAction_({
					sv.synth.asSynth.free();
					TreeSnapshot.get({ |sn| this.update(sn); })
				}),
				align:\topRight
			]
		)
	}

	makeSynthBody {
		|sv|
		^HLayout(this.makeSynthControls(sv), nil)
	}

	makeSynthControl {
		| sv, controlName, value |
		var view, valueField;
		view = View().minWidth_(50);
		view.layout = HLayout().spacing_(0).margins_(1);
		view.layout.add(nil);
		view.layout.add(
			StaticText()
			.maxHeight_(10)
			.string_(controlName.asString.toUpper)
			.font_(Font("M+ 1c", 8, false))
			.stringColor_(QtGUI.palette.windowText),
			align: \bottomRight
		);

		sv.controls[controlName] = valueField = (TextField()
			.palette_(QPalette().window_(Color.clear).base_(Color.clear))
			.maxHeight_(14)
			.minWidth_(230)
			.maxWidth_(230)
			.stringColor_(QtGUI.palette.highlightText)
			.font_(Font("M+ 1c", 10, true))
		);

		view.layout.add(valueField, align: \bottomRight);

		^[view, align:\right]
	}

	/*	makeSynthControl {
	| sv, controlName, value |
	var view, nameField, valueField;
	// view = View().minWidth_(50);
	// view.layout = HLayout().spacing_(0).margins_(1);

	nameField = (StaticText()
	.maxHeight_(10)
	.string_(controlName.asString.toUpper)
	.align_(\right)
	.font_(Font("M+ 1c", 8, false))
	.stringColor_(QtGUI.palette.windowText)
	);

	sv.controls[controlName] = valueField = (TextField()
	.palette_(QPalette().window_(Color.clear).base_(Color.clear))
	.maxHeight_(14)
	.align_(\left)
	.stringColor_(QtGUI.palette.highlightText)
	.font_(Font("M+ 1c", 10, true))
	);

	// view.layout.add(valueField, align: \bottomRight);
	// view.layout.add(nil);

	^[ [nameField, align: \bottomRight], [valueField, align: \bottomLeft] ];
	}*/

	makeSynthControls {
		|sv, cols=6|
		var layout, controls, controlViews;
		controls = sv.synth.controls.asKeyValuePairs.clump(2);
		controlViews = controls.collect({
			|keyval|
			this.makeSynthControl(sv, *keyval)
		});
		controlViews = controlViews ++ (nil ! (cols - (controlViews.size % cols)));

		^GridLayout.rows(
			*(controlViews.clump(cols))
		).spacing_(1);
	}

	makeSynthOutput {
		|sv, output|
		var type, bus;
		type = (output.rate == \audio).if(" ▸", " ▹");
		bus = output.startingChannel;
		if (bus.isNumber.not) { bus = "∗" };

		^(StaticText()
			.align_(\left)
			.string_("% %".format(type, bus))
			.font_(Font("M+ 1c", 10))
			.stringColor_(QtGUI.palette.highlightText)
			.background_(Color.grey(0.5, 0.5))
			.maxHeight_(12)
			.minWidth_(20)
			.maxWidth_(20)
		);
	}

	makeSynthOutputs {
		|synth|
		var view = View().layout_(VLayout().spacing_(1).margins_(0));

		synth.outputs.do {
			|output|
			view.layout.add(this.makeSynthOutput(synth, output));
		}

		^view;
	}

}

GroupSnapshotView {
	var <>snapshot, <>view, <>folded = false;

	*new {
		|groupSnapshot, view|
		^super.newCopyArgs(groupSnapshot, view);
	}

	set {}
}

SynthSnapshotView {
	var <>snapshot, <>view, <>controls;

	*new {
		|synthSnapshot, view|
		^super.newCopyArgs(synthSnapshot, view, IdentityDictionary());
	}

	synth { ^snapshot }

	set {
		|newSynth|
		snapshot = newSynth;
		snapshot.controls.keysValuesDo {
			|controlName, value|
			var view = controls[controlName];
			if (view.hasFocus.not) {
				view.string_("%".format(value.round(0.001)))
				.action_({
					|v|
					var node, val;
					val = v.value;
					if ((val[0] == $c) || (val[0] == $a)) {
						snapshot.asSynth.map(controlName.asSymbol, val[1..].asInteger)
					} {
						snapshot.asSynth.set(controlName.asSymbol, val.asFloat);
					};

					{ v.background = Color.clear }.defer(0.1);
					v.background_(v.background.blend(Color.green(1, 0.1)));
					v.focus(false);
				})
			}
		}
	}
}