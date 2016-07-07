package main

type (
  T struct{
    *T1
  }

  T1 int

  AnotherT int

  I interface{
    f()
    f2()
    f3()
    f4()
  }
)

func (*T) f(){}
func (*T1) f2(){}
func (T) f3(){}
func (T1) f4(){}


func (*AnotherT) f(){}
func (AnotherT) f2(){}
func (AnotherT) f3(){}


func _() {

  t := new(T)
  forI(t)

  t2 := new(AnotherT)
  forI(<warning descr="Cannot use t2 (type *AnotherT) as type I">t2</warning>)
}

func forI(I){}
