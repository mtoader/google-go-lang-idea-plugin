package MethodFromAnotherPackage

import (
	"MethodFromAnotherPackage/p1"
)

func good1() {
	t := p1.T1{}
	t.Good2()

	t./*begin*/NotExist/*end.Unresolved symbol: 'NotExist'*/()
}