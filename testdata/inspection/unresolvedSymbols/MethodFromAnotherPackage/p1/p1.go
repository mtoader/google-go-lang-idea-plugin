package p1

type T1 struct{}

func (t T1) Good2() {

}

type T2 chan int

func (t T2) Good3() {

}

func GetT3()T3{
	return T3(nil)
}
type T3 func()

func (t T3) Good4() {

}

type T4 map[string]string

func (t T4) Good5() {

}

type T5 []string

func (t T5) Good6() {

}

func GetT6()T6{
	return T6(0)
}

type T6 int
func (t T6) Good7() {

}

type T7 interface{
	Good8()
}

func GetT7()T7{
	return t8(0)
}

type t8 int
func (t t8) Good8() {

}
