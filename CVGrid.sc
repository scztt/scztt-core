CVGrid : Singleton {
	var <cvList, <cvViews, <gridView, rows=8;

	*font {
		| size=10, bold=false |
		^Font("M+ 1c", size, bold)
	}

	init {
		cvList = List();
	}

	set {
		|...cvs|
		cvList = cvs.clump(2).collect {
			|pair|
			CVGridCell(*pair);
		};
		cvList.postln;

		this.update();
	}

	fromEnvir {
		|...cvs|
		cvList = cvs.collect {
			|cv|
			CVGridCell(currentEnvironment.findKeyForValue(cv), cv)
		}
	}

	rows_{
		|val|
		rows = val;
		this.update();
	}

	update {
		var cells;
		if (gridView.notNil) {
			cvViews.do(_.remove());
			cvViews = cvList.collect(_.view());
			cells = cvViews ++ (nil ! (rows - (cvViews.size % rows))) ++ (nil ! rows);
			cells = cells.clump(rows);
			cells.postln;
			gridView.layout = GridLayout.rows(
				*cells
			)
		}
	}

	front {
		gridView ?? { gridView = this.createGridView(); this.update(); };
		gridView.front;
	}

	createGridView {
		^(gridView = View().onClose_({
			gridView = nil;
		}));
	}

	view {
		^gridView;
	}
}

CVGridCell {
	var <>name, <>cv, view, title, bar, value, cvConnection;

	*new {
		|name, cv|
		^super.newCopyArgs(name, cv);
	}

	view {
		view ?? { view = this.createView() };
		this.update();
		^view;
	}

	createView {
		var v;
		v = View().layout_(VLayout().spacing_(1).margins_(1));
		if (cv.notNil) {
			v.layout.add(
				(title = StaticText()
					.align_(\left)
					.font_(CVGrid.font(10, true))
					.maxHeight_(11)
					.stringColor_(QtGUI.palette.windowText)),
				align: \bottomLeft
			);

			v.layout.add(
				bar = UserView().maxHeight_(1).minHeight_(1).drawFunc_(this.drawFunc(_))
			);

			v.layout.add(
				(value = NumberBox()
					.setProperty(\styleSheet, "border: transparent; background: transparent;")
					.align_(\left)
					.font_(CVGrid.font(18, false))
					.normalColor_(QtGUI.palette.highlightText)
					.background_(Color.clear)
					.focusColor_(Color.clear)
				)
			);

			cv.connect(value);
			cvConnection = cv.action_({
				{ bar.refresh() }.defer
			});
		};

		v.layout.add(nil);

		v.onClose ={
			view = nil;
			cvConnection.remove();
		};
		^v
	}

	drawFunc {
		|v|
		Pen.fillColor_(Color.hsv(0.555, 1, 0.6 + 0.05, 0.3));
		Pen.fillRect(Rect(0, 0, v.bounds.width, 1));
		Pen.fillColor_(Color.hsv(0.555, 1, 0.6 + 0.2));
		Pen.fillRect(Rect(0, 0, v.bounds.width * cv.input, 1));
	}

	update {
		if (cv.notNil) {
			title.string = name.toUpper();
			//		title.sizeHint;
			value.value = cv.value;
		}
	}
}