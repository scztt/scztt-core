CVGrid : Singleton {
	var <cvList, <cvViews, gridView, rows=8, cols, <maxWidth;

	*font {
		| size=10, bold=false |
		^Font("M+ 1c", size, bold)
	}

	init {
		cvList = List();
	}

	maxWidth_{
		|w|
		maxWidth = w;

		if (cvViews.size > 0) {
			cvViews.do {
				|v|
				v.maxWidth = maxWidth;
			}
		}
	}

	set {
		|...cvs|
		this.clear();

		cvs.clump(2).collect {
			|pair|
			this.add(pair[0], pair[1], nil, false);
		};

		this.update();
	}

	add {
		|name, cv, index, update=true|
		var view;

		index = index ?? { cvList.size };

		if (name.isNil || cv.isNil) {
			view = CVGridCellEmpty();
		} {
			if (cv.isKindOf(Dictionary)) {
				view = CVGridGroup(name, cv);
			} {
				view = CVGridCell(name, cv);
			}
		};

		cvList.insert(index, view);

		if (update) { this.update() };
	}

	fromEnvir {
		|envir ...cvs|
		if (envir.isKindOf(Environment) || envir.isKindOf(EnvironmentRedirect)) {
			// just use envir
		} {
			cvs = [envir] ++ cvs;
			envir = currentEnvironment;
		};

		if (cvs.isEmpty) {
			cvs = envir.keys.asArray.sort.collect(envir.at(_));
		};

		cvList = cvs.collect {
			|cv|
			if (cv.notNil) {
				CVGridCell(envir.findKeyForValue(cv), cv)
			} {
				CVGridCellEmpty()
			}
		}
	}

	fromEvent {
		|event|
		var list = List();
		event.keysValuesDo {
			|name, cv|
			if (cv.isKindOf(Dictionary)) {

			}
		}
	}

	rows_{
		|val|
		rows = val;
		cols = nil;
		this.update();
	}

	cols_{
		|val|
		rows = nil;
		cols = val;
		this.update();
	}

	update {
		var cells;

		if (gridView.notNil) {
			cvViews.do(_.remove());
			cvViews = cvList.collect(_.view());
			cvViews.do(_.maxWidth_(maxWidth ?? 200));
			cells = cvViews.copy;// ++ (nil ! (rows - (cvViews.size % rows))) ++ (nil ! rows);

			if (cols.notNil) {
				if (cells.size < cols) {
					cells = cells ++ (nil ! (cols - cells.size))
				};
				cells = cells.clump(cols);
				gridView.layout = GridLayout.rows(
					*cells
				);
				cols.do(gridView.layout.setColumnStretch(_, 1));
			} {
				if (cells.size < rows) {
					cells = cells ++ (nil ! (rows - cells.size))
				};
				cells = cells.clump(rows);
				gridView.layout = GridLayout.columns(
					*cells
				);
				rows.do(gridView.layout.setRowStretch(_, 1));
			};
		}
	}

	gridView {
		gridView ?? { gridView = this.createGridView(); this.update(); };
		^gridView
	}

	front {
		this.gridView.front;
	}

	createGridView {
		^(gridView = View().onClose_({
			gridView = nil;
		}).autoRememberPosition(\CVGrid, name));
	}

	clear {
		cvList = List();
		cvViews.do(_.remove());
		cvViews = List();
		this.update();
	}

	view {
		^gridView;
	}

	close {
		gridView !? { gridView.close }
	}
}

CVGridGroup {
	var <>name, <contents, rows=8, view, title;

	*new {
		|name, contents, rows|
		^super.newCopyArgs(name, contents, rows).init;
	}

	init {

	}

	view {
		view ?? { view = this.createView() };
	}

	createView {
		var v;
		v = View().layout
	}
}

CVGridCellEmpty {
	var view, <cv;
	view {
		view ?? { view = View() };
		^view;
	}
}

CVGridCell {
	var <>name, <>cv, view, title, bar, value, unitsStr, cvConnection,
	<step=1, <modStep=0.1;

	*new {
		|name, cv|
		^super.newCopyArgs(name ?? ("_"), cv).init;
	}

	init {
		this.step = (cv.spec.minval - cv.spec.maxval).abs / 100;
	}

	view {
		if (view.isNil or: { view.isClosed() }) {
			view = this.createView();
		};
		this.update();
		^view;
	}

	step_{
		|val|
		step = val;
		this.update();
	}

	modStep_{
		|val|
		modStep = val;
		this.update();
	}

	units {
		|units|
		cv.spec.units = units;
		this.update();
	}

	createView {
		var v, decimals;
		v = View().layout_(VLayout().spacing_(1).margins_(1)).maxWidth_(250);
		v.mouseDownAction_({ |...args| this.sliderClick(*args) });
		v.mouseMoveAction_({ |...args| this.sliderClick(*args) });
		v.maxWidth_(100).maxHeight_(100);


		if (cv.notNil) {
			decimals = case(
				{ cv.spec.step >= 1 }, { 0 },
				{ cv.spec.step >= 0.1 }, { 1 },
				{ 2 }
			);

			v.layout.add(HLayout(
				[value = NumberBox()
					.setProperty(\styleSheet, "border: transparent; background: transparent;")
					.align_(\right)
					.font_(CVGrid.font(18, false))
					.clipLo_(cv.spec.minval)
					.clipHi_(cv.spec.maxval)
					.minDecimals_(decimals).maxDecimals_(decimals)
					.normalColor_(QtGUI.palette.highlightText)
					.background_(Color.clear)
					.focusColor_(Color.clear),
				align: \bottomRight],
				[unitsStr = StaticText()
					.align_(\left)
					.font_(CVGrid.font(10, false))
					.stringColor_(QtGUI.palette.windowText.alpha_(0.6))
					.fixedWidth_(16)
					.background_(Color.clear)
					.string_(cv.spec.units.asString ?? "asdf"),
				align: \bottomLeft]
			), align: \bottom);
			v.layout.add(
				(bar = UserView()
					.fixedHeight_(1)
					.drawFunc_(this.drawFunc(_))
				)
			);
			v.layout.add(
				(title = StaticText()
					.align_(\center)
					.font_(CVGrid.font(10, true))
					.maxHeight_(11)
					.stringColor_(QtGUI.palette.windowText)
				),
				align: \top
			);

			unitsStr.bounds = unitsStr.bounds.size_(unitsStr.sizeHint);

			cvConnection = ConnectionList [
				cv.signal(\value).connectTo(
					value.valueSlot,
					bar.methodSlot("refresh()")
				),
				value.signal(\value).connectTo(cv.valueSlot)
			];
		};

		v.layout.add(nil);

		v.onClose = {
			view = nil;
			cvConnection.free();
		};
		^v
	}

	sliderClick {
		| view, x, y |
		var relX = (x.clip(0, inf) / view.bounds.width);
		cv.input = relX; // * (cv.spec.maxval - cv.spec.minval) + cv.spec.minval;
	}

	drawFunc {
		|v|
		Pen.fillColor_(Color.hsv(0.555, 1, 0.6 + 0.05, 0.3));
		Pen.fillRect(Rect(0, 0, v.bounds.width, 1));
		Pen.fillColor_(Color.hsv(0.555, 1, 0.6 + 0.2));
		Pen.fillRect(Rect(0, 0, v.bounds.width * cv.input, 1));
	}

	update {
		if (cv.notNil && view.notNil) {
			title.string = name.toUpper();
			//		title.sizeHint;
			value.value = cv.value;
			value.scroll_step = step;
			value.shift_scale = step / modStep;
			unitsStr = cv.spec.units.asString;
		}
	}
}