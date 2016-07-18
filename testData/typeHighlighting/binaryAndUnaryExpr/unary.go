package main

func _(){

  var b bool
  var i int

  c := !b
  c = <warning descr="invalid operation: ! int">!i</warning>

  q := &i
  q = &b

  w := <warning descr="invalid operation: + bool">+b</warning>
  w = +i
  w = <warning descr="invalid operation: ^ bool">^b</warning>
  w = ^i

  w = <warning descr="invalid operation: - string">-"string"</warning>

  var ch chan<- int

  w = <warning descr="invalid operation: <-ch (receive from send-only type chan<- int)"><-ch</warning>
  w = <warning descr="invalid operation: <-i (receive from non-chan type int)"><-i</warning>



}