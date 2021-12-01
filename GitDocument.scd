GitDocument {
	classvar gitCache, statusCache;
	var <>document, git;

	*initClass {
		gitCache = ();
		statusCache = ();
	}

	*new {
		|doc|
		^super.newCopyArgs(doc)
	}

	*current {
		^GitDocument(Document.current)
	}

	*prMakeTempGit {
		|path|
	}

	*prGitRepoFor {
		|doc|
		var git = Git(Git.findRepoRoot(doc.path));
		if (git.localPath.notNil) {
			^git
		} {
			^nil
		}
	}

	prStatusFromString {
		|string|
		var statusRegex, status;
		statusRegex = "//.?@STATUS:.*?\\n(^//(.*?\n))*";
		^string.findRegexp(statusRegex) !? {
			|status|
			if (status[0].notNil) {
				status = status[0][1];
				status = status.split($\n)[1..];
				status = status.collect({
					|s|
					(s
						.replace("// ", "")
						.replace("//", "")
					)
				});

				while { status.last.trim.isEmpty } {
					status = status[0..status.size-2]
				};

				status;
			} { nil }
		};
	}

	onSave {
		|doc|
		var savedStatus, nowStatus, summary;

		if (this.git.notNil) {
			savedStatus = this.savedStatus();
			if (savedStatus.isNil) { ^this };

			nowStatus = this.status();

			Log(GitDocument).info("savedStatus: %", savedStatus);
			Log(GitDocument).info("nowStatus: %", nowStatus);

			if (savedStatus != nowStatus) {
				Log(GitDocument).info("Status has changed, committing...");

				nowStatus[0] = "[auto] %: %".format(
					PathName(document.path).fileName,
					nowStatus[0]
				);

				this.commit(*nowStatus);
			} {
				Log(GitDocument).info("Status has not changed from saved copy")
			}
		}
	}

	prepareCapture {
		var nowStatus, modified;
		this.git !? {
			|git|
			modified = git.status.select({ |l| l[\status] == \modified });
			if (modified.detect({
				|l|
				l[\file].asString.absolutePath == document.path.absolutePath
			}).notNil) {
				Log(GitDocument).info("Committing changes before capture.");
				nowStatus = this.status();
				nowStatus[0] = "[rec ] %: %".format(
					PathName(document.path).fileName,
					nowStatus[0]
				);

				this.commit(*nowStatus);
			}
		}
	}

	commit {
		|summary ... etc|
		this.git !? {
			|git|
			"Auto-committing % with status:\n\t%".format(
				PathName(document.path).fileName,
				summary
			).postln;

			git.reset();
			git.commit(
				summary,
				etc.join("\n"),
				[this.gitPath]
			);
		} ?? {
			"No git repo for file %".format(document.path).warn
		}
	}

	status {
		^this.prStatusFromString(document.string);
	}

	gitPath {
		|path|
		^this.git !? {
			|git|
			var gitPath = git.localPath;
			path = path ?? { document.path };
			if (gitPath.last == $/) { gitPath = gitPath[0..gitPath.size-2] };

			PathName(path).asRelativePath(gitPath);
		}
	}

	savedStatus {
		^this.gitPath !? {
			|gitPath|
			this.prStatusFromString(git.show(gitPath, "HEAD"));
		}
	}

	git {
		^git ?? { git = this.class.prGitRepoFor(document) }
	}

	initialize {
		|files, summary="Initial commit"|
		var newGit;

		files = files ?? PathName(document.path).fileName;

		if (this.git.notNil) { "Git repo for % is already initialized".format(document.path).warn; ^this };
		if (files.isKindOf(String)) { files = [files] };

		newGit = Git(PathName(document.path).parentPath);

		newGit.init();
		newGit.add(*files);
		newGit.commit(summary);
		git = newGit;

		Document.changed(\current, Document.current);
	}

	prMakeMoveCommit {
		|git, oldPath, newPath, commitMsg|
		var commitHash;

		// 1: move to new location
		git.reset();
		git.move(oldPath, newPath);
		git.commit(commitMsg);
		commitHash = git.sha();

		// 2: reset to where we were before
		git.reset("HEAD^", hard:true);
		^commitHash
	}

	splitToNew {
		|filenames, splitContent, originalContent|
		this.git !? {
			|git|
			var keepOriginal, oldPath, newPath, tempHashes, tempPath;
			var step = 0;

			oldPath = this.gitPath;
			tempPath = PathName(git.localPath +/+ oldPath).withName();
			tempPath = tempPath.withName("split_%_" ++ tempPath.fileNameWithoutDoubleExtension);
			tempPath = tempPath.format(Date.seed);
			tempPath = this.gitPath(tempPath);

			// Validation
			if (originalContent.size == 0) { originalContent = nil };
			keepOriginal = originalContent.notNil;

			if (filenames.isCollection && filenames.isKindOf(String).not) {
				if (splitContent.isKindOf(String)) {
					Error("Multiple split files specified, but splitContent is not a collection").throw
				}
			};

			if (filenames.isString) {
				filenames = [filenames]
			};

			if (splitContent.notNil && splitContent.isString) {
				splitContent = [splitContent]
			};

			filenames.do {
				|filename|
				var newPathAbsolute = git.localPath +/+ filename;
				if (File.exists(newPathAbsolute)) {
					Error("Can't split, file '%' already exists.".format(newPathAbsolute)).throw
				};
			};

			// Do our splitting
			// 1: Move to a new location, and then reset
			tempHashes = filenames.collect {
				|newPath, i|
				this.prMakeMoveCommit(
					git,
					oldPath: oldPath,
					newPath: newPath,
					commitMsg: "[split] '%' -> '%'".format(oldPath, newPath)
				);
			};

			// 3: move to temp location
			if (keepOriginal) {
				git.move(oldPath, tempPath);
				git.commit("[split] moving '%' -> '%'".format(oldPath, tempPath));
			};

			// 4: merge new location and temp location commits
			tempHashes.do {
				|tempHash, i|
				git.merge(tempHash);
				git.commit(
					"[split] '%' -> '%'".format(oldPath, filenames[i]),
					all:true
				);
				(git.localPath +/+ "*").pathMatch().do(_.postln);
			};

			// 5: move temp to original location, or delete
			if (keepOriginal) {
				git.move(tempPath, oldPath);
				git.commit("[split] Moving back '%'".format(oldPath))
			} {
				git.remove(tempPath);
				git.add(tempPath);
				git.commit("[split] Removing '%'".format(tempPath))
			};

			// 6: Optional, commit changes
			if (splitContent.notNil) {
				splitContent.do {
					|content, i|
					var newPath = filenames[i];
					var newPathAbsolute = git.localPath +/+ newPath;
					File.use(newPathAbsolute, "w", {
						|f|
						f.putString(content);
					});

					git.add(newPath);
				}
			};

			if (originalContent.notNil) {
				File.use(document.path, "w", {
					|f|
					f.putString(originalContent);
				});

				git.add(oldPath);
			};

			if (originalContent.notNil || splitContent.notNil) {
				git.commit("[split] finalizing content of split files");
			}
		}
	}

	splitSelection {
		|name|
		var selection = Document.current.selectedString;
		var newDocString = Document.current.replace(
			selection,
			"Require('%')".format(name)
		);

		this.splitToNew(
			name,
			selection,
			newDocString
		);

		Document.current.string = newDocString;
	}

	splitSelectionGui {
		.attachHoverScope
	}
}

+Git {
	init {
		^this.git(["init"])
	}

	show {
		|file, revision="HEAD"|
		^this.git(["show", "%:%".format(revision, file)])
	}

	move {
		|path, newPath|
		^this.git(["mv", path, newPath]);
	}

	remove {
		|...paths|
		^this.git(["rm", "--cached", "--"] ++ paths)
	}

	branches {
		var current, result;

		result = this.git(["branch"]).split(Char.nl);

		current = result.detect({ |b| b[0] == $* });
		current = current !? {
			result.remove(current);
			result.addFirst(current);
		};
		result = result.collect {
			|br|
			br = br[2..]
		};

		^result
	}

	merge {
		|commit, strategy|
		^this.git(["merge", commit])
	}

	sha {
		^this.git(["rev-parse HEAD"]);
	}
}

+PathName {
	withName {
		|name|
		^PathName(this.pathOnly +/+ name ++ "." ++ this.extension())
	}
}

+Document {
	*prCurrent_{|newCurrent|
		current = this.current;
		if((newCurrent === current).not, {
			if(current.notNil, {current.didResignKey});
			newCurrent.didBecomeKey;
			this.changed(\current, newCurrent);
		});
	}
}