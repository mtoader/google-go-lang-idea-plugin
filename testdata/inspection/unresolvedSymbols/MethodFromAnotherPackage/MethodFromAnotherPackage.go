package MethodFromAnotherPackage

import (
	"MethodFromAnotherPackage/p1"
)

func test1() {
	t := p1.T1{}
	t.Good2()

	t./*begin*/NotExist1/*end.Unresolved symbol: 'NotExist1'*/()
}
func test2() {
	t := make(p1.T2)
	t.Good3()

	t./*begin*/NotExist2/*end.Unresolved symbol: 'NotExist2'*/()
}

func test3() {
	t := p1.GetT3()
	t.Good4()

	t./*begin*/NotExist3/*end.Unresolved symbol: 'NotExist3'*/()
}
func test4() {
	t := make(p1.T4)
	t.Good5()

	t./*begin*/NotExist4/*end.Unresolved symbol: 'NotExist4'*/()
}
func test5() {
	t := make(p1.T5,33)
	t.Good6()

	t./*begin*/NotExist5/*end.Unresolved symbol: 'NotExist5'*/()
}
func test6(){
	t := p1.GetT6()
	t.Good7()

	t./*begin*/NotExist6/*end.Unresolved symbol: 'NotExist6'*/()

}

func test7(){
	t := p1.GetT7()
	t.Good8()

	t./*begin*/NotExist7/*end.Unresolved symbol: 'NotExist7'*/()
}