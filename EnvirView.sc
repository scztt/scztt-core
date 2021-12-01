EnvirWindow {
	classvar <window, envirs, envirTabMap,
	bigFont, smallFont;

	*initClass {
		envirs = IdentityDictionary();
		envirTabMap = IdentityDictionary();
	}

	*show {
		if (window.isNil) {
			window = this.makeWindow();
			window.onClose_({
				window = nil;
				envirTabMap.clear();
			});
		};

		window.front();
	}

	*makeWindow {
		window = View(bounds:Rect(100, 100, 300, 400));
		bigFont = Font("Helvetica", 16, true);
		smallFont = Font("Helvetica", 10);
		window.layout = HLayout();

		envirs.keysValuesDo({
			| envir, name |
			this.buildTabFor(envir, name);
		});

		window.recallPosition(\EnvirView);
		window.autoRememberPosition(\EnvirView);

		^window;
	}

	*add {
		| envir, name |
		name = name ? envir[\name] ? "unnamed";

		if (envirs[envir].isNil) {
			envirs[envir] = name;

			if (window.notNil) {
				{
					envirTabMap[envir] = this.buildTabFor(envir, name);
					//window.bounds = window.bounds.width_(window.bounds.width * envirTabMap.size / (envirTabMap.size - 1));
				}.defer();
			}
		}
	}

	*remove {
		| envir |
		var tab = envirTabMap[envir];

		envirs[envir] = nil;

		if (window.notNil) {
			{
				tab.remove();
				envirTabMap[envir] = nil;
				//window.bounds = window.bounds.width_(window.bounds.width * envirTabMap.size / (envirTabMap.size + 1));
			}.defer;
		}
	}

	*update {
		| envir |
		this.add(envir);

		if (window.notNil) {
			{ this.buildTabFor(envir, envirs[envir], true) }.defer;
		}
	}

	*buildTabFor {
		|envir, name, update = false|
		var scrollView, view, subView, hasContents = false, collView, cvCount = 0, closeButton, copyButton, pushButton;

		if (update && envirTabMap[envir].notNil) {
			view = envirTabMap[envir];
			try {
				view.children.do(_.remove());
			}
		} {
			view = View();
		};

		closeButton = (Button()
			.states_([["—"]])
			.font_(smallFont)
			.maxHeight_(16).maxWidth_(16)
			.action_({
				this.remove(envir);
			})
		);

		copyButton = (Button()
			.states_([["c"]])
			.font_(smallFont)
			.maxHeight_(16).maxWidth_(16)
			.action_({
				var str = List();
				envir.keysValuesDo({
					|key, val|
					if (val.isKindOf(CV)) {
						str.add("~%.value%= %;".format(
							key.asString,
							("\t" ! (28 - (key.asString.size + 9) / 4).max(0)).join(""),
							val.value
						));
					}
				});

				if (str.notEmpty()) {
					ScIDE.newDocument(name ++ " cv's", str.join("\n"));
				};
		}));

		pushButton = (Button()
			.states_([["↑"]])
			.font_(smallFont)
			.maxHeight_(16).maxWidth_(16)
			.action_({
				"% pushed.".format(envir[\name]).postln;
				envir.push();
			})
		);

		view.layout_(VLayout(
			[HLayout(
				[StaticText().font_(bigFont).string_(name), align:\right ],
				nil,
				[pushButton, align: \right ],
				[copyButton, align: \right ],
				[closeButton, align: \right ],
			), align: \top],
			ScrollView().canvas_(
				subView = View().layout_(VLayout().spacing_([0, 0, 0, 0]))
			)
		));

		envir.keysValuesDo({
			|key, val|
			var hidden = true, items = List(), item;

			if (val.isKindOf(CV)) {
				cvCount = cvCount + 1;
				subView.layout.insert(
					this.buildCVView(val, "~" ++ key.asString),
					index:0,
					stretch:0,
					align: \topRight,
				);
			} {
				if (val.isKindOf(Collection)) {

					hasContents = false;
					collView = View().layout_(QVLayout(
						StaticText().string_("~" ++ key.asString).font_(bigFont).align_(\right).mouseDownAction_({
							if (hidden) {
								hidden = false;
								items.do(_.visible_(true));
							} {
								hidden = true;
								items.do(_.visible_(false));
							}
						})
					));
					collView.background = Color.grey(0.6, 0.2);

					val.do({
						|item, i|
						if (item.isKindOf(CV)) {
							hasContents = true;
							item = this.buildCVView(item, "[%]".format(i));
							items.add(item);
							item.visible = false;
							collView.layout.add(item);
						}
					});

					if (hasContents) {
						subView.layout.add(collView);
					};
				}
			}
		});

		subView.layout.insert(nil, index:cvCount ,stretch:1);

		envirTabMap[envir] = view;

		//scrollView = ScrollView().layout_(VLayout(view));
		window.layout.add(view);

		^view;
	}

	*buildCVView {
		|cv, label|
		var view, numBox, font;
		view = View().layout_(HLayout(
			nil,
			StaticText().string_(label ++ ":").font_(smallFont),
			numBox = NumberBox().maxWidth_(40).maxHeight_(16).align_(\right).font_(smallFont)
		).spacing_(0).margins_(0));
		cv.connect(numBox);
		^view;
	}
}

+State {
	updateEnvirWindow {
		{ EnvirWindow.update(this.envir) }.defer;
	}
}