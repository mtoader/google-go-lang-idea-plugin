package main

import "unknown"

func f (unknown.int){}

func _() {

  var t unknown.int
  f(t)

  v := make([]unknown.t, 0, 10)
  unknown.f(v)
  unknown.f(v...)

}
