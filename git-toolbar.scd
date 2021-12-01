(
var w, size;
var repoButton, historyMenu, changedMenu, branchMenu, closeButton, newRepoButton, commitButton, branchButton;
var gitToolbar, noGitToolbar;
var noGitMenus, gitMenus;
var buttonSize = 18;

size = 450@22;

w = Window(
	bounds: Rect(
		Window.screenBounds.width - size.x - 8,
		Window.screenBounds.height - size.y,
		size.x,
		size.y
	),
	resizable:false, border:false
);

closeButton = MenuAction(nil, { w.close }).icon_(Material("close", buttonSize));

w.view.layout_(HLayout(
	[
		gitToolbar = ToolBar(
			branchMenu = Menu(),
			historyMenu = Menu(),
			changedMenu = Menu(),
			repoButton = MenuAction(),
			branchButton = MenuAction().icon_(Material("device_hub", buttonSize)),
			commitButton = MenuAction().icon_(Material("save", buttonSize)).action_({
				Require("/Users/fsc/Desktop/scztt-Core/git-dialog.scd", always:true)
			}),
			closeButton
		), align: \right
	],
	WindowHandleView().fixedWidth_(32),
).margins_(0).spacing_(0));

gitToolbar.background = Color.white;

w.alwaysOnTop = true;
w.front();

gitMenus = [branchMenu, historyMenu, changedMenu, repoButton, branchButton, commitButton, closeButton];
noGitMenus = [
	newRepoButton = (MenuAction()
		.icon_(Material("create_new_folder", buttonSize))
		.action_({
			var git = GitDocument.current.git;
			if (git.isNil) { GitDocument.current.initialize }
		})
	),
	closeButton
];

Document.signal(\current).connectToUnique({
	var status;
	var doc = GitDocument.current;


	doc.git !? {
		|git|

		repoButton.icon = Material("folder_open", buttonSize);
		repoButton.action = { "open '%'".format(doc.git.localPath).unixCmd  };

		branchMenu.clear();
		branchMenu.string = git.branch();

		git.branches.do {
			|br, i|
			branchMenu.addAction(
				MenuAction(br)
				.checked_(i == 0)
				.action_({
					git.checkout(br)
				})
			);
		};

		historyMenu.clear();
		historyMenu.string = git.sha[0..6];
		git.log(16).do {
			|log|
			historyMenu.addAction(MenuAction(
				"[%] %".format(
					log[\commit_hash][0..6],
					log[\subject][0..60]
				)
			))
		};

		status = git.status;
		changedMenu.clear();
		changedMenu.string = "(% files modified)".format(status.size);
		status.do {
			|s|
			changedMenu.addAction(
				MenuAction(
					"% %".format(
						switch (
							s[\status],
							\modified, "*",
							\added, "+",
							\deleted, "-",
							\renamed, ">",
							"~"
						),
						PathName(s[\file]).asRelativePath(git.localPath[0..git.localPath.size-2])
					)
				).checked_(s[\staged])
			);
		};

		gitToolbar.clear();
		gitMenus.do(gitToolbar.addAction(_));

	} ?? {
		gitToolbar.clear();
		noGitMenus.do(gitToolbar.addAction(_));
		// menus.do(_.string_(""));
	};
}).freeAfter(w);
)
