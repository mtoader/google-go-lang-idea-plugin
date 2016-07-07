package main

type T struct{}

func _() {

  for i, c := range "asdasda" {
    f1(i)
    f2(c)
    f3(<warning descr="Cannot use i (type int) as type int32">i</warning>)
  }

  var m map[string]float32
  for k, v := range m {
    f4(k)
    f5(v)

    f4(<warning descr="Cannot use v (type float32) as type string">v</warning>)
    f5(<warning descr="Cannot use k (type string) as type float32">k</warning>)
  }

  var ch chan complex128
  for i := range ch {
    f6(i)

    f5(<warning descr="Cannot use i (type complex128) as type float32">i</warning>)
  }
}

func f1(int){}
func f2(rune){}
func f3(int32){}

func f4(string){}
func f5(float32){}

func f6(complex128){}

