package main

type T struct {
	x, y int
}

var _ = []*T{
	<weak_warning descr="Redundant type declaration">&T<caret>{1, 2}</weak_warning>,
}

func main (){
}