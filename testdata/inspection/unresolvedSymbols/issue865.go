package main

type Foo struct {
	Things []string
}

func main() {
	good1 := doSomething()  // foo marked as Unused
	for _, v := range (*good1).Things { // foo marked as Unresolved
		_ = v
	}

	good2 := doSomething()
	_=(*good2).Things
}

func doSomething() *Foo {
	return &Foo{}
}