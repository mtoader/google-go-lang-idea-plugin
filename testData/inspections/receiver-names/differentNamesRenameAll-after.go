package main;

import "fmt"

type MyType int;

func (m MyType) asd() {}

func (m MyType) foo() {}

func (m MyType) f2() {
  i := m
  fmt.Println(i)
}