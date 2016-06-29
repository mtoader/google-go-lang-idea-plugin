package main;

import "fmt"

type MyType int;

func (<weak_warning descr="Receiver has generic name"><weak_warning descr="Receiver names are different">me</weak_warning></weak_warning> MyType) asd() {}

func (<weak_warning descr="Receiver has generic name"><weak_warning descr="Receiver names are different">me<caret></weak_warning></weak_warning> MyType) f1() {
  fmt.Println(me)
}

func (<weak_warning descr="Receiver names are different">t2</weak_warning> MyType) f2() {
  t2 = nil
}