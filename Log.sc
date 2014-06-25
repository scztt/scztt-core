Log : Singleton {
	classvar defaultFormatter, onErrorAction;
	var <>actions, <>formatter, <>shouldPost = true, <>maxLength = 500, <lines;

	*initClass {
		defaultFormatter = {
			|item, log|
			"[%]".format(log.name.asString().toUpper()).padRight(12) ++ item[\string];
		}
	}

	*logErrors {
		| shouldLog = true |
		var rootThread = thisThread;

		while { rootThread.parent.notNil } {
			rootThread = rootThread.parent;
		};

		if (shouldLog) {
			rootThread.exceptionHandler =  {
				thisThread.exceptionHandler = {
					| exc |
					try {
						Log(\error, exc.errorString.replace("ERROR: ", ""));
					};

					thisThread.parent.handleError(exc);
				};
			};

			OnError.add(onErrorAction = {
				Log(\error, "---");
			})
		} {
			rootThread.exceptionHandler = nil;
		}
	}

	init {
		actions = IdentitySet();
		lines = LinkedList(maxLength);
		formatter = defaultFormatter;
	}

	addEntry {
		| item |
		lines.add(item);
		if (lines.size() > maxLength) {
			lines.popFirst();
		}
	}

	set {
		| str, level = \default |
		var logItem = (
			\string: str,
			\level: level,
			\time: Date().rawSeconds
		);
		logItem[\formatted] = this.format(logItem);

		this.addEntry(logItem);

		if (shouldPost) {
			logItem[\formatted].postln;
		};

		actions.do({
			| action |
			action.value(logItem, this);
		});
	}

	format {
		| item |
		^formatter.value(item, this);
	}
}

LogWindow : Singleton {
	var <action, <window, <textView, <names, <logs, textViewSize = 0, connected = false,
	font, boldFont, regularColor, errorColor;

	init {
		logs = IdentitySet();
		names = IdentitySet();
		font = Font("Source Code Pro", 11);
		boldFont = Font("Source Code Pro", 11, bold: true);
		regularColor = Color.grey(0.3);
		errorColor = Color.red(0.8);

		action = {
			| item, log |
			var logString, logStringSize;

			if (item.notNil && textView.notNil) {
				if (textView.isClosed.not) {
					{
						logString = item[\formatted] + "\n";
						logStringSize = logString.size();

						textView.setStringColor(regularColor, textViewSize-1, 1);
						textView.setString(logString, 999999999, 0);
						textView.setFont(boldFont, textViewSize, 12);
						if (log.name == \error) {
							textView.setStringColor(errorColor, textViewSize, logStringSize - 1)
						};

						textViewSize = logStringSize + textViewSize;
						textView.select(textViewSize, 0);
					}.defer();
				}
			}
		}
	}

	set {
		| namesArray |
		var newNames;

		if (namesArray.isKindOf(Symbol) || namesArray.isKindOf(String)) {
			namesArray = [ namesArray ];
		};

		newNames = IdentitySet.newFrom(namesArray);

		if ((names -- newNames).size() > 0) {
			this.clear();
			names = newNames;
			logs = names.collect({
				|name|
				Log(name);
			});
			this.gui();
		}
	}

	disconnect {
		if (connected) {
			logs.do({
				| log |
				log.actions.remove(action);
			});
		}
	}

	connect {
		if (connected.not) {
			if (window.notNil) {
				if (window.isClosed.not) {
					logs.do(this.initForLog(_));
				}
			}
		}
	}

	initForLog {
		| log |
		log.actions.add(action);
	}


	update {
		action.value();
	}

	clear {
		if (window.notNil) {
			textView.string = "\n";
			textViewSize = 1;
		}
	}

	close {
		if (window.notNil) {
			window.rememberPosition(LogWindow, name);
			window.close();
		}
	}

	gui {
		if (window.notNil and: { window.isClosed }) { window = nil };
		if (window.isNil) {
			{
				window = View();
				textView = TextView()
				.autohidesScrollers_(true)
				.editable_(false)
				.background_(Color(0.85, 0.9, 0.85, 0.7))
				.font_(font);

				window.recallPosition(\LogWindow, name);
				window.autoRememberPosition(\LogWindow, name);

				window.layout_(VLayout(textView).margins_(5).spacing_(0));

				CmdPeriod.add(this);

				this.connect();
				window.onClose_({
					this.disconnect();
					window = nil;
					textView = nil;
					CmdPeriod.remove(this);
					textViewSize = 0;
				});
			}.defer();
		};

		this.update();
		window.front;
	}

	cmdPeriod {
		this.clear();
	}
}