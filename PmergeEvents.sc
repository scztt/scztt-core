// PmergeEvents : FilterPattern {
//     var key;

//     *new {
//         |pattern, key=\identity|
//         ^super.newCopyArgs(pattern, key)
//     }

//     embedInStream {
//         |inval|
//         var inputStream = pattern.asStream;
//         var next, nextDelta;

//         while { 
//             next = inputStream.next(inval);
//             next.notNil;
//         } {
//             inval = ();
//             next.keysValuesDo {
//                 |id, event|
//                 event = event.copy.put(key, id);
//                 inval[id] = event.yield;
//             }
//         }
//     }
// }

// PsplitEvents : FilterPattern {
//     var key;

//     *new {
//         |pattern, key=\identity|
//         ^super.newCopyArgs(pattern, key)
//     }

//     embedInStream {
//         |inval|
//         var inputStream = pattern.asStream;
//         var next, nextDelta;

//         while { 
//             next = inputStream.next(inval);
//             next.notNil;
//         } {
//             inval = ();
//             next.keysValuesDo {
//                 |id, event|
//                 event = event.copy.put(key, id);
//                 inval[id] = event.yield;
//             }
//         }
//     }
// }


