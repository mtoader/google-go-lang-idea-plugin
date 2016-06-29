package main;

import "fmt"

type MyType int;

func (m MyType) asd() {}

func (m<caret> MyType) f1() {
  fmt.Println(m)
}

func (m MyType) f2() {
  m = nil
}