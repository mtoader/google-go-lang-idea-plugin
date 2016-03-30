package main

func main() {
	for _, i := range []int{1, 2, 3} {
		println(<weak_warning descr="Suspicious: obtaining address of a for loop variable">&i</weak_warning>)
	}
}
