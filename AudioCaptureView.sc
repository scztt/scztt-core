AudioCaptureView : Singleton {
	var view, label, <capture, recording, metadata,
	recordingPath,
	recordButton, recordingStatus, recordingHistory, <track, artist, album, commit, path, channels;
	var connections, progressConn, amplitudeConn, status, gitUpdate;

	init {
		var fieldChanged;
		view = View().layout_(VLayout());
		view.canReceiveDragHandler_({
			|view|
			if (view.class.currentDrag.class == String) {
				File.exists(view.class.currentDrag)
			} {
				false;
			}
		});

		view.receiveDragHandler_({
			|view|
			var commit, dir;
			this.setPath(view.class.currentDrag);
		});

		label = {
			|text, align=\right|
			(StaticText()
				.string_(text ++ ":")
				.align_(align)
				.fixedWidth_(70)
				.font_(this.font)
			)
		};

		capture = AudioCapture(thisProcess.platform.recordingsDir, Server.default);
		recordingHistory = GraphCounter("", nil, this.font(8), Color.green, 0.0, 1.0, historySize:160);

		recording = View().layout_(VLayout(
			HLayout(
				nil,
				label.("CHANNELS"),
				channels = NumberBox().maxWidth_(60).value_(capture.numChannels)
			).margins_(0),
			HLayout(
				recordButton = Button().states_([
					["RECORD", nil, Color.green.sat_(0.7).alpha_(0.5)],
					["STOP", nil, Color.red.sat_(0.7).alpha_(0.5)],
					["..."]
				]).updateOnAction(true).font_(this.font(12, true)),

				[recordingStatus = StaticText().background_(Color.grey(1).alpha_(0.1)).font_(this.font(10, true)), \stretch:4]
			).margins_(0),
			recordingHistory.view;
		));

		metadata = View().layout_(VLayout(
			HLayout(
				label.("TRACK"),
				track = TextField().minWidth_(300).font_(this.font).string_(capture.track).updateOnAction(true)
			),
			HLayout(
				label.("ARTIST"),
				artist = TextField().minWidth_(300).font_(this.font).string_(capture.artist).updateOnAction(true)
			),
			HLayout(
				label.("ALBUM"),
				album = TextField().minWidth_(300).font_(this.font).string_(capture.album).updateOnAction(true)
			),
			HLayout(
				StaticText().string_("GIT COMMIT:").align_(\right).fixedWidth_(70).font_(this.font),
				commit = TextField().minWidth_(300).font_(this.font).string_(capture.findGitCommit)
			),
			path = label.("").minHeight_(22).maxWidth_(500).stringColor_(Color.blue(1).sat_(0.5)).mouseUpAction_({ |v| this.openPath(v) }),
			nil
		).spacing_(4));

		gitUpdate = SkipJack({
			commit.string = capture.findGitCommit();
		}, 3, { metadata.isClosed() }, clock:AppClock);

		track.keyUpAction = track.doAction(_);
		artist.keyUpAction = artist.doAction(_);
		album.keyUpAction = album.doAction(_);

		recordButton.enabled = capture.server.serverRunning;

		view.layout.add(metadata);
		view.layout.add(recording);

		this.connect();

		view.onClose = view.onClose.addFunc({
			this.disconnect();
			this.clear();
		});

		view.keyUpAction = {
			|v, char, mod, unicode, code|
			if (code == 53) {
				view.close();
			}
		};

		view.autoRememberPosition(\AudioCaptureView);
	}

	font {
		|size=10, bold=false, italic=false|
		^Font("M+ 1c", size, bold, italic)
	}

	connect {
		if (connections.size > 0) { this.disconnect };
		connections = ConnectionList [
			progressConn = capture.signal(\recordingProgress).connectTo(this.methodSlot("onRecordProgress(*args)")),

			capture.server.signal(\serverRunning).connectTo({
				|s|
				recordButton.enabled = s.serverRunning;
			}),

			capture.signal(\track).connectTo(		track.valueSlot),
			track.signal(\value).connectTo(			capture.valueSlot("track")),

			capture.signal(\artist).connectTo(		artist.valueSlot),
			artist.signal(\value).connectTo(		capture.valueSlot("artist")),

			capture.signal(\album).connectTo(		album.valueSlot),
			album.signal(\value).connectTo(			capture.valueSlot("album")),

			capture.signal(\numChannels).connectTo(	channels.valueSlot),
			channels.signal(\value).connectTo(		capture.valueSlot("numChannels")),

			capture.signal(\status).connectTo(		this.methodSlot("onStatus(value)")),

			recordButton.signal(\value).connectTo(	this.methodSlot("onRecord(value)"))
		];

		connections.addAll(
			[track, artist, album, channels].collect {
				|v|
				v.signal(\value).collapse(3).connectTo(this.methodSlot(\putDefaults));
			};
		)
	}

	disconnect {
		connections ?? {
			connections.disconnect();
			connections = nil;
		}
	}

	putDefaults {
		Archive.global.put(\AudioCaptureView, \defaults, recordingPath.asSymbol, \artist, capture.artist);
		Archive.global.put(\AudioCaptureView, \defaults, recordingPath.asSymbol, \album, capture.album);
		Archive.global.put(\AudioCaptureView, \defaults, recordingPath.asSymbol, \track, capture.track);
		Archive.global.put(\AudioCaptureView, \defaults, recordingPath.asSymbol, \numChannels, capture.numChannels);
		Archive.write();
	}

	getDefaults {
		var defaultAlbum = Archive.global.at(\AudioCaptureView, \defaults, recordingPath.asSymbol, \album) ?? name;
		capture.album = defaultAlbum;

		Archive.global.at(\AudioCaptureView, \defaults, recordingPath.asSymbol, \artist) !? capture.artist_(_);
		Archive.global.at(\AudioCaptureView, \defaults, recordingPath.asSymbol, \track) !? capture.track_(_);
		Archive.global.at(\AudioCaptureView, \defaults, recordingPath.asSymbol, \numChannels) !? capture.numChannels_(_);
	}

	onRecordProgress {
		|time, size|
		var str = "  RECORDING: ";
		str = str + time.asTimeString(1);
		str = str + "(%)".format((size / 1000000).round(0.1) + "mb");
		recordingStatus.string = str;
	}

	onStatus {
		|inStatus|
		switch (inStatus)
		{ \recording } {
			recordingStatus.string = "  RECORDING";
			recordButton.enabled = true;
			progressConn.connect();
			amplitudeConn = capture.amplitudeUpdater.signal(\value).defer.connectTo(
				recordingHistory.valueSlot
			)
		}
		{ \encoding } {
			recordingStatus.string = "  TRANSCODING...";
			recordButton.enabled = false;
			progressConn.disconnect();

			amplitudeConn.free();
			recordingHistory.clear();
		}
		{ \stopped } {
			recordingStatus.string = "  STOPPED";
			recordButton.enabled = true;
			recordButton.value = 0;
			progressConn.disconnect();

			amplitudeConn.free();
			recordingHistory.clear();
		}
	}

	recording {
		^(capture !? { capture.recording }).asBoolean;
	}

	set {
		|inPath|
		this.setPath(inPath)
	}

	front {
		view.front;
	}

	hide {
		view.visible = false;
	}

	setPath {
		|inPath|
		recordingPath = inPath;
		if (recordingPath.detect(_.isPathSeparator).isNil) {
			recordingPath = (PathName(thisProcess.nowExecutingPath).parentPath() +/+ recordingPath);
		};
		capture.path = recordingPath;
		path.string = capture.nextPath();
		commit.string = capture.findGitCommit();
		this.getDefaults();
	}

	openPath {
		|view|
		var result, cmd = "   open -R \"%\"   2>&1 ".format(view.string);
		result = cmd.unixCmdGetStdOut();

		if (result.contains("does not exist")) {
			cmd = "     open -R \"`dirname \"%\"`\"  2>&1  ".format(view.string);
			cmd.unixCmdGetStdOut();
		}
	}

	onRecord {
		|buttonState|
		switch (buttonState)
		{ 1 } {
			capture.start();
		}
		{ 2 } {
			capture.stop();
		}
		{ 0 } {
			//"stopped".postln;
		}
	}
}
