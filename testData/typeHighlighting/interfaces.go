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

  T2 struct {
    I
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

  t2:= new(T2)
  forI(t2)

  t3 := new(AnotherT)
  forI(<error descr="Cannot use t3 (type *AnotherT) as type I">t3</error>)

  forI(struct{I}{nil})
}

func forI(I){}
