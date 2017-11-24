package main

type b func(intParam int)
type a b

func c() {
	aFunc := generateFunc()
	if f, ok := aFunc.(a); ok {
		<error descr="not enough arguments in call to f">f</error>()
	}
}

func generateFunc() interface{} {
	return a(func (intParam int) { })
}

func main() {
	c()
}

func _() {
	<error descr="not enough arguments in call to ff">ff</error>(1, f())
}

func f() (int, int) {
	return 2, 3
}

func ff(int, int, int) () {

}

func foo(_ int) {
}

func bar() {
}

func _() {
	<error descr="not enough arguments in call to foo">foo</error>(<error>bar()</error>)
}