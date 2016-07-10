package main

func <warning descr="Unused function 'demo'">demo</warning>() {
	a := func() {
		demo()
	}
	_ = a
}

func <warning descr="Unused function 'demo2'">demo2</warning>() {}

func demo3() {}


func alpha() {
	alpha()
}

func <warning descr="Unused function 'beta'">beta</warning>() {
	alpha()
}


func bar() {
	foo()
}

func foo() {
	foo()
}


func init() {
	demo3()
}

func main() {

}