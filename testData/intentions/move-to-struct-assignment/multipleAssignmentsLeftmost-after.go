package main

type S struct {
	foo string
}

func main() {
	var b, a
	s := S{foo: "bar"}
	b, a = "b", "a"
	print(s.foo)
	print(b)
	print(a)
}