+Object {
	assertShape {
		|...args|
		if (this.shape != args) {
			Exception("Expected array of shape % (array is %)".format(args, this.shape)).throw;
		}
	}

	assertChannels {
		|...args|
		if (this.asArray.shape != args) {
			Exception("Expected array of shape % (array is %)".format(args, this.shape)).throw;
		}
	}
}
