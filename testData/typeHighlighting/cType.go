package main

import "C"

func f(C.uint32_t){}
func f2(*C.uint32_t){}

func main() {
  var t C.uint32_t
  f(t)

  var t2 uint32
  f(t2)

  var t3 *C.uint32_t
  f2(t3)
}