
MonitorView : Singleton {
	var view, oscFunc;
	var window;
	var <monitorTree, monitorViewTree;
	var cols=4, colNum=0, rowNum=0;

	view {
		if (window.isNil) {
			this.makeView();
		};

		^window;
	}

	reconcileViews {
		var added, removed;
		monitorTree
	}

	makeView {
		window = Window(bounds:400@300);
		window.layout_(GridLayout());
		window.autoRememberPosition(\MonitorView, name);

		monitorTree = MultiLevelIdentityDictionary();
		monitorViewTree = MultiLevelIdentityDictionary(); // this should always parallel monitorTree

		fork({
			inf.do {
				monitorTree.leafDo {
					|key, value|
					var counter;
					if (monitorViewTree.at(*key).isNil) {
						counter = MonitorCounter("%:%".format(*key), "", Font("M+ 2c", 10), Color.blue);
						monitorViewTree.put(key[0], key[1], counter);
						window.layout.add(counter.view, rowNum, colNum);
						colNum = (colNum + 1) % cols;
						if (colNum == 0) { rowNum = rowNum + 1 };
					} {
						counter = monitorViewTree.at(key[0], key[1]);
					};
					counter.value = value;
				};

				1.wait;
			}
		}, AppClock);

		if (oscFunc.isNil) {
			oscFunc = OSCFunc({
				|msg|
				this.onMsg(msg[1], msg[0].asString[9..].asSymbol, msg[3], msg[4], msg[5]);
			}, '/monitor/*', dispatcher:MonitorDispatcher())
		}
	}

	onMsg {
		|node, name, value, min, max|
		monitorTree.put(node, name, [value, min, max]);
	}
}

MonitorDispatcher : OSCMessageDispatcher {

	value {|msg, time, addr, recvPort|
		var pattern;
		pattern = msg[0];
		active.keysValuesDo({|key, func|
			if(pattern.matchOSCAddressPattern(key), {func.value(msg, time, addr, recvPort);});
		})
	}

	typeKey { ^('OSC matched').asSymbol }

}

MonitorCounter {
	var name, units, font, color, min, max, maxColor, minFixed=false, maxFixed=false,
	<view, heading, number,
	valueHistory, minHistory, maxHistory, timeHistory,
	<history, historySize=512, <>historyTime = 5, <>span=1,
	counter = 0
	;

	*new {
		|name, units, font, color, min, max, maxColor|
		^super.newCopyArgs(name, units, font, color, min, max, maxColor, min.notNil, max.notNil)
		.init;
	}

	init {
		var mod = if (QtGUI.palette.window.asHSV[2] > 0.5) {-0.5} {0.3};
		history = Array[historySize];

		timeHistory = Array.newFrom(0 ! historySize);
		valueHistory = Array.newFrom(0 ! historySize);
		minHistory = Array.newFrom(0 ! historySize);
		maxHistory = Array.newFrom(0 ! historySize);

		view = UserView().layout_(VLayout(
			heading = (StaticText()
				.string_(name)
				.font_(font)
				.stringColor_(Color.grey)
				.align_(\center)
			),
			number = (StaticText()
				.font_(font.boldVariant.size_(font.size + 2))
				.stringColor_(Color.grey(0.7 + mod))
				.align_(\center)
			)
		).margins_(0).spacing_(0))
		.drawFunc_({
			|v|
			var curTime, i, b = v.bounds, size;
			var now = AppClock.seconds;
			var x, y, j;

			Pen.push();

			Pen.width = 0;
			Pen.scale(b.width, b.height.neg);
			Pen.translate(0, -1);
			Pen.moveTo(0@0);

			curTime = now;
			i = 1;

			while { i < historySize } {
				j = ((counter - i) % historySize).asInteger;
				curTime = timeHistory[j];
				if ((now - curTime) < historyTime) {
					x = (now - curTime).linlin(0, historyTime, 0, 1);
					y = maxHistory[j].linlin(min, max, 0, 1);
					Pen.lineTo((x@y));

					i = i + 1;
				} {
					i = historySize;
				}
			};

			Pen.lineTo(0@0);
			if (maxColor.notNil && (history.first.linlin(min, max, 0, 1) > 0.9)) {
				Pen.fillColor = maxColor.alpha_(0.3);
				Pen.strokeColor = maxColor.blend(Color.white, 0.1).alpha_(0.8);
			} {
				Pen.fillColor = color.alpha_(0.3);
				Pen.strokeColor = color.blend(Color.white, 0.1).alpha_(0.8);
			};
			Pen.draw(3);

			Pen.pop();

		})
		.mouseDownAction_(this.resetMinMax(_));
		this.resetMinMax();
	}

	resetMinMax {
		if (maxFixed.not) { max = -9999999 };
		if (minFixed.not) { min = 9999999 };
		view.refresh();
	}

	size_{
		|count|
		var diff = count - history.size;
		if (diff > 0) {
			history = history ++ LinkedList([0,0,0,0] ! diff);
		};
		if (diff < 0) {
			history = history[0..(count - 1)];
		};
	}

	value_{
		|val|
		timeHistory[counter] = AppClock.seconds;
		valueHistory[counter] = val[0];
		maxHistory[counter] = val[1];
		minHistory[counter] = val[2];
		counter = (counter + 1) % historySize;

		if ((val[2] < min) && minFixed.not) {
			min = val[2];
		};
		if ((val[1] > max) && maxFixed.not) {
			max = val[1];
		};

		number.string = (val[0].round(0.01).asString + units);
		view.refresh();
	}
}


+UGen {
	monitor {
		|name, rate=1|
		var trig = Impulse.kr(rate);
		SendReply.kr(trig, ("/monitor/" ++ name).asSymbol,
			[this, RunningMin.kr(this, trig), RunningMax.kr(this, trig)]
			, NodeID.ir);
		^this
	}
}


