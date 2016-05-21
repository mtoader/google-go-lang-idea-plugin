package foo

type Foo struct {
	ba<caret> int `json:"baz"`
}

func _() {
	_ = Foo{
		ba: 1,
	}
}
