+LSPCompletionHandler {
	*classMethodHandler {
		^this.prNew(
			name: "method_class",
			trigger: ".",
			prefixHandler: {
				|prefix|
				var prefixClass = prefix.findRegexp("([A-Z]\\w*)$");

				^if (prefixClass.notEmpty) {
					prefixClass = prefixClass[1][1];
					if ((prefixClass = prefixClass.asSymbol.asClass).notNil) {
						prefixClass
					} {
						nil
					}
				} {
					nil
				}
			},
			action: {
				|prefixClass, trigger, completion, provideCompletionsFunc|
				var firstMatch, lastMatch, matches, names;
				var results;

				#methodNames, methods = LSPDatabase.methodsForClass(prefixClass);

				firstMatch = (
					name: completionString.asSymbol,
					ownerClass: precursor
				);

				firstMatch = methodNames.indexForInserting(completionString.asSymbol);
				lastMatch = matches.indexForInserting((completionString ++ 127.asAscii).asSymbol);

				lastMatch = min(lastMatch, firstMatch + LSPCompletionHandler.completionLimit);

				if (firstMatch.notNil and: { lastMatch.notNil }) {
					results = Array(lastMath - firstMatch);

					(firstMatch..(lastMatch-1)).do {
						|methodIndex, i|
						var method = methods[methodIndex];

						results.add((
							label: 			"%%".format(method.name, LSPDatabase.methodArgString(method)),
							filterText: 	method.name,
							insertText:		"%%".format(method.name, LSPDatabase.methodInsertString(method)),
							insertTextFormat: 2, // Snippet,
							labelDetails:	LSPDatabase.methodDetailsString(method),
							kind: 			2, // ??

							// @TODO Add documentation and detail
							// detail:			nil,
							// documentation: (
							// 	kind: 		"markdown",
							// 	value:		LSPDatabase.methodDocumentationString(method)
							// )
						))
					};

					provideCompletionsFunc.value(results, true);
				};
			}
		)
	}
}
