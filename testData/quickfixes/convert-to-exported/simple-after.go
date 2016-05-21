package foo

type Foo struct {
	Ba int `json:"baz"`
}

func _() {
	_ = Foo{
		Ba: 1,
	}
}
