package main

type T []complex128

func (T) f(int) T
func (*T) f1(int) T

func foo (func(T, int) T){}
func fooPointer (func(*T, int) T){}
func foo2(T){}
func foo3( func(int) T){}


func main() {
  var x T

  foo(T.f)
  foo3(x.f)
  fooPointer((*T).f1)

  foo3(<warning descr="Cannot use (*T).f1 (type func (*T, int) (T)) as type func(int) T">(*T).f1</warning>)
  foo(<warning descr="Cannot use x.f (type func (int) T) as type func(T, int) T">x.f</warning>)

  foo2(x.f(1))
  foo2(x.f(1).f(100))


  foo(<warning descr="Cannot use x.f(1) (type T) as type func(T, int) T">x.f(1)</warning>)
}