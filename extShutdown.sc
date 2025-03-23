+ShutDown {
    *run {
        objects.copy.do({ 
            arg item, i; 
            try {
                item.doOnShutDown;  
            } {
                |e|
                ("Error: " ++ e ++ " while shutting down " ++ item).postln;
                e.reportError();
            }
        });
    }
}

