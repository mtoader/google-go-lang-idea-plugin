package main

func fFloat(float64){}
func fInt(int){}
func fComplex(complex128){}

func _() {

  var c complex128

  fFloat(real(c))
  fFloat(imag(c))

  fComplex(complex(1,2))

}
