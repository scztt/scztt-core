ObjectBrowserView : View {
	classvar summaryDict;

	var <obj;
	var objList;
	var <tree, idObjMap, unexpanded;

	*initClass {
		summaryDict = IdentityDictionary();
	}

	*new {
		|...args|
		^super.new(*args).init
	}

	fonts {
		^(
			name: 	Font("M+ 2c", 12),
			class:	Font("M+ 2c", 12),
			summary:Font("M+ 2c", 10)
		)
	}

	init {
		this.layout = HLayout(
			tree = TreeView(bounds:450@300).columns_(["name", "class", "summary"])
		);

		unexpanded = IdentitySet();
		idObjMap = IdentityDictionary();

		tree.connectFunction('expanded(QcTreeWidget::ItemPtr)', { |v, i| this.onExpand(v, i) });
		tree.connectFunction('collapsed(QcTreeWidget::ItemPtr)', { |v, i| this.onCollapse(v, i) });

		tree.currentItem = 0;
	}

	obj_{
		|inObj, depth=2|
		if (inObj != obj) {
			var newItem;

			obj = inObj;

			tree.clear();
			objList = List();

			newItem = this.insertRowFor(0, "[object]", obj);
			this.populateObject(newItem, obj, depth);
		}
	}

	removeChildren {
		|item|
		var child;
		while { (child = item.childAt(0)) != nil } {
			tree.removeItem(child);
		};
	}

	onExpand {
		|tree, item|
		item.prValidItem(tree);
		if (unexpanded.includes(item.id)) {
			unexpanded.remove(item.id);
			this.removeChildren(item);
			this.populateObject(item, idObjMap[item.id], 1);
		};

		this.columnResize();
	}

	onCollapse {
		|tree, item|
		var child;
		item.prValidItem(tree);

		this.removeChildren(item);
		item.insertChild(0, ["placeholder", "", ""]);

		unexpanded.add(item.id);
		this.columnResize();
	}

	insertRowFor {
		|index, name, obj, item|
		var newItem, nameStr, classStr;

		nameStr = " ".wrapExtend(name.asString.size * 2);
		classStr = " ".wrapExtend(obj.class.name.asString.size * 2);

		if (item.notNil) {
			item.insertChild(0, [nameStr, classStr, ""]);
			newItem = item.childAt(0);
			objList.insert(item.index, obj);
		} {
			tree.insertItem(index, [nameStr, classStr, ""]);
			newItem = tree.itemAt(index);
			objList.insert(index, obj);
		};

		newItem.setView(0, this.makeNameView(name));
		newItem.setView(1, this.makeClassView(obj));
		newItem.setView(2, this.makeSummaryView(obj));
		idObjMap[newItem.id] = obj;

		if (obj.slotSize > 0) {
			unexpanded.add(newItem.id);
			newItem.insertChild(0, ["placeholder", "", ""])
		};

		^newItem;
	}

	populateObject {
		|item, obj, depth=1|
		if ((depth > 0) && obj.notNil) {
			var varNames;

			case
			{ obj.isKindOf(Dictionary) } {
				^this.populateDictionary(item, obj, depth)
			}
			{ obj.isKindOf(Order) } {
				^this.populateOrder(item, obj, depth)
			}
			{ obj.isKindOf(Collection) } {
				^this.populateArray(item, obj, depth)
			}
			{ obj.isKindOf(QObject) } {
				// Do not return - we want regular object props also
				this.populateQObject(item, obj, depth)
			};

			varNames = obj.class.instVarNames;
			obj.slotSize.reverseDo {
				|i|
				var name = varNames.notNil.if({ varNames[i] }, { i });
				var newItem = this.insertRowFor(i, name, obj.slotAt(i), item);
				this.populateObject(newItem, obj.slotAt(i), depth - 1)
			};

			this.columnResize();
		}
	}

	populateDictionary {
		|item, obj, depth=1|
		obj.keys.asArray.sort({ |a, b| a.asString.toLower < b.asString.toLower }).reverse.do {
			|key, i|
			var val, newItem;
			val = obj.at(key);
			key = if (key.isKindOf(Symbol) || key.isKindOf(String)) {
				key.asString
			} {
				"[%]".format(key.class.name)
			};
			newItem = this.insertRowFor(i, key, val, item);
			this.populateObject(newItem, val, depth - 1)
		}
	}

	populateOrder {
		|item, obj, depth=1|
		var key, val, newItem;

		obj.indices.reverseDo({
			|index, i|
			i = obj.indices.size - i - 1;

			val = obj.array[i];
			key = index;

			newItem = this.insertRowFor(i, key, val, item);
			this.populateObject(newItem, val, depth - 1)
		})
	}

	populateArray {
		|item, obj, depth=1, limit=64|
		var array = obj.asArray;
		var i = min(limit, array.size - 1);
		while { i >= 0 } {
			var index = obj.isKindOf(ArrayedCollection).if(i, "");
			var subObj = array[i];
			var newItem = this.insertRowFor(i, index, subObj, item);
			this.populateObject(newItem, subObj, depth - 1);
			i = i - 1;
		}
	}

	populateQObject {
		|item, obj, depth=1|
		obj.properties.do {
			|property, i|
			var val, newItem;
			val = try { obj.getProperty(property) } { "?" };
			newItem = this.insertRowFor(i, "% [qt]".format(property), val, item);
			this.populateObject(newItem, val, depth - 1)
		}
	}

	makeNameView {
		|name|
		var view, text;
		text = StaticText()
		// .fixedHeight_(26)
		.minWidth_(150)
		.string_(name)
		.font_(this.fonts[\name]);
		text.minWidth = text.sizeHint.width;

		view = View().layout_(HLayout(text).margins_(2));
		view.fixedWidth = 200;

		^text
	}

	makeClassView {
		|obj|
		var view, text = StaticText()
		// .fixedHeight_(26)
		.minWidth_(150)
		.string_(obj.class.name)
		.font_(this.fonts[\class]);

		view = View().layout_(HLayout(text).margins_(2));
		^text
	}

	makeSummaryView {
		|obj|
		var text = StaticText()
			// .fixedHeight_(26)
			.maxWidth_(400)
			.stringColor_(Color.grey(0.5))
			.align_(\left)
			.font_(this.fonts[\summary]);

		case
		{ obj.isKindOf(String) } {
			text.string = obj;
		}
		{ obj.isKindOf(Collection) } {
			text.string = "[size=%]".format(obj.size);
		}
		{
			text.string = obj.asString;
		};

		^View().layout_(HLayout(text).margins_(2))
	}

	columnResize {
		2.do {
			|i|
			tree.invokeMethod(\resizeColumnToContents, [i]);
		};
	}
}

+Object {
	inspect {
		if (TreeView().methods(false, true, false).detect({ |s| s.asString.contains("expanded(QcTreeWidget::ItemPtr)") }).notNil) {
			var view = ObjectBrowserView().obj_(this).bounds_(Rect(300, 200, 400, 300)).front;
			view.autoRememberPosition(\ObjectBrowserView, \inspect);
			^view
		} {
			^this.inspectorClass.new(this)
		}
	}
}