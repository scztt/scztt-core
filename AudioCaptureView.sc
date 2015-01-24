AudioCaptureView : Singleton {
	var view, label, <capture, recording, metadata,
	recordButton, recordingStatus, track, artist, album, commit, path;

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
				.font_(Font("M+ 1c", 10, false))
			)
		};

		capture = AudioCapture(thisProcess.platform.recordingsDir, Server.default);

		recording = View().layout_(HLayout(
			recordButton = Button().states_([
				["RECORD", nil, Color.green.sat_(0.7).alpha_(0.5)],
				["STOP", nil, Color.red.sat_(0.7).alpha_(0.5)],
				["..."]
			]).font_(Font("M+ 1c", 12, true)).action_({ |v| this.recordAction(v) }),

			[recordingStatus = StaticText().background_(Color.grey(1).alpha_(0.1)).font_(Font("M+ 1c", 10, true)), \stretch:4]
		));

		fieldChanged = { |...args| this.fieldChanged(*args) };
		metadata = View().layout_(VLayout(
			HLayout(
				label.("TRACK"),
				track = TextField().minWidth_(300).font_(Font("M+ 1c", 10)).keyUpAction_(fieldChanged).string_(capture.track)
			),
			HLayout(
				label.("ARTIST"),
				artist = TextField().minWidth_(300).font_(Font("M+ 1c", 10)).keyUpAction_(fieldChanged).string_(capture.artist)
			),
			HLayout(
				label.("ALBUM"),
				album = TextField().minWidth_(300).font_(Font("M+ 1c", 10)).keyUpAction_(fieldChanged).string_(capture.album)
			),
			HLayout(
				StaticText().string_("GIT COMMIT:").align_(\right).fixedWidth_(70).font_(Font("M+ 1c", 10)),
				commit = TextField().minWidth_(300).font_(Font("M+ 1c", 10))
			),
			path = label.("").minHeight_(22).maxWidth_(500).stringColor_(Color.blue(1).sat_(0.5)).mouseUpAction_({ |v| this.openPath(v) }),
			nil
		).spacing_(4));

		view.layout.add(metadata);
		view.layout.add(recording);

		view.onClose = {
			this.clear();
		};

		//view.front;

		//QtGUI.palette = QPalette.light

	}

	recording {
		^(capture !? { capture.recording }).asBoolean;
	}

	set {
		|inPath|
		if (File.exists(inPath)) {
			if (inPath.isFile()) {
				inPath = PathName(inPath).parentPath()
			};

			this.setPath(inPath);
		};
	}

	front {
		view.front;
	}

	hide {
		view.visible = false;
	}

	setPath {
		|inPath|
		var dir;
		capture = AudioCapture.copy(capture);
		capture.path = inPath;
		capture.metadata_(artist.string, track.string, album.string);
		commit.string = AudioCapture.findGitCommit(inPath);
		path.string = capture.recordingsDir +/+ capture.nextName();
	}

	fieldChanged {
		capture.metadata_(artist.string, track.string, album.string);
		path.string = capture.recordingsDir +/+ capture.nextName();
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

	recordAction {
		|button|
		if (button.value == 1) {
			//"recording".postln;

			if (capture.encode) {
				recordingStatus.string = "  RECORDING";
				capture.onStopped = { recordingStatus.string = "  STOPPED"; button.enabled = false; };
				capture.onTranscodeStart = { recordingStatus.string = "  TRANSCODING..." };
				capture.onTranscodeComplete = {
					recordingStatus.string = "  TRANSCODE COMPLETE";
					button.enabled = true;
					button.value = 0;
					capture = AudioCapture.copy(capture);
					this.setPath(capture.recordingsDir);
				};
			} {
				capture.onStopped = {
					recordingStatus.string = "  STOPPED"
				};
			};
			capture.onRecordProgress = {
				|time, size|
				var str = "  RECORDING: ";
				str = str + time.asTimeString(1);
				str = str + "(%)".format((size / 1048576).round(0.1) + "mb");
				recordingStatus.string = str;
			};
			capture.start();
		};
		if (button.value == 2) {
			capture.stop();
		};
		if (button.value == 0) {
			//"stopped".postln;
		};
	}
}
