Gdef : Singleton {
	classvar <groupOrder, rebuildBundle, <nodeMap;
	var <group, <afterList, <beforeList, <server, <>permanent=true;
	classvar blockUpdate=false, updateCount=0;

	*initClass {
		[Event, EventTypesWithCleanup].do(Class.initClassTree(_));

		groupOrder = LinkedList();
		nodeMap = IdentityDictionary();
		ServerBoot.add(this);
		CmdPeriod.add({
			this.all.reject(_.permanent).postln.do {
				|gdef|
				gdef.clear();
			}
		});

		this.registerEventTypes();
	}

	*registerEventTypes {
		Event.addEventType(\gdef, {
			var server, gdef;
			server = ~server ?? { Server.default };

			~gdef = Gdef(
				~name ?? \default,
				before:~before.asArray,
				after:~after.asArray
			).permanent_(false);

			~id = ~gdef.asControlInput;

			~schedBundleArray.value(~lag, ~timingOffset, server, Gdef.takeFinalBundle, false);
		});

		Event.addEventType(\gdefClear, {
			~gdef.clear();
		});
	}

	*new {
		|name, after=([]), before=([])|
		^super.new(name, after, before);
	}

	*beforeUse {
		blockUpdate = true;
		updateCount = 0;
	}

	*afterUse {
		blockUpdate = false;
		if (updateCount > 0) {
			updateCount = 0;
			this.rebuildAll();
		};
	}

	init {
		afterList = [];
		beforeList = [];
		server = Server.default;
	}

	prNormalizeList {
		|list|
		list = list.isString.if({ [list] }, { list.asArray });
		^list.collect({ |item| item.isKindOf(Gdef).if({ item.name }, { item }) });
	}

	set {
		|after=([]), before=([])|

		afterList = this.prNormalizeList(after);
		beforeList = this.prNormalizeList(before);
		this.class.rebuildAll()
	}

	group_{
		|newGroup|
		if (group.isNil or: { group.nodeID != newGroup.nodeID }) {
			group.free;
			group = newGroup;
			group.onFree {
				|originalGroup|
				if (originalGroup == group) {
					group = nil;
				}
			}
		}
	}

	*clear {
		groupOrder = LinkedList();
		^super.clear();
	}

	clear {
		if (group.notNil) {
			nodeMap[group.nodeID] = nil;
			group.free;
			group = nil;
		}
		^super.clear()
	}

	before { |...gdefs| 	beforeList = beforeList.addAll(this.prNormalizeList(gdefs)) }
	after { |...gdefs| 		afterList = afterList.addAll(this.prNormalizeList(gdefs)) }

	*rebuildAll {
		var rules, order, iter=0, key;

		if (blockUpdate) { updateCount = updateCount + 1; ^this };

		rules = this.prRules();
		order = List();
		while { rules.notEmpty && (iter < 1000) } {
			key = rules.keys.asArray[0];
			this.doSort(key, rules, order);
			iter = iter + 1
		};

		if (order != groupOrder or: {
			order.detect({
				|o|
				Gdef.all.at(o) !? { |g| g.group.isNil } ?? { false }
			}).notNil
		}) {
			groupOrder = order;
			this.prSend();
		}
	}

	*doOnServerBoot {
		this.prSend(true);
	}

	*prSend {
		|reset=false|
		var previous = nil;
		nodeMap = IdentityDictionary();

		if (Server.default.serverRunning) {
			rebuildBundle = Server.default.makeBundle(false, {
				groupOrder.do {
					|gdef|

					gdef = Gdef.all.at(gdef);

					if (gdef.notNil) {
						if (gdef.group.isNil || reset) {
							if (previous.notNil) {
								gdef.group = Group(previous, \addAfter);
							} {
								gdef.group = Group(Server.default, \addToHead);
							}
							// "adding group % after %".format(gdef.group.nodeID, previous.tryPerform(\nodeID)).postln;
						} {
							if (previous.notNil) { gdef.group.moveAfter(previous ?? Server.default) };
							// "moving group % after %".format(gdef.group.nodeID, previous.tryPerform(\nodeID)).postln;
						};

						nodeMap[gdef.group.nodeID] = gdef;

						previous = gdef.group;
					};
				}
			}, rebuildBundle);

			{ this.prSendFinalBundle }.defer(0)
		}
	}

	*takeFinalBundle {
		var bundle = rebuildBundle;
		rebuildBundle = nil;
		^bundle
	}

	*prSendFinalBundle {
		rebuildBundle !? {
			// "final bundle is: %".format(rebuildBundle).postln;
			Server.default.makeBundle(nil, {}, rebuildBundle);
			rebuildBundle = nil;
		}
	}

	*prRules {
		var rules = ();

		this.all.do {
			|def|
			var list;

			list = rules[def.name] ?? { rules[def.name] = list = IdentitySet(); list; };
			list.addAll(def.afterList);

			def.beforeList.do {
				|before|
				list = rules[before] ?? { rules[before] = list = IdentitySet(); list; };
				list.add(def.name);
			}
		};

		^rules
	}

	*prValidate {
		var violations;
		var rules = this.prRules();
		rules.keysValuesDo {
			|item, afters|
			afters.do {
				|after|
				var itemIndex, afterIndex;
				itemIndex = groupOrder.indexOf(item);
				afterIndex = groupOrder.indexOf(after);
				if (itemIndex.notNil && afterIndex.notNil) {
					if (itemIndex < afterIndex) {
						violations = violations.add([item, after, itemIndex, afterIndex])
					}
				}
			}
		};

		violations.do {
			|v|
			"Rule (% after %) was broken - item at index %, after at index %".format(
				"//" ++ v[0], "//" ++ v[1],
				v[2], v[3]
			).postln;
		}
	}

	*doSort {
		|key, rules, order, index=0|
		var item, after;

		if (order.includes(key)) {
			Error("Group list already contains % - probably a constraint error?".format(key)).throw;
		};

		after = rules[key] ?? {[]};
		rules[key] = nil;

		// "Processing rule % -> % (for index %)".format(key, after, index).postln;

		while { after.notEmpty } {
			var itemAfter = after.pop();
			var foundIndex = order.indexOf(itemAfter);
			if (foundIndex.notNil) {
				// "Item % found at %".format(itemAfter, foundIndex).postln;
				index = max(foundIndex, index);
			} {
				// "Item % not found...".format(itemAfter).postln;
				index = max(index, this.doSort(itemAfter, rules, order, index + 1) ?? index);
				// this.doSort(itemAfter, rules, order, index + 1)
			};
		};

		if (order.includes(key)) {
			Error("uh oh: we already have %".format(key)).throw;
		};

		index = index + 1;
		order.insert(index, key);
		// "  Finally item % inserted at %: %".format(key, index, order).postln;

		^index;
	}

	asPattern {
		^Pfunc({ this.group })
	}
	pat 		{ ^this.asPattern }
	asStream 	{ ^this.asPattern.asStream }

	asPbus {
		|pattern, dur=2.0, fadeTime=0.02, numChannels=2, rate=\audio|
		var result = Pbus(pattern, dur, fadeTime, numChannels, rate) <> Pbind(\group, this.asPattern);
		if (pattern.isNil) {
			result = result <> Pid()
		};
		^result;
	}

	pbus {
		|pattern, dur=2.0, fadeTime=0.02, numChannels=2, rate=\audio|
		^this.asPbus(pattern, dur, fadeTime, numChannels, rate)
	}

	asControlInput { ^group.asControlInput }
	asGroup { ^group }
	asTarget { ^group }
}

// Bgdef : Gdef {
// 	var <bus, numChannels, rate;
//
// 	*new {
// 		|name, after=([]), before=([]), channels: 2, rate:\audio|
// 		^super.new(name, after, before, channels, rate);
// 	}
//
// 	set {
// 		|after=([]), before=([]), channels, inRate|
// 		numChannels = channels;
// 		rate = inRate;
// 		this.prUpdateBus();
//
// 		^super.set(after, before);
// 	}
//
// 	prUpdateBus {
// 		var needsChange = bus.isNil or: {
// 			(bus.numChannels != numChannels)
// 			|| (bus.rate != rate)
// 		};
// 		Bus
// 	}
//
// 	*doOnServerBoot {
// 		super.doOnServerBoot();
//
// 		bus = nil;
// 		this.prUpdateBus();
// 	}
// }
