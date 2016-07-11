package main

import "unsafe"

type T int
type T2 [][]string

func main() {
  var (
    i int
    ii []int
    t1 T
    t2 T2
  )

  unsafe.Sizeof(i)
  unsafe.Sizeof(ii)
  unsafe.Sizeof(t1)
  unsafe.Sizeof(t2)
}