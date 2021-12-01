+View {
	*queryDialog {
		|labelString, initialString, okAction, cancelAction, position|
		var window, label, field, ok, cancel, bounds, okMenuAction, cancelMenuAction;

		window = Window(bounds:100@100, border:false);
		window.view.layout_(
			VLayout(
				HLayout(
					label = StaticText()
					.string_(labelString)
					.font_(Font("M+ 1c", 14, true)),

					field = TextField()
					.string_(initialString)
					.minWidth_(250)
					.font_(Font("M+ 1c", 14, false)),
				).margins_(0).spacing_(0),
				HLayout(
					cancel = Button()
					.states_([["Cancel"]])
					.font_(Font("M+ 1c", 14, false)),

					ok = Button()
					.states_([["Ok"]])
					.font_(Font("M+ 1c", 14, true)),
				).margins_(0).spacing_(0)
			).margins_(20)
		);

		field.setContextMenuActions(
			okMenuAction = MenuAction("Ok", {
				protect {
					okAction.(field.string);
				} {
					window.close();
				}
			}),
			cancelMenuAction = MenuAction("Cancel", {
				protect {
					cancelAction.();
				} {
					window.close();
				}
			}).shortcut_("Escape")
		);
		cancel.setContextMenuActions(cancelMenuAction);

		ok.action = 				{ okMenuAction.onTriggered(true) };
		cancel.action = 			{ cancelMenuAction.onTriggered(true) };
		field.action = 				{ okMenuAction.onTriggered(true) };
		window.endFrontAction = 	{ cancelMenuAction.onTriggered(true) };

		label.minWidth = label.sizeHint.width;

		bounds = Rect.aboutPoint(
			position ?? {
				QtGUI.cursorPosition.x@(
					Window.screenBounds.bottom - QtGUI.cursorPosition.y
				)
			},
			window.bounds.width / 2, window.bounds.height / 2
		);

		if (bounds.left < 0) { bounds.left = 0 };
		if (bounds.top < 0) { bounds.top = 0 };
		if (bounds.bottom > Window.screenBounds.bottom ) { bounds.bottom = Window.screenBounds.bottom };
		if (bounds.right > Window.screenBounds.right ) { bounds.right = Window.screenBounds.right };

		field.focus();

		window.bounds = bounds;
		window.front;
	}
}
