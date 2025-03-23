+RecordWidget {
    gitName {
        |commit, date|
        var name;
        name = "%-%-%".format(date.month, date.day, date.year);
        
        // if (track.notNil) {
        // 	name = track + name;
        // };
        //
        // if (commit.notNil) {
        // 	name = name + "[%]".format(commit[0..7])
        // };
        
        ^name;
    }
    
    resolvedRecFolder {
        var actualPath, docPath;
        var git, gitRoot, capturePath;
        
        actualPath = recPath ?? {
            thisProcess.platform.recordingsDir
        };
        
        GitProject.current !? {
            |gitProj|
            actualPath = gitProj.capturePath ?? { actualPath };
            try { File.mkdir(actualPath) };
        };
        
        ^actualPath;
    }
    
    resolvedRecPath {
        var dir = this.resolvedRecFolder();
        var git, timestamp, name, gitRoot, path, extension, increment;
        
        extension = "." ++ server.recHeaderFormat;
        
        GitProject.current !? {
            |gitProj|
            gitProj.prepareCapture(dir);
            
            name = Date.localtime.stamp;
            gitProj.git.log(1)[0] !? _[\commit_hash] !? {
                |hash|
                name = name ++ " [" ++ hash[0..7] ++ "]"
            };
            path = dir +/+ name ++ extension;
        } ?? {
            timestamp = Date.localtime.stamp;
            name = server.recorder.filePrefix ++ timestamp ++ extension;
            path = dir +/+ name;
        };
        
        ^path
    }
}
