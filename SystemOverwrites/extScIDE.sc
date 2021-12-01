// +ScIDE {
// 	*prClassIntrospection {
// 		|class|
// 		^[
// 			class.name,
// 			class.class.name,
// 			class.superclass !? {class.superclass.name},
// 			class.filenameSymbol,
// 			class.charPos,
// 			class.methods.collect({ |m| this.serializeMethodDetailed(m) })
// 			++ class.completions()
// 		];
// 	}
//
// 	*sendIntrospection {
// 		var res = [];
// 		Class.allClasses.do {
// 			|class|
// 			var introspection = this.prClassIntrospection(class);
// 			res = res.add(introspection);
//
// 			class.tryPerform(\pseudoclasses, introspection).do {
// 				|pseudoclass|
// 				if (pseudoclass.size == 6) {
// 					res = res.add(pseudoclass);
// 				} {
// 					"pseudoclass doesn't look right: %".format(pseudoclass).warn;
// 				}
// 			}
// 		};
// 		this.send(\introspection, res);
// 	}
// }
//
// +Class {
// 	completions {
// 		var completions = [], class = this.name.asString;
//
// 		if(class.contains("Meta_")) {
// 			class = class.replace("Meta_", "").asSymbol.asClass;
// 			if (class.respondsTo(\extraCompletions)) {
// 				completions = class.extraCompletions();
// 				completions = completions.collect {
// 					|completion|
// 					[
// 						this.name,
// 						completion[0],
// 						this.filenameSymbol,
// 						this.charPos,
// 						completion[1]
// 					]
// 				}
// 			};
// 		};
//
// 		^completions;
// 	}
// }
//
// +Scale {
// 	*extraCompletions {
// 		Scale.initClass();
// 		^Scale.all.parent.keys.asArray.collect({
// 			|scale|
// 			[
// 				scale.asSymbol,
// 				[]
// 			]
// 		});
// 	}
// }