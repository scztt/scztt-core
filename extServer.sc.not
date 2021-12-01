+Server {
	plotTree {|interval=0.5|
		var onClose, window = Window.new(name.asString + "Node Tree",
			Rect(128, 64, 400, 400),
			scroll:true
		).front;
		window.view.hasHorizontalScroller_(false).background_(Color.grey(0.9));
		onClose = this.plotTreeView(interval, window.view, { defer {window.close}; });

		window.recallPosition(\plotTreeView);
		window.autoRememberPosition(\plotTreeView);

		window.onClose = {
			onClose.value;
		};
	}

	plotTreeView {|interval=0.5, actionIfFail|
		var resp, done = false;
		var collectChildren, levels, countSize;
		var view, userview, bounds;
		var updater, updateFunc;
		var tabSize = 25;
		var pen, font, maxSize = 10, string, freeFunc;
		var groupIdentical, closeButtons, closeButtonRect;

		pen = GUI.current.pen;
		font = Font.sansSerif(10);

		view = UserView();
		view.mouseDownAction_({
			|v, x, y|
			closeButtons.keysValuesDo({
				|rect, action|
				if (rect.containsPoint(x@y)) {
					action.value();
				}
			})
		});

		view = ScrollView();
		view.canvas = userview = UserView();
		userview.drawFunc = {
			|v|
			var xtabs = 0, ytabs = 0, drawFunc;

			closeButtons = IdentityDictionary();

			drawFunc = {|group|
				var thisSize, rect, endYTabs;
				xtabs = xtabs + 1;
				ytabs = ytabs + 1;
				pen.font = font;

				group.do({|node|
					if(node.value.isArray, {
						thisSize = countSize.value(node);
						endYTabs = ytabs + thisSize + 0.2;
						rect = Rect(xtabs * tabSize + 0.5,
							ytabs * tabSize + 0.5,
							v.bounds.width - (xtabs * tabSize * 2),
							thisSize * tabSize;
						);
						pen.fillColor = Color.grey(0.8);
						pen.fillRect(rect);
						pen.strokeRect(rect);
						pen.color = Color.black;
						pen.stringInRect(
							" Group" + node.key.asString +
							(node.key == 1).if("- default group", ""),
							rect
						);

						drawFunc.value(node.value);
						ytabs = endYTabs;
					},{
						if (node.value.isKindOf(Event)) {
							string = " % (x%)".format(node.key.asString, node.value[\all].size);
							freeFunc = {
								this.makeBundle(nil, {
									node.value[\all].do({
										|n|
										"freeing %".format(n.key).postln;
										Node.basicNew(this, n.key).free;
									})
								})
							};
						} {
							string = " % %".format(node.key.asString, node.value.asString);
							freeFunc = {
								"freeing %".format(node.key).postln;
								Node.basicNew(this, node.key).free
							};
						};

						rect = Rect(xtabs * tabSize + 0.5,
							ytabs * tabSize + 0.5,
							7 * tabSize,
							0.8 * tabSize
						);
						pen.fillColor = Color.white;
						pen.fillRect(rect);
						pen.strokeRect(rect);
						pen.color = Color.black;
						pen.stringInRect(string, rect);

						closeButtonRect = Rect.fromPoints(rect.rightTop + (-10@12), rect.rightTop);
						pen.stringInRect("x", closeButtonRect);
						closeButtons[closeButtonRect] = freeFunc;

						ytabs = ytabs + 1;
					});
				});
				xtabs = xtabs - 1;
			};

			groupIdentical.value(levels);
			drawFunc.value(levels);
		};

		// msg[1] controls included
		// msg[2] nodeID of queried group
		// initial number of children
		resp = OSCFunc({ arg msg;
			var finalEvent;
			var i = 2, j, controls, printControls = false, dumpFunc;
			if(msg[1] != 0, {printControls = true});
			dumpFunc = {|numChildren|
				var event, children;
				event = ().group;
				event.id = msg[i];
				event.instrument = nil; // need to know it's a group
				i = i + 2;
				children = Array.fill(numChildren, {
					var id, child;
					// i = id
					// i + 1 = numChildren
					// i + 2 = def (if synth)
					id = msg[i];
					if(msg[i+1] >=0, {
						child = dumpFunc.value(msg[i+1]);
					}, {
						j = 4;
						child = ().synth.instrument_(msg[i+2]);
						if(printControls, {
							controls = ();
							msg[i+3].do({
								controls[msg[i + j]] = msg[i + j + 1];
								j = j + 2;
							});
							child.controls = controls;
							i = i + 4 + (2 * controls.size);
						}, {i = i + 3 });
					});
					child.id = id;
				});
				event.children = children;
				event;
			};
			finalEvent = dumpFunc.value(msg[3]);
			done = true;
			collectChildren = {|group|
				group.children.collect({|child|
					if(child.children.notNil,{
						child.id -> collectChildren.value(child);
					}, {
						child.id -> child.instrument;
					});
				});
			};
			levels = collectChildren.value(finalEvent);
			countSize = {|array|
				var size = 0;
				array.do({|elem|
					if(elem.value.isArray, { size = size + countSize.value(elem.value) + 2}, {size = size + 1;});
				});
				size
			};

			groupIdentical = {|group|
				var metaNode, synthGroups;
				synthGroups = ();
				group.do({
					|node|
					if (node.value.isArray.not) {
						synthGroups[node.value] = synthGroups[node.value] ? List();
						synthGroups[node.value].add(node);
					} {
						groupIdentical.value(node.value);
					}
				});

				synthGroups.keysValuesDo({
					| key, nodes |
					var metaNode;

					if (nodes.size > maxSize) {
						group.removeAll(nodes);
						metaNode = ();
						metaNode[\all] = nodes;
						group.add(key -> metaNode);
					}
				});
			};

			defer {
				userview.bounds = Rect(0, 0, view.bounds.width, max(400, tabSize * (countSize.value(levels) + 2)));
			}
		}, '/g_queryTree.reply', addr).fix;

		updateFunc = {
			fork {
				loop {
					this.sendMsg("/g_queryTree", 0, 0);
					interval.wait;
				}
			}
		};
		updater = updateFunc.value;
		CmdPeriod.add(updateFunc);
		SystemClock.sched(3, {
			if(done.not, {
				actionIfFail.value();
				"Server failed to respond to Group:queryTree!".warn;
			});
		});

		view.onClose = {
			updater.stop;
			CmdPeriod.remove(updateFunc);
			resp.free;
		};

		^view;
	}
}