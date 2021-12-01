AudioCapture {
	classvar <>defaultArtist, <>defaultAlbum, <>defaultTrack;

	var <>server, <recordingsDir, <>nameFunction, <bus, <>recHeaderFormat="wav", <>recSampleFormat="float", recording = false,
	<recordingPath, <>inputBus, <numChannels, encode=true,
	<artist, <album, <track, <notes, date,
	<amplitudeUpdater;
	var progressRoutine;
	var <lastPath;

	*new {
		|path, server|
		^super.newCopyArgs(server).init.path_(path);
	}

	init {
		nameFunction 	= this.defaultNameFunction(_);
		server 			= server ? Server.default;
		album 			= defaultAlbum;
		artist 			= defaultArtist;
		track 			= defaultTrack;
		numChannels 	= server.options.numOutputBusChannels;
	}

	numChannels_{
		|n|
		n = n.asInteger;
		if (numChannels != n) {
			numChannels = n;
			this.changed(\numChannels, numChannels);
		}
	}

	path_{
		| path |
		path = path.standardizePath;
		if (path != recordingsDir) {
			recordingsDir = path;
			this.changed(\path, path);
		};
	}

	artist_{
		|inArtist|
		if (artist != inArtist) {
			artist = inArtist;
			this.changed(\artist, artist);
		}
	}

	track_{
		|inTrack|
		if (track != inTrack) {
			track = inTrack;
			this.changed(\track, track);
		}
	}

	album_{
		|inAlbum|
		if (album != inAlbum) {
			album = inAlbum;
			this.changed(\album, album);
		}
	}

	metadata_{
		|inArtist, inTrack, inAlbum|
		this.artist = inArtist;
		this.track = inTrack;
		this.album = inAlbum;
	}

	date {
		^Date.localtime()
	}

	defaultNameFunction {
		var name, commit;
		commit = this.findGitCommit();
		name = "%-%-%".format(this.date.month, this.date.day, this.date.year);

		if (track.notNil) {
			name = track + name;
		};

		if (commit.notNil) {
			name = name + "[%]".format(commit[0..7])
		};

		^name;
	}

	nextPath {
		var name, path, i=1;
		name = nameFunction.value();
		path = recordingsDir +/+ name ++ "." ++ recHeaderFormat;
		while { File.exists(path) } {
			path = recordingsDir +/+ ("% %".format(name, i)) ++ "." ++ recHeaderFormat;
			i = i + 1;
		};
		^path;
	}

	findGitCommit {
		var git, gitRoot;
		gitRoot = Git.findRepoRoot(recordingsDir);
		if (gitRoot.notNil) {
			git = Git(gitRoot);
			^git.log(1)[0][\commit_hash]
		};
		^nil;
	}

	start {
		var path, name, i=1, startTime;
		if (recording.not) {
			recording = true;
			if (File.exists(recordingsDir).not) { File.mkdir(recordingsDir) };

			amplitudeUpdater = InputBusStatsUpdater((0..numChannels - 1), {
				|values, reset|
				RunningMax.ar(ArrayMax.ar(values.asArray.abs), reset);
			}, server, 10);

			recordingPath = this.nextPath();
			server.recHeaderFormat = recHeaderFormat;
			server.recSampleFormat = recSampleFormat;
			server.record(recordingPath, inputBus, numChannels);
			startTime = AppClock.seconds;

			this.changed(\status, \recording);
			progressRoutine = SkipJack({
				if (File.exists(recordingPath)) {
					this.changed(\recordingProgress,
						AppClock.seconds - startTime,
						File.fileSize(recordingPath)
					)
				}
			}, 1);
		} {
			"Already recording".warn;
		}
	}

	stop {
		var path, soundFile, track, artist, album, commit;
		if (recording) {
			amplitudeUpdater.free;
			amplitudeUpdater = nil;

			recording = false;
			lastPath = recordingPath;
			recordingPath = nil;
			progressRoutine.stop();
			progressRoutine = nil;

			server.stopRecording();
			{
				if (encode) {
					if (numChannels > 2) {
						"Cannot encode to mp3 for >2 channels".warn;
						this.changed(\status, \stopped);
					} {
						try {
							soundFile = SoundFile();
							if (soundFile.openRead(lastPath)) {
								this.doEncode(lastPath, soundFile.numFrames / soundFile.sampleRate,
									track:	track,
									artist:	artist,
									album:	album
								);
							}
						} {
							|e|
							"Problem reading sound file %: %".format(lastPath, e.errorString).error;
							this.changed(\status, \stopped);
						}
					}
				} {
					this.changed(\status, \stopped);
				}
			}.defer(0.5);
		} {
			"Not currently recording".warn;
		}
	}

	mdFlags {
		var str = "";
		if (artist.notNil) { str = str ++ " --ta \"%\" ".format(artist) };
		if (track.notNil) { str = str ++ " --tt \"%\" ".format(track) };
		if (album.notNil) { str = str ++ " --tl \"%\" ".format(album) };
		if (notes.notNil) { str = str ++ " --tc \"% [%]\" ".format(notes, this.findGitCommit()) };
		str = str ++ " --ty \"%\" ".format(this.date.year);
		^str;
	}

	doEncode {
		| path, duration, track, artist, album, commit, targetSize=18, targetBR |
		var result, filesize, cmdLine;
		filesize = File.fileSize(path.postln);
		if (targetBR.isNil) {
			targetBR = (targetSize * 1000 * 1000) / (duration);
			targetBR = (targetBR / 1000 * 8).floor; // 8 -> bytes to bits
		};
		targetBR = min(targetBR, 320);
		cmdLine = "/usr/local/bin/lame -b % -B % % % %".format(
			targetBR - 20,
			targetBR + 10,
			this.mdFlags(),
			path.replace(" ", "\\ "),
			PathName(path).extension_("mp3").fullPath.replace(" ", "\\ ")
		);

		{
			var pipe;
			this.changed(\status, \encoding);

			"Running: %\n".postf(cmdLine);
			pipe = cmdLine.unixCmd({
				|exit, pid|
				exit.postln;
			}, true);
			pipe.postln;

			this.changed(\status, \stopped);
		}.fork(AppClock)

	}
}

