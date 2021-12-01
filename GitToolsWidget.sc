GitToolsWidget : ServerWidgetBase {
	*initClass {
		Class.initClassTree(ServerView);
		ServerView.widgets = ServerView.widgets.add(GitToolsWidget)
	}

	view {
		var view, size;
		var fileName, repoButton, historyMenu, changedMenu, branchMenu, newRepoButton, commitButton, branchButton;
		var gitToolbar, noGitToolbar;
		var noGitMenus, gitMenus;
		var buttonSize = 18;

		view = View();
		view.fixedHeight = 28;
		view.layout_(HLayout(
			[
				gitToolbar = ToolBar(
					fileName = MenuAction().enabled_(false),
					branchMenu = Menu(),
					historyMenu = Menu(),
					changedMenu = Menu(),
					branchButton = MenuAction().icon_(Material("device_hub", buttonSize)),
					commitButton = MenuAction().icon_(Material("save", buttonSize)).action_({
						Require("/Users/fsc/Desktop/scztt-Core/git-dialog.scd", always:true)
					}),
					MenuAction.separator,
					repoButton = MenuAction(),
				),
				align: \right
			]
		).margins_(0).spacing_(0));

		gitMenus = [fileName, branchMenu, historyMenu, changedMenu, branchButton, commitButton, MenuAction.separator, repoButton];
		noGitMenus = [
			fileName,
			newRepoButton = (MenuAction()
				.icon_(Material("create_new_folder", buttonSize))
				.action_({
					var git = GitDocument.current.git;
					if (git.isNil) { GitDocument.current.initialize }
				})
			),
		];

		branchButton.action = {
			GitProject.current !? _.branchGui({
				Document.changed(\current);
			});
		};

		Document.signal(\current).connectToUnique({
			var status;
			var doc = GitDocument.current;

			fileName.string = Document.current !? _.title;

			doc !? _.git !? {
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
			};
		}).freeAfter(view);

		Document.changed(\current);

		view.toFrontAction = { Document.changed(\current) };

		^view
	}
}