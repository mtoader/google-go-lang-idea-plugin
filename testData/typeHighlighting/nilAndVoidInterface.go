package main

func _() {
  f1(nil)
  f2(nil, nil, nil)
  f2(nil, nil)
  f2(nil, nil, nil...)
  f2(nil, nil, nil)
  f2(nil, nil, nil, nil)

  f3 (1)
  f3(nil)


  nil := 1

  f1(<warning descr="Cannot use nil (type int) as type string">nil</warning>)
  f2(<warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>)
  f2(<warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>)
  f2(<warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>...)
  f2(<warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>)
  f2(<warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>, <warning descr="Cannot use nil (type int) as type string">nil</warning>)

  f3 (1)
  f3(nil)

}

func f1(string){}
func f2(string, string, ...string){}

func f3(interface{})()