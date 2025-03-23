// FlatArrayEntryDescription {
//     var <>class, <>size;

//     *new {
//         |class, size|
//         ^super.newCopyArgs(class, size);
//     }

//     create {
//         |array|
//         this.class.fromFlatOutput(

//         )
//     }
// }

// +Object {
//     toFlatOutput {
//         Error("Don't know how to handle '%' as a Synth output").throw
//     }
// }

// +Number {
//     fromFlatOutput {
//         |arrayStream, descriptionStream|
//         var desc = descriptionStream.next;
//         description.class.newFrom(*array);
//     }

//     toFlatOutput {
//         ^[this, FlatArrayEntryDescription(
//             class: this.class,
//             size: 1
//         )]
//     }
// }

// +Collection {
//     *fromFlatOutput {
//         |description, arrayStream, descriptionStream|
//         ^description.size.collectAs({
//             |i|
//             var desc = descriptionStream.next;

//             desc.class.fromFlatOutput(
//                 desc,
//                 arrayStream,
//                 descriptionStream
//             )
//         }, description.class)
//     }

//     toFlatOutput {
//         var flatArray = [];
//         var descriptions = [];

//         descriptions = descriptions.add(FlatArrayEntryDescription(
//             this.class,
//             this.size
//         ));

//         this.collect {
//             |item, i|
//             var childArray, childDesc;

//             #childArray, childDesc = item.toFlatOutput();

//             flatArray = flatArray.addAll(childArray);
//             descriptions = descriptions.addAll(childDesc);

//         };

//         ^[flatArray, descriptions]
//     }
// }

