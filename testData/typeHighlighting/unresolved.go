package main

import "unknown"

func f (unknown.int){}

func f2 (unresolvedType){}

type I interface{ f() }
func f3 (I){}

type S struct {
  *unresolvedType
}
func f4([]int){}

func _() {

  var t unknown.int
  f(t)

  v := make([]unknown.t, 0, 10)
  unknown.f(v)
  unknown.f(v...)
  f2(2)
  f2("s")

  var t unresolvedType
  f2(t)
  f2(nil)

  var t2 *unresolvedType2
  f3(t2)

  s := new(S)
  f4(*s.F)

  f3(s)
}
