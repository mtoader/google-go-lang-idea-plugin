package main

type T struct{}

func _() {

  t := T{}
  f1(t)

  t2 := *T{}
  f1(t)

}

func f1(T){}
