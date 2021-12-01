(
var obj, remoteValue, changeFunc, updateFunc;
var controller;
var actions = [];

obj = Node();
remoteValue = Node();
changeFunc = {
	obj.group = 100.rand;
	obj.changed(\group);
};
updateFunc = {
	|who, what, val|
	remoteValue.group = who.group;
};

obj.addDependant(updateFunc);

///////////////////////////////////////////////////////////////
"Direct update: ".post;
{

	10000.do {
		obj.group = 100.rand;
		remoteValue.group = obj.group;
	}
}.bench;


///////////////////////////////////////////////////////////////
"Via dependent / changed: ".post;
{
	10000.do {
		changeFunc.();
	}
}.bench;


///////////////////////////////////////////////////////////////
"Via SimpleController: ".post;
controller = SimpleController(obj).put(\group, updateFunc);
{

	10000.do {
		changeFunc.();
	}
}.bench;


///////////////////////////////////////////////////////////////
"Via NotificationCenter: ".post;
changeFunc = {
	obj.group = 100.rand;
	NotificationCenter.notify(obj, \group, obj.group);
};
NotificationCenter.register(obj, \group, updateFunc, {
	|val|
	remoteValue.group = val
});
{
	10000.do {
		changeFunc.();
	}
}.bench;

///////////////////////////////////////////////////////////////
"Via raw func list: ".post;
changeFunc = {
	|actions|
	obj.group = 100.rand;
	actions.do { |a| a.(obj, obj.group) }
};

{
	var actions = [updateFunc];
	10000.do {
		changeFunc.(actions);
	}
}.bench;

///////////////////////////////////////////////////////////////
"Via raw obj set list: ".post;
changeFunc = {
	|actions|
	obj.group = 100.rand;
	actions.([remoteValue], obj.group)
};

{
	var actions = "{|objs, val| objs[0].group = val}".interpret;
	10000.do {
		changeFunc.(actions);
	}
}.bench;

)