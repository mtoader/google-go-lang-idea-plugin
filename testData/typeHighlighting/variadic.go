package main

type Container []int

func _() {
  var sl []int
  var c Container

  f1(1, 2, 3, 4)
  f1(sl...)
  f1()
  f2(1, 2)
  f2(1, 2, 3)
  f2(1, 2, 3, 4)
  f2(1, 2, sl...)
  f1(c...)

  f1(1, <warning descr="Cannot use sl (type []int) as type int">sl</warning>...)
  f1(<warning descr="Cannot use \"s\" (type string) as type int">"s"</warning>)
  f1(<warning descr="Cannot use sl (type []int) as type int">sl</warning>)
  f2(1, <warning descr="Cannot use sl (type []int) as type int">sl</warning>...)
  f2(1, 2, 3, <warning descr="Cannot use sl (type []int) as type int">sl</warning>...)

}

func f1(slice ... int){
  f2(1, 2, slice...)
}

func f2(int, int, ...int){}


func f3(s string, args ...interface{}) {
  f4(s, args...)
}

func f4(s string, args ...interface{}) {

}
