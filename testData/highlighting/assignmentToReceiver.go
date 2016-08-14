package demo

import "fmt"

type demo struct {
	Val int
}

func (d *demo) change() {
	<weak_warning descr="Assignment to method receiver propagates only to callees but not to callers">d = nil</weak_warning>
	d.myVal()
}

func (d *demo) myVal() {
	fmt.Printf("my val: %#v\n", d)
}

func (d demo) change2() {
	<weak_warning descr="Assignment to method receiver doesn't propagate to other calls">d = demo{}</weak_warning>
	d.myVal()
}

func (d *demo) change3() {
	d.Val = 3
	d.myVal()
}

func _() {
	d := &demo{}
	d.myVal()
	d.change()
	d.myVal()
	d.Val = 2
	d.change2()
	d.myVal()
	d.change3()
	d.myVal()
}

type Flags chan string

func (f Flags) Add(flag string) {
	f <- flag
}

type Flagz []string

func (f *Flagz) Add(flag string) {
	*f = append(*f, flag)
}

func (f *Flagz) Clear() {
	<weak_warning descr="Assignment to method receiver propagates only to callees but not to callers">f</weak_warning> = nil
}

type Flagm map[string]string

func (f Flagm) Add(flag string) {
	f = make(map[string]string)
}
