package main;

import "fmt"

type MyType int;

func (t1 MyType) asd() {}

func (t2 MyType) foo() {}

func (m MyType) f2() {
  i := m
  fmt.Println(i)
}