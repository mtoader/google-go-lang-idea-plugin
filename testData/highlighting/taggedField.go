package demo

type Foo struct {
	<weak_warning descr="Cannot use tag with multiple fields in declaration">bar, bz int `json:"bar"`</weak_warning>
	Baz     int `json:"baz"`
	<weak_warning descr="Field \"ba\" is not exported but has a tag attached to it">ba      int `json:"baz"`</weak_warning>
	Demo    int `json:"demo"`
}

func _() {
	_ = Foo{
		ba: 1,
	}
}
