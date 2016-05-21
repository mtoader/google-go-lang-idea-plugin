package foo

type Foo struct {
	bar int `json:"baz"`
	baz int
	Foo int `json:"foo"`
}

func _() {
	_ = Foo{
		bar: 1,
		baz: 1,
		Foo: 1,
	}
}
