package main

func <warning descr="Unused function 'demo'">demo</warning>() {
	a := func() {
		demo()
	}
	_ = a
}

func <warning descr="Unused function 'demo2'">demo2</warning>() {}

func demo3() {}

func init() {
	demo3()
}

func main() {

}