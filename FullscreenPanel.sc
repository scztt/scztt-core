FullscreenPanel : Singleton {
	var view;

	view {
		^view ?? { this.makeView() };
	}

	makeView {
		var minSize = (Window.screenBounds.extent - (10@20)) / (4@5);
		view = View().layout_(GridLayout.rows(
			*(5.collect {
				4.collect {
					View().minSize_(minSize);
				}
			})
		)).fullScreen(true);
		view.onClose_({
			view = nil;
		});
		view;
		^view
	}

	front {
		this.view.front;
	}

	close {
		if (view.notNil) {
			view.endFullScreen();
			view.close();
		}
	}

	place {
		| view, row, column, rowSpan=1, columnSpan=1 |
		var containerView = View();
		containerView.background_(Color.grey(0.5, 0.1));
		containerView.layout_(HLayout(view));
		this.view.layout.addSpanning(
			containerView, row, column, rowSpan, columnSpan
		);
	}
}