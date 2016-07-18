package main

type T struct{ name string }

func f(*T){}

func _() {
  x := &T{name : "name"}
  f(x)

  fRune(123)
  fRune(1.0)
  fRune(1.0 + 0i)
  fRune('2')

  fInt32('2')
  fInt32(   2   )
  fInt32(2.0)
  fInt32(2.0 + 0.0i)

  fFloat32('2')
  fFloat32(2)
  fFloat32(2.4)
  fFloat32(2.5 )


  fComplex('2')
  fComplex(2)
  fComplex(2.5)
  fComplex(2.5 + 6i)

  fString("123")

  fString(<error descr="Cannot use 'r' (type untyped rune) as type string">'r'</error>)
  fString(<error descr="Cannot use 2 (type untyped int) as type string">2</error>)
  fString(<error descr="Cannot use 3.6 (type untyped float64) as type string">3.6</error>)
  fString(<error descr="Cannot use 3.5 + 6i (type untyped complex128) as type string">3.5 + 6i</error>)

  fRune(<error descr="Cannot use \"qwe\" (type string) as type rune">"qwe"</error>)
  fInt32(<error descr="Cannot use \"qwe\" (type string) as type int32">"qwe"</error>)
  fFloat32(<error descr="Cannot use \"qwe\" (type string) as type float32">"qwe"</error>)
  fComplex(<error descr="Cannot use \"qwe\" (type string) as type complex128">"qwe"</error>)

}

func fRune(rune) {}
func fInt32(int32) {}
func fFloat32(float32) {}
func fComplex(complex128) {}
func fString(string) {}