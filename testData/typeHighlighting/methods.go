package main

type T []complex128

type T2 struct{ i int }
type T3 struct { q T2 }
type S struct { T }
type P struct { s *S }

func (T) f(int) T {return nil}
func (*T) f1(int) T {return nil}

func (T2) t2f(int) T {return nil}

func foo (func(T, int) T){}
func fooPointer (func(*T, int) T){}
func foo2(T){}
func foo3( func(int) T){}

func foo4() T2 { return *new(T2)}
func foo5(func (*S, int) T){}

func (P) fp (int) T {return nil}
const p = P(something)

func main() {

  var x T
  var y T2
  var z T3

  foo(T.f)
  foo3(x.f)
  fooPointer((*T).f1)
  foo3(T2{ i : 2 }.t2f)
  foo3(T2(y).t2f)
  foo3(z.q.t2f)
  foo3(foo4().t2f)

  foo3(<warning descr="Cannot use (*T).f1 (type func (*T, int) (T)) as type func(int) T">(*T).f1</warning>)
  foo(<warning descr="Cannot use x.f (type func (int) T) as type func(T, int) T">x.f</warning>)

  foo2(x.f(1))
  foo2(x.f(1).f(100))

  foo(<warning descr="Cannot use x.f(1) (type T) as type func(T, int) T">x.f(1)</warning>)

  foo5((*S).f1)

  foo5(<warning descr="Cannot use p.s.f1 (type func (int) T) as type func (*S, int) T">p.s.f1</warning>)
  foo5(<warning descr="Cannot use p.fp (type func (int) T) as type func (*S, int) T">p.fp</warning>)
}