package main

func main() {
	<error descr="Unused variable 'sum1'">sum1</error> := 0
	sum1 = 10

	var <error descr="Unused variable 'sum'">sum</error> = 0
	sum = 10

	var sum3 = 0
	sum3 += 10

	sum4 := 0
	sum4 += 10
	
	var       i int
	f(func() { i  = 0; println("test") })
}

func f(m func()) {
	m()
}

func foo() (int, int) {
	return 4, 5
}

func _() {
	<error descr="Assignment count mismatch: 2 element(s) assigned to 1 element(s)"><error descr="Unused variable 'x'">x</error> := <error descr="Multiple-value foo() in single-value context">foo()</error>, <error descr="Multiple-value foo() in single-value context">foo()</error></error>
}