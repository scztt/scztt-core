EventTrace : Singleton {
	var <>logEvent=true, <>logFinish=false, <>logPostFinish=false, <>logMsg, <>logBundle=false;
	var <>key;
	var <logs, logsMap;

	*new {
		|name, event, finish, postFinish, msg, bundle, all|
		if (all == true) {
			event = finish = postFinish = msg = bundle = true;
		};

		^super.new(name, event, finish, postFinish, msg, bundle)
	}

	init {
		logsMap = ();
		logs = [];
	}

	all {
		logEvent = logFinish = logPostFinish = logBundle = logMsg = true
	}

	set {
		|event, finish, postFinish, msg, bundle|

		this.logEvent = event ?? { logEvent };
		this.logFinish = finish ?? { logFinish };
		this.logPostFinish = postFinish ?? { logPostFinish };
		this.logMsg = msg ?? { logMsg };
		this.logBundle = bundle ?? { logBundle };
	}

	asStream {
		var stream = EventTraceStream(this);
		var log = EventTraceLog();

		logsMap[stream] = log;
		logs = logs.add(log);

		^stream
	}

	onEvent {
		|stream, beats, event|
		var finishFunc, callbackFunc, oldSchedFunc, oldSchedArrayFunc, oldMsgFunc;
		var log = logsMap[stream];
		var loggable;

		if (logEvent) {
			log.addEvent(\event, beats, event.copy);
		};

		if (logFinish || logPostFinish || logBundle) {

			finishFunc = {

				if (logFinish) {
					currentEnvironment[\finish] = currentEnvironment[\finish].removeFunc(finishFunc);
					loggable = currentEnvironment.copy;
					log.addEvent(\finish, beats, loggable);
				};

				if (logPostFinish) {
					callbackFunc = {
						currentEnvironment[\callback] = currentEnvironment[\callback].removeFunc(callbackFunc);
						loggable = currentEnvironment.copy;
						log.addEvent(\postFinish, beats, loggable)
					};

					currentEnvironment[\callback] = currentEnvironment[\callback].addFunc(callbackFunc)
				};

				if (logMsg) {
					oldMsgFunc = currentEnvironment[\getMsgFunc];

					currentEnvironment[\getMsgFunc] = {
						|instrument|

					}
				};

				if (logBundle) {
					oldSchedFunc = currentEnvironment[\schedBundle];
					currentEnvironment[\schedBundle] = {
						|...args|
						currentEnvironment[\schedBundle] = oldSchedFunc;
						log.addEvent(\bundle, beats, args);
						oldSchedFunc.value(*args);
					};

					oldSchedArrayFunc = currentEnvironment[\schedBundleArray];
					currentEnvironment[\schedBundleArray] = {
						|...args|
						currentEnvironment[\schedBundleArray] = oldSchedArrayFunc;
						log.addEvent(\bundle, beats, args);
						oldSchedArrayFunc.value(*args);
					}
				}
			};

			event[\finish] = event[\finish].addFunc(finishFunc)
		};
	}

	latest {
		^logs.last
	}
}

EventTraceLog {
	var <logs, <lastBeat=0;

	*new {
		^super.new.init
	}

	init {
		logs = ();
	}

	addEvent {
		|logName, beats, event|
		var log;

		log = logs.atDefault(logName, { Order() });

		log[beats] = log[beats].add(event);

		lastBeat = beats;
		{
			this.changed(\items);
		}.defer(0.00001)
	}

	at {
		|key|
		^logs[key]
	}

	atRecent {
		|key, quant=4|
		var startTime = lastBeat.trunc(quant);
		^logs[key].select {
			|item, beats|
			beats >= startTime
		};
	}
}

EventTraceStream : Stream {
	var parent, routine;
	var <startTime, <absoluteStartTime;

	*new {
		|parent|
		^super.newCopyArgs(parent).init
	}

	init {
		routine = Routine({
			|inEvent|
			startTime = thisThread.beats;
			absoluteStartTime = Main.elapsedTime;

			loop {
				if (inEvent.isNil) { ^inEvent };

				parent.onEvent(
					this,
					thisThread.beats - startTime,
					inEvent
				);

				inEvent = inEvent.yield;
			};

			inEvent
		});
	}

	next {
		|inEvent|
		^routine.next(inEvent);
	}
}

+TreeViewItem {
	expand {
		^treeView.invokeMethod(\setExpanded, [this, true] );
	}

	collapse {
		^treeView.invokeMethod(\setExpanded, [this, false] );
	}
}
