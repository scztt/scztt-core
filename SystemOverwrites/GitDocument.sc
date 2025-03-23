GitProject {
    classvar <>documentsCache, <>projectsCache;
    var <>gitRoot, <git;
    
    *current {
        ^this.fromDocument(Document !? _.current);
    }
    
    *fromDocument {
        |doc|
        if (doc.isNil) { ^nil };
        ^projectsCache[doc.path.asSymbol] ?? {
            projectsCache[doc.path.asSymbol] = (
                if (doc.isKindOf(GitDocument)) {
                    doc.git !? {
                        |git|
                        GitProject.prNew(git.localPath)
                    }
                } {
                    GitDocument.gitRepoFor(doc) !? {
                        |git|
                        GitProject.prNew(git.localPath)
                    }
                }
            );
            projectsCache[doc.path.asSymbol];
        }
    }
    
    *prNew {
        |path|
        ^super.newCopyArgs(path).init
    }
    
    *initClass {
        if (\Document.asClass.notNil) {
            Class.initClassTree(Document);
            
            Document.initAction = Document.initAction.addFunc({
                |newDoc|
                GitProject.onDocAdded(newDoc);
                newDoc.onClose = newDoc.onClose.addFunc({
                    GitProject.onDocRemoved(newDoc);
                })
            });
        };
        
        documentsCache = ();
        projectsCache = ();
    }
    
    *onDocsChanged {
        |doc|
        GitProject.fromDocument(doc) !? {
            |proj|
            proj.prSetCache(proj.prFindDocuments())
        }
    }
    
    *onDocAdded { |doc| this.onDocsChanged(doc) }
    *onDocRemoved { |doc| this.onDocsChanged(doc) }
    
    init {
        git = Git(gitRoot);
    }
    
    prFindDocuments {
        var docs;
        
        docs = Document.allDocuments.select {
            |doc|
            doc.path !? {
                |path|
                PathName(path).asRelativePath(gitRoot).contains("..").not
            } ?? false
        };
        
        ^docs.collect(GitDocument(_))
    }
    
    prSetCache {
        |docs|
        documentsCache[gitRoot.asSymbol] = docs;
        ^docs
    }
    
    current {
        ^GitProject.fromDocument(Document.current)
    }
    
    documents {
        var key = gitRoot.asSymbol;
        ^this.class.documentsCache[key] ?? {
            this.prSetCache(this.prFindDocuments())
        }
    }
    
    capturePathName {
        ^"_capture"
    }
    
    capturePath {
        ^(gitRoot +/+ this.capturePathName)
    }
    
    prepareCapture {
        |recordDir|
        var nowStatus, modified;
        
        git.ignore(this.capturePathName +/+ "**");
        git.add(".gitignore");
        
        modified = git.status.select({
            |l|
            (l[\status] == \modified) || (l[\staged] == true)
        });
        
        if (modified.size > 0) {
            Log(GitDocument).info("Committing changes before capture: %".format(modified));
            
            this.commitAll(
                "[rec ] Checkpoint before capture",
                "Recording to %".format(recordDir),
                all:true
            );
        }
    }
    
    add {
        |...docs|
        git.add(*docs)
    }
    
    commitAll {
        |summary, body, all=false|
        var docs = this.documents;
        
        docs.do {
            |doc|
            doc.save();
        };
        this.add(*docs.collect(_.gitPath));
        
        if (git.status().size > 0) {
            "Auto-committing % documents with status:\n\t%".format(
                docs.size,
                summary
            ).postln;
            
            if (all) {
                git.commit(summary, body, all:all)
            } {
                git.commit(summary, body, filesToAdd: docs.collect(_.gitPath))
            }
        }
    }
    
    checkoutBranch {
        |newName|
        if (git.branch != newName) {
            // this.commitAll("Commit before branching", all:true);
            git.checkoutBranch(newName);
        }
    }
    
    branchGui {
        |doneAction|
        View.queryDialog(
            "Branch name: ",
            "",
            okAction: {
                |name|
                if (name.size > 0) {
                    this.checkoutBranch(name);
                    doneAction.(name);
                }
            },
            cancelAction: {}
        )
    }
}

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
    
    *gitRepoFor {
        |doc|
        ^Git.findRepoRoot(doc !? _.path) !? {
            |repoRoot|
            Git(repoRoot)
        }
    }
    
    *gitRepoForPath {
        |path|
        ^Git.findRepoRoot(path) !? {
            |repoRoot|
            Git(repoRoot)
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
    
    path {
        ^document.path
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
    
    commit {
        |summary ... etc|
        this.git !? {
            |git|
            "Auto-committing % with status:\n\t%".format(
                PathName(document.path).fileName,
                summary
            ).postln;
            
            if (document.isEdited) { document.save() };
            
            git.commit(
                summary,
                etc.join("\n"),
                all: true
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
        ^(git ?? { git = this.class.gitRepoFor(document) })
    }
    
    save {
        document.save()
    }
    
    initialize {
        |files, summary="Initial commit"|
        var newGit, project;
        
        files = files ?? PathName(document.path).fileName;
        
        if (this.git.notNil) { "Git repo for % is already initialized".format(document.path).warn; ^this };
        if (files.isKindOf(String)) { files = [files] };
        
        newGit = Git(PathName(document.path).parentPath);
        newGit.init();
        newGit.add(*files);
        
        project = GitProject.fromDocument(this);
        
        newGit.ignore(project.capturePathName +/+ "**");
        newGit.add(".gitignore");
        newGit.commit(summary);
        git = newGit;
        
        Document.changed(\current, Document.current);
    }
    
    prMakeMoveCommit {
        |git, oldPath, newPath, commitMsg|
        var commitHash;
        
        // 1: move to new location
        // git.reset();
        git.move(oldPath, newPath);
        git.commit(commitMsg, filesToAdd:[oldPath, newPath]);
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
            tempPath = tempPath.withName("split_%_" ++ tempPath.fileNameWithoutDoubleExtension).fullPath;
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
                git.commit("[split] '%' -> '%'".format(oldPath, tempPath));
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
                git.commit("[split] '%' -> '%'".format(tempPath, oldPath))
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
        document.save();
        document.getTextAsync({
            |currentText|
            
            var selection = document.selectedString;
            var requireString = "Require(\"%\")".format(name);
            var newDocString = currentText.replace(
                selection,
                requireString
            );
            
            document.selectRange(0, 0);
            // document.text = newDocString;
            
            this.splitToNew(
                name,
                selection,
                newDocString
            );
            
            document.reload();
        })
    }
    
    splitSelectionGui {
        |doneAction|
        this.git !? {
            var name = this.gitPath(PathName(document.path).parentPath);
            
            this.git !? {
                document.selectedString.postln;
                "=== SPLITTING % lines ====".format(document.selectedString.split(Char.nl).size);
                
                View.queryDialog(
                    "Split to file name: ",
                    name,
                    okAction: {
                        |name|
                        if (name.endsWith(".scd").not) { name = name ++ ".scd" };
                        this.splitSelection(name);
                        doneAction.(name);
                    },
                    cancelAction: {}
                )
            }
        }
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
    
    branch {
        ^this.git(["rev-parse", "--abbrev-ref", "HEAD"])
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
    
    checkoutBranch {
        |branch|
        ^this.git(["checkout", "-B", branch.quote])
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
    
    gitDocument { ^GitDocument(this) }
    
    save { \ScIDE.asClass !? _.save(quuid); }
    reload { \ScIDE.asClass !? _.reload(quuid); }
}

+ScIDE {
    *save {|quuid|
        this.send(\saveDocument, [quuid]);
    }
    
    *reload {|quuid|
        this.send(\reloadDocument, [quuid]);
    }
}
