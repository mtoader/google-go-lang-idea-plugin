package main

import "C"

type T []complex128

func _() {
  f1(nil)
  f2(nil...)

  f3 (1)
  f3(nil)


  f2(<error descr="Cannot use nil as type string">nil</error>)
  f2(<error descr="Cannot use nil as type string">nil</error>, <error descr="Cannot use nil as type string">nil</error>)

  nil := 1

  f1(<error descr="Cannot use nil (type int) as type T">nil</error>)
  f2(<error descr="Cannot use nil (type int) as type string">nil</error>, <error descr="Cannot use nil (type int) as type string">nil</error>)
  f2(<error descr="Cannot use nil (type int) as type string">nil</error>...)

  f3 (1)
  f3(nil)

  f4(nil)
  f4(<error descr="Cannot use nil (type int) as type C.scmp_cast_t">nil</error>...)
}

func f1(T){}
func f2(...string){}

func f3(interface{}){}

func f4(C.scmp_cast_t){}