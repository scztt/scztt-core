+NN {
    *resolvePath {
        |path|
        var roots, extensions;
        var fixedPath, foundPaths, attempts;
        
        roots = [
            "~/Desktop/F O R E S T R Y/RAVE models/".standardizePath,
        ];
        extensions = ["ts", "mts"];
        
        fixedPath = path.asString
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)");
        
        attempts = List();
        foundPaths = Require.resolvePaths(fixedPath, roots, extensions, attempts);
        
        if (foundPaths.isEmpty) {
            "No models found, attempted paths: ".warn;
            attempts.do {
                |a|
                "\t%.{%}".format(a, extensions.join(",")).warn
            };
            Error("No paths found").throw;
        } {
            ^foundPaths
        };        
    }
    
    *load { |key, path, id(-1), server(Server.default), action|
        var model = this.model(key);
        
        if (path.isKindOf(String).not) {
            Error("NN.load: path needs to be a string, got: %").format(path).throw
        };        
        path = this.resolvePath(path);
        
        if (path.size > 1) {
            Error("Path matched more than one model").throw;
        } {
            path = path[0]
        };
        
        if (model.isNil or: { model.path != path }) {
            if (this.isNRT) {
                var info =  this.prGetCachedInfo(path) ?? {
                    Error("NN.load (nrt): model info not found for %".format(path)).throw;
                };
                model = NNModel.fromInfo(info, this.nextModelID);
                this.prPutModel(key, model);
            } {
                model = NNModel.load(path, id, server, action: { |m|
                    this.prPutModel(key, m);
                    // call action after adding to registry: in case action needs key
                    action.value(m);
                });
            };
        };
        
        if (this.isNRT) {
            server.sendMsg(*model.loadMsg);
        }
        ^model;
    }
    
}
