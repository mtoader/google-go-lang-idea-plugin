package main

type S struct {
	B
}

type B struct {
	bar string
}

func main() {
	var s S
	s.bar<caret> = "bar"
	print(s.bar)
}