LSPDatabase {

}

LSPConnection {
	classvar <handlers, <preprocessor;

	var <>inPort, <>outPort;
	var socket;

	*new {
		|settings|
		^super.new.init(settings.copy.addAll(this.envirSettings))
	}

	*envirSettings {
		^(
			enabled: "SCLANG_LSP_ENABLE".getenv().notNil,
			inPort: "SCLANG_LSP_CLIENTPORT".getenv() ?? { 57210 } !? _.asInteger,
			outPort: "SCLANG_LSP_SERVERPORT".getenv() ?? { 57211 } !? _.asInteger
		)
	}

	init {
		|settings|

		// Settings
		inPort = settings[\inPort];
		outPort = settings[\output];

		// Initialization
		handlers = ();

		preprocessor = {
			|params|

			params["textDocument"] !? {
				|doc|
				params["textDocument"] = LSCDocument.findByQUuid(doc["uri"]);
			};

			params["params"]["position"] !? {
				|position|
				position["line"] = position["line"].asInteger;
				position["character"] = position["character"].asInteger;
			};
		};
	}

	start {
		// @TODO: What do we do before start / after stop? Errors?
		// socket =
	}

	stop {

	}

	// 'action' should be a function of the form { |params, respondFunc| } where
	// respondFunc can be called with data that will be sent as a response.
	addHander {
		|method, action|
		method = method.asSymbol;

		if (handlers[method].isNil) {
			Log(\LSP).info("Adding handler for method '%'", method);
		} {
			Log(\LSP).warning("Overwriting handler for method '%'", method);
		};

		handlers[method] = action;
	}

	prOnReceived {
		|a, b, message|

		Log(\LSP).info("Message received: ", message);

		this.prHandleMessage(
			this.prParseMessage(message)
		)
	}

	prParseMessage {
		|message|
		var object;

		try {
			object = message.parseJSON;
		} {
			|e|
			// @TODO: Improve error messaging and behavior.
			"Problem parsing message (%)".format(e).error;
		};

		^object
	}

	prEncodeMessage {
		|object|
		var message;

		try {
			message = object.toJSON();
		} {
			|e|
			// @TODO: Improve error messaging and behavior.
			"Problem encoding message (%)".format(e).error;
			^this
		};

		socket.sendRaw(message)
	}

	prHandleMessage {
		|object|
		var id, method, params, handler;

		id 		= object["id"];
		method 	= object["method"].asSymbol;
		params 	= object["params"];

		handler = handlers[method];

		if (handler.isNil) {
			Log(\LSP).info("No handler found for method: %")
		} {
			Log(\LSP).info("Found method handler: %");

			// Preprocess param values into a usable state
			preprocessor.value(params);

			try {
				handler.value(params, {
					|result|
					this.prHandleResponse(id, result)
				})
			} {
				|e|
				e.throw;
				// @TODO Handle error
			}
		}
	}

	prHandleResponse {
		|id, result|
		var message = (
			id: id,
			result: result
		).toJSON;

		this.prSendMessage(message);
	}

	prSendMessage {
		|message|
		// @TODO message should use JSON-RPC Content-Length header, support multi-part sending for long messages.
		socket.sendRaw(message);
	}
}

LSPCompletionHandler {
	classvar <>completionHandlers;
	classvar completionLimit = 30;
	var <name, trigger, prefixHandler, action;

	// 'trigger' is one or more characters that can START this completion handler
	// 'prefixHandler' is a function that returns the parsed prefix (e.g. a class name) before the trigger.
	//   If there is no prefix handler, the handler is always processed. If the prefix handler returns nil,
	//   it is skipped.
	// 'action' is a function to process the prefix, where the arguments are: |doc, provideCompletions|
	*addHandler {
		|name, triggerOrHandler, prefixHandler, action|
		completionHandlers = completionHandlers.add(this.prNew(name, triggerOrHandler, prefixHandler, action))
	}

	*prNew {
		|name, trigger, prefixHandler, action|
		^super.newCopyArgs(name, trigger, prefixHandler, action)
	}

	*initClass {
		completionHandlers.addAll([
			// LSPCompletionHandler.classMethodHandler,
			// LSPCompletionHandler.singletonHandler,
			// LSPCompletionHandler.environmentVarHandler,
			// LSPCompletionHandler.anonymousMethodHandler,
		]);
	}

	*prGetCompletionString {
		|lineString, triggerCharacters|
	}

	*prValidateHandler {
		|handler, prefix, trigger, completion|
		var validatedPrefix;

		^(
			(handler.trigger == trigger)
			and: {
				(validatedPrefix = handler.validatePrefix(prefix)).notNil
			}
		)
	}

	// prefix: 		all text before the trigger
	// trigger: 	one character that triggered the completion
	// completion:	the remaining characters to do completion on
	*handle {
		|params, respondFunc|
		var doc, line, char;
		var lineString;
		var prefix, trigger, completion, triggerCharacters;
		var validatePrefix, handler;

		doc = params["textDocument"];
		line = params["position"]["line"];
		char = params["position"]["character"];
		triggerCharacters = params["context"]["triggerCharacter"] ?? { this.triggerCharacters };

		lineString = LSPDatabase.getDocumentLine(doc, line);

		lineString !? {
			#prefix, trigger, completion = this.prGetCompletionString(
				lineString,
				triggerCharacters
			);

			if (completion.size > 0) {
				Log(\LSP).info("Doing completion on: % / % / %", prefix, trigger, completion);

				// Find the first handler for which prValidateHandler returns true.
				handler = completionHandlers.detect(
					this.prValidateHandler(_, prefix, trigger, completion)
				);

				if (handler.notNil) {
					Log(\LSP).info("Using handler: %", handler.name);
					handler[\action].handle(prefix, trigger, completion, respondFunc);
				} {
					Log(\LSP).info("No handler for completion: % / % / %", prefix, trigger, completion);
				}
			}
		}
	}

	handle {
		|prefix, trigger, completion, respondFunc|
		action.value(
			prefix, trigger, completion,
			{
				|completions, isIncomplete=true|
				respondFunc.value((
					isIncomplete: isIncomplete,
					items: completions
				))
			}
		);
	}
}
