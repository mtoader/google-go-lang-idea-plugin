package main

type T struct {
	x, y int
}

var _ = map[string]struct {
	x, y int
}{
	struct{ x, y <caret>int }{3, 4},
}

func main (){
}