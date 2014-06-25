WindowHandleView : UserView {
	var <background, dragging, dragStart, <>win, isWin;

	*qtClass { ^'QcCustomPainted' }

	*new { arg parent, bounds;
		var me = super.new(parent, bounds ?? {this.sizeHint} );
		me.canFocus = false;
		me.drawImage();
		me.drawFunc = me.drawBorder(_);
		me.mouseDownAction_(me.mouseDown(_,_,_));
		me.mouseMoveAction_(me.mouseMove(_,_,_));
		me.mouseUpAction_(me.mouseUp(_,_,_));
		^me;
	}

	mouseDown {
		|x, y|
		isWin = false;
		win = this.parents.last;
		if (win.respondsTo(\findWindow)) {
			win = win.findWindow();
			isWin = true;
		};
		dragStart = x@y;
	}

	mouseMove {
		|x, y|
		var moved, newPos = x@y;
		moved = newPos - dragStart;
		if (isWin) { moved.y = moved.y.neg };
		win.bounds = win.bounds.moveBy(moved.x, moved.y);
		//dragStart = newPos;
	}

	mouseUp {
		|v, x, y|
	}

	background_{
		|bk|
		this.setBackgroundImage(background = bk, 5);
	}

	drawBorder {
		var b, w, h;
		b = this.bounds.moveTo(0, 0);
		w = b.width;
		h = b.height;
		Pen.strokeColor = Color.grey(0.1, 0.3);
		Pen.line(0@h, 0@0);
		Pen.line(0@0, w@0);
		Pen.stroke();

		Pen.strokeColor = Color.grey(0.5, 0.2);
		Pen.line(w@0, w@h);
		Pen.line(w@h, 0@h);
		Pen.stroke();
	}

	drawImage {
		|v|
		var w = 63, h = 63, onW=4, offW=3, color, height;
		this.background = Image.newEmpty(w, h);
		background.draw({
			var x;
			(w / onW + (h / onW)).do {
				| x |
				Pen.width = 1;

				x = x * (onW + offW);
				Pen.strokeColor = Color.grey(0.5, 0.6);
				Pen.line(x@0, (x-h)@h);
				Pen.stroke();

				x = x + onW;
				Pen.strokeColor = Color.grey(0.1, 0.4);
				Pen.line(x@0, (x-h)@h);
				Pen.stroke();
			};
		})
	}
}