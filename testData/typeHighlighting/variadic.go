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

  f1(1, <error descr="Cannot use sl (type []int) as type int">sl</error>...)
  f1(<error descr="Cannot use \"s\" (type string) as type int">"s"</error>)
  f1(<error descr="Cannot use sl (type []int) as type int">sl</error>)
  f2(1, <error descr="Cannot use sl (type []int) as type int">sl</error>...)
  f2(1, 2, 3, <error descr="Cannot use sl (type []int) as type int">sl</error>...)

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
