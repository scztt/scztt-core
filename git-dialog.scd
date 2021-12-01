(
var path, git,
commitView, vlayout,
name, tag, update, sj, commit, cancel, body, subject, ok, files, last;
var filesList = Set();

path = Document.current.path;
if (path.isNil) {
	"Current document has no path".warn;
} {
	git = Git(Git.findRepoRoot(path));
	if (git.isNil) {
		"Cannot find a git repo for path: %".format(path).warn;
	} {
		commitView = nil;

		~gitWindow !? {
			~gitWindow.close;
		};

		last = git.log[0];

		~gitWindow = Window(resizable:true, border:true).layout_(
			VLayout(
				WindowHandleView().fixedHeight_(20),
				vlayout = VLayout(
					VLayout(
						HLayout(
							StaticText().string_("REPO:").align_(\right).fixedWidth_(60),
							name = DragSink().string_(git.localPath)
						).margins_(0).spacing_(1),
						HLayout(
							60,
							StaticText().string_(
								"% [%]".format(
									last !? _[\commit_hash] !? _[0..7] ?? "000000",
									last !? _[\subject] ?? ""
								)
							)
						).margins_(0).spacing_(1),
						10,
					).margins_(0).spacing_(2),
					nil
				).margins_(10).spacing_(8)
			).margins_(1).spacing_(0)
		);
		~gitWindow.onClose = {
			~gitWindow = nil;
			sj.stop;
		};

		~gitWindow.view.keyUpAction = {
			|v, char, mod, unicode, code|
			if (code == 53) {
				cancel.action.();
			}
		};

		~gitWindow.autoRememberPosition(\gitWindow);
		~gitWindow.front;

		commitView = View().layout_(
			VLayout(
				HLayout(
					60,
					ScrollView(bounds:100@120).canvas_(
						files = View().minHeight_(100).layout_(VLayout(nil).margins_(0).spacing_(1))
					)
				).spacing_(2).margins_(0),
				HLayout(
					StaticText().string_("SUBJECT:").align_(\right).fixedWidth_(60),
					subject = TextField().focus()
				).margins_(0).spacing_(2),
				HLayout(
					[StaticText().string_("BODY:").align_(\right).fixedWidth_(60), align:\top],
					body = TextView().minHeight_(60)
				).margins_(0).spacing_(2),
				HLayout(
					HLayout(
						StaticText().string_("TAG:").align_(\right).fixedWidth_(60),
						tag = TextField()
					).margins_(0).spacing_(1),
					nil,
					cancel = Button().states_([["Cancel"]]),
					ok = Button().states_([["Ok"]])
				)
			).margins_(0)
		);

		git.status.do {
			|stat|
			var checked = false;
			var bt = StaticText().string_("");
			bt.mouseDownAction = {
				checked = checked.not;
				bt.string = "% %".format(
					if (checked, "☒", "☐"),
					stat.file.replace(git.localPath, "");
				);

				if (checked) {
					filesList.add(stat.file);
					if (filesList.isEmpty.not) { ok.enabled = true }
				} {
					filesList.remove(stat.file);
					if (filesList.isEmpty) { ok.enabled = false }
				}
			};

			bt.mouseDownAction.();

			files.layout.insert(bt, 0);
		};

		vlayout.add(commitView);

		commitView.onClose = { commitView = nil };

		ok.action = {
			if (subject.string.isEmpty.not) {
				"COMMITING:\n%\n%\n%".format(subject.string, body.string, filesList.asArray.join("\n")).postln;
				filesList.do(git.add(_));
				git.commit(subject.string, body.string, filesList.asArray).postln;
				~gitWindow.close();
				~gitWindow = nil;

				if (tag.string.notEmpty) {
					git.addTag(tag.string);
				}
			}
		};

		cancel.action = {
			~gitWindow.close();
			~gitWindow = nil;
		};
	};
};
)




