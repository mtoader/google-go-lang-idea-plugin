package main

import "C"

type T []complex128

func _() {
  f1(nil)
  f2(nil...)

  f3 (1)
  f3(nil)


  f2(<warning descr="Cannot use nil as type string">nil</warning>)
  f2(<warning descr="Cannot use nil as type string">nil</warning>, <warning descr="Cannot use nil as type string">nil</warning>)

  nil := 1

  f1(<warning descr="Cannot use nil (type int) as type T">nil</warning>)
  f2(<warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>)
  f2(<warning descr="Cannot use nil (type int) as type string">nil</warning>...)

  f3 (1)
  f3(nil)

  f4(nil)
  f4(<warning descr="Cannot use nil (type int) as type C.scmp_cast_t">nil</warning>...)
}

func f1(T){}
func f2(...string){}

func f3(interface{}){}

func f4(C.scmp_cast_t){}