package main;

import "fmt"

type MyType int;

func (<weak_warning descr="Receiver names are different">t1</weak_warning> MyType) asd() {}

func (<weak_warning descr="Receiver names are different">t2</weak_warning> MyType) foo() {}

func (<weak_warning descr="Receiver names are different">myType<caret></weak_warning> MyType) f2() {
  i := myType
  fmt.Println(i)
}