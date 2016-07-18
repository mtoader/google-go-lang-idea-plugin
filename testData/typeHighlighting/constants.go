package main

const (
  r = '1'
  i = 123
  f = 2.3
  f2 = 2.0
  c = 2.0 + 1.0i
  c2 = complex(2.0, 0)
  c3 = 2.0 + 0.0i
  s = "asd"
  )

type T int
const C T = 3

const (
  q1 = C
  q2 = C
  str = "str"
  q3 = C
)


func _() {
  fString(str)

  fRune(r)
  fRune(i)
  fRune(f2)
  fRune(c2)

  fInt32(r)
  fInt32(i)
  fInt32(f2)
  fInt32(c2)

  fFloat32(r)
  fFloat32(i)
  fFloat32(f)
  fFloat32(c3)

  fComplex(r)
  fComplex(i)
  fComplex(f)
  fComplex(c)

  fString(s)

  fString(<error descr="Cannot use r (type untyped rune) as type string">r</error>)
  fString(<error descr="Cannot use i (type untyped int) as type string">i</error>)
  fString(<error descr="Cannot use f (type untyped float64) as type string">f</error>)
  fString(<error descr="Cannot use c (type untyped complex128) as type string">c</error>)

  fRune(<error descr="Cannot use s (type string) as type rune">s</error>)
  fInt32(<error descr="Cannot use s (type string) as type int32">s</error>)
  fFloat32(<error descr="Cannot use s (type string) as type float32">s</error>)
  fComplex(<error descr="Cannot use s (type string) as type complex128">s</error>)

}

func fRune(rune) {}
func fInt32(int32) {}
func fFloat32(float32) {}
func fComplex(complex128) {}
func fString(string) {}