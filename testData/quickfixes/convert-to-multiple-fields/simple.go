package foo

type Foo struct {
	bar, baz<caret> int `json:"baz"`
	Foo int `json:"foo"`
}

func _() {
	_ = Foo{
		bar: 1,
		baz: 1,
		Foo: 1,
	}
}
