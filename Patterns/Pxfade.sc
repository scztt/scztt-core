Pxfade : Pattern {
	classvar <fadeFuncs;
	var <>inputs, <>fade, <>fadeFunc, <>fadeFuncArgs, <>parallel=false;

	*initClass {
		fadeFuncs = (
			event: Prout({
				var keys = IdentitySet();
				var blendStreams = ();
				var ignores=[\octave, \degree], midinoteAssigned=false, scale, octDegree;
				var resolvedFadeFunc, merged;

				loop {
					yield({
						|a, b, xfade, itemFadeFunc=\random ...args|

						if (itemFadeFunc.isSymbol) {
							itemFadeFunc = fadeFuncs[itemFadeFunc] ?? {
								Error("No fadeFunc with specified name %".format(itemFadeFunc)).throw;
							};
						};

						if (resolvedFadeFunc != itemFadeFunc) {
							resolvedFadeFunc = itemFadeFunc;
							blendStreams.clear();
						};

						if (a.isNil && b.isNil) {
							merged = nil;
						} {
							a = a ?? {()};
							b = b ?? {()};

							[a, b].do {
								|event|
								if (event[\midinote].isNumber.not) {
									event[\midinote] = event[\scale].octDegreeToMidi(
										event[\octave] ?? 4,
										event[\degree] ?? 0
									)
								} { midinoteAssigned = midinoteAssigned || true };
							};

							merged = a.merge(b, {
								|aVal, bVal, key|
								var stream, value;

								if (ignores.includes(key).not or: { key.asString.beginsWith("_") }) {
									stream = blendStreams[key] ?? {
										blendStreams[key] = resolvedFadeFunc.asStream;
										blendStreams[key];
									};

									value = stream.next().value(aVal, bVal, xfade, *args);

									// "% / %: a(%) => b(%) = %".format(key, xfade, aVal, bVal, value).postln;

									value;
								}
							}).parent_(a.parent).proto_(a.proto);

							if (midinoteAssigned.not) {
								merged.putAll(
									(merged[\scale] ?? { Scale.chromatic }).midiToOctDegree(merged[\midinote])
								);
								merged[\midinote] = nil;
							};
						};

						merged;
					})
				}
			}),
			octDegree: {
				|a, b, xfade, scale=(Scale.chromatic), fadeFunc=\blend ...args|
				var degreeA, octA, degreeB, octB, midinoteA, midinoteB, value;
				#degreeA, octA = a;
				#degreeB, octB = b;

				midinoteA = scale.octDegreeToMidi(octA, degreeA);
				midinoteB = scale.octDegreeToMidi(octB, degreeB);

				if (fadeFunc.isSymbol) {
					fadeFunc = fadeFuncs[fadeFunc] ?? {
						Error("No fadeFunc with specified name %".format(fadeFunc)).throw;
					};
				};

				value = fadeFunc.asStream.next().value(midinoteA, midinoteB, xfade, *args);
				value = scale.midiToOctDegree(value);
				[value.octave, value.degree];
			},
			random: {
				|a, b, xfade|
				if (xfade > rrand(0.0, 1.0)) {
					b
				} {
					a
				}
			},
			blend: {
				|a, b, xfade|
				if (a.respondsTo('+')) {
					a.blend(b, xfade);
				} {
					fadeFuncs[\default].(a, b, xfade);
				}
			},
			expBlend: {
				|a, b, xfade|
				if (a.respondsTo('+')) {
					a.expBlend(b, xfade);
				} {
					fadeFuncs[\default].(a, b, xfade);
				}
			},
			randBlend: {
				|a, b, xfade|
				var f = { |r| log((r - 1).pow(2) / r.pow(2)) };
				if (a.respondsTo('+')) {
					a.blend(b, xfade.lincurve(0.0, 1.0, 0.0, 1.0, f.value(1.0.rand)));
				} {
					fadeFuncs[\default].(a, b, xfade);
				}
			},

			weightHash: {
				|a, b, xfade|
				var weights, choice, f;

				f = { |r| log((r - 1).pow(2) / r.pow(2)) };

				weights = [
					a.hash.linlin(-2147483647.0, 2147483647.0, 0.0, 1.0),
					b.hash.linlin(-2147483647.0, 2147483647.0, 0.0, 1.0)
				];

				weights = [
					(1 - xfade).lincurve(0.0, 1.0, 0.0, 1.0, f.(weights[0])),
					(xfade).lincurve(0.0, 1.0, 0.0, 1.0, f.(weights[1]))
				];
				weights = weights.normalizeSum;

				choice = weights[0] < weights[1];
				if (choice) {
					b
				} {
					a
				}
			},
			combinedHash: {
				|a, b, xfade|
				var hash;

				hash = [a, b].hash.linlin(-2147483647.0, 2147483647.0, 0.0, 1.0);
				if (xfade > hash) {
					b
				} {
					a
				}
			},
			balancedSum: Prout({
				var sumA = 0, sumB = 0, sumActual = 0;

				loop {
					yield({
						|a, b, fade, leak=0.05, targetFadeFunc=\blend|
						var distA, distB, target, value, expBlend;

						if (a.respondsTo('+').not) {
							fadeFuncs[\default].(a, b, fade);
						} {
							if (targetFadeFunc.isSymbol) {
								targetFadeFunc = fadeFuncs[targetFadeFunc] ?? {
									Error("No targetFadeFunc with specified name %".format(targetFadeFunc)).throw;
								};
							};

							sumA = sumA * (1 - leak);
							sumB = sumB * (1 - leak);
							sumActual = sumActual * (1 - leak);

							sumA = sumA + a;
							sumB = sumB + b;

							target = targetFadeFunc.(sumA, sumB, fade);

							distA = abs((sumActual + a) - target);
							distB = abs((sumActual + b) - target);

							value = (distA < distB).if(a, b);
							sumActual = sumActual + value;

							value;
						}
					})
				}
			}),
			flipFlop: Prout({
				var index, rand;

				loop {
					yield({
						|a, b, fade, flipProp=0.1|

						if (index.isNil || (1.0.rand < flipProp)) {
							index = (1.0.rand > fade).if(0, 1);
						};

						[a, b][index];
					})
				}
			}),
		);
		fadeFuncs[\default] = fadeFuncs[\random];
	}

	*new {
		|inputs, fade, fadeFunc=\random ...args|
		^super.newCopyArgs(inputs, fade, fadeFunc, args);
	}

	*par {
		|inputs, fade, fadeFunc=\random ...args|
		^super.newCopyArgs(inputs, fade, fadeFunc, args).parallel_(true);
	}

	embedInStream {
		|inval|
		var blendStream;
		var fadeFuncStream;

		if (parallel) { ^this.embedInStreamParallel(inval) };

		fadeFuncStream = fadeFunc;

		if (fadeFuncStream.isSymbol) {
			fadeFuncStream = fadeFuncs[fadeFuncStream] ?? {
				Error("No fadeFunc with specified name %".format(fadeFuncStream)).throw;
			};
		};

		fadeFuncStream = Pfunc({
			|func|
			{
				|...args|
				var argInputs, fade, fadeFuncArgs, fadeStart, subFade, subInputs;
				argInputs = args[0..(inputs.size - 1)];
				fade = args[inputs.size];
				fadeFuncArgs = args[(inputs.size+1)..];
				subFade = fade.mod(1.0);
				fadeStart = (fade - subFade).round(1).asInteger;
				subInputs = [
					argInputs.wrapAt(fadeStart),
					argInputs.wrapAt(fadeStart+1)
				];
				func.value(subInputs[0], subInputs[1], subFade, *fadeFuncArgs);
			}
		}) <> fadeFuncStream;

		(inputs ++ [fade] ++ fadeFuncArgs).postln;
		blendStream = Pnaryop(\value, fadeFuncStream, inputs ++ [fade] ++ fadeFuncArgs).asStream;

		while {
			(inval = blendStream.next(inval)).notNil
		} {
			inval = inval.yield;
		}
	}

	embedInStreamParallel {
		|inval|
		var blendStream;
		var fadeFuncStream;
		var parInputs, parLastValues;
		var lastFade = 0;
		var aTotalDuration=0, aPlayedDuration=0, bTotalDuration=0, bPlayedDuration=0, lastTimeA=0, lastTimeB=0;
		var falloff = 0.95;

		fadeFuncStream = fadeFunc;

		if (fadeFuncStream.isSymbol) {
			fadeFuncStream = fadeFuncs[fadeFuncStream] ?? {
				Error("No fadeFunc with specified name %".format(fadeFuncStream)).throw;
			};
		};

		parInputs = inputs.collect {
			|input, i|
			Pset('_fadeIndex', i, input)
		};
		parLastValues = Array.fill(parInputs.size, { Event.silent(1) });

		fadeFuncStream = Pfunc({
			|fadeFunc|
			{
				|...args|
				var delta, nextEvent, nextEventIndex, nextDur, fade, fadeDelta, fadeFuncArgs, targetRatio, shouldPlay,
				isA, ifNotPlayedRatio, ifPlayedRatio;

				nextEvent = args[0];
				fade = args[1];
				fadeFuncArgs = args[2..];

				delta = nextEvent[\delta];
				nextDur = nextEvent.use { ~dur.value * ~stretch.value };

				nextEventIndex = nextEvent['_fadeIndex'];
				if (nextEventIndex.isNil) {
					nextEvent
				} {
					if (lastFade.floor != fade.floor) {
						aTotalDuration = bTotalDuration = aPlayedDuration = bPlayedDuration = lastTimeA = lastTimeB = 0;
					};

					fadeDelta = fade - lastFade;
					lastFade = fade;

					targetRatio = abs(fade - nextEventIndex);
					isA = fade > nextEventIndex;

					if (nextEvent.isRest.not and: { targetRatio <= 1.0 }) {
						targetRatio = 1.0 - targetRatio;
						if ((targetRatio > 0.999) or: { targetRatio < 0.001 }) {
							shouldPlay = targetRatio > 0.99;
						} {
							if (isA) {
								aTotalDuration = aTotalDuration * falloff.pow((fadeDelta.abs * 100));
								aTotalDuration = aTotalDuration * falloff.pow(thisThread.beats - lastTimeA);

								aPlayedDuration = aPlayedDuration * falloff.pow((fadeDelta.abs * 100));
								aPlayedDuration = aPlayedDuration * falloff.pow(thisThread.beats - lastTimeA);

								lastTimeA = thisThread.beats;

								aTotalDuration = aTotalDuration + nextDur;
							} {
								bTotalDuration = bTotalDuration * falloff.pow((fadeDelta.abs * 100));
								bTotalDuration = bTotalDuration * falloff.pow(thisThread.beats - lastTimeB);

								bPlayedDuration = bPlayedDuration * falloff.pow((fadeDelta.abs * 100));
								bPlayedDuration = bPlayedDuration * falloff.pow(thisThread.beats - lastTimeA);
							lastTimeB = thisThread.beats;

							bTotalDuration = bTotalDuration + nextDur;
						};

						// Scale nextDur by inverse of desired ratio
						nextDur = nextDur * targetRatio.reciprocal;

						ifNotPlayedRatio = log2(isA.if(aPlayedDuration, bPlayedDuration) / isA.if(aTotalDuration, bTotalDuration));
						ifPlayedRatio = (nextDur + isA.if(aPlayedDuration, bPlayedDuration)) / isA.if(aTotalDuration, bTotalDuration);

						shouldPlay = ifNotPlayedRatio < 0;

						if (shouldPlay) {
							if (isA) {
								aPlayedDuration = aPlayedDuration + nextDur;
							} {
								bPlayedDuration = bPlayedDuration + nextDur;
							};
						}
					};

					if (shouldPlay) {
						nextEvent;
					} {
						Event.silent(delta);
					}
				} {
					// Fade is nowhere near this event, so we just silence.
					Event.silent(delta);
				}

				}
			}
		});

		blendStream = Pnaryop(\value, fadeFuncStream, [Ppar(parInputs), fade] ++ fadeFuncArgs).asStream;

		while {
			(inval = blendStream.next(inval)).notNil
		} {
			inval = inval.yield;
		}
	}


	// embedInStreamParallel {
	// 	|inval|
	// 	var blendStream;
	// 	var fadeFuncStream;
	// 	var parInputs, parLastValues;
	// 	var restUntil = 0;
	//
	// 	fadeFuncStream = fadeFunc;
	//
	// 	if (fadeFuncStream.isSymbol) {
	// 		fadeFuncStream = fadeFuncs[fadeFuncStream] ?? {
	// 			Error("No fadeFunc with specified name %".format(fadeFuncStream)).throw;
	// 		};
	// 	};
	//
	// 	parInputs = inputs.collect {
	// 		|input, i|
	// 		Pbind('_fadeIndex', i) <> input
	// 	};
	// 	parLastValues = Array.fill(parInputs.size, { Event.silent(1) });
	//
	// 	fadeFuncStream = Pfunc({
	// 		|fadeFunc|
	// 		{
	// 			|...args|
	// 			var nextEvent, fade, fadeFuncArgs, fadeStart, subFade, subInputs, resultEvent, delta;
	//
	// 			nextEvent = args[0];
	// 			fade = args[1];
	// 			fadeFuncArgs = args[2..];
	//
	// 			delta = nextEvent[\delta];
	// 			nextEvent['_nextScheduledTime'] = thisThread.beats + nextEvent.use({ ~dur.value * ~stretch.value });
	//
	// 			parLastValues[nextEvent['_fadeIndex']] = nextEvent;
	//
	// 			"Time is %".format(thisThread.beats).postln;
	//
	// 			if (delta > 0 and: { thisThread.beats >= restUntil }) {
	// 				subFade = fade.mod(1.0);
	// 				fadeStart = (fade - subFade).round(1).asInteger;
	//
	// 				subInputs = [
	// 					parLastValues.wrapAt(fadeStart),
	// 					parLastValues.wrapAt(fadeStart+1)
	// 				];
	//
	// 				if (subInputs[0].isNil) {
	// 					if (subInputs[1].isNil) {
	// 						resultEvent = Event.silent(delta);
	// 					} {
	// 						resultEvent = subInputs[1];
	// 					}
	// 				} {
	// 					if (subInputs[1].isNil) {
	// 						resultEvent = subInputs[0];
	// 					} {
	// 						// Both subInputs are non-nil, so merge them...
	// 						"Event a: %".format(subInputs[0]).postln;
	// 						"Event b: %".format(subInputs[1]).postln;
	//
	// 						resultEvent = fadeFunc.value(subInputs[0], subInputs[1], subFade, *fadeFuncArgs);
	//
	// 						"Merged event: %".format(resultEvent).postln;
	//
	// 						if (resultEvent.isNil) {
	// 							resultEvent = Event.silent(delta);
	// 						} {
	// 							restUntil = resultEvent['_nextScheduledTime'];
	// 							"Muting new events from now(%) until %".format(thisThread.beats, restUntil).postln;
	//
	// 							if (resultEvent.isRest.not) {
	// 								resultEvent.use {
	// 									~dur = (restUntil - thisThread.beats) / ~stretch.value;
	// 								};
	// 							};
	//
	// 							resultEvent;
	// 						}
	// 					}
	// 				};
	//
	// 				resultEvent[\delta] = delta;
	//
	// 				resultEvent;
	// 			} {
	// 				"Waiting for % beats".format(delta).postln;
	// 				// Another event coming soon, lets wait for that to do our fade...
	// 				Event.silent(delta);
	// 			}
	// 		}
	// 	}) <> fadeFuncStream;
	//
	// 	blendStream = Pnaryop(\value, fadeFuncStream, [Ppar(parInputs), fade] ++ fadeFuncArgs).asStream;
	//
	// 	while {
	// 		(inval = blendStream.next(inval)).notNil
	// 	} {
	// 		inval = inval.yield;
	// 	}
	// }
}

+Object {
	px {
		|other, fade, fadeFunc=\random ...args|
		^Pxfade([this, other], fade, fadeFunc, *args)
	}
}