package main

func _(){

  var a int = 3
  var b uint = 4
  var d int = 5

  c:= <warning descr="invalid operation: a + b (mismatched types int and uint)">a + b</warning>
  c = <warning descr="invalid operation: a - b (mismatched types int and uint)">a - b</warning>
  c = <warning descr="invalid operation: a * b (mismatched types int and uint)">a * b</warning>
  c = <warning descr="invalid operation: a / b (mismatched types int and uint)">a / b</warning>
  c = <warning descr="invalid operation: a % b (mismatched types int and uint)">a % b</warning>

  c = <warning descr="invalid operation: a & b (mismatched types int and uint)">a & b</warning>
  c = <warning descr="invalid operation: a | b (mismatched types int and uint)">a | b</warning>
  c = <warning descr="invalid operation: a ^ b (mismatched types int and uint)">a ^ b</warning>
  c = <warning descr="invalid operation: a &^ b (mismatched types int and uint)">a &^ b</warning>

  c = b << <warning descr="invalid operation: a (shift count type int, must be unsigned integer)">a</warning>
  c = b >> <warning descr="invalid operation: a (shift count type int, must be unsigned integer)">a</warning>

  c = a + d
  c = a * d
  c = a % d
  c = a | d

  var q bool
  var w bool

  e := q && w
  e = q || w
  e = !q

  //e = a && a
  e = <warning descr="invalid operation: a || a (operator || not defined on int)">a || a</warning>

}